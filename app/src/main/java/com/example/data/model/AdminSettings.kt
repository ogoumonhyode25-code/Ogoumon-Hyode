package com.example.data.model

data class AdminSettings(
    val appName: String = "VidéoCash",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "VidéoCash est temporairement en maintenance. Merci de patienter.",
    val registrationEnabled: Boolean = true,
    val publishingEnabled: Boolean = true,
    val videoUploadEnabled: Boolean = true,
    val rewardEnabled: Boolean = true,
    val withdrawalsEnabled: Boolean = true,
    val minAppVersion: String = "1.0.0",
    val dailyUploadLimit: Int = 10,
    val viewReward: Long = 10L,
    val dailyViewLimit: Int = 50,
    val minimumWatchSeconds: Int = 15,
    val minimumWithdrawal: Long = 1000L,
    val maximumDailyWithdrawal: Long = 100000L,
    val autoApproveWithdrawalUnder: Long = 0L, // 0 = désactivé (revue manuelle stricte)
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "admin"
)
