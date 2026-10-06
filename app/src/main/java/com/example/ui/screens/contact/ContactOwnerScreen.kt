package com.example.ui.screens.contact

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary
import com.example.ui.theme.VideoCashPurpleSecondary

/**
 * Informations officielles sur le propriétaire et administrateur de VidéoCash
 */
object OwnerInfo {
    const val APP_NAME = "VidéoCash"
    const val OWNER_NAME = "Hyode OGOUMON"
    const val EMAIL = "ogoumonhyode25@gmail.com"
    const val PHONE = "0769432630"
    const val PHONE_DIAL = "+2250769432630"
    const val ROLE = "Propriétaire / Administrateur"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactOwnerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Contact / Propriétaire",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_contact_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D081E)
                )
            )
        },
        containerColor = Color(0xFF090614)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // En-tête avec avatar stylisé du Propriétaire
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(VideoCashPurplePrimary, VideoCashGoldAccent)
                        )
                    )
                    .testTag("owner_avatar")
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Nom et Badge vérifié
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = OwnerInfo.OWNER_NAME,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Vérifié",
                    tint = VideoCashGoldAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rôle officiel
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF22153E),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = OwnerInfo.ROLE,
                    color = VideoCashGoldAccent,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Application
            Text(
                text = "Application : ${OwnerInfo.APP_NAME}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFB1A7CC)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Actions directes : Appeler et Envoyer un email
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bouton Appeler
                Button(
                    onClick = { dialPhone(context, OwnerInfo.PHONE) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VideoCashEmerald
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_call_owner")
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Appeler",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Appeler",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                }

                // Bouton Envoyer un email
                Button(
                    onClick = { sendEmail(context, OwnerInfo.EMAIL, OwnerInfo.APP_NAME) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VideoCashPurplePrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_email_owner")
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Envoyer un e-mail",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Envoyer email",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Carte des coordonnées détaillées
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140D29)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Coordonnées Officielles",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VideoCashGoldAccent
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ContactDetailRow(
                        icon = Icons.Default.Person,
                        label = "Nom du propriétaire",
                        value = OwnerInfo.OWNER_NAME
                    )

                    HorizontalDivider(
                        color = Color(0xFF281F47),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    ContactDetailRow(
                        icon = Icons.Default.Badge,
                        label = "Fonction",
                        value = OwnerInfo.ROLE
                    )

                    HorizontalDivider(
                        color = Color(0xFF281F47),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    ContactDetailRow(
                        icon = Icons.Default.Phone,
                        label = "Téléphone",
                        value = OwnerInfo.PHONE,
                        isClickable = true,
                        onClick = { dialPhone(context, OwnerInfo.PHONE) }
                    )

                    HorizontalDivider(
                        color = Color(0xFF281F47),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    ContactDetailRow(
                        icon = Icons.Default.Email,
                        label = "Adresse e-mail",
                        value = OwnerInfo.EMAIL,
                        isClickable = true,
                        onClick = { sendEmail(context, OwnerInfo.EMAIL, OwnerInfo.APP_NAME) }
                    )

                    HorizontalDivider(
                        color = Color(0xFF281F47),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    ContactDetailRow(
                        icon = Icons.Default.Business,
                        label = "Nom de l'application",
                        value = OwnerInfo.APP_NAME
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Note de transparence et de contact
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF19122C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = VideoCashGoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pour toute question relative à l'administration de la plateforme, aux partenariats ou aux signalements, vous pouvez joindre directement le propriétaire par téléphone ou par courrier électronique.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFCCC5E3),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContactDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF231742),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VideoCashGoldAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF9E95B8)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        if (isClickable) {
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = VideoCashGoldAccent
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp,
                    vertical = 4.dp
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, VideoCashGoldAccent.copy(alpha = 0.5f)),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Ouvrir",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * Lance l'application Téléphone de l'appareil
 */
private fun dialPhone(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir l'application téléphone : ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Lance l'application E-mail de l'appareil
 */
private fun sendEmail(context: Context, emailAddress: String, appName: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            putExtra(Intent.EXTRA_SUBJECT, "[$appName] Contact avec le Propriétaire / Administrateur")
        }
        context.startActivity(Intent.createChooser(intent, "Envoyer un e-mail via..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir l'application e-mail : ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
