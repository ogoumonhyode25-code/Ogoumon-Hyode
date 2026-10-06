package com.example.data.model

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reporterUsername: String = "",
    val targetType: String = TYPE_VIDEO, // VIDEO, COMMENT, USER
    val targetId: String = "",
    val targetTitle: String = "",
    val reason: String = REASON_SPAM, // SPAM, SCAM, INAPPROPRIATE, VIOLENCE, COPYRIGHT, HARASSMENT, OTHER
    val description: String = "",
    val status: String = STATUS_PENDING, // pending, reviewing, resolved, rejected
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val resolvedBy: String = "",
    val resolution: String = ""
) {
    companion object {
        const val TYPE_VIDEO = "VIDEO"
        const val TYPE_COMMENT = "COMMENT"
        const val TYPE_USER = "USER"

        const val REASON_SPAM = "SPAM"
        const val REASON_SCAM = "SCAM"
        const val REASON_INAPPROPRIATE = "INAPPROPRIATE"
        const val REASON_VIOLENCE = "VIOLENCE"
        const val REASON_COPYRIGHT = "COPYRIGHT"
        const val REASON_HARASSMENT = "HARASSMENT"
        const val REASON_OTHER = "OTHER"

        const val STATUS_PENDING = "pending"
        const val STATUS_REVIEWING = "reviewing"
        const val STATUS_RESOLVED = "resolved"
        const val STATUS_REJECTED = "rejected"

        val ALL_REASONS = listOf(
            REASON_SPAM,
            REASON_SCAM,
            REASON_INAPPROPRIATE,
            REASON_VIOLENCE,
            REASON_COPYRIGHT,
            REASON_HARASSMENT,
            REASON_OTHER
        )

        val ALL_TYPES = listOf(
            TYPE_VIDEO,
            TYPE_COMMENT,
            TYPE_USER
        )
    }

    val reasonLabel: String
        get() = when (reason) {
            REASON_SPAM -> "Spam ou publicité abusive"
            REASON_SCAM -> "Arnaque ou fraude financière"
            REASON_INAPPROPRIATE -> "Contenu inapproprié"
            REASON_VIOLENCE -> "Violence ou incitation"
            REASON_COPYRIGHT -> "Violation de droits d'auteur"
            REASON_HARASSMENT -> "Harcèlement ou diffamation"
            else -> "Autre infraction"
        }

    val statusLabel: String
        get() = when (status) {
            STATUS_PENDING -> "En attente"
            STATUS_REVIEWING -> "En cours d'examen"
            STATUS_RESOLVED -> "Résolu"
            STATUS_REJECTED -> "Rejeté"
            else -> status
        }
}
