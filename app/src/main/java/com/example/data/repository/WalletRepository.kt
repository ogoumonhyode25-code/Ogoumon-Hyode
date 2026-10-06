package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.RewardSettings
import com.example.data.model.Wallet
import com.example.data.model.Withdrawal
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WalletRepository {
    private val firestore get() = FirebaseManager.firestore
    private val functions get() = FirebaseManager.functions

    /**
     * Écoute en temps réel le portefeuille de l'utilisateur.
     * Les données viennent directement de Firestore (`wallets/{uid}`).
     */
    fun getWalletFlow(userId: String): Flow<Wallet> = callbackFlow {
        val fs = firestore
        if (fs == null || userId.isBlank()) {
            // Mode hors ligne ou fallback
            trySend(Wallet(uid = userId, availableBalance = 5000L))
            awaitClose { }
            return@callbackFlow
        }

        val docRef = fs.collection("wallets").document(userId)
        val registration: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("WalletRepository", "Erreur écoute wallet: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val wallet = snapshot.toObject(Wallet::class.java)
                if (wallet != null) {
                    trySend(wallet)
                }
            } else {
                // Si le document n'existe pas encore, on l'initialise
                trySend(Wallet(uid = userId))
            }
        }

        awaitClose { registration.remove() }
    }

    /**
     * Récupère les paramètres de récompenses depuis Firestore (`settings/rewards`)
     */
    suspend fun getRewardSettings(): RewardSettings {
        val fs = firestore ?: return RewardSettings()
        return try {
            val doc = fs.collection("settings").document("rewards").get().await()
            if (doc.exists()) {
                doc.toObject(RewardSettings::class.java) ?: RewardSettings()
            } else {
                RewardSettings()
            }
        } catch (e: Exception) {
            Log.w("WalletRepository", "Erreur lecture settings/rewards: ${e.message}")
            RewardSettings()
        }
    }

    /**
     * Demande un retrait sécurisé.
     * En production : appelle la Cloud Function "requestWithdrawal".
     * En simulation / développement : exécute la transaction atomique via SimulationEngine.
     */
    suspend fun requestWithdrawal(
        userId: String,
        amount: Long,
        method: String,
        account: String,
        isSimulationMode: Boolean = true
    ): Result<Withdrawal> {
        val settings = getRewardSettings()

        if (isSimulationMode || functions == null) {
            return SimulationEngine.requestWithdrawal(
                userId = userId,
                amount = amount,
                method = method,
                account = account,
                settings = settings
            )
        }

        // Appel Cloud Function pour la production
        return try {
            val data = hashMapOf(
                "amount" to amount,
                "method" to method,
                "account" to account
            )
            val result = functions!!.getHttpsCallable("requestWithdrawal").call(data).await()
            val resMap = result.data as? Map<*, *>
            val withdrawalId = resMap?.get("id") as? String ?: ""

            Result.success(
                Withdrawal(
                    id = withdrawalId,
                    userId = userId,
                    amount = amount,
                    method = method,
                    account = account,
                    status = Withdrawal.STATUS_PENDING
                )
            )
        } catch (e: Exception) {
            Log.e("WalletRepository", "Erreur Cloud Function requestWithdrawal: ${e.message}")
            // Fallback gracieux en simulation si le backend n'est pas encore déployé
            SimulationEngine.requestWithdrawal(
                userId = userId,
                amount = amount,
                method = method,
                account = account,
                settings = settings
            )
        }
    }

    /**
     * Initialise le portefeuille si besoin
     */
    suspend fun ensureWalletExists(userId: String): Wallet {
        return SimulationEngine.getOrCreateWallet(userId)
    }
}
