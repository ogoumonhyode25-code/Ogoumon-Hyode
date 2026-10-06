package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Follow
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class FollowRepository {

    private val firestore get() = FirebaseManager.firestore

    companion object {
        private const val TAG = "FollowRepository"
    }

    /**
     * Vérifie si followerId suit already followingId.
     */
    suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        if (followerId.isBlank() || followingId.isBlank() || followerId == followingId) return false
        val db = firestore ?: return false
        return try {
            val docId = "${followerId}_$followingId"
            val snapshot = db.collection("follows").document(docId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur isFollowing: ${e.message}")
            false
        }
    }

    /**
     * Bascule le statut de suivi (follow / unfollow) avec mise à jour des compteurs utilisateur.
     */
    suspend fun toggleFollow(followerId: String, followingId: String): Result<Boolean> {
        if (followerId == followingId) {
            return Result.failure(Exception("Vous ne pouvez pas vous suivre vous-même."))
        }
        val db = firestore ?: return Result.failure(Exception("Firestore non disponible"))

        return try {
            val docId = "${followerId}_$followingId"
            val followRef = db.collection("follows").document(docId)
            val followerUserRef = db.collection("users").document(followerId)
            val followingUserRef = db.collection("users").document(followingId)

            val isNowFollowing = db.runTransaction { transaction ->
                val followDoc = transaction.get(followRef)
                if (followDoc.exists()) {
                    // Unfollow
                    transaction.delete(followRef)
                    transaction.update(followerUserRef, "followingCount", FieldValue.increment(-1))
                    transaction.update(followingUserRef, "followersCount", FieldValue.increment(-1))
                    false
                } else {
                    // Follow
                    val follow = Follow(
                        id = docId,
                        followerId = followerId,
                        followingId = followingId,
                        createdAt = System.currentTimeMillis()
                    )
                    transaction.set(followRef, follow)
                    transaction.update(followerUserRef, "followingCount", FieldValue.increment(1))
                    transaction.update(followingUserRef, "followersCount", FieldValue.increment(1))
                    true
                }
            }.await()

            Result.success(isNowFollowing)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur toggleFollow: ${e.message}")
            Result.failure(e)
        }
    }
}
