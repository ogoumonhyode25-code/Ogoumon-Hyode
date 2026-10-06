package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object MediaHelper {

    private const val TAG = "MediaHelper"

    // Limite maximale de taille de vidéo : 100 Mo
    const val MAX_VIDEO_SIZE_BYTES = 100L * 1024L * 1024L // 100 Mo

    // Seuil de vue (3 secondes minimum pour enregistrer une vue)
    const val VIEW_THRESHOLD_SECONDS = 3L

    /**
     * Récupère la taille d'un fichier Uri en octets.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lecture taille: ${e.message}")
            0L
        }
    }

    /**
     * Extrait une miniature (Bitmap) à partir d'un URI vidéo local.
     */
    fun extractVideoThumbnail(context: Context, videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            // Extraire la première frame à 1 seconde (1 000 000 microsecondes)
            retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
        } catch (e: Exception) {
            Log.e(TAG, "Erreur extraction miniature: ${e.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Convertit un Bitmap en tableau d'octets JPEG compressé.
     */
    fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
