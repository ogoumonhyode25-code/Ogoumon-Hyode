package com.example.data.model

data class Follow(
    val id: String = "",
    val followerId: String = "",
    val followingId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
