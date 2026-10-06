package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.User
import com.example.data.model.Wallet
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Exceptions personnalisées pour l'authentification avec messages en français.
 */
class AccountSuspendedException(message: String = "Ce compte a été suspendu par l'administrateur.") : Exception(message)
class UsernameAlreadyTakenException(message: String = "Ce nom d'utilisateur est déjà utilisé.") : Exception(message)
class UserNotFoundException(message: String = "Aucun profil utilisateur trouvé pour ce compte.") : Exception(message)

class AuthRepository {

    private val auth get() = FirebaseManager.auth
    private val firestore get() = FirebaseManager.firestore

    /**
     * Vérifie si un nom d'utilisateur est disponible dans Firestore.
     */
    suspend fun isUsernameAvailable(username: String): Boolean {
        val db = firestore ?: return true
        val cleanUsername = username.trim().lowercase()
        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("username", cleanUsername)
                .limit(1)
                .get()
                .await()
            querySnapshot.isEmpty
        } catch (e: Exception) {
            Log.e("AuthRepository", "Erreur lors de la vérification du nom d'utilisateur: ${e.message}")
            true
        }
    }

    /**
     * Inscription d'un nouvel utilisateur avec e-mail et mot de passe.
     * Crée le document users/{uid} et initialise le portefeuille wallets/{uid} à 0 FCFA.
     */
    suspend fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        phone: String,
        password: String
    ): Result<User> {
        val authClient = auth ?: return Result.failure(Exception("Service d'authentification indisponible."))
        val db = firestore ?: return Result.failure(Exception("Base de données Firestore indisponible."))

        val cleanUsername = username.trim().lowercase()
        val cleanEmail = email.trim()

        // 1. Vérification de la disponibilité du nom d'utilisateur
        if (!isUsernameAvailable(cleanUsername)) {
            return Result.failure(UsernameAlreadyTakenException())
        }

        return try {
            // 2. Création du compte Firebase Auth
            val authResult = authClient.createUserWithEmailAndPassword(cleanEmail, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Impossible de créer l'utilisateur."))
            val uid = firebaseUser.uid

            // 3. Envoi de l'e-mail de vérification
            try {
                firebaseUser.sendEmailVerification().await()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Échec de l'envoi de l'e-mail de vérification: ${e.message}")
            }

            // 4. Création de l'objet User
            val newUser = User(
                uid = uid,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                username = cleanUsername,
                email = cleanEmail,
                phone = phone.trim(),
                photoUrl = null,
                bio = "",
                followersCount = 0L,
                followingCount = 0L,
                videosCount = 0L,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                role = "user", // Rôle par défaut strict (jamais admin côté client)
                status = "active",
                emailVerified = firebaseUser.isEmailVerified
            )

            // 5. Enregistrement du profil dans Firestore (users/{uid})
            db.collection("users").document(uid).set(newUser).await()

            // 6. Initialisation du portefeuille dans Firestore (wallets/{uid}) avec 0 FCFA
            val initialWallet = Wallet(
                uid = uid,
                availableBalance = 0L,
                pendingBalance = 0L,
                totalEarned = 0L,
                totalWithdrawn = 0L,
                currency = "FCFA",
                updatedAt = System.currentTimeMillis()
            )
            db.collection("wallets").document(uid).set(initialWallet).await()

            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Connexion d'un utilisateur avec e-mail et mot de passe.
     * Vérifie l'état du compte (bloque l'accès si suspendu).
     */
    suspend fun login(email: String, password: String): Result<User> {
        val authClient = auth ?: return Result.failure(Exception("Service d'authentification indisponible."))
        val db = firestore ?: return Result.failure(Exception("Base de données Firestore indisponible."))

        return try {
            val authResult = authClient.signInWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Connexion échouée."))
            val uid = firebaseUser.uid

            // Récupération du profil dans Firestore
            val userDoc = db.collection("users").document(uid).get().await()
            if (!userDoc.exists()) {
                // Si le document n'existe pas encore, on génère un profil de base
                val fallbackUser = User(
                    uid = uid,
                    email = firebaseUser.email ?: email,
                    emailVerified = firebaseUser.isEmailVerified
                )
                db.collection("users").document(uid).set(fallbackUser).await()
                return Result.success(fallbackUser)
            }

            val user = userDoc.toObject(User::class.java) ?: return Result.failure(UserNotFoundException())

            // Contrôle de sécurité : compte suspendu
            if (user.isSuspended) {
                authClient.signOut()
                return Result.failure(AccountSuspendedException())
            }

            // Mettre à jour l'état de vérification de l'email si changé
            if (user.emailVerified != firebaseUser.isEmailVerified) {
                db.collection("users").document(uid).update("emailVerified", firebaseUser.isEmailVerified)
            }

            Result.success(user.copy(emailVerified = firebaseUser.isEmailVerified))
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Connexion / Inscription via Google Sign-In avec ID Token.
     * Crée automatiquement le profil Firestore et le portefeuille si c'est la première connexion.
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        val authClient = auth ?: return Result.failure(Exception("Service d'authentification indisponible."))
        val db = firestore ?: return Result.failure(Exception("Base de données Firestore indisponible."))

        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = authClient.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Échec de la connexion Google."))
            val uid = firebaseUser.uid

            // Vérification si l'utilisateur a déjà un profil dans Firestore
            val userDoc = db.collection("users").document(uid).get().await()
            if (userDoc.exists()) {
                val existingUser = userDoc.toObject(User::class.java) ?: return Result.failure(UserNotFoundException())
                if (existingUser.isSuspended) {
                    authClient.signOut()
                    return Result.failure(AccountSuspendedException())
                }
                Result.success(existingUser.copy(emailVerified = firebaseUser.isEmailVerified))
            } else {
                // Création automatique du compte Google pour un nouvel utilisateur
                val displayName = firebaseUser.displayName.orEmpty().trim()
                val nameParts = displayName.split(" ")
                val firstName = nameParts.firstOrNull() ?: "Utilisateur"
                val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""
                val cleanEmail = firebaseUser.email.orEmpty().trim()
                val suggestedUsername = cleanEmail.substringBefore("@").replace(".", "_").lowercase()

                val newUser = User(
                    uid = uid,
                    firstName = firstName,
                    lastName = lastName,
                    username = if (suggestedUsername.length >= 3) suggestedUsername else "user_${uid.take(6)}",
                    email = cleanEmail,
                    phone = firebaseUser.phoneNumber.orEmpty(),
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    bio = "Membre VidéoCash connecté avec Google",
                    followersCount = 0L,
                    followingCount = 0L,
                    videosCount = 0L,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    role = User.ROLE_USER,
                    status = User.STATUS_ACTIVE,
                    emailVerified = firebaseUser.isEmailVerified
                )

                // Enregistrement profil et initialisation du portefeuille
                db.collection("users").document(uid).set(newUser).await()

                val initialWallet = Wallet(
                    uid = uid,
                    availableBalance = 0L,
                    pendingBalance = 0L,
                    totalEarned = 0L,
                    totalWithdrawn = 0L,
                    currency = "FCFA",
                    updatedAt = System.currentTimeMillis()
                )
                db.collection("wallets").document(uid).set(initialWallet).await()

                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Réinitialisation de mot de passe par e-mail.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val authClient = auth ?: return Result.failure(Exception("Service d'authentification indisponible."))
        return try {
            authClient.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Renvoie l'e-mail de vérification à l'utilisateur actuellement connecté.
     */
    suspend fun resendEmailVerification(): Result<Unit> {
        val user = auth?.currentUser ?: return Result.failure(Exception("Aucun utilisateur connecté."))
        return try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Déconnexion sécurisée.
     */
    fun logout() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Erreur lors de la déconnexion: ${e.message}")
        }
    }

    /**
     * Suppression définitive du compte utilisateur (Conformité Google Play).
     * Désactive le profil utilisateur, marque le statut Firestore à 'deleted'
     * et supprime le compte Firebase Auth.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val authClient = auth ?: return Result.failure(Exception("Service d'authentification indisponible."))
        val db = firestore ?: return Result.failure(Exception("Base de données Firestore indisponible."))
        val currentUser = authClient.currentUser ?: return Result.failure(Exception("Aucun utilisateur connecté."))
        val uid = currentUser.uid

        return try {
            // 1. Mise à jour de l'état du profil dans Firestore (anonymisation & archivage légal d'audit)
            db.collection("users").document(uid).update(
                mapOf(
                    "status" to "deleted",
                    "bio" to "[Compte supprimé]",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            // 2. Suppression définitive dans Firebase Authentication
            currentUser.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Récupère l'ID de l'utilisateur actuellement connecté.
     */
    fun getCurrentUserId(): String? = auth?.currentUser?.uid

    /**
     * Traduction claire des exceptions Firebase en français.
     */
    private fun mapFirebaseException(e: Exception): Exception {
        return when (e) {
            is FirebaseAuthWeakPasswordException ->
                Exception("Le mot de passe est trop faible. Minimum 6 caractères.")
            is FirebaseAuthInvalidCredentialsException ->
                Exception("Adresse e-mail ou mot de passe incorrect.")
            is FirebaseAuthInvalidUserException ->
                Exception("Aucun compte n'est associé à cette adresse e-mail.")
            is FirebaseAuthUserCollisionException ->
                Exception("Un compte existe déjà avec cette adresse e-mail.")
            is FirebaseNetworkException ->
                Exception("Problème de connexion réseau. Veuillez vérifier votre accès Internet.")
            is AccountSuspendedException -> e
            is UsernameAlreadyTakenException -> e
            else -> Exception(e.localizedMessage ?: "Une erreur inattendue est survenue.")
        }
    }
}
