package com.example.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.User
import com.example.data.model.Wallet
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Idle : ProfileUiState
    object Loading : ProfileUiState
    data class Success(val message: String) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val currentUid: String?
        get() = authRepository.getCurrentUserId()

    // Profil de l'utilisateur en temps réel
    val userProfile: StateFlow<User?> = if (currentUid != null) {
        userRepository.observeUserProfile(currentUid!!)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        MutableStateFlow(null)
    }

    // Portefeuille de l'utilisateur en temps réel
    val userWallet: StateFlow<Wallet?> = if (currentUid != null) {
        userRepository.observeUserWallet(currentUid!!)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        MutableStateFlow(null)
    }

    fun resetState() {
        _uiState.value = ProfileUiState.Idle
    }

    /**
     * Met à jour les informations du profil utilisateur.
     */
    fun updateProfile(
        firstName: String,
        lastName: String,
        username: String,
        bio: String,
        onSuccess: () -> Unit
    ) {
        val uid = currentUid
        if (uid == null) {
            _uiState.value = ProfileUiState.Error("Session expirée. Veuillez vous reconnecter.")
            return
        }

        if (firstName.isBlank() || lastName.isBlank() || username.isBlank()) {
            _uiState.value = ProfileUiState.Error("Le prénom, le nom et le nom d'utilisateur sont obligatoires.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = userRepository.updateUserProfile(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                username = username,
                bio = bio
            )

            result.fold(
                onSuccess = {
                    _uiState.value = ProfileUiState.Success("Profil mis à jour avec succès !")
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Échec de la mise à jour.")
                }
            )
        }
    }

    /**
     * Téléverse une nouvelle photo de profil et l'applique au compte.
     */
    fun uploadProfilePhoto(imageUri: Uri) {
        val uid = currentUid
        if (uid == null) {
            _uiState.value = ProfileUiState.Error("Session expirée. Veuillez vous reconnecter.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = userRepository.uploadProfilePhoto(uid, imageUri)
            result.fold(
                onSuccess = {
                    _uiState.value = ProfileUiState.Success("Photo de profil mise à jour !")
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Impossible de téléverser la photo.")
                }
            )
        }
    }

    /**
     * Renvoyer l'e-mail de vérification du compte.
     */
    fun resendEmailVerification() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = authRepository.resendEmailVerification()
            result.fold(
                onSuccess = {
                    _uiState.value = ProfileUiState.Success("E-mail de confirmation envoyé ! Vérifiez votre boîte de réception.")
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Impossible d'envoyer l'e-mail.")
                }
            )
        }
    }

    /**
     * Déconnexion sécurisée de l'utilisateur.
     */
     fun logout(onLoggedOut: () -> Unit) {
         authRepository.logout()
         onLoggedOut()
     }

    /**
     * Suppression définitive du compte (politique Google Play).
     */
    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = authRepository.deleteAccount()
            result.fold(
                onSuccess = {
                    _uiState.value = ProfileUiState.Success("Votre compte a été supprimé avec succès.")
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(
                        error.message ?: "Impossible de supprimer le compte. Veuillez vous reconnecter puis réessayer."
                    )
                }
            )
        }
    }
}
