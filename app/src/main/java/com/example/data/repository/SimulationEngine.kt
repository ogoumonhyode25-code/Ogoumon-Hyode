package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.RewardSettings
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.data.model.Withdrawal
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Moteur de simulation sécurisé pour le développement et les tests.
 * Reproduit EXACTEMENT la logique serveur des Cloud Functions avec des transactions atomiques
 * et prévient tout abus, double dépense ou modification frauduleuse.
 * 
 * IMPORTANT : Ce mode est clairement isolé et identifié "SIMULATION".
 */
object SimulationEngine {
    private const val TAG = "SimulationEngine"

    /**
     * Initialise le portefeuille d'un utilisateur s'il n'existe pas encore.
     */
    suspend fun getOrCreateWallet(userId: String): Wallet {
        val firestore = FirebaseManager.firestore ?: return Wallet(uid = userId)
        val docRef = firestore.collection("wallets").document(userId)

        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(Wallet::class.java) ?: Wallet(uid = userId)
            } else {
                // Création initiale sécurisée avec solde de bienvenue en simulation (ex: 5 000 FCFA pour tester les retraits)
                val initialWallet = Wallet(
                    uid = userId,
                    availableBalance = 5000L,
                    pendingBalance = 0L,
                    totalEarned = 5000L,
                    totalWithdrawn = 0L,
                    currency = "XOF",
                    updatedAt = System.currentTimeMillis()
                )
                docRef.set(initialWallet).await()

                // Transaction d'accueil
                val welcomeTx = Transaction(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = Transaction.TYPE_REWARD,
                    amount = 5000L,
                    currency = "XOF",
                    source = "welcome_bonus_simulation",
                    status = Transaction.STATUS_COMPLETED,
                    description = "Bonus de bienvenue simulation VidéoCash",
                    createdAt = System.currentTimeMillis(),
                    referenceId = "SIM-WELCOME-$userId"
                )
                firestore.collection("transactions").document(welcomeTx.id).set(welcomeTx).await()

                initialWallet
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur getOrCreateWallet: ${e.message}")
            Wallet(uid = userId, availableBalance = 5000L)
        }
    }

    /**
     * Simule l'exécution atomique de processQualifiedView (identique à la Cloud Function).
     */
    suspend fun processQualifiedView(
        userId: String,
        videoId: String,
        watchDurationSeconds: Int,
        settings: RewardSettings
    ): Result<Long> {
        val firestore = FirebaseManager.firestore
            ?: return Result.failure(Exception("Firestore non initialisé"))

        if (watchDurationSeconds < settings.minimumWatchSeconds) {
            return Result.failure(Exception("Durée de visionnage insuffisante (${watchDurationSeconds}s < ${settings.minimumWatchSeconds}s)"))
        }

        if (!settings.rewardEnabled) {
            return Result.failure(Exception("Le programme de récompenses est temporairement désactivé."))
        }

        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val eventId = "${userId}_${videoId}_$todayDate"
        val eventRef = firestore.collection("rewardEvents").document(eventId)
        val walletRef = firestore.collection("wallets").document(userId)

        return try {
            val rewardAmount = settings.viewReward

            firestore.runTransaction { transaction ->
                val eventDoc = transaction.get(eventRef)
                if (eventDoc.exists()) {
                    throw IllegalStateException("Récompense déjà attribuée pour cette vidéo aujourd'hui.")
                }

                val walletDoc = transaction.get(walletRef)
                val currentWallet = if (walletDoc.exists()) {
                    walletDoc.toObject(Wallet::class.java) ?: Wallet(uid = userId)
                } else {
                    Wallet(uid = userId)
                }

                val updatedWallet = currentWallet.copy(
                    availableBalance = currentWallet.availableBalance + rewardAmount,
                    totalEarned = currentWallet.totalEarned + rewardAmount,
                    updatedAt = System.currentTimeMillis()
                )

                // 1. Enregistrer l'événement pour empêcher les doublons
                val eventData = hashMapOf(
                    "id" to eventId,
                    "userId" to userId,
                    "videoId" to videoId,
                    "amount" to rewardAmount,
                    "date" to todayDate,
                    "createdAt" to System.currentTimeMillis()
                )
                transaction.set(eventRef, eventData)

                // 2. Mettre à jour le portefeuille de manière atomique
                transaction.set(walletRef, updatedWallet)

                // 3. Créer la transaction financière
                val txId = UUID.randomUUID().toString()
                val tx = Transaction(
                    id = txId,
                    userId = userId,
                    type = Transaction.TYPE_REWARD,
                    amount = rewardAmount,
                    currency = "XOF",
                    source = "qualified_view",
                    status = Transaction.STATUS_COMPLETED,
                    description = "Récompense pour vue qualifiée",
                    createdAt = System.currentTimeMillis(),
                    referenceId = eventId
                )
                transaction.set(firestore.collection("transactions").document(txId), tx)

                // 4. Créer la notification
                val notifId = UUID.randomUUID().toString()
                val notification = AppNotification(
                    id = notifId,
                    userId = userId,
                    title = "Gain validé !",
                    body = "Félicitations, +${Wallet.formatCurrency(rewardAmount)} ont été crédités sur votre solde pour votre visionnage.",
                    type = AppNotification.TYPE_REWARD_APPROVED,
                    createdAt = System.currentTimeMillis(),
                    referenceId = txId
                )
                transaction.set(firestore.collection("notifications").document(notifId), notification)

                rewardAmount
            }.await()

            Result.success(rewardAmount)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur processQualifiedView: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Simule l'exécution atomique de requestWithdrawal (identique à la Cloud Function).
     * Réserve immédiatement le montant : availableBalance -= amount, pendingBalance += amount.
     */
    suspend fun requestWithdrawal(
        userId: String,
        amount: Long,
        method: String,
        account: String,
        settings: RewardSettings
    ): Result<Withdrawal> {
        val firestore = FirebaseManager.firestore
            ?: return Result.failure(Exception("Firestore non initialisé"))

        if (amount < settings.minimumWithdrawal) {
            return Result.failure(Exception("Montant inférieur au minimum requis de ${Wallet.formatCurrency(settings.minimumWithdrawal)}."))
        }

        if (amount > settings.maximumDailyWithdrawal) {
            return Result.failure(Exception("Montant supérieur au plafond journalier de ${Wallet.formatCurrency(settings.maximumDailyWithdrawal)}."))
        }

        if (account.isBlank()) {
            return Result.failure(Exception("Le numéro ou identifiant de paiement est obligatoire."))
        }

        val walletRef = firestore.collection("wallets").document(userId)
        val withdrawalId = UUID.randomUUID().toString()
        val withdrawalRef = firestore.collection("withdrawals").document(withdrawalId)

        return try {
            val withdrawal = firestore.runTransaction { transaction ->
                val walletDoc = transaction.get(walletRef)
                if (!walletDoc.exists()) {
                    throw IllegalStateException("Portefeuille introuvable.")
                }

                val currentWallet = walletDoc.toObject(Wallet::class.java)!!
                if (currentWallet.availableBalance < amount) {
                    throw IllegalStateException("Solde disponible insuffisant (${Wallet.formatCurrency(currentWallet.availableBalance)} disponibles).")
                }

                // Déduction atomique du solde disponible vers le solde en attente (anti double-dépense)
                val updatedWallet = currentWallet.copy(
                    availableBalance = currentWallet.availableBalance - amount,
                    pendingBalance = currentWallet.pendingBalance + amount,
                    updatedAt = System.currentTimeMillis()
                )
                transaction.set(walletRef, updatedWallet)

                // Création de la demande de retrait
                val newWithdrawal = Withdrawal(
                    id = withdrawalId,
                    userId = userId,
                    amount = amount,
                    currency = "XOF",
                    method = method,
                    account = account,
                    status = Withdrawal.STATUS_PENDING,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    adminNote = "Demande en attente de vérification",
                    providerReference = "SIM-WD-$withdrawalId"
                )
                transaction.set(withdrawalRef, newWithdrawal)

                // Création d'une transaction de type retrait (en attente)
                val txId = UUID.randomUUID().toString()
                val tx = Transaction(
                    id = txId,
                    userId = userId,
                    type = Transaction.TYPE_WITHDRAWAL,
                    amount = amount,
                    currency = "XOF",
                    source = method,
                    status = Transaction.STATUS_PENDING,
                    description = "Demande de retrait vers $method ($account)",
                    createdAt = System.currentTimeMillis(),
                    referenceId = withdrawalId
                )
                transaction.set(firestore.collection("transactions").document(txId), tx)

                // Log d'audit
                val auditId = UUID.randomUUID().toString()
                val audit = AuditLog(
                    id = auditId,
                    actorId = userId,
                    actorRole = "user",
                    action = "withdrawal_requested",
                    targetId = withdrawalId,
                    description = "Demande de retrait de ${Wallet.formatCurrency(amount)} via $method",
                    createdAt = System.currentTimeMillis()
                )
                transaction.set(firestore.collection("auditLogs").document(auditId), audit)

                // Notification
                val notifId = UUID.randomUUID().toString()
                val notif = AppNotification(
                    id = notifId,
                    userId = userId,
                    title = "Demande de retrait reçue",
                    body = "Votre demande de retrait a bien été reçue.",
                    type = AppNotification.TYPE_WITHDRAWAL_CREATED,
                    createdAt = System.currentTimeMillis(),
                    referenceId = withdrawalId
                )
                transaction.set(firestore.collection("notifications").document(notifId), notif)

                newWithdrawal
            }.await()

            Result.success(withdrawal)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur requestWithdrawal: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Annule une demande de retrait encore 'pending' et restitue le montant au solde disponible.
     */
    suspend fun cancelWithdrawal(userId: String, withdrawalId: String): Result<Unit> {
        val firestore = FirebaseManager.firestore
            ?: return Result.failure(Exception("Firestore non initialisé"))

        val withdrawalRef = firestore.collection("withdrawals").document(withdrawalId)
        val walletRef = firestore.collection("wallets").document(userId)

        return try {
            firestore.runTransaction { transaction ->
                val withdrawalDoc = transaction.get(withdrawalRef)
                if (!withdrawalDoc.exists()) {
                    throw IllegalStateException("Demande de retrait introuvable.")
                }

                val withdrawal = withdrawalDoc.toObject(Withdrawal::class.java)!!
                if (withdrawal.userId != userId) {
                    throw IllegalStateException("Non autorisé à annuler cette demande.")
                }

                if (withdrawal.status != Withdrawal.STATUS_PENDING) {
                    throw IllegalStateException("Seules les demandes en attente peuvent être annulées.")
                }

                val walletDoc = transaction.get(walletRef)
                val currentWallet = walletDoc.toObject(Wallet::class.java)!!

                // Restitution atomique du montant : pendingBalance -= amount, availableBalance += amount
                val updatedWallet = currentWallet.copy(
                    availableBalance = currentWallet.availableBalance + withdrawal.amount,
                    pendingBalance = (currentWallet.pendingBalance - withdrawal.amount).coerceAtLeast(0L),
                    updatedAt = System.currentTimeMillis()
                )
                transaction.set(walletRef, updatedWallet)

                // Mise à jour de la demande de retrait
                transaction.update(withdrawalRef, mapOf(
                    "status" to Withdrawal.STATUS_CANCELLED,
                    "updatedAt" to System.currentTimeMillis(),
                    "adminNote" to "Annulé par l'utilisateur"
                ))

                // Transaction de remboursement / restitution
                val txId = UUID.randomUUID().toString()
                val tx = Transaction(
                    id = txId,
                    userId = userId,
                    type = Transaction.TYPE_REFUND,
                    amount = withdrawal.amount,
                    currency = "XOF",
                    source = "withdrawal_cancelled",
                    status = Transaction.STATUS_COMPLETED,
                    description = "Restitution suite à annulation de retrait",
                    createdAt = System.currentTimeMillis(),
                    referenceId = withdrawalId
                )
                transaction.set(firestore.collection("transactions").document(txId), tx)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur cancelWithdrawal: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Action d'administration simulée pour tester l'approbation, le refus ou le paiement.
     */
    suspend fun simulateAdminAction(
        withdrawalId: String,
        newStatus: String,
        adminNote: String = ""
    ): Result<Unit> {
        val firestore = FirebaseManager.firestore
            ?: return Result.failure(Exception("Firestore non initialisé"))

        val withdrawalRef = firestore.collection("withdrawals").document(withdrawalId)

        return try {
            firestore.runTransaction { transaction ->
                val withdrawalDoc = transaction.get(withdrawalRef)
                if (!withdrawalDoc.exists()) {
                    throw IllegalStateException("Retrait introuvable.")
                }
                val withdrawal = withdrawalDoc.toObject(Withdrawal::class.java)!!
                val walletRef = firestore.collection("wallets").document(withdrawal.userId)
                val walletDoc = transaction.get(walletRef)
                val wallet = walletDoc.toObject(Wallet::class.java) ?: Wallet(uid = withdrawal.userId)

                when (newStatus) {
                    Withdrawal.STATUS_PAID -> {
                        // Paiement effectué : pendingBalance -= amount, totalWithdrawn += amount
                        val updatedWallet = wallet.copy(
                            pendingBalance = (wallet.pendingBalance - withdrawal.amount).coerceAtLeast(0L),
                            totalWithdrawn = wallet.totalWithdrawn + withdrawal.amount,
                            updatedAt = System.currentTimeMillis()
                        )
                        transaction.set(walletRef, updatedWallet)
                        transaction.update(withdrawalRef, mapOf(
                            "status" to Withdrawal.STATUS_PAID,
                            "updatedAt" to System.currentTimeMillis(),
                            "processedAt" to System.currentTimeMillis(),
                            "adminNote" to if (adminNote.isNotBlank()) adminNote else "Paiement simulé confirmé avec succès"
                        ))
                        val notifId = UUID.randomUUID().toString()
                        val notif = AppNotification(
                            id = notifId,
                            userId = withdrawal.userId,
                            title = "Paiement effectué",
                            body = "Votre paiement a été effectué.",
                            type = AppNotification.TYPE_WITHDRAWAL_PAID,
                            createdAt = System.currentTimeMillis(),
                            referenceId = withdrawalId
                        )
                        transaction.set(firestore.collection("notifications").document(notifId), notif)
                    }
                    Withdrawal.STATUS_REJECTED -> {
                        // Refus : restitution du montant vers availableBalance
                        val updatedWallet = wallet.copy(
                            availableBalance = wallet.availableBalance + withdrawal.amount,
                            pendingBalance = (wallet.pendingBalance - withdrawal.amount).coerceAtLeast(0L),
                            updatedAt = System.currentTimeMillis()
                        )
                        transaction.set(walletRef, updatedWallet)
                        transaction.update(withdrawalRef, mapOf(
                            "status" to Withdrawal.STATUS_REJECTED,
                            "updatedAt" to System.currentTimeMillis(),
                            "processedAt" to System.currentTimeMillis(),
                            "adminNote" to if (adminNote.isNotBlank()) adminNote else "Demande refusée (simulation)"
                        ))

                        val notifId = UUID.randomUUID().toString()
                        val notif = AppNotification(
                            id = notifId,
                            userId = withdrawal.userId,
                            title = "Retrait refusé",
                            body = "Votre demande de retrait a été refusée.",
                            type = AppNotification.TYPE_WITHDRAWAL_REJECTED,
                            createdAt = System.currentTimeMillis(),
                            referenceId = withdrawalId
                        )
                        transaction.set(firestore.collection("notifications").document(notifId), notif)
                    }
                    Withdrawal.STATUS_APPROVED -> {
                        transaction.update(withdrawalRef, mapOf(
                            "status" to Withdrawal.STATUS_APPROVED,
                            "updatedAt" to System.currentTimeMillis(),
                            "adminNote" to if (adminNote.isNotBlank()) adminNote else "Demande approuvée"
                        ))

                        val notifId = UUID.randomUUID().toString()
                        val notif = AppNotification(
                            id = notifId,
                            userId = withdrawal.userId,
                            title = "Retrait approuvé",
                            body = "Votre demande de retrait a été approuvée.",
                            type = AppNotification.TYPE_WITHDRAWAL_APPROVED,
                            createdAt = System.currentTimeMillis(),
                            referenceId = withdrawalId
                        )
                        transaction.set(firestore.collection("notifications").document(notifId), notif)
                    }
                    else -> {
                        transaction.update(withdrawalRef, mapOf(
                            "status" to newStatus,
                            "updatedAt" to System.currentTimeMillis(),
                            "adminNote" to adminNote
                        ))
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur simulateAdminAction: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Suspension simulée d'un utilisateur
     */
    suspend fun simulateSuspendUser(userId: String, reason: String): Result<Unit> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        return try {
            firestore.collection("users").document(userId).update(
                mapOf(
                    "status" to "suspended",
                    "suspendedReason" to reason,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            // Audit log
            val auditId = UUID.randomUUID().toString()
            val audit = AuditLog(
                id = auditId,
                actorId = FirebaseManager.currentUser?.uid ?: "admin_master",
                actorEmail = FirebaseManager.currentUser?.email ?: "admin@videocash.ci",
                actorRole = "admin",
                action = AuditLog.ACTION_USER_SUSPENDED,
                targetId = userId,
                targetName = "Utilisateur $userId",
                description = "Suspension de compte : $reason",
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("auditLogs").document(auditId).set(audit).await()

            // Notification
            val notifId = UUID.randomUUID().toString()
            val notif = AppNotification(
                id = notifId,
                userId = userId,
                title = "Compte restreint",
                body = "Votre compte VidéoCash a été suspendu pour la raison suivante : $reason",
                type = AppNotification.TYPE_ADMIN,
                createdAt = System.currentTimeMillis(),
                referenceId = userId
            )
            firestore.collection("notifications").document(notifId).set(notif).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur simulateSuspendUser: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Réactivation simulée d'un utilisateur
     */
    suspend fun simulateReactivateUser(userId: String): Result<Unit> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        return try {
            firestore.collection("users").document(userId).update(
                mapOf(
                    "status" to "active",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            val auditId = UUID.randomUUID().toString()
            val audit = AuditLog(
                id = auditId,
                actorId = FirebaseManager.currentUser?.uid ?: "admin_master",
                actorEmail = FirebaseManager.currentUser?.email ?: "admin@videocash.ci",
                actorRole = "admin",
                action = AuditLog.ACTION_USER_REACTIVATED,
                targetId = userId,
                targetName = "Utilisateur $userId",
                description = "Réactivation du compte utilisateur",
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("auditLogs").document(auditId).set(audit).await()

            val notifId = UUID.randomUUID().toString()
            val notif = AppNotification(
                id = notifId,
                userId = userId,
                title = "Compte réactivé",
                body = "Votre compte VidéoCash est de nouveau actif. Vous pouvez reprendre vos activités.",
                type = AppNotification.TYPE_ADMIN,
                createdAt = System.currentTimeMillis(),
                referenceId = userId
            )
            firestore.collection("notifications").document(notifId).set(notif).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur simulateReactivateUser: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Modération de vidéo (masquage/blocage)
     */
    suspend fun simulateModerateVideo(videoId: String, status: String, reason: String): Result<Unit> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        return try {
            val videoDoc = firestore.collection("videos").document(videoId).get().await()
            val creatorId = videoDoc.getString("userId") ?: ""
            val videoDesc = videoDoc.getString("description") ?: videoId

            firestore.collection("videos").document(videoId).update(
                mapOf(
                    "status" to status,
                    "moderationReason" to reason,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            val auditId = UUID.randomUUID().toString()
            val audit = AuditLog(
                id = auditId,
                actorId = FirebaseManager.currentUser?.uid ?: "admin_master",
                actorEmail = FirebaseManager.currentUser?.email ?: "admin@videocash.ci",
                actorRole = "admin",
                action = AuditLog.ACTION_VIDEO_MODERATED,
                targetId = videoId,
                targetName = videoDesc.take(30),
                description = "Statut passé à $status : $reason",
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("auditLogs").document(auditId).set(audit).await()

            if (creatorId.isNotBlank()) {
                val notifId = UUID.randomUUID().toString()
                val notif = AppNotification(
                    id = notifId,
                    userId = creatorId,
                    title = "Vidéo modérée",
                    body = "Votre vidéo a été restreinte par l'administration : $reason",
                    type = AppNotification.TYPE_ADMIN,
                    createdAt = System.currentTimeMillis(),
                    referenceId = videoId
                )
                firestore.collection("notifications").document(notifId).set(notif).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur simulateModerateVideo: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Restauration de vidéo au statut publié
     */
    suspend fun simulateRestoreVideo(videoId: String): Result<Unit> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        return try {
            val videoDoc = firestore.collection("videos").document(videoId).get().await()
            val creatorId = videoDoc.getString("userId") ?: ""
            val videoDesc = videoDoc.getString("description") ?: videoId

            firestore.collection("videos").document(videoId).update(
                mapOf(
                    "status" to "published",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            val auditId = UUID.randomUUID().toString()
            val audit = AuditLog(
                id = auditId,
                actorId = FirebaseManager.currentUser?.uid ?: "admin_master",
                actorEmail = FirebaseManager.currentUser?.email ?: "admin@videocash.ci",
                actorRole = "admin",
                action = AuditLog.ACTION_VIDEO_RESTORED,
                targetId = videoId,
                targetName = videoDesc.take(30),
                description = "Vidéo restaurée au statut publié",
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("auditLogs").document(auditId).set(audit).await()

            if (creatorId.isNotBlank()) {
                val notifId = UUID.randomUUID().toString()
                val notif = AppNotification(
                    id = notifId,
                    userId = creatorId,
                    title = "Vidéo rétablie",
                    body = "Votre vidéo a été réactivée et est à nouveau visible sur le flux.",
                    type = AppNotification.TYPE_ADMIN,
                    createdAt = System.currentTimeMillis(),
                    referenceId = videoId
                )
                firestore.collection("notifications").document(notifId).set(notif).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur simulateRestoreVideo: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Suppression administrative de vidéo
     */
    suspend fun simulateDeleteVideo(videoId: String, reason: String): Result<Unit> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        return try {
            val videoDoc = firestore.collection("videos").document(videoId).get().await()
            val creatorId = videoDoc.getString("userId") ?: ""
            val videoDesc = videoDoc.getString("description") ?: videoId

            firestore.collection("videos").document(videoId).update(
                mapOf(
                    "status" to "deleted",
                    "deletedReason" to reason,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            val auditId = UUID.randomUUID().toString()
            val audit = AuditLog(
                id = auditId,
                actorId = FirebaseManager.currentUser?.uid ?: "admin_master",
                actorEmail = FirebaseManager.currentUser?.email ?: "admin@videocash.ci",
                actorRole = "admin",
                action = AuditLog.ACTION_VIDEO_DELETED,
                targetId = videoId,
                targetName = videoDesc.take(30),
                description = "Suppression définitive : $reason",
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("auditLogs").document(auditId).set(audit).await()

            if (creatorId.isNotBlank()) {
                val notifId = UUID.randomUUID().toString()
                val notif = AppNotification(
                    id = notifId,
                    userId = creatorId,
                    title = "Vidéo supprimée",
                    body = "Votre vidéo a été retirée définitivement de la plateforme : $reason",
                    type = AppNotification.TYPE_ADMIN,
                    createdAt = System.currentTimeMillis(),
                    referenceId = videoId
                )
                firestore.collection("notifications").document(notifId).set(notif).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur simulateDeleteVideo: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Traitement et résolution d'un signalement
     */
    suspend fun simulateResolveReport(reportId: String, status: String, resolution: String): Result<Unit> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        return try {
            val reportDoc = firestore.collection("reports").document(reportId).get().await()
            val targetType = reportDoc.getString("targetType") ?: "UNKNOWN"
            val targetId = reportDoc.getString("targetId") ?: reportId

            firestore.collection("reports").document(reportId).update(
                mapOf(
                    "status" to status,
                    "resolution" to resolution,
                    "resolvedBy" to (FirebaseManager.currentUser?.uid ?: "admin_master"),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            val auditId = UUID.randomUUID().toString()
            val audit = AuditLog(
                id = auditId,
                actorId = FirebaseManager.currentUser?.uid ?: "admin_master",
                actorEmail = FirebaseManager.currentUser?.email ?: "admin@videocash.ci",
                actorRole = "admin",
                action = AuditLog.ACTION_REPORT_RESOLVED,
                targetId = reportId,
                targetName = "$targetType : $targetId",
                description = "Signalement $status : $resolution",
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("auditLogs").document(auditId).set(audit).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur simulateResolveReport: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Ajustement administratif atomique de portefeuille
     */
    suspend fun simulateCreateAdminAdjustment(userId: String, amount: Long, reason: String): Result<Unit> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        return try {
            val walletRef = firestore.collection("wallets").document(userId)
            val txRef = firestore.collection("transactions").document()
            val auditRef = firestore.collection("auditLogs").document()
            val notifRef = firestore.collection("notifications").document()

            firestore.runTransaction { tx ->
                val snap = tx.get(walletRef)
                val currentWallet = if (snap.exists()) {
                    snap.toObject(Wallet::class.java) ?: Wallet(uid = userId)
                } else {
                    Wallet(uid = userId)
                }

                val newBalance = currentWallet.availableBalance + amount
                if (newBalance < 0) {
                    throw IllegalStateException("L'ajustement résulterait en un solde négatif")
                }

                val newTotalEarned = if (amount > 0) currentWallet.totalEarned + amount else currentWallet.totalEarned

                tx.set(walletRef, mapOf(
                    "uid" to userId,
                    "availableBalance" to newBalance,
                    "pendingBalance" to currentWallet.pendingBalance,
                    "totalEarned" to newTotalEarned,
                    "totalWithdrawn" to currentWallet.totalWithdrawn,
                    "currency" to "XOF",
                    "updatedAt" to System.currentTimeMillis()
                ))

                // Transaction
                val transactionObj = Transaction(
                    id = txRef.id,
                    userId = userId,
                    type = Transaction.TYPE_ADJUSTMENT,
                    amount = amount,
                    currency = "XOF",
                    source = "admin_adjustment",
                    status = Transaction.STATUS_COMPLETED,
                    description = "Ajustement administratif : $reason",
                    createdAt = System.currentTimeMillis(),
                    referenceId = FirebaseManager.currentUser?.uid ?: "admin_master"
                )
                tx.set(txRef, transactionObj)

                // Audit log
                val auditObj = AuditLog(
                    id = auditRef.id,
                    actorId = FirebaseManager.currentUser?.uid ?: "admin_master",
                    actorEmail = FirebaseManager.currentUser?.email ?: "admin@videocash.ci",
                    actorRole = "admin",
                    action = AuditLog.ACTION_ADMIN_ADJUSTMENT,
                    targetId = userId,
                    targetName = "Portefeuille $userId",
                    description = "Ajustement de ${if (amount > 0) "+" else ""}$amount FCFA : $reason",
                    createdAt = System.currentTimeMillis()
                )
                tx.set(auditRef, auditObj)

                // Notification
                val notifObj = AppNotification(
                    id = notifRef.id,
                    userId = userId,
                    title = if (amount > 0) "Crédit exceptionnel" else "Ajustement de solde",
                    body = "Votre solde a été ajusté de ${if (amount > 0) "+" else ""}$amount FCFA : $reason",
                    type = AppNotification.TYPE_ADMIN,
                    createdAt = System.currentTimeMillis(),
                    referenceId = txRef.id
                )
                tx.set(notifRef, notifObj)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur simulateCreateAdminAdjustment: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Envoi d'une notification administrateur
     */
    suspend fun simulateSendAdminNotification(
        title: String,
        body: String,
        targetUserId: String?,
        targetAudience: String
    ): Result<Unit> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        return try {
            if (targetAudience == "all") {
                val usersSnap = firestore.collection("users").limit(100).get().await()
                val batch = firestore.batch()
                usersSnap.documents.forEach { doc ->
                    val notifRef = firestore.collection("notifications").document()
                    val notif = AppNotification(
                        id = notifRef.id,
                        userId = doc.id,
                        title = title,
                        body = body,
                        type = AppNotification.TYPE_ADMIN,
                        createdAt = System.currentTimeMillis(),
                        referenceId = "broadcast"
                    )
                    batch.set(notifRef, notif)
                }
                batch.commit().await()
            } else if (!targetUserId.isNullOrBlank()) {
                val notifRef = firestore.collection("notifications").document()
                val notif = AppNotification(
                    id = notifRef.id,
                    userId = targetUserId,
                    title = title,
                    body = body,
                    type = AppNotification.TYPE_ADMIN,
                    createdAt = System.currentTimeMillis(),
                    referenceId = "direct"
                )
                notifRef.set(notif).await()
            }

            // Audit
            val auditId = UUID.randomUUID().toString()
            val audit = AuditLog(
                id = auditId,
                actorId = FirebaseManager.currentUser?.uid ?: "admin_master",
                actorEmail = FirebaseManager.currentUser?.email ?: "admin@videocash.ci",
                actorRole = "admin",
                action = AuditLog.ACTION_ADMIN_NOTIFICATION_SENT,
                targetId = targetUserId ?: "all_users",
                targetName = if (targetAudience == "all") "Tous les utilisateurs" else targetUserId.orEmpty(),
                description = "Diffusion notification : $title",
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("auditLogs").document(auditId).set(audit).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur simulateSendAdminNotification: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Initialisation de données factices complètes pour tester immédiatement toutes les rubriques admin
     */
    suspend fun seedAdminDataIfEmpty() {
        val firestore = FirebaseManager.firestore ?: return
        try {
            val usersCount = firestore.collection("users").limit(1).get().await().size()
            if (usersCount == 0) {
                val batch = firestore.batch()

                // Utilisateur Admin
                val adminId = "admin_master"
                val adminDoc = firestore.collection("users").document(adminId)
                batch.set(adminDoc, mapOf(
                    "uid" to adminId,
                    "firstName" to "Responsable",
                    "lastName" to "VidéoCash",
                    "username" to "admin_videocash",
                    "email" to "admin@videocash.ci",
                    "phone" to "+2250700000001",
                    "role" to "admin",
                    "status" to "active",
                    "videosCount" to 3L,
                    "followersCount" to 1420L,
                    "createdAt" to System.currentTimeMillis() - 86400000L * 60
                ))

                // Utilisateurs types
                val users = listOf(
                    mapOf("uid" to "u1", "firstName" to "Amadou", "lastName" to "Koné", "username" to "amadou_k", "email" to "amadou@gmail.com", "phone" to "+2250707123456", "role" to "user", "status" to "active", "videosCount" to 8L, "followersCount" to 340L),
                    mapOf("uid" to "u2", "firstName" to "Fatou", "lastName" to "Diop", "username" to "fatou_dance", "email" to "fatou@yahoo.fr", "phone" to "+2250505987654", "role" to "user", "status" to "active", "videosCount" to 15L, "followersCount" to 5800L),
                    mapOf("uid" to "u3", "firstName" to "Kouamé", "lastName" to "Brou", "username" to "kouame_humour", "email" to "kouame@gmail.com", "phone" to "+2250101456789", "role" to "user", "status" to "suspended", "videosCount" to 2L, "followersCount" to 45L),
                    mapOf("uid" to "u4", "firstName" to "Aya", "lastName" to "Traoré", "username" to "aya_cuisine", "email" to "aya@orange.ci", "phone" to "+2250708654321", "role" to "user", "status" to "active", "videosCount" to 22L, "followersCount" to 12300L)
                )

                users.forEach { u ->
                    val uid = u["uid"] as String
                    batch.set(firestore.collection("users").document(uid), u + ("createdAt" to (System.currentTimeMillis() - 86400000L * 15)))
                    batch.set(firestore.collection("wallets").document(uid), mapOf(
                        "uid" to uid,
                        "availableBalance" to 12500L,
                        "pendingBalance" to 2000L,
                        "totalEarned" to 35000L,
                        "totalWithdrawn" to 20500L,
                        "currency" to "XOF",
                        "updatedAt" to System.currentTimeMillis()
                    ))
                }

                // Signalements factices
                val reports = listOf(
                    mapOf("id" to "rep_1", "reporterId" to "u1", "reporterUsername" to "amadou_k", "targetType" to "VIDEO", "targetId" to "v101", "targetTitle" to "Blague risquée", "reason" to "INAPPROPRIATE", "description" to "Contenu non conforme pour jeune public", "status" to "pending", "createdAt" to System.currentTimeMillis() - 3600000L * 4),
                    mapOf("id" to "rep_2", "reporterId" to "u2", "reporterUsername" to "fatou_dance", "targetType" to "USER", "targetId" to "u3", "targetTitle" to "kouame_humour", "reason" to "SPAM", "description" to "Messages publicitaires massifs en commentaires", "status" to "reviewing", "createdAt" to System.currentTimeMillis() - 3600000L * 12),
                    mapOf("id" to "rep_3", "reporterId" to "u4", "reporterUsername" to "aya_cuisine", "targetType" to "COMMENT", "targetId" to "c201", "targetTitle" to "Commentaire désobligeant", "reason" to "HARASSMENT", "description" to "Insultes répétées sous la vidéo de recette", "status" to "resolved", "createdAt" to System.currentTimeMillis() - 86400000L * 2, "resolution" to "Commentaire supprimé et avertissement transmis.")
                )
                reports.forEach { rep ->
                    val id = rep["id"] as String
                    batch.set(firestore.collection("reports").document(id), rep)
                }

                // Retraits types
                val withdrawals = listOf(
                    mapOf("id" to "w_101", "userId" to "u1", "amount" to 5000L, "currency" to "XOF", "method" to "Wave", "account" to "+2250707123456", "status" to "pending", "createdAt" to System.currentTimeMillis() - 3600000L * 2),
                    mapOf("id" to "w_102", "userId" to "u2", "amount" to 15000L, "currency" to "XOF", "method" to "Orange Money", "account" to "+2250505987654", "status" to "approved", "createdAt" to System.currentTimeMillis() - 3600000L * 18, "adminNote" to "Compte vérifié"),
                    mapOf("id" to "w_103", "userId" to "u4", "amount" to 20000L, "currency" to "XOF", "method" to "MTN MoMo", "account" to "+2250708654321", "status" to "paid", "createdAt" to System.currentTimeMillis() - 86400000L * 3, "processedAt" to (System.currentTimeMillis() - 86400000L * 2), "providerReference" to "CI-MTN-882391")
                )
                withdrawals.forEach { w ->
                    val id = w["id"] as String
                    batch.set(firestore.collection("withdrawals").document(id), w)
                }

                // Audit logs types
                val auditLogs = listOf(
                    mapOf("id" to "aud_1", "actorId" to "admin_master", "actorEmail" to "admin@videocash.ci", "actorRole" to "admin", "action" to "USER_SUSPENDED", "targetId" to "u3", "targetName" to "kouame_humour", "description" to "Suspension temporaire pour propos inappropriés", "createdAt" to System.currentTimeMillis() - 86400000L * 1),
                    mapOf("id" to "aud_2", "actorId" to "admin_master", "actorEmail" to "admin@videocash.ci", "actorRole" to "admin", "action" to "WITHDRAWAL_APPROVED", "targetId" to "w_102", "targetName" to "w_102", "description" to "Validation retrait 15 000 FCFA vers Orange Money", "createdAt" to System.currentTimeMillis() - 3600000L * 14),
                    mapOf("id" to "aud_3", "actorId" to "admin_master", "actorEmail" to "admin@videocash.ci", "actorRole" to "admin", "action" to "SETTINGS_CHANGED", "targetId" to "settings", "targetName" to "Paramètres récompenses", "description" to "Ajustement du plafond journalier à 100 000 FCFA", "createdAt" to System.currentTimeMillis() - 86400000L * 5)
                )
                auditLogs.forEach { a ->
                    val id = a["id"] as String
                    batch.set(firestore.collection("auditLogs").document(id), a)
                }

                batch.commit().await()
                Log.d(TAG, "seedAdminDataIfEmpty: Données d'administration initialisées avec succès.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "seedAdminDataIfEmpty error: ${e.message}")
        }
    }
}
