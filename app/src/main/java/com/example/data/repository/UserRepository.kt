package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.User
import com.example.data.model.Wallet
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore get() = FirebaseManager.firestore
    private val storage get() = FirebaseManager.storage

    /**
     * Récupère le profil utilisateur une fois depuis Firestore.
     */
    suspend fun getUserProfile(uid: String): Result<User> {
        val db = firestore ?: return Result.failure(Exception("Firestore indisponible."))
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Utilisateur non trouvé."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Écoute en temps réel les changements du profil utilisateur.
     */
    fun observeUserProfile(uid: String): Flow<User?> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Erreur écoute profil: ${error.message}")
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Met à jour les informations du profil utilisateur.
     * Note: L'email et le rôle ne peuvent pas être modifiés ici pour des raisons de sécurité.
     */
    suspend fun updateUserProfile(
        uid: String,
        firstName: String,
        lastName: String,
        username: String,
        bio: String
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore indisponible."))
        val cleanUsername = username.trim().lowercase()

        return try {
            // Vérifier si le nouveau nom d'utilisateur n'appartient pas déjà à quelqu'un d'autre
            val query = db.collection("users")
                .whereEqualTo("username", cleanUsername)
                .limit(2)
                .get()
                .await()

            val isTakenByOther = query.documents.any { it.id != uid }
            if (isTakenByOther) {
                return Result.failure(Exception("Ce nom d'utilisateur est déjà pris."))
            }

            val updates = mapOf(
                "firstName" to firstName.trim(),
                "lastName" to lastName.trim(),
                "username" to cleanUsername,
                "bio" to bio.trim(),
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Téléverse une nouvelle photo de profil dans Firebase Storage (profile_photos/{uid}.jpg)
     * et met à jour l'URL dans le document utilisateur Firestore.
     */
    suspend fun uploadProfilePhoto(uid: String, imageUri: Uri): Result<String> {
        val storageClient = storage ?: return Result.failure(Exception("Firebase Storage indisponible."))
        val db = firestore ?: return Result.failure(Exception("Firestore indisponible."))

        return try {
            val photoRef = storageClient.reference.child("profile_photos/$uid.jpg")
            photoRef.putFile(imageUri).await()
            val downloadUrl = photoRef.downloadUrl.await().toString()

            // Mise à jour de l'URL dans Firestore
            db.collection("users").document(uid).update(
                mapOf(
                    "photoUrl" to downloadUrl,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Écoute en temps réel le solde et le statut du portefeuille de l'utilisateur.
     */
    fun observeUserWallet(uid: String): Flow<Wallet?> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("wallets").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Erreur écoute portefeuille: ${error.message}")
                    return@addSnapshotListener
                }
                val wallet = snapshot?.toObject(Wallet::class.java)
                trySend(wallet)
            }

        awaitClose { listener.remove() }
    }
}
