package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Transaction
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TransactionRepository {
    private val firestore get() = FirebaseManager.firestore

    /**
     * Écoute en temps réel les transactions de l'utilisateur ordonnées par date antéchronologique.
     */
    fun getTransactionsFlow(userId: String, filterType: String? = null): Flow<List<Transaction>> = callbackFlow {
        val fs = firestore
        if (fs == null || userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var query: Query = fs.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (!filterType.isNullOrBlank() && filterType != "all") {
            query = query.whereEqualTo("type", filterType)
        }

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("TransactionRepository", "Erreur écoute transactions: ${error.message}")
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
