package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.contact.ContactOwnerScreen
import com.example.ui.screens.legal.PrivacyPolicyScreen
import com.example.ui.screens.legal.TermsScreen
import com.example.ui.screens.main.MainContainerScreen
import com.example.ui.screens.profile.DeleteAccountScreen
import com.example.ui.screens.profile.EditProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.support.HelpSupportScreen

@Composable
fun VideoCashNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    val authViewModel: AuthViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // 1. Écran de démarrage (Splash)
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigate = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Écran de Connexion (Login)
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        // 3. Écran d'Inscription (Register)
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        // 4. Écran Mot de passe oublié (ForgotPassword)
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        // 5. Écran Principal (Accueil, Découvrir, Publier, Portefeuille, Profil)
        composable(Screen.Main.route) {
            MainContainerScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminDashboard.route)
                },
                onNavigateToPrivacy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.TermsOfService.route)
                },
                onNavigateToHelp = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onNavigateToDeleteAccount = {
                    navController.navigate(Screen.DeleteAccount.route)
                },
                onNavigateToContact = {
                    navController.navigate(Screen.ContactOwner.route)
                },
                profileViewModel = profileViewModel
            )
        }

        // 6. Écran de Modification du Profil (EditProfile)
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = profileViewModel
            )
        }

        // 7. Écran Admin Dashboard & Espace d'Administration Sécurisé
        composable(Screen.AdminDashboard.route) {
            com.example.ui.screens.admin.AdminContainerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 8. Politique de Confidentialité (Google Play)
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 9. Conditions Générales d'Utilisation
        composable(Screen.TermsOfService.route) {
            TermsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 10. Aide, FAQ & Support
        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 11. Suppression Définitive du Compte (Conformité Play Console)
        composable(Screen.DeleteAccount.route) {
            DeleteAccountScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAccountDeleted = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = profileViewModel
            )
        }

        // 12. Écran Mode Maintenance
        composable(Screen.Maintenance.route) {
            com.example.ui.screens.maintenance.MaintenanceScreen(
                onRetry = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 13. Page Contact / Propriétaire
        composable(Screen.ContactOwner.route) {
            ContactOwnerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
