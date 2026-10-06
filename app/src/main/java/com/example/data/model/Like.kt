package com.example.data.model

data class Like(
    val id: String = "",
    val videoId: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
