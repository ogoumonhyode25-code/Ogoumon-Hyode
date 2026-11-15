package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.data.model.DownloadedVideoItem
import com.example.data.model.DownloadStatus
import com.example.data.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gestionnaire de téléchargements mobiles de VidéoCash.
 * Permet de télécharger une vidéo pour le visionnage hors-ligne,
 * d'enregistrer dans les dossiers publics Téléchargements via DownloadManager
 * ou en stockage d'application persistant, et de lister/supprimer les vidéos téléchargées.
 */
object MobileDownloadManager {
    private const val TAG = "MobileDownloadManager"
    private const val PREFS_NAME = "videocash_downloads_prefs"
    private const val KEY_DOWNLOADED_LIST = "downloaded_videos_list"

    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()

    private val _downloadsList = MutableStateFlow<List<DownloadedVideoItem>>(emptyList())
    val downloadsList: StateFlow<List<DownloadedVideoItem>> = _downloadsList.asStateFlow()

    /**
     * Initialise le gestionnaire et charge l'index local des téléchargements.
     */
    fun init(context: Context) {
        loadPersistedDownloads(context)
    }

    /**
     * Vérifie si une vidéo est déjà téléchargée et disponible localement.
     */
    fun isVideoDownloaded(videoId: String): Boolean {
        val item = _downloadsList.value.find { it.videoId == videoId } ?: return false
        return item.isFilePresent
    }

    /**
     * Obtient le chemin de fichier local si disponible.
     */
    fun getLocalVideoPath(videoId: String): String? {
        val item = _downloadsList.value.find { it.videoId == videoId } ?: return null
        return if (item.isFilePresent) item.localFilePath else null
    }

