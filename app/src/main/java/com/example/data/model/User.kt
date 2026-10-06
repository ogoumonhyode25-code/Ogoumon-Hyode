package com.example.data.model

/**
 * Modèle de données pour l'utilisateur VidéoCash.
 * Représente le document stocké dans Firestore sous users/{uid}.
 */
data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val bio: String = "",
    val followersCount: Long = 0,
    val followingCount: Long = 0,
    val videosCount: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val role: String = ROLE_USER, // "user" ou "admin" (défini uniquement côté serveur)
    val status: String = STATUS_ACTIVE, // "active", "suspended", "deleted"
    val emailVerified: Boolean = false
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ADMIN = "admin"

        const val STATUS_ACTIVE = "active"
        const val STATUS_SUSPENDED = "suspended"
        const val STATUS_DELETED = "deleted"
    }

    val fullName: String
        get() = "$firstName $lastName".trim()

    val isSuspended: Boolean
        get() = status.equals(STATUS_SUSPENDED, ignoreCase = true)

    val isAdmin: Boolean
        get() = role.equals(ROLE_ADMIN, ignoreCase = true)
}
