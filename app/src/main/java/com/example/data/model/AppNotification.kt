package com.example.data.model

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = TYPE_SYSTEM,
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val referenceId: String = ""
) {
    companion object {
        const val TYPE_WITHDRAWAL_CREATED = "WITHDRAWAL_CREATED"
        const val TYPE_WITHDRAWAL_APPROVED = "WITHDRAWAL_APPROVED"
        const val TYPE_WITHDRAWAL_REJECTED = "WITHDRAWAL_REJECTED"
        const val TYPE_WITHDRAWAL_PAID = "WITHDRAWAL_PAID"
        const val TYPE_REWARD_APPROVED = "REWARD_APPROVED"
        const val TYPE_NEW_COMMENT = "NEW_COMMENT"
        const val TYPE_NEW_LIKE = "NEW_LIKE"
        const val TYPE_SYSTEM = "SYSTEM"
        const val TYPE_ADMIN = "ADMIN"
    }

    val isFinancial: Boolean
        get() = type in listOf(
            TYPE_WITHDRAWAL_CREATED,
            TYPE_WITHDRAWAL_APPROVED,
            TYPE_WITHDRAWAL_REJECTED,
            TYPE_WITHDRAWAL_PAID,
            TYPE_REWARD_APPROVED
        )

    val badgeLabel: String
        get() = when (type) {
            TYPE_WITHDRAWAL_CREATED -> "Retrait reçu"
            TYPE_WITHDRAWAL_APPROVED -> "Retrait approuvé"
            TYPE_WITHDRAWAL_REJECTED -> "Retrait refusé"
            TYPE_WITHDRAWAL_PAID -> "Paiement envoyé"
            TYPE_REWARD_APPROVED -> "Gain validé"
            TYPE_NEW_COMMENT -> "Commentaire"
            TYPE_NEW_LIKE -> "J'aime"
            TYPE_ADMIN -> "Administration"
            else -> "Système"
        }
}