    /**
     * Lance le téléchargement mobile complet d'une vidéo (fichier MP4).
     */
    suspend fun downloadVideo(
        context: Context,
        video: Video,
        onSuccess: (DownloadedVideoItem) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val videoUrl = video.videoUrl
        if (videoUrl.isBlank()) {
            onError("L'adresse de la vidéo est invalide.")
            return
        }

        // Marquer le début du téléchargement
        updateProgress(video.id, 0.05f)

        withContext(Dispatchers.IO) {
            try {
                // Dossier de destination local dans le stockage privé de l'application
                val downloadsDir = File(context.filesDir, "offline_videos")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val safeId = video.id.ifBlank { "video_${System.currentTimeMillis()}" }
                val targetFile = File(downloadsDir, "${safeId}.mp4")

                // Si le fichier existe déjà, le nettoyer
                if (targetFile.exists()) {
                    targetFile.delete()
                }

                // Téléchargement via flux HTTP avec suivi de la progression
                val url = URL(videoUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    connect()
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    withContext(Dispatchers.Main) {
                        clearProgress(video.id)
                        onError("Erreur réseau ($responseCode) lors du téléchargement.")
                    }
                    return@withContext
                }

                val fileLength = connection.contentLength.toLong()
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (fileLength > 0) {
                        val progress = (totalBytesRead.toFloat() / fileLength.toFloat()).coerceIn(0.1f, 0.98f)
                        updateProgress(video.id, progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                connection.disconnect()

                val actualSize = targetFile.length()
                val downloadedItem = DownloadedVideoItem(
                    videoId = video.id,
                    title = video.description.take(50).ifBlank { "Vidéo #${video.id.take(6)}" },
                    username = video.username.ifBlank { "Créateur" },
                    category = video.category.ifBlank { "Général" },
                    remoteUrl = video.videoUrl,
                    localFilePath = targetFile.absolutePath,
                    localThumbnailPath = video.thumbnailUrl,
                    downloadedAt = System.currentTimeMillis(),
                    fileSizeBytes = actualSize,
                    status = DownloadStatus.COMPLETED
                )

                // Enregistrer dans la liste persistante
                saveDownloadedItem(context, downloadedItem)
                updateProgress(video.id, 1.0f)

                withContext(Dispatchers.Main) {
                    clearProgress(video.id)
                    onSuccess(downloadedItem)
                }

                // Déclencher aussi une sauvegarde dans DownloadManager pour que l'utilisateur puisse le voir dans l'app Fichiers / Téléchargements
                try {
                    enqueueDownloadManagerRequest(context, video)
                } catch (e: Exception) {
                    Log.w(TAG, "DownloadManager notice optional: ${e.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error downloading video: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    clearProgress(video.id)
                    onError("Échec du téléchargement : ${e.localizedMessage ?: "Erreur inconnue"}")
                }
            }
        }
    }

    /**
     * Enqueue un téléchargement système via DownloadManager Android pour la galerie / dossier public.
     */
    private fun enqueueDownloadManagerRequest(context: Context, video: Video) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
        val uri = Uri.parse(video.videoUrl)
        val title = "VideoCash_${video.id.take(8)}.mp4"

        val request = DownloadManager.Request(uri).apply {
            setTitle("VidéoCash - ${video.description.take(30).ifBlank { "Vidéo" }}")
            setDescription("Téléchargement pour visionnage mobile hors-ligne")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        downloadManager.enqueue(request)
    }

    /**
     * Supprime une vidéo téléchargée localement.
     */
    fun deleteDownloadedVideo(context: Context, videoId: String) {
        val current = _downloadsList.value.toMutableList()
        val itemIndex = current.indexOfFirst { it.videoId == videoId }
        if (itemIndex != -1) {
            val item = current[itemIndex]
            try {
                val f = File(item.localFilePath)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete local file: ${e.message}")
            }
            current.removeAt(itemIndex)
            _downloadsList.value = current
            persistDownloads(context, current)
        }
    }

    /**
     * Supprime toutes les vidéos téléchargées pour libérer l'espace disque.
     */
    fun clearAllDownloads(context: Context) {
        _downloadsList.value.forEach { item ->
            try {
                val f = File(item.localFilePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }
        _downloadsList.value = emptyList()
        persistDownloads(context, emptyList())
    }

    private fun updateProgress(videoId: String, progress: Float) {
        val map = _downloadProgressMap.value.toMutableMap()
        map[videoId] = progress
        _downloadProgressMap.value = map
    }

    private fun clearProgress(videoId: String) {
        val map = _downloadProgressMap.value.toMutableMap()
        map.remove(videoId)
        _downloadProgressMap.value = map
    }

    private fun saveDownloadedItem(context: Context, item: DownloadedVideoItem) {
        val current = _downloadsList.value.filter { it.videoId != item.videoId }.toMutableList()
        current.add(0, item)
        _downloadsList.value = current
        persistDownloads(context, current)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun persistDownloads(context: Context, list: List<DownloadedVideoItem>) {
        try {
            val jsonArray = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("videoId", item.videoId)
                    put("title", item.title)
                    put("username", item.username)
                    put("category", item.category)
                    put("remoteUrl", item.remoteUrl)
                    put("localFilePath", item.localFilePath)
                    put("localThumbnailPath", item.localThumbnailPath)
                    put("downloadedAt", item.downloadedAt)
                    put("fileSizeBytes", item.fileSizeBytes)
                    put("status", item.status.name)
                }
                jsonArray.put(obj)
            }
            getPrefs(context).edit().putString(KEY_DOWNLOADED_LIST, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist downloads: ${e.message}")
        }
    }

    private fun loadPersistedDownloads(context: Context) {
        try {
            val raw = getPrefs(context).getString(KEY_DOWNLOADED_LIST, null) ?: return
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<DownloadedVideoItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val localPath = obj.optString("localFilePath")
                val item = DownloadedVideoItem(
                    videoId = obj.optString("videoId"),
                    title = obj.optString("title"),
                    username = obj.optString("username"),
                    category = obj.optString("category"),
                    remoteUrl = obj.optString("remoteUrl"),
                    localFilePath = localPath,
                    localThumbnailPath = obj.optString("localThumbnailPath"),
                    downloadedAt = obj.optLong("downloadedAt", System.currentTimeMillis()),
                    fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                    status = DownloadStatus.valueOf(obj.optString("status", DownloadStatus.COMPLETED.name))
                )
                // Conserver uniquement si le fichier est présent sur disque
                if (item.isFilePresent) {
                    list.add(item)
                }
            }
            _downloadsList.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load persisted downloads: ${e.message}")
        }
    }
}
