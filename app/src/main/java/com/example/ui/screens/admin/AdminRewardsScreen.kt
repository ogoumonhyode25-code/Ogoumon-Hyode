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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdminSettings
import com.example.data.model.Wallet
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary

@Composable
fun AdminRewardsScreen(
    viewModel: AdminViewModel
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val adminSettings by viewModel.adminSettings.collectAsStateWithLifecycle()

    var viewReward by remember(adminSettings) { mutableStateOf(adminSettings.viewReward.toString()) }
    var dailyLimit by remember(adminSettings) { mutableStateOf(adminSettings.dailyViewLimit.toString()) }
    var minWatchSec by remember(adminSettings) { mutableStateOf(adminSettings.minimumWatchSeconds.toString()) }
    var minWithdrawal by remember(adminSettings) { mutableStateOf(adminSettings.minimumWithdrawal.toString()) }
    var maxDailyWithdrawal by remember(adminSettings) { mutableStateOf(adminSettings.maximumDailyWithdrawal.toString()) }
    var rewardEnabled by remember(adminSettings) { mutableStateOf(adminSettings.rewardEnabled) }
    var withdrawalsEnabled by remember(adminSettings) { mutableStateOf(adminSettings.withdrawalsEnabled) }

    var showAdjustmentDialog by remember { mutableStateOf(false) }
    var targetUserId by remember { mutableStateOf("") }
    var adjustAmount by remember { mutableStateOf("") }
    var adjustReason by remember { mutableStateOf("") }
    var isDeduction by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("admin_rewards_screen")
    ) {
        Text(
            text = "Gestion des Récompenses",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = "Suivi économique et configuration des gains",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Métriques économiques
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Total distribué", color = Color(0xFF9E95B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Wallet.formatCurrency(stats.totalRewardsDistributed)} F",
                        style = MaterialTheme.typography.titleLarge.copy(color = VideoCashGoldAccent, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("7 derniers jours", color = Color(0xFF9E95B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Wallet.formatCurrency(stats.rewards7Days)} F",
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bouton d'action Ajustement Manuel
        Button(
            onClick = { showAdjustmentDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281F47)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = VideoCashGoldAccent)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Effectuer un ajustement financier manuel", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Paramètres configurables
        Text(
            text = "Paramètres du système de gains",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Switch activation récompenses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Système de récompense actif", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Attribue des FCFA lors du visionnage", color = Color(0xFF9E95B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = rewardEnabled,
                        onCheckedChange = { rewardEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VideoCashEmerald,
                            checkedTrackColor = VideoCashEmerald.copy(alpha = 0.5f)
                        )
                    )
                }

                // Switch activation retraits
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Demandes de retrait actives", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Permet aux utilisateurs de demander un retrait", color = Color(0xFF9E95B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = withdrawalsEnabled,
                        onCheckedChange = { withdrawalsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VideoCashEmerald,
                            checkedTrackColor = VideoCashEmerald.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = viewReward,
                    onValueChange = { viewReward = it },
                    label = { Text("Récompense par vue (FCFA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dailyLimit,
                    onValueChange = { dailyLimit = it },
                    label = { Text("Limite quotidienne de vidéos rémunérées") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minWatchSec,
                    onValueChange = { minWatchSec = it },
                    label = { Text("Durée minimum de visionnage (secondes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minWithdrawal,
                    onValueChange = { minWithdrawal = it },
                    label = { Text("Seuil minimum de retrait (FCFA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maxDailyWithdrawal,
                    onValueChange = { maxDailyWithdrawal = it },
                    label = { Text("Plafond quotidien de retrait (FCFA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val newSettings = adminSettings.copy(
                            viewReward = viewReward.toLongOrNull() ?: adminSettings.viewReward,
                            dailyViewLimit = dailyLimit.toIntOrNull() ?: adminSettings.dailyViewLimit,
                            minimumWatchSeconds = minWatchSec.toIntOrNull() ?: adminSettings.minimumWatchSeconds,
                            minimumWithdrawal = minWithdrawal.toLongOrNull() ?: adminSettings.minimumWithdrawal,
                            maximumDailyWithdrawal = maxDailyWithdrawal.toLongOrNull() ?: adminSettings.maximumDailyWithdrawal,
                            rewardEnabled = rewardEnabled,
                            withdrawalsEnabled = withdrawalsEnabled
                        )
                        viewModel.saveAdminSettings(newSettings)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enregistrer les paramètres")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal Ajustement financier manuel
    if (showAdjustmentDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustmentDialog = false },
            title = { Text("Ajustement financier sécurisé", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Règle de conformité : Cet ajustement sera exécuté via Cloud Function et consigné de manière permanente dans les auditLogs.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isDeduction = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isDeduction) VideoCashEmerald else Color(0xFF1E153A)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Créditer (+)")
                        }
                        Button(
                            onClick = { isDeduction = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDeduction) Color(0xFFF43F5E) else Color(0xFF1E153A)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Débiter (-)")
                        }
                    }

                    OutlinedTextField(
                        value = targetUserId,
                        onValueChange = { targetUserId = it },
                        label = { Text("UID de l'utilisateur") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adjustAmount,
                        onValueChange = { adjustAmount = it },
                        label = { Text("Montant (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adjustReason,
                        onValueChange = { adjustReason = it },
                        label = { Text("Motif justificatif obligatoire") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = adjustAmount.toLongOrNull() ?: 0L
                        val finalAmount = if (isDeduction) -kotlin.math.abs(amt) else kotlin.math.abs(amt)
                        if (targetUserId.isNotBlank() && amt > 0 && adjustReason.isNotBlank()) {
                            viewModel.createAdminAdjustment(targetUserId, finalAmount, adjustReason)
                            showAdjustmentDialog = false
                            targetUserId = ""
                            adjustAmount = ""
                            adjustReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald),
                    enabled = targetUserId.isNotBlank() && (adjustAmount.toLongOrNull() ?: 0L) > 0 && adjustReason.isNotBlank()
                ) {
                    Text("Appliquer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustmentDialog = false }) {
                    Text("Annuler", color = Color(0xFF9E95B8))
                }
            },
            containerColor = Color(0xFF191136)
        )
    }
}
