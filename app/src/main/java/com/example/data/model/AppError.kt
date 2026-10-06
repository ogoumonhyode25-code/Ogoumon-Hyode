package com.example.data.model

sealed class AppError(val code: String, val userMessage: String) : Exception(userMessage) {
    object AuthRequired : AppError("AUTH_REQUIRED", "Connexion requise pour effectuer cette action.")
    object PermissionDenied : AppError("PERMISSION_DENIED", "Vous n'avez pas les droits nécessaires.")
    object InvalidAmount : AppError("INVALID_AMOUNT", "Le montant spécifié est invalide ou inférieur au minimum autorisé.")
    object InsufficientBalance : AppError("INSUFFICIENT_BALANCE", "Solde disponible insuffisant pour cette opération.")
    object WithdrawalExists : AppError("WITHDRAWAL_EXISTS", "Une demande de retrait identique ou en cours est déjà en traitement.")
    object AccountSuspended : AppError("ACCOUNT_SUSPENDED", "Ce compte est temporairement restreint ou suspendu.")
    object VideoNotFound : AppError("VIDEO_NOT_FOUND", "La vidéo demandée est introuvable ou a été retirée.")
    object RewardNotAllowed : AppError("REWARD_NOT_ALLOWED", "Cette vue n'est pas éligible à une rémunération.")
    object RateLimited : AppError("RATE_LIMITED", "Trop de requêtes successives. Veuillez patienter un instant.")
    data class Unknown(val detail: String) : AppError("UNKNOWN", detail.ifBlank { "Une erreur inattendue est survenue." })

    companion object {
        fun fromCode(code: String, fallbackMessage: String = ""): AppError {
            return when (code.uppercase()) {
                "AUTH_REQUIRED" -> AuthRequired
                "PERMISSION_DENIED" -> PermissionDenied
                "INVALID_AMOUNT" -> InvalidAmount
                "INSUFFICIENT_BALANCE" -> InsufficientBalance
                "WITHDRAWAL_EXISTS" -> WithdrawalExists
                "ACCOUNT_SUSPENDED" -> AccountSuspended
                "VIDEO_NOT_FOUND" -> VideoNotFound
                "REWARD_NOT_ALLOWED" -> RewardNotAllowed
                "RATE_LIMITED" -> RateLimited
                else -> Unknown(fallbackMessage)
            }
        }
    }
}
