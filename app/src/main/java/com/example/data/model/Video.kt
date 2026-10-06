package com.example.data.model

data class Video(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val description: String = "",
    val hashtags: List<String> = emptyList(),
    val category: String = "Divertissement",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val durationSeconds: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_PUBLISHED
) {
    companion object {
        const val STATUS_PROCESSING = "processing"
        const val STATUS_PUBLISHED = "published"
        const val STATUS_BLOCKED = "blocked"
        const val STATUS_DELETED = "deleted"

        val CATEGORIES = listOf(
            "Divertissement",
            "Musique",
            "Sport",
            "Éducation",
            "Humour",
            "Actualités",
            "Mode",
            "Cuisine",
            "Technologie",
            "Autre"
        )
    }

    val formattedViews: String
        get() = formatCount(viewsCount)

    val formattedLikes: String
        get() = formatCount(likesCount)

    val formattedComments: String
        get() = formatCount(commentsCount)

    private fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
}
