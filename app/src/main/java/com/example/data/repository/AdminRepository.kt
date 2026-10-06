package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.AdminSettings
import com.example.data.model.AuditLog
import com.example.data.model.Report
import com.example.data.model.RewardSettings
import com.example.data.model.Transaction
import com.example.data.model.User
import com.example.data.model.Video
import com.example.data.model.Withdrawal
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AdminDashboardStats(
    val totalUsers: Long = 0L,
    val activeUsers: Long = 0L,
    val suspendedUsers: Long = 0L,
    val totalVideos: Long = 0L,
    val publishedVideos: Long = 0L,
    val pendingVideos: Long = 0L,
    val deletedVideos: Long = 0L,
    val pendingWithdrawalsCount: Long = 0L,
    val pendingWithdrawalsAmount: Long = 0L,
    val totalRewardsDistributed: Long = 0L,
    val totalReportsCount: Long = 0L,
    val pendingReportsCount: Long = 0L,
    // Statistiques périodiques (vues, récompenses, etc.)
    val views7Days: Long = 0L,
    val rewards7Days: Long = 0L,
    val withdrawals7Days: Long = 0L,
    val views30Days: Long = 0L,
    val rewards30Days: Long = 0L,
    val withdrawals30Days: Long = 0L
)

class AdminRepository {
    private val TAG = "AdminRepository"
    private val firestore get() = FirebaseManager.firestore
    private val functions get() = FirebaseManager.functions

    /**
     * Initialise les données de démonstration si les collections sont vides
     */
    suspend fun seedIfEmpty() {
        SimulationEngine.seedAdminDataIfEmpty()
    }

