package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.RewardSettings
import com.example.data.model.Transaction
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RewardRepository(
    private val walletRepository: WalletRepository = WalletRepository()
) {
    private val firestore get() = FirebaseManager.firestore
    private val functions get() = FirebaseManager.functions

    /**
     * Enregistre une vue qualifiée côté serveur pour calculer et créditer la récompense.
     * Le client n'attribue JAMAIS d'argent directement :
     * 1. Il transmet l'événement au serveur (Cloud Function ou SimulationEngine).
     * 2. Le serveur valide les critères (authentifié, durée minimale, limites journalières, non-duplication).
     * 3. Le serveur effectue la transaction atomique.
     */
    suspend fun recordQualifiedView(
        userId: String,
        videoId: String,
        watchDurationSeconds: Int,
        isSimulationMode: Boolean = true
    ): Result<Long> {
        val settings = walletRepository.getRewardSettings()

        if (watchDurationSeconds < settings.minimumWatchSeconds) {
            return Result.failure(Exception("Vue non qualifiée : durée de visionnage trop courte (${watchDurationSeconds}s)."))
        }

        if (isSimulationMode || functions == null) {
            return SimulationEngine.processQualifiedView(
                userId = userId,
                videoId = videoId,
                watchDurationSeconds = watchDurationSeconds,
                settings = settings
            )
        }

        return try {
            val data = hashMapOf(
                "videoId" to videoId,
                "watchDuration" to watchDurationSeconds
            )
            val result = functions!!.getHttpsCallable("processQualifiedView").call(data).await()
            val resMap = result.data as? Map<*, *>
            val rewardAmount = (resMap?.get("rewardAmount") as? Number)?.toLong() ?: settings.viewReward

            Result.success(rewardAmount)
        } catch (e: Exception) {
            Log.e("RewardRepository", "Erreur Cloud Function processQualifiedView: ${e.message}")
            // Fallback en simulation sécurisée
            SimulationEngine.processQualifiedView(
                userId = userId,
                videoId = videoId,
                watchDurationSeconds = watchDurationSeconds,
                settings = settings
            )
        }
    }

    /**
     * Écoute l'historique des récompenses obtenues par l'utilisateur (transactions de type "reward").
     */
    fun getRewardHistoryFlow(userId: String): Flow<List<Transaction>> = callbackFlow {
        val fs = firestore
        if (fs == null || userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val query = fs.collection("transactions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", Transaction.TYPE_REWARD)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("RewardRepository", "Erreur écoute récompenses: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { registration.remove() }
    }
}
