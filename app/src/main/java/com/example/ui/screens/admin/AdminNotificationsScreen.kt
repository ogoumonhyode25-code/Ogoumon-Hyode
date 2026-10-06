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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashPurplePrimary

@Composable
fun AdminNotificationsScreen(
    viewModel: AdminViewModel
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var targetAudience by remember { mutableStateOf("ALL") } // "ALL", "SPECIFIC", "SYSTEM"
    var targetUserId by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("admin_notifications_screen")
    ) {
        Text(
            text = "Notifications Administrateur",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = "Diffusion de messages Push FCM et alertes système",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sélection de la cible
        Text("Destinataires :", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { targetAudience = "ALL" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (targetAudience == "ALL") VideoCashPurplePrimary else Color(0xFF140D2B)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tous", fontSize = 12.sp)
            }

            Button(
                onClick = { targetAudience = "SPECIFIC" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (targetAudience == "SPECIFIC") VideoCashPurplePrimary else Color(0xFF140D2B)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Utilisateur", fontSize = 12.sp)
            }

            Button(
                onClick = { targetAudience = "SYSTEM" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (targetAudience == "SYSTEM") VideoCashPurplePrimary else Color(0xFF140D2B)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Système", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (targetAudience == "SPECIFIC") {
                    OutlinedTextField(
                        value = targetUserId,
                        onValueChange = { targetUserId = it },
                        label = { Text("UID de l'utilisateur destinataire") },
                        placeholder = { Text("ex: user_12345...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la notification") },
                    placeholder = { Text("ex: Maintenance programmée / Nouvelle récompense !") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Contenu du message") },
                    placeholder = { Text("Rédigez le texte détaillé ici...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val isFormValid = title.isNotBlank() && body.isNotBlank() && (targetAudience != "SPECIFIC" || targetUserId.isNotBlank())

                Button(
                    onClick = {
                        if (isFormValid) {
                            viewModel.sendAdminNotification(
                                title = title,
                                body = body,
                                targetUserId = if (targetAudience == "SPECIFIC") targetUserId else null,
                                targetAudience = targetAudience
                            )
                            title = ""
                            body = ""
                            targetUserId = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diffuser la notification", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Conseils de diffusion
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191136)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Bonnes pratiques", color = Color(0xFFD8B4FE), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "• Les notifications globales sont envoyées via le topic FCM 'all_users'.\n" +
                    "• Les notifications ciblées utilisent le token d'enregistrement de l'appareil utilisateur.\n" +
                    "• Chaque envoi est consigné dans les Audit Logs système.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
