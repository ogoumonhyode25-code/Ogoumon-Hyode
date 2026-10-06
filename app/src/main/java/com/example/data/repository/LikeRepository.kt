package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Like
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class LikeRepository {

    private val firestore get() = FirebaseManager.firestore

    companion object {
        private const val TAG = "LikeRepository"
    }

    /**
     * Vérifie si un utilisateur a aimé une vidéo.
     */
    suspend fun isVideoLiked(videoId: String, userId: String): Boolean {
        val db = firestore ?: return false
        return try {
            val docId = "${videoId}_$userId"
            val snapshot = db.collection("likes").document(docId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur isVideoLiked: ${e.message}")
            false
        }
    }

    /**
     * Bascule l'état de like avec une transaction atomique.
     * @return Le nouvel état (true si aimé, false si retiré)
     */
    suspend fun toggleLike(videoId: String, userId: String): Result<Boolean> {
        val db = firestore ?: return Result.failure(Exception("Firestore non disponible"))
        return try {
            val docId = "${videoId}_$userId"
            val likeRef = db.collection("likes").document(docId)
            val videoRef = db.collection("videos").document(videoId)

            val isNowLiked = db.runTransaction { transaction ->
                val likeDoc = transaction.get(likeRef)
                val videoDoc = transaction.get(videoRef)

                if (likeDoc.exists()) {
                    // Retirer le like
                    transaction.delete(likeRef)
                    val currentLikes = videoDoc.getLong("likesCount") ?: 1L
                    val newCount = (currentLikes - 1L).coerceAtLeast(0L)
                    transaction.update(videoRef, "likesCount", newCount)
                    false
                } else {
                    // Ajouter le like
                    val like = Like(
                        id = docId,
                        videoId = videoId,
                        userId = userId,
                        createdAt = System.currentTimeMillis()
                    )
                    transaction.set(likeRef, like)
                    transaction.update(videoRef, "likesCount", FieldValue.increment(1))
                    true
                }
            }.await()

            Result.success(isNowLiked)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur toggleLike: ${e.message}")
            Result.failure(e)
        }
    }
}
