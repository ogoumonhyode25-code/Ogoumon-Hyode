package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Video

object ShareHelper {

    /**
     * Ouvre la Sharesheet native Android pour partager une vidéo.
     */
    fun shareVideo(context: Context, video: Video) {
        val shareMessage = buildString {
            append("Regarde cette vidéo sur VidéoCash de @${video.username} !\n\n")
            if (video.description.isNotBlank()) {
                append("\"${video.description}\"\n\n")
            }
            if (video.videoUrl.isNotBlank()) {
                append(video.videoUrl)
            } else {
                append("https://videocash.app/v/${video.id}")
            }
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            putExtra(Intent.EXTRA_TITLE, "VidéoCash - @${video.username}")
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Partager la vidéo via")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
