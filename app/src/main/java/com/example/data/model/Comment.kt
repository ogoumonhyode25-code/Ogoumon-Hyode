package com.example.data.model

data class Comment(
    val id: String = "",
    val videoId: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_ACTIVE
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_FLAGGED = "flagged"
        const val STATUS_DELETED = "deleted"
    }
}
