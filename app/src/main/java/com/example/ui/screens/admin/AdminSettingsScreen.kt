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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashPurplePrimary

@Composable
fun AdminSettingsScreen(
    viewModel: AdminViewModel
) {
    val settings by viewModel.adminSettings.collectAsStateWithLifecycle()

    var registrationEnabled by remember(settings) { mutableStateOf(settings.registrationEnabled) }
    var rewardEnabled by remember(settings) { mutableStateOf(settings.rewardEnabled) }
    var withdrawalsEnabled by remember(settings) { mutableStateOf(settings.withdrawalsEnabled) }
    var videoUploadEnabled by remember(settings) { mutableStateOf(settings.videoUploadEnabled) }
    var maintenanceMode by remember(settings) { mutableStateOf(settings.maintenanceMode) }
    var maintenanceMessage by remember(settings) { mutableStateOf(settings.maintenanceMessage) }
    var minAppVersion by remember(settings) { mutableStateOf(settings.minAppVersion) }
    var dailyUploadLimit by remember(settings) { mutableStateOf(settings.dailyUploadLimit.toString()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("admin_settings_screen")
    ) {
        Text(
            text = "Paramètres Généraux",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = "Configuration globale de l'application et disjoncteurs de sécurité",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (maintenanceMode) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3F1322)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFDA4AF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Mode Maintenance actif : Seuls les administrateurs peuvent accéder à la plateforme.",
                        color = Color(0xFFFDA4AF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section Disjoncteurs
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Interrupteurs de service", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                SettingsSwitchRow(
                    title = "Inscriptions d'utilisateurs",
                    subtitle = "Autoriser la création de nouveaux comptes",
                    checked = registrationEnabled,
                    onCheckedChange = { registrationEnabled = it }
                )

                SettingsSwitchRow(
                    title = "Publication de vidéos",
                    subtitle = "Autoriser l'envoi de nouvelles vidéos par les créateurs",
                    checked = videoUploadEnabled,
                    onCheckedChange = { videoUploadEnabled = it }
                )

                SettingsSwitchRow(
                    title = "Système de récompenses",
                    subtitle = "Créditer les utilisateurs lors du visionnage",
                    checked = rewardEnabled,
                    onCheckedChange = { rewardEnabled = it }
                )

                SettingsSwitchRow(
                    title = "Système de retraits",
                    subtitle = "Permettre aux utilisateurs de demander un retrait Mobile Money",
                    checked = withdrawalsEnabled,
                    onCheckedChange = { withdrawalsEnabled = it }
                )

                SettingsSwitchRow(
                    title = "Mode maintenance global",
                    subtitle = "Restreint l'accès à tous les utilisateurs réguliers",
                    checked = maintenanceMode,
                    onCheckedChange = { maintenanceMode = it },
                    accentColor = Color(0xFFF43F5E)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section Configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Règles & Versions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                OutlinedTextField(
                    value = maintenanceMessage,
                    onValueChange = { maintenanceMessage = it },
                    label = { Text("Message de maintenance affiché") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minAppVersion,
                    onValueChange = { minAppVersion = it },
                    label = { Text("Version minimale requise de l'application") },
                    placeholder = { Text("ex: 1.0.0") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dailyUploadLimit,
                    onValueChange = { dailyUploadLimit = it },
                    label = { Text("Nombre maximum de vidéos/jour par utilisateur") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val updated = settings.copy(
                            registrationEnabled = registrationEnabled,
                            rewardEnabled = rewardEnabled,
                            withdrawalsEnabled = withdrawalsEnabled,
                            videoUploadEnabled = videoUploadEnabled,
                            maintenanceMode = maintenanceMode,
                            maintenanceMessage = maintenanceMessage,
                            minAppVersion = minAppVersion,
                            dailyUploadLimit = dailyUploadLimit.toIntOrNull() ?: settings.dailyUploadLimit
                        )
                        viewModel.saveAdminSettings(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Appliquer la configuration")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = VideoCashEmerald
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(subtitle, color = Color(0xFF9E95B8), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.5f)
            )
        )
    }
}
