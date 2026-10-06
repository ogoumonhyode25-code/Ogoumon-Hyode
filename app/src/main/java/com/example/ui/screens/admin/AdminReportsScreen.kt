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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Report
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashPurplePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminReportsScreen(
    viewModel: AdminViewModel
) {
    val reports by viewModel.filteredReports.collectAsStateWithLifecycle()
    val statusFilter by viewModel.reportFilterStatus.collectAsStateWithLifecycle()
    val typeFilter by viewModel.reportFilterType.collectAsStateWithLifecycle()

    var activeReport by remember { mutableStateOf<Report?>(null) }
    var actionDialogType by remember { mutableStateOf<String?>(null) } // "RESOLVE" ou "REJECT"
    var resolutionNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_reports_screen")
    ) {
        Text(
            text = "Signalements & Modération",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = "${reports.size} signalement(s) répertorié(s)",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filtres par statut
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val statuses = listOf(
                "ALL" to "Tous",
                "pending" to "En attente",
                "reviewing" to "En examen",
                "resolved" to "Résolus",
                "rejected" to "Rejetés"
            )
            items(statuses) { (key, label) ->
                val selected = statusFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.reportFilterStatus.value = key },
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

        Spacer(modifier = Modifier.height(8.dp))

        // Filtres par type de cible (VIDEO, COMMENT, USER)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val types = listOf(
                "ALL" to "Tous les types",
                "VIDEO" to "Vidéos",
                "COMMENT" to "Commentaires",
                "USER" to "Utilisateurs"
            )
            items(types) { (key, label) ->
                val selected = typeFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.reportFilterType.value = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF140D2B),
                        labelColor = Color(0xFF9E95B8),
                        selectedContainerColor = Color(0xFF281F47),
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = Color(0xFF281F47),
                        selectedBorderColor = Color(0xFF38BDF8)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun signalement trouvé.",
                    color = Color(0xFF9E95B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    AdminReportCard(
                        report = report,
                        onOpen = { activeReport = report },
                        onReview = {
                            viewModel.resolveReport(report.id, "reviewing", "Dossier pris en charge par l'administrateur")
                        },
                        onResolve = {
                            activeReport = report
                            actionDialogType = "RESOLVE"
                        },
                        onReject = {
                            activeReport = report
                            actionDialogType = "REJECT"
                        }
                    )
                }
            }
        }
    }

    // Modal de détails d'un signalement
    activeReport?.let { report ->
        if (actionDialogType == null) {
            AlertDialog(
                onDismissRequest = { activeReport = null },
                title = { Text("Signalement #${report.id.take(8)}", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Type : ${report.targetType}", color = Color(0xFFD8B4FE), fontWeight = FontWeight.Bold)
                        Text("Cible : ${report.targetTitle.ifBlank { report.targetId }}", color = Color.White)
                        Text("Motif : ${report.reasonLabel}", color = Color(0xFFF43F5E), fontWeight = FontWeight.SemiBold)
                        Text("Description de l'auteur :", color = Color(0xFF9E95B8), fontSize = 12.sp)
                        Text(report.description.ifBlank { "Aucun commentaire fourni." }, color = Color(0xFFCBD5E1))
                        Text("Statut : ${report.statusLabel}", color = VideoCashEmerald, fontWeight = FontWeight.Bold)
                        if (report.resolution.isNotBlank()) {
                            Text("Résolution : ${report.resolution}", color = Color(0xFF9E95B8), fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { activeReport = null }, colors = ButtonDefaults.buttonColors(containerColor = VideoCashPurplePrimary)) {
                        Text("Fermer")
                    }
                },
                containerColor = Color(0xFF191136)
            )
        }
    }

    // Dialogue Résoudre ou Rejeter
    actionDialogType?.let { type ->
        val report = activeReport ?: return@let
        val isResolve = type == "RESOLVE"

        AlertDialog(
            onDismissRequest = { actionDialogType = null; resolutionNote = "" },
            title = {
                Text(
                    if (isResolve) "Résoudre le signalement" else "Rejeter le signalement",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        if (isResolve) "Confirmer la résolution après traitement du contenu."
                        else "Indiquer la raison du rejet de ce signalement non fondé.",
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resolutionNote,
                        onValueChange = { resolutionNote = it },
                        label = { Text("Note de résolution") },
                        placeholder = { Text("Ex: Contenu supprimé / Faux signalement") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val status = if (isResolve) Report.STATUS_RESOLVED else Report.STATUS_REJECTED
                        viewModel.resolveReport(report.id, status, resolutionNote)
                        actionDialogType = null
                        activeReport = null
                        resolutionNote = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isResolve) VideoCashEmerald else Color(0xFFF43F5E)
                    )
                ) {
                    Text(if (isResolve) "Valider résolution" else "Confirmer rejet")
                }
            },
            dismissButton = {
                TextButton(onClick = { actionDialogType = null; resolutionNote = "" }) {
                    Text("Annuler", color = Color(0xFF9E95B8))
                }
            },
            containerColor = Color(0xFF191136)
        )
    }
}

@Composable
fun AdminReportCard(
    report: Report,
    onOpen: () -> Unit,
    onReview: () -> Unit,
    onResolve: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRENCH) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_report_card_${report.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF281F47)
                ) {
                    Text(
                        text = "${report.targetType} • #${report.id.take(6)}",
                        color = Color(0xFF38BDF8),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                val badgeColor = when (report.status) {
                    Report.STATUS_PENDING -> Color(0xFFF43F5E)
                    Report.STATUS_REVIEWING -> Color(0xFFF59E0B)
                    Report.STATUS_RESOLVED -> VideoCashEmerald
                    else -> Color(0xFF9E95B8)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = report.statusLabel,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.reasonLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )

            if (report.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1)),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Par @${report.reporterUsername.ifBlank { "anonyme" }} • ${dateFormat.format(Date(report.createdAt))}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8), fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Ouvrir", color = Color.White, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (report.status == Report.STATUS_PENDING) {
                    OutlinedButton(
                        onClick = onReview,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Traiter", color = Color(0xFFF59E0B), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (report.status != Report.STATUS_RESOLVED) {
                    Button(
                        onClick = onResolve,
                        colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Résoudre", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (report.status != Report.STATUS_REJECTED) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Rejeter", color = Color(0xFFF43F5E), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
