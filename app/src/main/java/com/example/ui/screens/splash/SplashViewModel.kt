package com.example.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.repository.AdminRepository
import com.example.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface SplashNavigationState {
    object Loading : SplashNavigationState
    data class Navigate(val destination: String) : SplashNavigationState
}

class SplashViewModel(
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _navigationState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            // Pause d'affichage du logo
            delay(1800)

            // Vérification du mode maintenance
            val adminSettings = adminRepository.getAdminSettings().getOrNull()
            val isMaintenance = adminSettings?.maintenanceMode == true

            val isLoggedIn = FirebaseManager.isUserLoggedIn

            if (isMaintenance) {
                // Si l'utilisateur est admin, il peut accéder malgré la maintenance
                if (isLoggedIn) {
                    val uid = FirebaseManager.currentUser?.uid
                    if (uid != null) {
                        val fs = FirebaseManager.firestore
                        if (fs != null) {
                            try {
                                val userDoc = fs.collection("users").document(uid).get().await()
                                val role = userDoc.getString("role")
                                if (role == "admin") {
                                    _navigationState.value = SplashNavigationState.Navigate(Screen.Main.route)
                                    return@launch
                                }
                            } catch (_: Exception) {
                                // Fallback normal en maintenance
                            }
                        }
                    }
                }
                _navigationState.value = SplashNavigationState.Navigate(Screen.Maintenance.route)
                return@launch
            }

            if (isLoggedIn) {
                _navigationState.value = SplashNavigationState.Navigate(Screen.Main.route)
            } else {
                _navigationState.value = SplashNavigationState.Navigate(Screen.Login.route)
            }
        }
    }
}
