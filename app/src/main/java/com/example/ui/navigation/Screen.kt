package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinations de navigation principales de l'application VidéoCash.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Main : Screen("main")
    object Home : Screen("home")
    object EditProfile : Screen("edit_profile")
    object AdminDashboard : Screen("admin_dashboard")
    object WithdrawForm : Screen("withdraw_form")
    object Certificates : Screen("certificates")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsOfService : Screen("terms_of_service")
    object HelpSupport : Screen("help_support")
    object DeleteAccount : Screen("delete_account")
    object Maintenance : Screen("maintenance")
    object ContactOwner : Screen("contact_owner")
}

/**
 * Éléments de la barre de navigation inférieure (Bottom Navigation).
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Home : BottomNavItem(
        route = "bottom_home",
        title = "Accueil",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_bottom_home"
    )

    object Discover : BottomNavItem(
        route = "bottom_discover",
        title = "Découvrir",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_bottom_discover"
    )

    object Publish : BottomNavItem(
        route = "bottom_publish",
        title = "Publier",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
        testTag = "nav_bottom_publish"
    )

    object Wallet : BottomNavItem(
        route = "bottom_wallet",
        title = "Portefeuille",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_bottom_wallet"
    )

    object Profile : BottomNavItem(
        route = "bottom_profile",
        title = "Profil",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_bottom_profile"
    )
}
