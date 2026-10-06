package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Comment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CommentRepository {

    private val firestore get() = FirebaseManager.firestore

    companion object {
        private const val TAG = "CommentRepository"
    }

    /**
     * Écoute en temps réel les commentaires d'une vidéo (les plus récents en premier).
     */
    fun observeComments(videoId: String): Flow<List<Comment>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("comments")
            .whereEqualTo("videoId", videoId)
            .whereEqualTo("status", Comment.STATUS_ACTIVE)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erreur écoute commentaires: ${error.message}")
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { it.toObject(Comment::class.java) } ?: emptyList()
                trySend(comments)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Ajoute un commentaire et incrémente atomiquement commentsCount sur la vidéo.
     */
    suspend fun addComment(
        videoId: String,
        userId: String,
        username: String,
        userPhotoUrl: String,
        text: String
    ): Result<Comment> {
        val db = firestore ?: return Result.failure(Exception("Firestore non disponible"))
        return try {
            val commentId = UUID.randomUUID().toString()
            val comment = Comment(
                id = commentId,
                videoId = videoId,
                userId = userId,
                username = username,
                userPhotoUrl = userPhotoUrl,
                text = text.trim(),
                createdAt = System.currentTimeMillis(),
                status = Comment.STATUS_ACTIVE
            )

            val commentRef = db.collection("comments").document(commentId)
            val videoRef = db.collection("videos").document(videoId)

            db.runBatch { batch ->
                batch.set(commentRef, comment)
                batch.update(videoRef, "commentsCount", FieldValue.increment(1))
            }.await()

            Result.success(comment)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur addComment: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Supprime un commentaire et décrémente commentsCount sur la vidéo.
     */
    suspend fun deleteComment(commentId: String, videoId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non disponible"))
        return try {
            val commentRef = db.collection("comments").document(commentId)
            val videoRef = db.collection("videos").document(videoId)

            db.runBatch { batch ->
                batch.update(commentRef, "status", Comment.STATUS_DELETED)
                batch.update(videoRef, "commentsCount", FieldValue.increment(-1))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur deleteComment: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Signale un commentaire comme inapproprié.
     */
    suspend fun flagComment(commentId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non disponible"))
        return try {
            db.collection("comments").document(commentId)
                .update("status", Comment.STATUS_FLAGGED)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur flagComment: ${e.message}")
            Result.failure(e)
        }
    }
}
