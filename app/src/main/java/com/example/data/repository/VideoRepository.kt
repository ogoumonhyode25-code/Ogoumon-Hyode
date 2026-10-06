package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Video
import com.example.util.MediaHelper
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VideoRepository {

    private val firestore get() = FirebaseManager.firestore
    private val storage get() = FirebaseManager.storage

    companion object {
        private const val TAG = "VideoRepository"
        const val PAGE_SIZE = 15L
    }

    /**
     * Récupère une liste paginée de vidéos publiées.
     */
    suspend fun getFeedVideos(
        category: String? = null,
        lastVisible: DocumentSnapshot? = null
    ): Result<Pair<List<Video>, DocumentSnapshot?>> {
        val db = firestore ?: return Result.failure(Exception("Service Firestore indisponible"))

        return try {
            var query = db.collection("videos")
                .whereEqualTo("status", Video.STATUS_PUBLISHED)

            if (!category.isNullOrBlank() && category != "Tous") {
                query = query.whereEqualTo("category", category)
            }

            query = query.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)

            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            val videos = snapshot.documents.mapNotNull { it.toObject(Video::class.java) }
            val newLastVisible = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null

            Result.success(Pair(videos, newLastVisible))
        } catch (e: Exception) {
            Log.e(TAG, "Erreur getFeedVideos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Écoute en temps réel les vidéos récentes publiées.
     */
    fun observeRecentVideos(category: String? = null): Flow<List<Video>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var query = db.collection("videos")
            .whereEqualTo("status", Video.STATUS_PUBLISHED)

        if (!category.isNullOrBlank() && category != "Tous") {
            query = query.whereEqualTo("category", category)
        }

        query = query.orderBy("createdAt", Query.Direction.DESCENDING).limit(PAGE_SIZE)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erreur écoute vidéos: ${error.message}")
                return@addSnapshotListener
            }
            val videos = snapshot?.documents?.mapNotNull { it.toObject(Video::class.java) } ?: emptyList()
            trySend(videos)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Incrémente le compteur de vues d'une vidéo (uniquement après seuil).
     */
    suspend fun incrementViewCount(videoId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore indisponible"))
        return try {
            db.collection("videos").document(videoId)
                .update("viewsCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur incrementViewCount: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Téléverse une vidéo et sa miniature vers Firebase Storage, puis crée le document dans Firestore.
     */
    suspend fun publishVideo(
        context: Context,
        userId: String,
        username: String,
        userPhotoUrl: String,
        videoUri: Uri,
        customThumbnailUri: Uri?,
        description: String,
        hashtags: List<String>,
        category: String,
        onProgress: (Int) -> Unit
    ): Result<Video> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        val st = storage ?: return Result.failure(Exception("Storage non initialisé"))

        return try {
            val videoId = UUID.randomUUID().toString()
            val videoRef = st.reference.child("videos/$userId/$videoId/video.mp4")
            val thumbnailRef = st.reference.child("videos/$userId/$videoId/thumbnail.jpg")

            // 1. Upload de la vidéo avec écoute de la progression (70% du total)
            onProgress(5)
            val videoUploadTask = videoRef.putFile(videoUri)

            videoUploadTask.addOnProgressListener { taskSnapshot ->
                val progress = if (taskSnapshot.totalByteCount > 0) {
                    (taskSnapshot.bytesTransferred * 70 / taskSnapshot.totalByteCount).toInt()
                } else 0
                onProgress(5 + progress)
            }

            videoUploadTask.await()
            val videoDownloadUrl = videoRef.downloadUrl.await().toString()
            onProgress(75)

            // 2. Traitement et upload de la miniature (20% du total)
            var thumbnailDownloadUrl = ""
            if (customThumbnailUri != null) {
                thumbnailRef.putFile(customThumbnailUri).await()
                thumbnailDownloadUrl = thumbnailRef.downloadUrl.await().toString()
            } else {
                val extractedBitmap = MediaHelper.extractVideoThumbnail(context, videoUri)
                if (extractedBitmap != null) {
                    val bytes = MediaHelper.bitmapToJpegBytes(extractedBitmap)
                    thumbnailRef.putBytes(bytes).await()
                    thumbnailDownloadUrl = thumbnailRef.downloadUrl.await().toString()
                }
            }
            onProgress(95)

            // 3. Enregistrement dans Firestore
            val now = System.currentTimeMillis()
            val video = Video(
                id = videoId,
                userId = userId,
                username = username,
                userPhotoUrl = userPhotoUrl,
                videoUrl = videoDownloadUrl,
                thumbnailUrl = thumbnailDownloadUrl,
                description = description.trim(),
                hashtags = hashtags,
                category = category,
                likesCount = 0L,
                commentsCount = 0L,
                viewsCount = 0L,
                createdAt = now,
                updatedAt = now,
                status = Video.STATUS_PUBLISHED
            )

            db.collection("videos").document(videoId).set(video).await()

            // Mettre à jour le profil de l'utilisateur pour incrémenter vidéos publiées
            db.collection("users").document(userId)
                .update("videosCount", FieldValue.increment(1))

            onProgress(100)
            Result.success(video)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur publication vidéo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Supprime une vidéo.
     */
    suspend fun deleteVideo(videoId: String, userId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore indisponible"))
        val st = storage ?: return Result.failure(Exception("Storage indisponible"))

        return try {
            // Marquer comme supprimée dans Firestore
            db.collection("videos").document(videoId)
                .update("status", Video.STATUS_DELETED)
                .await()

            // Décrémenter le compteur de vidéos de l'utilisateur
            db.collection("users").document(userId)
                .update("videosCount", FieldValue.increment(-1))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur suppression vidéo: ${e.message}")
            Result.failure(e)
        }
    }
}
