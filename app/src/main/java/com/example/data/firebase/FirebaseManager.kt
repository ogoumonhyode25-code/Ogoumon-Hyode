package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage

/**
 * Gestionnaire centralisé et sécurisé pour Firebase.
 * Gère l'initialisation sûre et permet de vérifier la présence de la session utilisateur.
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"

    private var isInitialized = false

    fun init(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            isInitialized = true
            Log.d(TAG, "Firebase initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning (google-services.json might be pending): ${e.message}")
        }
    }

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth unavailable: ${e.message}")
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore unavailable: ${e.message}")
            null
        }

    val storage: FirebaseStorage?
        get() = try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseStorage unavailable: ${e.message}")
            null
        }

    val functions: FirebaseFunctions?
        get() = try {
            FirebaseFunctions.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFunctions unavailable: ${e.message}")
            null
        }

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    val isUserLoggedIn: Boolean
        get() = currentUser != null

    fun isConfigured(): Boolean {
        return auth != null
    }
}
