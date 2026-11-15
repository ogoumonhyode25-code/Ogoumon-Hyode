package com.example.data.model

import java.io.File

/**
 * Statut d'un téléchargement mobile.
 */
enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

/**
 * Entité représentant une vidéo téléchargée localement sur l'appareil mobile.
 */
data class DownloadedVideoItem(
    val videoId: String,
    val title: String,
    val username: String,
    val category: String,
    val remoteUrl: String,
    val localFilePath: String,
    val localThumbnailPath: String = "",
    val downloadedAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.COMPLETED
) {
    val isFilePresent: Boolean
        get() = try {
            val f = File(localFilePath)
            f.exists() && f.length() > 0
        } catch (_: Exception) {
            false
        }

    val formattedSize: String
        get() {
            if (fileSizeBytes <= 0) return "Taille inconnue"
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format("%.1f Mo", mb)
            } else {
                val kb = fileSizeBytes / 1024.0
                String.format("%.0f Ko", kb)
            }
        }
}
