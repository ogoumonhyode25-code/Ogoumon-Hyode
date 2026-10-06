package com.example.data.model

data class UserDevice(
    val deviceId: String = "",
    val userId: String = "",
    val fcmToken: String = "",
    val platform: String = "android",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val active: Boolean = true
)
