package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Wallet
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onNavigateToSection: (AdminSection) -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("admin_dashboard_screen")
    ) {
        // En-tête de bienvenue Admin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Vue d'ensemble",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Statistiques de la plateforme en direct",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9E95B8))
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E153A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C1D95))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(VideoCashEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Système Actif",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = VideoCashEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cartes statistiques principales (Grille)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminStatCard(
                modifier = Modifier.weight(1f),
                title = "Utilisateurs",
                value = stats.totalUsers.toString(),
                subValue = "${stats.activeUsers} actifs • ${stats.suspendedUsers} susp.",
                icon = Icons.Default.People,
                accentColor = Color(0xFF38BDF8),
                onClick = { onNavigateToSection(AdminSection.USERS) }
            )

            AdminStatCard(
                modifier = Modifier.weight(1f),
                title = "Vidéos",
                value = stats.totalVideos.toString(),
                subValue = "${stats.publishedVideos} pub. • ${stats.pendingVideos} proc.",
                icon = Icons.Default.VideoLibrary,
                accentColor = Color(0xFFA855F7),
                onClick = { onNavigateToSection(AdminSection.VIDEOS) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminStatCard(
                modifier = Modifier.weight(1f),
                title = "Retraits en attente",
                value = stats.pendingWithdrawalsCount.toString(),
                subValue = "${Wallet.formatCurrency(stats.pendingWithdrawalsAmount)} FCFA",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = VideoCashGoldAccent,
                onClick = { onNavigateToSection(AdminSection.WITHDRAWALS) }
            )

            AdminStatCard(
                modifier = Modifier.weight(1f),
                title = "Signalements",
                value = stats.totalReportsCount.toString(),
                subValue = "${stats.pendingReportsCount} à traiter",
                icon = Icons.Default.Report,
                accentColor = if (stats.pendingReportsCount > 0) Color(0xFFF43F5E) else Color(0xFF9E95B8),
                onClick = { onNavigateToSection(AdminSection.REPORTS) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Carte Total Récompenses distribuées
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSection(AdminSection.REWARDS) }
                .testTag("admin_stat_rewards"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2E2406)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = VideoCashGoldAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Récompenses distribuées",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
                        )
                        Text(
                            text = "${Wallet.formatCurrency(stats.totalRewardsDistributed)} FCFA",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = VideoCashGoldAccent
                            )
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF6B7280)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section Tendances & Performances (7 jours & 30 jours)
        Text(
            text = "Tendances d'activité",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrendBox(
                modifier = Modifier.weight(1f),
                title = "7 derniers jours",
                views = stats.views7Days,
                rewards = stats.rewards7Days,
                withdrawals = stats.withdrawals7Days
            )
            TrendBox(
                modifier = Modifier.weight(1f),
                title = "30 derniers jours",
                views = stats.views30Days,
                rewards = stats.rewards30Days,
                withdrawals = stats.withdrawals30Days
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions rapides pour l'administrateur
        Text(
            text = "Actions rapides",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionItem(
                title = "Examiner les retraits en attente",
                description = "${stats.pendingWithdrawalsCount} demandes nécessitent une validation",
                icon = Icons.Default.AccountBalanceWallet,
                color = VideoCashGoldAccent,
                onClick = { onNavigateToSection(AdminSection.WITHDRAWALS) }
            )
            QuickActionItem(
                title = "Traiter les signalements de contenu",
                description = "${stats.pendingReportsCount} signalements non résolus",
                icon = Icons.Default.Report,
                color = Color(0xFFF43F5E),
                onClick = { onNavigateToSection(AdminSection.REPORTS) }
            )
            QuickActionItem(
                title = "Diffuser une notification système",
                description = "Envoyer un message à un ou plusieurs utilisateurs",
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF38BDF8),
                onClick = { onNavigateToSection(AdminSection.NOTIFICATIONS) }
            )
            QuickActionItem(
                title = "Consulter le journal d'audit",
                description = "Traçabilité intégrale de toutes les actions administratives",
                icon = Icons.Default.Security,
                color = VideoCashPurplePrimary,
                onClick = { onNavigateToSection(AdminSection.AUDIT_LOGS) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AdminStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("stat_card_${title.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF9E95B8),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun TrendBox(
    modifier: Modifier = Modifier,
    title: String,
    views: Long,
    rewards: Long,
    withdrawals: Long
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vues :", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8)))
                Text("$views", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Récompenses :", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8)))
                Text("${Wallet.formatCurrency(rewards)} F", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = VideoCashGoldAccent))
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Retraits payés :", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8)))
                Text("${Wallet.formatCurrency(withdrawals)} F", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = VideoCashEmerald))
            }
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF140D2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9E95B8)
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
