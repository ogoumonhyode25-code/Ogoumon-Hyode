package com.example.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDestination: (type: String, referenceId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSimulationMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("notifications_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications")
                        val unread = (uiState as? NotificationsUiState.Success)?.unreadCount ?: 0
                        if (unread > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (unread > 99) "99+" else "$unread",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    // Tout marquer comme lu
                    IconButton(onClick = { viewModel.markAllAsRead() }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Tout marquer comme lu"
                        )
                    }

                    // Menu de simulation pour tester chaque type
                    Box {
                        IconButton(onClick = { showSimulationMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Simuler une notification"
                            )
                        }

                        DropdownMenu(
                            expanded = showSimulationMenu,
                            onDismissRequest = { showSimulationMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Simuler: Retrait reçu") },
                                onClick = {
                                    showSimulationMenu = false
                                    viewModel.simulateNotification(AppNotification.TYPE_WITHDRAWAL_CREATED)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simuler: Retrait approuvé") },
                                onClick = {
                                    showSimulationMenu = false
                                    viewModel.simulateNotification(AppNotification.TYPE_WITHDRAWAL_APPROVED)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simuler: Retrait refusé") },
                                onClick = {
                                    showSimulationMenu = false
                                    viewModel.simulateNotification(AppNotification.TYPE_WITHDRAWAL_REJECTED)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simuler: Retrait payé") },
                                onClick = {
                                    showSimulationMenu = false
                                    viewModel.simulateNotification(AppNotification.TYPE_WITHDRAWAL_PAID)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simuler: Gain validé") },
                                onClick = {
                                    showSimulationMenu = false
                                    viewModel.simulateNotification(AppNotification.TYPE_REWARD_APPROVED)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simuler: Nouveau commentaire") },
                                onClick = {
                                    showSimulationMenu = false
                                    viewModel.simulateNotification(AppNotification.TYPE_NEW_COMMENT)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simuler: Nouveau J'aime") },
                                onClick = {
                                    showSimulationMenu = false
                                    viewModel.simulateNotification(AppNotification.TYPE_NEW_LIKE)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simuler: Alerte Admin") },
                                onClick = {
                                    showSimulationMenu = false
                                    viewModel.simulateNotification(AppNotification.TYPE_ADMIN)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is NotificationsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is NotificationsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadNotifications() }) {
                                Text("Réessayer")
                            }
                        }
                    }
                }
                is NotificationsUiState.Success -> {
                    // Barre de filtres
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf(
                            "ALL" to "Toutes",
                            "FINANCE" to "Gains & Retraits",
                            "SOCIAL" to "Social",
                            "SYSTEM" to "Système"
                        )

                        filters.forEach { (key, label) ->
                            FilterChip(
                                selected = state.activeFilter == key,
                                onClick = { viewModel.setFilter(key) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    val filteredList = state.notifications.filter { item ->
                        when (state.activeFilter) {
                            "FINANCE" -> item.isFinancial
                            "SOCIAL" -> item.type in listOf(AppNotification.TYPE_NEW_COMMENT, AppNotification.TYPE_NEW_LIKE)
                            "SYSTEM" -> item.type in listOf(AppNotification.TYPE_SYSTEM, AppNotification.TYPE_ADMIN)
                            else -> true
                        }
                    }

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Aucune notification pour le moment",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Vous serez prévenu ici dès qu'un gain est validé, qu'un retrait change d'état ou qu'une interaction survient.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredList, key = { it.id }) { notif ->
                                NotificationItemCard(
                                    notification = notif,
                                    onClick = {
                                        if (!notif.read) viewModel.markAsRead(notif.id)
                                        onNavigateToDestination(notif.type, notif.referenceId)
                                    },
                                    onDelete = { viewModel.deleteNotification(notif.id) },
                                    onMarkAsRead = { viewModel.markAsRead(notif.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    val dateStr = remember(notification.createdAt) {
        val diff = System.currentTimeMillis() - notification.createdAt
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        when {
            minutes < 1 -> "À l'instant"
            minutes < 60 -> "Il y a $minutes min"
            hours < 24 -> "Il y a $hours h"
            days < 7 -> "Il y a $days j"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.FRANCE).format(Date(notification.createdAt))
        }
    }

    val (icon, iconColor, bgIconColor) = when (notification.type) {
        AppNotification.TYPE_WITHDRAWAL_PAID ->
            Triple(Icons.Default.Payments, Color(0xFF1B5E20), Color(0xFFE8F5E9))
        AppNotification.TYPE_WITHDRAWAL_APPROVED ->
            Triple(Icons.Default.CheckCircle, Color(0xFF2E7D32), Color(0xFFE8F5E9))
        AppNotification.TYPE_WITHDRAWAL_REJECTED ->
            Triple(Icons.Default.Info, Color(0xFFC62828), Color(0xFFFFEBEE))
        AppNotification.TYPE_WITHDRAWAL_CREATED ->
            Triple(Icons.Default.Payments, Color(0xFFE65100), Color(0xFFFFF3E0))
        AppNotification.TYPE_REWARD_APPROVED ->
            Triple(Icons.Default.Stars, Color(0xFFF57F17), Color(0xFFFFFDE7))
        AppNotification.TYPE_NEW_COMMENT ->
            Triple(Icons.Default.Comment, Color(0xFF1565C0), Color(0xFFE3F2FD))
        AppNotification.TYPE_NEW_LIKE ->
            Triple(Icons.Default.Favorite, Color(0xFFC2185B), Color(0xFFFCE4EC))
        AppNotification.TYPE_ADMIN ->
            Triple(Icons.Default.AdminPanelSettings, Color(0xFF6A1B9A), Color(0xFFF3E5F5))
        else ->
            Triple(Icons.Default.Notifications, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.read) 0.5.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("notification_card_${notification.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icône de type
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgIconColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (!notification.read) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = bgIconColor
                    ) {
                        Text(
                            text = notification.badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = iconColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!notification.read) {
                            IconButton(
                                onClick = onMarkAsRead,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Marquer comme lu",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
