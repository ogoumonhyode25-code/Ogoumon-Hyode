package com.example.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.BottomNavItem
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.publish.PublishVideoScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.wallet.WalletContainerScreen
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary
import com.example.ui.theme.VideoCashPurpleSecondary

@Composable
fun MainContainerScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {},
    onNavigateToContact: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel()
) {
    var currentItem by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    val navItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Discover,
        BottomNavItem.Publish,
        BottomNavItem.Wallet,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D081E),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav")
            ) {
                navItems.forEach { item ->
                    val selected = currentItem == item
                    val isPublish = item == BottomNavItem.Publish

                    if (isPublish) {
                        // Bouton Publier mis en valeur (Bouton central style TikTok / Reels)
                        NavigationBarItem(
                            selected = false,
                            onClick = { currentItem = item },
                            icon = {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = VideoCashPurplePrimary,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag(item.testTag)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    VideoCashPurpleSecondary,
                                                    VideoCashGoldAccent
                                                )
                                            )
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = item.title,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            },
                            label = null,
                            alwaysShowLabel = false
                        )
                    } else {
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentItem = item },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VideoCashGoldAccent,
                                selectedTextColor = VideoCashGoldAccent,
                                unselectedIconColor = Color(0xFF8C82A8),
                                unselectedTextColor = Color(0xFF8C82A8),
                                indicatorColor = Color(0xFF221644)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF090614)),
            contentAlignment = Alignment.Center
        ) {
            when (currentItem) {
                BottomNavItem.Home -> HomeScreen(
                    onNavigateToProfile = { userId ->
                        currentItem = BottomNavItem.Profile
                    },
                    onNavigateToPublish = {
                        currentItem = BottomNavItem.Publish
                    }
                )
                BottomNavItem.Discover -> TabPlaceholderView(
                    title = "Explorer & Recherche",
                    subtitle = "Hashtags tendance, catégories et créateurs populaires",
                    badge = "Explorer"
                )
                BottomNavItem.Publish -> PublishVideoScreen(
                    onPublishSuccess = {
                        currentItem = BottomNavItem.Home
                    }
                )
                BottomNavItem.Wallet -> WalletContainerScreen(
                    onNavigateToHome = { currentItem = BottomNavItem.Home }
                )
                BottomNavItem.Profile -> ProfileScreen(
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onLogoutSuccess = onNavigateToAuth,
                    onNavigateToAdmin = onNavigateToAdmin,
                    onNavigateToPrivacy = onNavigateToPrivacy,
                    onNavigateToTerms = onNavigateToTerms,
                    onNavigateToHelp = onNavigateToHelp,
                    onNavigateToDeleteAccount = onNavigateToDeleteAccount,
                    onNavigateToContact = onNavigateToContact,
                    viewModel = profileViewModel
                )
            }
        }
    }
}

@Composable
fun TabPlaceholderView(
    title: String,
    subtitle: String,
    badge: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF140D2B)
        ),
        modifier = Modifier
            .padding(24.dp)
            .fillMaxSize(fraction = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF281754),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = badge,
                    color = VideoCashGoldAccent,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFAFA7C9)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
