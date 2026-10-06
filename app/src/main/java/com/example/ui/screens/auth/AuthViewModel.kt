package com.example.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val message: String, val user: User? = null) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

data class RegisterFormState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptTerms: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val usernameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()

    // --- FORM ACTIONS ---
    fun updateFirstName(value: String) {
        _registerForm.update { it.copy(firstName = value, firstNameError = null) }
    }

    fun updateLastName(value: String) {
        _registerForm.update { it.copy(lastName = value, lastNameError = null) }
    }

    fun updateUsername(value: String) {
        _registerForm.update { it.copy(username = value.trim(), usernameError = null) }
    }

    fun updateEmail(value: String) {
        _registerForm.update { it.copy(email = value.trim(), emailError = null) }
    }

    fun updatePhone(value: String) {
        _registerForm.update { it.copy(phone = value.trim(), phoneError = null) }
    }

    fun updatePassword(value: String) {
        _registerForm.update { it.copy(password = value, passwordError = null) }
    }

    fun updateConfirmPassword(value: String) {
        _registerForm.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun toggleTerms(accepted: Boolean) {
        _registerForm.update { it.copy(acceptTerms = accepted, termsError = null) }
    }

    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }

    fun setError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    // --- VALIDATION & REGISTER ---
    fun register() {
        val form = _registerForm.value

        var hasError = false
        var firstNameErr: String? = null
        var lastNameErr: String? = null
        var usernameErr: String? = null
        var emailErr: String? = null
        var phoneErr: String? = null
        var passwordErr: String? = null
        var confirmPasswordErr: String? = null
        var termsErr: String? = null

        if (form.firstName.isBlank()) {
            firstNameErr = "Le prénom est requis"
            hasError = true
        }

        if (form.lastName.isBlank()) {
            lastNameErr = "Le nom est requis"
            hasError = true
        }

        if (form.username.isBlank()) {
            usernameErr = "Le nom d'utilisateur est requis"
            hasError = true
        } else if (form.username.length < 3) {
            usernameErr = "Au moins 3 caractères requis"
            hasError = true
        } else if (!form.username.matches(Regex("^[a-zA-Z0-9_.]+$"))) {
            usernameErr = "Lettres, chiffres, points et tirets bas uniquement"
            hasError = true
        }

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (form.email.isBlank()) {
            emailErr = "L'adresse e-mail est requise"
            hasError = true
        } else if (!form.email.matches(emailRegex)) {
            emailErr = "Format d'e-mail invalide"
            hasError = true
        }

        if (form.phone.isBlank()) {
            phoneErr = "Le numéro de téléphone est requis"
            hasError = true
        } else if (form.phone.length < 8) {
            phoneErr = "Numéro de téléphone incomplet"
            hasError = true
        }

        if (form.password.isBlank()) {
            passwordErr = "Le mot de passe est requis"
            hasError = true
        } else if (form.password.length < 6) {
            passwordErr = "Minimum 6 caractères requis"
            hasError = true
        }

        if (form.confirmPassword != form.password) {
            confirmPasswordErr = "Les mots de passe ne correspondent pas"
            hasError = true
        }

        if (!form.acceptTerms) {
            termsErr = "Veuillez accepter les conditions d'utilisation"
            hasError = true
        }

        if (hasError) {
            _registerForm.update {
                it.copy(
                    firstNameError = firstNameErr,
                    lastNameError = lastNameErr,
                    usernameError = usernameErr,
                    emailError = emailErr,
                    phoneError = phoneErr,
                    passwordError = passwordErr,
                    confirmPasswordError = confirmPasswordErr,
                    termsError = termsErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.register(
                firstName = form.firstName,
                lastName = form.lastName,
                username = form.username,
                email = form.email,
                phone = form.phone,
                password = form.password
            )

            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(
                        message = "Compte créé avec succès ! Un e-mail de confirmation vous a été envoyé.",
                        user = user
                    )
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Échec de l'inscription.")
                }
            )
        }
    }

    // --- LOGIN ---
    fun login(email: String, password: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            _uiState.value = AuthUiState.Error("Veuillez saisir votre adresse e-mail.")
            return
        }
        if (password.isBlank()) {
            _uiState.value = AuthUiState.Error("Veuillez saisir votre mot de passe.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(cleanEmail, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(
                        message = "Connexion réussie.",
                        user = user
                    )
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Échec de la connexion.")
                }
            )
        }
    }

    // --- GOOGLE SIGN-IN ---
    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = AuthUiState.Error("Jeton d'authentification Google manquant.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(
                        message = "Connexion avec Google réussie.",
                        user = user
                    )
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Échec de la connexion avec Google.")
                }
            )
        }
    }

    // --- FORGOT PASSWORD ---
    fun sendPasswordReset(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _uiState.value = AuthUiState.Error("Veuillez entrer une adresse e-mail valide.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.sendPasswordReset(cleanEmail)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Success(
                        message = "Un lien de réinitialisation vous a été envoyé par e-mail."
                    )
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Impossible d'envoyer l'e-mail.")
                }
            )
        }
    }

    // --- LOGOUT ---
    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState.Idle
    }
}
