package com.example.data.model

data class RewardSettings(
    val viewReward: Long = 10L, // 10 FCFA par vue qualifiée
    val dailyViewLimit: Int = 50, // 50 vidéos max par jour récompensées
    val minimumWatchSeconds: Int = 15, // minimum 15 secondes pour qualifier une vue
    val minimumWithdrawal: Long = 1000L, // 1 000 FCFA minimum pour retirer
    val maximumDailyWithdrawal: Long = 100000L, // 100 000 FCFA max par jour
    val rewardEnabled: Boolean = true
)
