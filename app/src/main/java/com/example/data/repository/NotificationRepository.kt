package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.AppNotification
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NotificationRepository {
    private val firestore get() = FirebaseManager.firestore

    /**
     * Écoute en direct les notifications destinées à l'utilisateur connecté.
     */
    fun getNotificationsFlow(userId: String): Flow<List<AppNotification>> = callbackFlow {
        val fs = firestore
        if (fs == null || userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val query = fs.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("NotificationRepo", "Erreur écoute notifications: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(AppNotification::class.java) }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { registration.remove() }
    }

    /**
     * Marque une notification spécifique comme lue.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        val fs = firestore ?: return Result.failure(Exception("Firestore indisponible"))
        return try {
            fs.collection("notifications").document(notificationId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Erreur markAsRead: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Marque toutes les notifications non lues de l'utilisateur comme lues.
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> {
        val fs = firestore ?: return Result.failure(Exception("Firestore indisponible"))
        return try {
            val unreadDocs = fs.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()

            val batch = fs.batch()
            for (doc in unreadDocs.documents) {
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Erreur markAllAsRead: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Supprime une notification.
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val fs = firestore ?: return Result.failure(Exception("Firestore indisponible"))
        return try {
            fs.collection("notifications").document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Erreur deleteNotification: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Création d'une notification de test en mode simulation.
     */
    suspend fun createSimulatedNotification(
        userId: String,
        title: String,
        body: String,
        type: String,
        referenceId: String = ""
    ): Result<Unit> {
        val fs = firestore ?: return Result.failure(Exception("Firestore indisponible"))
        val notifId = UUID.randomUUID().toString()
        val notif = AppNotification(
            id = notifId,
            userId = userId,
            title = title,
            body = body,
            type = type,
            read = false,
            createdAt = System.currentTimeMillis(),
            referenceId = referenceId
        )

        return try {
            fs.collection("notifications").document(notifId).set(notif).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Erreur createSimulatedNotification: ${e.message}")
            Result.failure(e)
        }
    }
}
