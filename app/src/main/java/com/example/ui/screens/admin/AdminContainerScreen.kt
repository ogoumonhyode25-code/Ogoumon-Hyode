package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.VideoCashBackground
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary

enum class AdminSection(val title: String, val icon: ImageVector) {
    DASHBOARD("Tableau de bord", Icons.Default.Dashboard),
    USERS("Utilisateurs", Icons.Default.People),
    VIDEOS("Vidéos", Icons.Default.VideoLibrary),
    WITHDRAWALS("Retraits", Icons.Default.AccountBalanceWallet),
    REPORTS("Signalements", Icons.Default.Report),
    REWARDS("Récompenses", Icons.Default.MonetizationOn),
    TRANSACTIONS("Transactions", Icons.Default.ReceiptLong),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    STATISTICS("Statistiques", Icons.Default.BarChart),
    SETTINGS("Paramètres", Icons.Default.Settings),
    AUDIT_LOGS("Audit Logs", Icons.Default.Security)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContainerScreen(
    onNavigateBack: () -> Unit,
    adminViewModel: AdminViewModel = viewModel()
) {
    val isAdmin by adminViewModel.isAdmin.collectAsStateWithLifecycle()
    val isCheckingAdmin by adminViewModel.isCheckingAdmin.collectAsStateWithLifecycle()
    val message by adminViewModel.message.collectAsStateWithLifecycle()
    val isLoadingAction by adminViewModel.isLoadingAction.collectAsStateWithLifecycle()

    var currentSection by remember { mutableStateOf(AdminSection.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            adminViewModel.clearMessage()
        }
    }

    // Écran de chargement lors de la vérification de sécurité
    if (isCheckingAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VideoCashBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = VideoCashPurplePrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Vérification des privilèges administrateur...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    // Écran Accès Refusé si l'utilisateur n'est pas Admin
    if (!isAdmin) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Espace d'Administration", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VideoCashBackground)
                )
            },
            containerColor = VideoCashBackground
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF191136)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3F1322)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Accès Restreint",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        Text(
                            text = "Cette section est strictement réservée aux responsables de VidéoCash autorisés via Firebase Custom Claims.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFCBD5E1),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )

                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = VideoCashPurplePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retour à l'application")
                        }
                    }
                }
            }
        }
        return
    }

    // Interface Administrateur Complète
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF0F0926))) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Admin VidéoCash",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF3F1322),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E))
                            ) {
                                Text(
                                    text = "ROOT",
                                    color = Color(0xFFFDA4AF),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (isLoadingAction) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 12.dp),
                                color = VideoCashEmerald,
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F0926),
                        titleContentColor = Color.White
                    )
                )

                // Barre d'onglets de navigation entre sections
                ScrollableTabRow(
                    selectedTabIndex = currentSection.ordinal,
                    containerColor = Color(0xFF0F0926),
                    contentColor = VideoCashPurplePrimary,
                    edgePadding = 12.dp
                ) {
                    AdminSection.values().forEach { section ->
                        val selected = currentSection == section
                        Tab(
                            selected = selected,
                            onClick = { currentSection = section },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selected) VideoCashGoldAccent else Color(0xFF9E95B8)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = section.title,
                                        color = if (selected) Color.White else Color(0xFF9E95B8),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        containerColor = VideoCashBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentSection) {
                AdminSection.DASHBOARD -> AdminDashboardScreen(
                    viewModel = adminViewModel,
                    onNavigateToSection = { section -> currentSection = section }
                )
                AdminSection.USERS -> AdminUsersScreen(viewModel = adminViewModel)
                AdminSection.VIDEOS -> AdminVideosScreen(
                    viewModel = adminViewModel,
                    onViewReportsForVideo = {
                        adminViewModel.reportFilterType.value = "VIDEO"
                        currentSection = AdminSection.REPORTS
                    }
                )
                AdminSection.WITHDRAWALS -> AdminWithdrawalsScreen(viewModel = adminViewModel)
                AdminSection.REPORTS -> AdminReportsScreen(viewModel = adminViewModel)
                AdminSection.REWARDS -> AdminRewardsScreen(viewModel = adminViewModel)
                AdminSection.TRANSACTIONS -> AdminTransactionsScreen(viewModel = adminViewModel)
                AdminSection.NOTIFICATIONS -> AdminNotificationsScreen(viewModel = adminViewModel)
                AdminSection.STATISTICS -> AdminStatisticsScreen(viewModel = adminViewModel)
                AdminSection.SETTINGS -> AdminSettingsScreen(viewModel = adminViewModel)
                AdminSection.AUDIT_LOGS -> AdminAuditLogsScreen(viewModel = adminViewModel)
            }
        }
    }
}
