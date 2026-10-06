package com.example.data.model

data class AuditLog(
    val id: String = "",
    val actorId: String = "",
    val actorEmail: String = "",
    val actorRole: String = "admin", // "user", "admin", "system"
    val action: String = "",
    val targetId: String = "",
    val targetName: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ACTION_USER_SUSPENDED = "USER_SUSPENDED"
        const val ACTION_USER_REACTIVATED = "USER_REACTIVATED"
        const val ACTION_VIDEO_MODERATED = "VIDEO_MODERATED"
        const val ACTION_VIDEO_RESTORED = "VIDEO_RESTORED"
        const val ACTION_VIDEO_DELETED = "VIDEO_DELETED"
        const val ACTION_WITHDRAWAL_APPROVED = "WITHDRAWAL_APPROVED"
        const val ACTION_WITHDRAWAL_REJECTED = "WITHDRAWAL_REJECTED"
        const val ACTION_WITHDRAWAL_PAID = "WITHDRAWAL_PAID"
        const val ACTION_REWARD_ADJUSTED = "REWARD_ADJUSTED"
        const val ACTION_SETTINGS_CHANGED = "SETTINGS_CHANGED"
        const val ACTION_ADMIN_NOTIFICATION_SENT = "ADMIN_NOTIFICATION_SENT"
        const val ACTION_REPORT_RESOLVED = "REPORT_RESOLVED"
        const val ACTION_ADMIN_ADJUSTMENT = "ADMIN_ADJUSTMENT_CREATED"
    }

    val actionBadge: String
        get() = when (action) {
            ACTION_USER_SUSPENDED -> "Utilisateur suspendu"
            ACTION_USER_REACTIVATED -> "Utilisateur réactivé"
            ACTION_VIDEO_MODERATED -> "Vidéo modérée"
            ACTION_VIDEO_RESTORED -> "Vidéo restaurée"
            ACTION_VIDEO_DELETED -> "Vidéo supprimée"
            ACTION_WITHDRAWAL_APPROVED -> "Retrait approuvé"
            ACTION_WITHDRAWAL_REJECTED -> "Retrait refusé"
            ACTION_WITHDRAWAL_PAID -> "Retrait payé"
            ACTION_REWARD_ADJUSTED -> "Récompense ajustée"
            ACTION_SETTINGS_CHANGED -> "Paramètres modifiés"
            ACTION_ADMIN_NOTIFICATION_SENT -> "Notification envoyée"
            ACTION_REPORT_RESOLVED -> "Signalement résolu"
            ACTION_ADMIN_ADJUSTMENT -> "Ajustement solde"
            else -> action
        }

    val badgeText: String
        get() = actionBadge
}
