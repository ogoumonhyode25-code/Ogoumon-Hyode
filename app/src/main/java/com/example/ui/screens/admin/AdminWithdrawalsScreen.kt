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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Withdrawal
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminWithdrawalsScreen(
    viewModel: AdminViewModel
) {
    val withdrawals by viewModel.filteredWithdrawals.collectAsStateWithLifecycle()
    val currentFilter by viewModel.withdrawalFilter.collectAsStateWithLifecycle()

    var withdrawalToApprove by remember { mutableStateOf<Withdrawal?>(null) }
    var withdrawalToReject by remember { mutableStateOf<Withdrawal?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var withdrawalToMarkPaid by remember { mutableStateOf<Withdrawal?>(null) }
    var providerReference by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_withdrawals_screen")
    ) {
        Text(
            text = "Gestion des Retraits",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = "${withdrawals.size} demande(s) de retrait",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filtres par statut
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val statusFilters = listOf(
                "ALL" to "Tous",
                Withdrawal.STATUS_PENDING to "En attente",
                Withdrawal.STATUS_REVIEWING to "En revue",
                Withdrawal.STATUS_APPROVED to "Validés",
                Withdrawal.STATUS_PROCESSING to "En cours",
                Withdrawal.STATUS_PAID to "Payés",
                Withdrawal.STATUS_REJECTED to "Rejetés"
            )
            items(statusFilters) { (key, label) ->
                val selected = currentFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.withdrawalFilter.value = key },
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

        if (withdrawals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune demande de retrait dans cette catégorie.", color = Color(0xFF9E95B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(withdrawals, key = { it.id }) { w ->
                    AdminWithdrawalCard(
                        withdrawal = w,
                        onApprove = { withdrawalToApprove = w },
                        onReject = { withdrawalToReject = w },
                        onMarkPaid = { withdrawalToMarkPaid = w }
                    )
                }
            }
        }
    }

    // Modal Validation (Approve)
    withdrawalToApprove?.let { w ->
        AlertDialog(
            onDismissRequest = { withdrawalToApprove = null },
            title = { Text("Approuver la demande", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Confirmez-vous l'approbation du retrait de ${w.formattedAmount} FCFA pour le compte ${w.method} (${w.account}) ?",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWithdrawal(w.id, Withdrawal.STATUS_APPROVED, "Demande approuvée par l'administrateur")
                        withdrawalToApprove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald)
                ) {
                    Text("Approuver")
                }
            },
            dismissButton = {
                TextButton(onClick = { withdrawalToApprove = null }) {
                    Text("Annuler", color = Color(0xFF9E95B8))
                }
            },
            containerColor = Color(0xFF191136)
        )
    }

    // Modal Rejet (Reject) avec motif
    withdrawalToReject?.let { w ->
        AlertDialog(
            onDismissRequest = { withdrawalToReject = null; rejectionReason = "" },
            title = { Text("Rejeter la demande", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Indiquez le motif du rejet (le solde sera recrédité sur le portefeuille de l'utilisateur) :", color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Motif du rejet (obligatoire)") },
                        placeholder = { Text("Ex: Numéro de compte erroné, fraude...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReason.isNotBlank()) {
                            viewModel.updateWithdrawal(w.id, Withdrawal.STATUS_REJECTED, rejectionReason)
                            withdrawalToReject = null
                            rejectionReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                    enabled = rejectionReason.isNotBlank()
                ) {
                    Text("Confirmer le rejet")
                }
            },
            dismissButton = {
                TextButton(onClick = { withdrawalToReject = null; rejectionReason = "" }) {
                    Text("Annuler", color = Color(0xFF9E95B8))
                }
            },
            containerColor = Color(0xFF191136)
        )
    }

    // Modal Marquer Payé (PAID) avec référence opérateur obligatoire
    withdrawalToMarkPaid?.let { w ->
        AlertDialog(
            onDismissRequest = { withdrawalToMarkPaid = null; providerReference = "" },
            title = { Text("Confirmer le paiement", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Règle de sécurité : Entrez l'identifiant de transaction délivré par ${w.method} attestant du virement réel.",
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = providerReference,
                        onValueChange = { providerReference = it },
                        label = { Text("Réf. transaction opérateur (ex: TXN_WAVE_99812)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ref = providerReference.ifBlank { "MANUAL_PAID_${System.currentTimeMillis()}" }
                        viewModel.updateWithdrawal(w.id, Withdrawal.STATUS_PAID, "Paiement confirmé", ref)
                        withdrawalToMarkPaid = null
                        providerReference = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald)
                ) {
                    Text("Marquer PAYÉ")
                }
            },
            dismissButton = {
                TextButton(onClick = { withdrawalToMarkPaid = null; providerReference = "" }) {
                    Text("Annuler", color = Color(0xFF9E95B8))
                }
            },
            containerColor = Color(0xFF191136)
        )
    }
}

@Composable
fun AdminWithdrawalCard(
    withdrawal: Withdrawal,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onMarkPaid: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRENCH) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_withdrawal_card_${withdrawal.id}")
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
                        text = "${withdrawal.method.uppercase()} • #${withdrawal.id.take(8)}",
                        color = VideoCashGoldAccent,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                val statusColor = when (withdrawal.status) {
                    Withdrawal.STATUS_PAID -> VideoCashEmerald
                    Withdrawal.STATUS_APPROVED -> Color(0xFF38BDF8)
                    Withdrawal.STATUS_PENDING -> Color(0xFFF59E0B)
                    Withdrawal.STATUS_REJECTED -> Color(0xFFF43F5E)
                    else -> Color(0xFF9E95B8)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = withdrawal.status.uppercase(),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${withdrawal.formattedAmount} FCFA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Numéro : ${withdrawal.account}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = dateFormat.format(Date(withdrawal.createdAt)),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8), fontSize = 11.sp)
                    )
                    if (withdrawal.providerReference.isNotBlank()) {
                        Text(
                            text = "Réf : ${withdrawal.providerReference.take(12)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = VideoCashEmerald, fontSize = 10.sp)
                        )
                    }
                }
            }

            if (withdrawal.adminNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Motif rejet : ${withdrawal.adminNote}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF43F5E), fontSize = 11.sp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions selon le statut actuel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (withdrawal.status == Withdrawal.STATUS_PENDING || withdrawal.status == Withdrawal.STATUS_REVIEWING) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Rejeter", color = Color(0xFFF43F5E), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Approuver", fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (withdrawal.status == Withdrawal.STATUS_APPROVED || withdrawal.status == Withdrawal.STATUS_PROCESSING || withdrawal.status == Withdrawal.STATUS_PENDING) {
                    Button(
                        onClick = onMarkPaid,
                        colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Marquer Payé", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
