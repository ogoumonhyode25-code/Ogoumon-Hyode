package com.example.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Gestionnaire d'authentification Google moderne utilisant Android Credential Manager.
 */
object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    /**
     * Lance le flux Credential Manager pour obtenir le jeton ID de compte Google
     */
    suspend fun requestGoogleIdToken(
        context: Context,
        onTokenReceived: (idToken: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(context)
        val serverClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            "635489204234-videocash.apps.googleusercontent.com"
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                Log.d(TAG, "Google ID token retrieved successfully.")
                onTokenReceived(idToken)
            } else {
                Log.w(TAG, "Unexpected credential type: ${credential.type}")
                onError("Type d'identifiant Google inattendu.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In canceled by user.")
            // L'utilisateur a annulé sans erreur bloquante
        } catch (e: NoCredentialException) {
            Log.w(TAG, "NoCredentialException: Aucun compte Google enregistré sur l'appareil ou services Google Play non connectés.")
            onError("Aucun compte Google n'est configuré sur cet appareil. Veuillez ajouter un compte Google dans les paramètres ou utiliser la connexion par e-mail.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: ${e.message}", e)
            onError("Connexion Google indisponible : ${e.localizedMessage ?: "Vérifiez vos services Google Play."}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In: ${e.message}", e)
            onError(e.localizedMessage ?: "Impossible d'initier la connexion avec Google.")
        }
    }
}