    /**
     * Écoute en direct les statistiques du tableau de bord
     */
    fun getDashboardStatsFlow(): Flow<AdminDashboardStats> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(AdminDashboardStats())
            awaitClose { }
            return@callbackFlow
        }

        val usersReg = fs.collection("users").addSnapshotListener { usersSnap, _ ->
            val users = usersSnap?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()

            fs.collection("videos").get().addOnSuccessListener { videosSnap ->
                val videos = videosSnap.documents.mapNotNull { it.toObject(Video::class.java) }

                fs.collection("withdrawals").get().addOnSuccessListener { withSnap ->
                    val withdrawals = withSnap.documents.mapNotNull { it.toObject(Withdrawal::class.java) }

                    fs.collection("reports").get().addOnSuccessListener { repSnap ->
                        val reports = repSnap.documents.mapNotNull { it.toObject(Report::class.java) }

                        fs.collection("transactions").get().addOnSuccessListener { txSnap ->
                            val txs = txSnap.documents.mapNotNull { it.toObject(Transaction::class.java) }

                            val now = System.currentTimeMillis()
                            val sevenDaysAgo = now - 7 * 86400000L
                            val thirtyDaysAgo = now - 30 * 86400000L

                            val pendingW = withdrawals.filter { it.status == Withdrawal.STATUS_PENDING }
                            val totalRewards = txs.filter { it.type == Transaction.TYPE_REWARD }.sumOf { it.amount }

                            val txs7 = txs.filter { it.createdAt >= sevenDaysAgo }
                            val txs30 = txs.filter { it.createdAt >= thirtyDaysAgo }

                            val stats = AdminDashboardStats(
                                totalUsers = users.size.toLong(),
                                activeUsers = users.count { it.status == "active" }.toLong(),
                                suspendedUsers = users.count { it.status == "suspended" }.toLong(),
                                totalVideos = videos.size.toLong(),
                                publishedVideos = videos.count { it.status == Video.STATUS_PUBLISHED }.toLong(),
                                pendingVideos = videos.count { it.status == Video.STATUS_PROCESSING }.toLong(),
                                deletedVideos = videos.count { it.status == Video.STATUS_DELETED || it.status == Video.STATUS_BLOCKED }.toLong(),
                                pendingWithdrawalsCount = pendingW.size.toLong(),
                                pendingWithdrawalsAmount = pendingW.sumOf { it.amount },
                                totalRewardsDistributed = totalRewards,
                                totalReportsCount = reports.size.toLong(),
                                pendingReportsCount = reports.count { it.status == Report.STATUS_PENDING }.toLong(),
                                views7Days = videos.filter { it.createdAt >= sevenDaysAgo }.sumOf { it.viewsCount },
                                rewards7Days = txs7.filter { it.type == Transaction.TYPE_REWARD }.sumOf { it.amount },
                                withdrawals7Days = withdrawals.filter { it.createdAt >= sevenDaysAgo && it.status == Withdrawal.STATUS_PAID }.sumOf { it.amount },
                                views30Days = videos.filter { it.createdAt >= thirtyDaysAgo }.sumOf { it.viewsCount },
                                rewards30Days = txs30.filter { it.type == Transaction.TYPE_REWARD }.sumOf { it.amount },
                                withdrawals30Days = withdrawals.filter { it.createdAt >= thirtyDaysAgo && it.status == Withdrawal.STATUS_PAID }.sumOf { it.amount }
                            )
                            trySend(stats)
                        }
                    }
                }
            }
        }

        awaitClose {
            usersReg.remove()
        }
    }

    /**
     * Écoute en direct tous les utilisateurs avec filtres et recherche
     */
    fun getUsersFlow(): Flow<List<User>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val reg = fs.collection("users")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erreur getUsersFlow: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    /**
     * Récupère un utilisateur spécifique
     */
    suspend fun getUserById(userId: String): User? {
        val fs = firestore ?: return null
        return try {
            val doc = fs.collection("users").document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Récupère les vidéos publiées par un utilisateur spécifique
     */
    suspend fun getUserVideos(userId: String): List<Video> {
        val fs = firestore ?: return emptyList()
        return try {
            val snap = fs.collection("videos").whereEqualTo("userId", userId).get().await()
            snap.documents.mapNotNull { it.toObject(Video::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Récupère les transactions d'un utilisateur spécifique
     */
    suspend fun getUserTransactions(userId: String): List<Transaction> {
        val fs = firestore ?: return emptyList()
        return try {
            val snap = fs.collection("transactions").whereEqualTo("userId", userId).get().await()
            snap.documents.mapNotNull { it.toObject(Transaction::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Récupère les retraits d'un utilisateur spécifique
     */
    suspend fun getUserWithdrawals(userId: String): List<Withdrawal> {
        val fs = firestore ?: return emptyList()
        return try {
            val snap = fs.collection("withdrawals").whereEqualTo("userId", userId).get().await()
            snap.documents.mapNotNull { it.toObject(Withdrawal::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Suspend un utilisateur
     */
    suspend fun suspendUser(userId: String, reason: String): Result<Unit> {
        val fn = functions
        return if (fn != null) {
            try {
                fn.getHttpsCallable("suspendUser")
                    .call(mapOf("userId" to userId, "reason" to reason))
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback simulateSuspendUser: ${e.message}")
                SimulationEngine.simulateSuspendUser(userId, reason)
            }
        } else {
            SimulationEngine.simulateSuspendUser(userId, reason)
        }
    }

    /**
     * Réactive un utilisateur
     */
    suspend fun reactivateUser(userId: String): Result<Unit> {
        val fn = functions
        return if (fn != null) {
            try {
                fn.getHttpsCallable("reactivateUser")
                    .call(mapOf("userId" to userId))
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback simulateReactivateUser: ${e.message}")
                SimulationEngine.simulateReactivateUser(userId)
            }
        } else {
            SimulationEngine.simulateReactivateUser(userId)
        }
    }

    /**
     * Écoute en direct toutes les vidéos pour la modération
     */
    fun getVideosFlow(): Flow<List<Video>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val reg = fs.collection("videos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erreur getVideosFlow: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Video::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    /**
     * Modère une vidéo (masquer ou bloquer)
     */
    suspend fun moderateVideo(videoId: String, status: String, reason: String): Result<Unit> {
        val fn = functions
        return if (fn != null) {
            try {
                fn.getHttpsCallable("moderateVideo")
                    .call(mapOf("videoId" to videoId, "status" to status, "reason" to reason))
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback simulateModerateVideo: ${e.message}")
                SimulationEngine.simulateModerateVideo(videoId, status, reason)
            }
        } else {
            SimulationEngine.simulateModerateVideo(videoId, status, reason)
        }
    }

    /**
     * Restaure une vidéo au statut publié
     */
    suspend fun restoreVideo(videoId: String): Result<Unit> {
        val fn = functions
        return if (fn != null) {
            try {
                fn.getHttpsCallable("restoreVideo")
                    .call(mapOf("videoId" to videoId))
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback simulateRestoreVideo: ${e.message}")
                SimulationEngine.simulateRestoreVideo(videoId)
            }
        } else {
            SimulationEngine.simulateRestoreVideo(videoId)
        }
    }

    /**
     * Supprime définitivement une vidéo
     */
    suspend fun deleteVideo(videoId: String, reason: String): Result<Unit> {
        val fn = functions
        return if (fn != null) {
            try {
                fn.getHttpsCallable("deleteVideo")
                    .call(mapOf("videoId" to videoId, "reason" to reason))
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback simulateDeleteVideo: ${e.message}")
                SimulationEngine.simulateDeleteVideo(videoId, reason)
            }
        } else {
            SimulationEngine.simulateDeleteVideo(videoId, reason)
        }
    }

    /**
     * Écoute en direct tous les signalements
     */
    fun getReportsFlow(): Flow<List<Report>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val reg = fs.collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erreur getReportsFlow: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Report::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    /**
     * Résout ou rejette un signalement
     */
    suspend fun resolveReport(reportId: String, status: String, resolution: String): Result<Unit> {
        val fn = functions
        return if (fn != null) {
            try {
                fn.getHttpsCallable("resolveReport")
                    .call(mapOf("reportId" to reportId, "status" to status, "resolution" to resolution))
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback simulateResolveReport: ${e.message}")
                SimulationEngine.simulateResolveReport(reportId, status, resolution)
            }
        } else {
            SimulationEngine.simulateResolveReport(reportId, status, resolution)
        }
    }

    /**
     * Écoute tous les retraits de la plateforme
     */
    fun getAllWithdrawalsFlow(): Flow<List<Withdrawal>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val reg = fs.collection("withdrawals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erreur getAllWithdrawalsFlow: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Withdrawal::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    /**
     * Transition de statut de retrait via Cloud Function ou Simulation
     */
    suspend fun updateWithdrawalStatus(
        withdrawalId: String,
        newStatus: String,
        adminNote: String = "",
        providerReference: String = ""
    ): Result<Unit> {
        val fn = functions
        if (fn != null) {
            try {
                when (newStatus) {
                    Withdrawal.STATUS_APPROVED -> {
                        fn.getHttpsCallable("approveWithdrawal")
                            .call(mapOf("withdrawalId" to withdrawalId, "note" to adminNote))
                            .await()
                    }
                    Withdrawal.STATUS_REJECTED -> {
                        fn.getHttpsCallable("rejectWithdrawal")
                            .call(mapOf("withdrawalId" to withdrawalId, "reason" to adminNote))
                            .await()
                    }
                    Withdrawal.STATUS_PAID -> {
                        fn.getHttpsCallable("completeWithdrawal")
                            .call(mapOf("withdrawalId" to withdrawalId, "providerReference" to providerReference))
                            .await()
                    }
                    else -> {
                        SimulationEngine.simulateAdminAction(withdrawalId, newStatus, adminNote)
                    }
                }
                return Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback simulateAdminAction: ${e.message}")
            }
        }
        return SimulationEngine.simulateAdminAction(withdrawalId, newStatus, adminNote)
    }

    /**
     * Écoute en direct les transactions de l'ensemble de la plateforme
     */
    fun getAllTransactionsFlow(): Flow<List<Transaction>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val reg = fs.collection("transactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erreur getAllTransactionsFlow: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Transaction::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    /**
     * Création d'un ajustement financier administratif
     */
    suspend fun createAdminAdjustment(userId: String, amount: Long, reason: String): Result<Unit> {
        val fn = functions
        return if (fn != null) {
            try {
                fn.getHttpsCallable("createAdminAdjustment")
                    .call(mapOf("userId" to userId, "amount" to amount, "reason" to reason))
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback simulateCreateAdminAdjustment: ${e.message}")
                SimulationEngine.simulateCreateAdminAdjustment(userId, amount, reason)
            }
        } else {
            SimulationEngine.simulateCreateAdminAdjustment(userId, amount, reason)
        }
    }

    /**
     * Écoute en direct des paramètres de récompense
     */
    fun getRewardSettingsFlow(): Flow<RewardSettings> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(RewardSettings())
            awaitClose { }
            return@callbackFlow
        }

        val reg = fs.collection("settings").document("rewards")
            .addSnapshotListener { snapshot, _ ->
                val settings = snapshot?.toObject(RewardSettings::class.java) ?: RewardSettings()
                trySend(settings)
            }

        awaitClose { reg.remove() }
    }

    /**
     * Écoute en direct des paramètres d'administration généraux
     */
    fun getAdminSettingsFlow(): Flow<AdminSettings> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(AdminSettings())
            awaitClose { }
            return@callbackFlow
        }

        val reg = fs.collection("settings").document("global")
            .addSnapshotListener { snapshot, _ ->
                val settings = snapshot?.toObject(AdminSettings::class.java) ?: AdminSettings()
                trySend(settings)
            }

        awaitClose { reg.remove() }
    }

    /**
     * Récupère de manière ponctuelle les paramètres d'administration généraux (pour le check splash / maintenance)
     */
    suspend fun getAdminSettings(): Result<AdminSettings> {
        val fs = firestore ?: return Result.success(AdminSettings())
        return try {
            val snapshot = fs.collection("settings").document("global").get().await()
            val settings = snapshot.toObject(AdminSettings::class.java) ?: AdminSettings()
            Result.success(settings)
        } catch (e: Exception) {
            Log.w(TAG, "Erreur getAdminSettings: ${e.message}")
            Result.success(AdminSettings())
        }
    }

    /**
     * Met à jour les paramètres administratifs et de récompenses
     */
    suspend fun updateSettings(adminSettings: AdminSettings): Result<Unit> {
        val fs = firestore ?: return Result.failure(Exception("Firestore indisponible"))
        return try {
            val batch = fs.batch()
            batch.set(fs.collection("settings").document("global"), adminSettings)
            batch.set(
                fs.collection("settings").document("rewards"),
                RewardSettings(
                    viewReward = adminSettings.viewReward,
                    dailyViewLimit = adminSettings.dailyViewLimit,
                    minimumWatchSeconds = adminSettings.minimumWatchSeconds,
                    minimumWithdrawal = adminSettings.minimumWithdrawal,
                    maximumDailyWithdrawal = adminSettings.maximumDailyWithdrawal,
                    rewardEnabled = adminSettings.rewardEnabled
                )
            )

            val auditRef = fs.collection("auditLogs").document()
            val audit = AuditLog(
                id = auditRef.id,
                actorId = FirebaseManager.currentUser?.uid ?: "admin_master",
                actorEmail = FirebaseManager.currentUser?.email ?: "admin@videocash.ci",
                actorRole = "admin",
                action = AuditLog.ACTION_SETTINGS_CHANGED,
                targetId = "settings",
                targetName = "Configuration système",
                description = "Mise à jour des règles globales et des récompenses.",
                createdAt = System.currentTimeMillis()
            )
            batch.set(auditRef, audit)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur updateSettings: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Écoute en direct des journaux d'audit (Audit Logs)
     */
    fun getAuditLogsFlow(): Flow<List<AuditLog>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val reg = fs.collection("auditLogs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erreur getAuditLogsFlow: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(AuditLog::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    /**
     * Envoie une notification administrative
     */
    suspend fun sendAdminNotification(
        title: String,
        body: String,
        targetUserId: String?,
        targetAudience: String
    ): Result<Unit> {
        val fn = functions
        return if (fn != null) {
            try {
                fn.getHttpsCallable("sendAdminNotification")
                    .call(
                        mapOf(
                            "title" to title,
                            "body" to body,
                            "targetUserId" to targetUserId,
                            "targetAudience" to targetAudience
                        )
                    )
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback simulateSendAdminNotification: ${e.message}")
                SimulationEngine.simulateSendAdminNotification(title, body, targetUserId, targetAudience)
            }
        } else {
            SimulationEngine.simulateSendAdminNotification(title, body, targetUserId, targetAudience)
        }
    }
}
