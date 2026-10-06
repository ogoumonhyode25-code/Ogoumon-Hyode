package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuditLog
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashPurplePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminAuditLogsScreen(
    viewModel: AdminViewModel
) {
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    var selectedActionFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(auditLogs, selectedActionFilter) {
        if (selectedActionFilter == "ALL") auditLogs
        else auditLogs.filter { it.action.equals(selectedActionFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_audit_logs_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Journal d'Audit",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${filteredLogs.size} action(s) consignée(s)",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E153A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C1D95))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = VideoCashEmerald, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inviolable", color = VideoCashEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filtres par type d'action
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val actions = listOf(
                "ALL" to "Toutes",
                AuditLog.ACTION_USER_SUSPENDED to "Suspensions",
                AuditLog.ACTION_USER_REACTIVATED to "Réactivations",
                AuditLog.ACTION_VIDEO_MODERATED to "Modérations",
                AuditLog.ACTION_WITHDRAWAL_PAID to "Paiements",
                AuditLog.ACTION_SETTINGS_CHANGED to "Paramètres",
                AuditLog.ACTION_ADMIN_ADJUSTMENT to "Ajustements"
            )
            items(actions) { (key, label) ->
                val selected = selectedActionFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { selectedActionFilter = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF140D2B),
                        labelColor = Color(0xFF9E95B8),
                        selectedContainerColor = VideoCashPurplePrimary,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = Color(0xFF281F47),
                        selectedBorderColor = VideoCashPurplePrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun événement dans le journal d'audit.", color = Color(0xFF9E95B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    AuditLogItemCard(log = log)
                }
            }
        }
    }
}

@Composable
fun AuditLogItemCard(log: AuditLog) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.FRENCH) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeColor = when {
                    log.action.contains("SUSPEND", ignoreCase = true) || log.action.contains("DELETE", ignoreCase = true) -> Color(0xFFF43F5E)
                    log.action.contains("PAID", ignoreCase = true) || log.action.contains("REACTIVATE", ignoreCase = true) -> VideoCashEmerald
                    log.action.contains("MODERATE", ignoreCase = true) || log.action.contains("SETTINGS", ignoreCase = true) -> Color(0xFFF59E0B)
                    else -> Color(0xFF38BDF8)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = log.badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = dateFormat.format(Date(log.createdAt)),
                    color = Color(0xFF9E95B8),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Medium)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Auteur : ${log.actorEmail.ifBlank { log.actorId }}",
                    color = Color(0xFF9E95B8),
                    fontSize = 11.sp
                )
                if (log.targetName.isNotBlank()) {
                    Text(
                        text = "Cible : ${log.targetName}",
                        color = Color(0xFFD8B4FE),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
