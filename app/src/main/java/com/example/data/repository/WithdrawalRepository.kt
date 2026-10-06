package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Withdrawal
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WithdrawalRepository {
    private val firestore get() = FirebaseManager.firestore
    private val functions get() = FirebaseManager.functions

    /**
     * Écoute en temps réel l'historique des retraits de l'utilisateur.
     */
    fun getWithdrawalsFlow(userId: String): Flow<List<Withdrawal>> = callbackFlow {
        val fs = firestore
        if (fs == null || userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val query: Query = fs.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("WithdrawalRepository", "Erreur écoute retraits: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(Withdrawal::class.java) }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { registration.remove() }
    }

    /**
     * Récupère un retrait par son identifiant unique.
     */
    suspend fun getWithdrawalById(withdrawalId: String): Withdrawal? {
        val fs = firestore ?: return null
        return try {
            val doc = fs.collection("withdrawals").document(withdrawalId).get().await()
            if (doc.exists()) doc.toObject(Withdrawal::class.java) else null
        } catch (e: Exception) {
            Log.e("WithdrawalRepository", "Erreur getWithdrawalById: ${e.message}")
            null
        }
    }

    /**
     * Annule une demande de retrait en attente.
     * En production : appelle Cloud Function "cancelWithdrawal".
     * En simulation : exécute l'annulation atomique via SimulationEngine.
     */
    suspend fun cancelWithdrawal(
        userId: String,
        withdrawalId: String,
        isSimulationMode: Boolean = true
    ): Result<Unit> {
        if (isSimulationMode || functions == null) {
            return SimulationEngine.cancelWithdrawal(userId, withdrawalId)
        }

        return try {
            val data = hashMapOf("withdrawalId" to withdrawalId)
            functions!!.getHttpsCallable("cancelWithdrawal").call(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WithdrawalRepository", "Erreur Cloud Function cancelWithdrawal: ${e.message}")
            SimulationEngine.cancelWithdrawal(userId, withdrawalId)
        }
    }

    /**
     * Simulation d'action administrateur (approbation, refus, paiement) pour les tests.
     */
    suspend fun simulateAdminAction(
        withdrawalId: String,
        newStatus: String,
        adminNote: String = ""
    ): Result<Unit> {
        return SimulationEngine.simulateAdminAction(withdrawalId, newStatus, adminNote)
    }
}
