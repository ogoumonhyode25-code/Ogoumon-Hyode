package com.example.ui.screens.profile

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.data.model.Wallet
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.SupportAgent
import com.example.ui.theme.VideoCashPurpleSecondary

@Composable
fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {},
    onNavigateToContact: () -> Unit = {},
    viewModel: ProfileViewModel
) {
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val wallet by viewModel.userWallet.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090614))
            .testTag("profile_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // En-tête titre
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mon Profil",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                if (user?.role == "admin") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3F1322),
                        modifier = Modifier
                            .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(12.dp))
                            .clickable { onNavigateToAdmin() }
                            .testTag("badge_admin_header")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Admin",
                                tint = Color(0xFFFDA4AF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ESPACE ADMIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFDA4AF),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Message d'état (Succès / Erreur)
            when (uiState) {
                is ProfileUiState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0B2E1D),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, VideoCashEmerald, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VideoCashEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (uiState as ProfileUiState.Success).message,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD1FAE5))
                            )
                        }
                    }
                }
                is ProfileUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3F1322),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, Color(0xFFE11D48), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFFDA4AF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (uiState as ProfileUiState.Error).message,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFF1F2))
                            )
                        }
                    }
                }
                else -> {}
            }

            // Avertissement e-mail non vérifié
            if (user != null && !user!!.emailVerified) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2E1C0A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, VideoCashGoldAccent, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Attention",
                                tint = VideoCashGoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Adresse e-mail non vérifiée",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VideoCashGoldAccent
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vérifiez votre e-mail pour sécuriser vos retraits et recevoir vos notifications.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE5D5B8))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Renvoyer l'e-mail de vérification",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = VideoCashGoldAccent,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable { viewModel.resendEmailVerification() }
                                .padding(vertical = 4.dp)
                                .testTag("btn_resend_email_verification")
                        )
                    }
                }
            }

            // Photo de profil & Informations principales
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(
                            listOf(VideoCashGoldAccent, VideoCashPurplePrimary)
                        ),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                if (!user?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user?.photoUrl,
                        contentDescription = "Photo de profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E143B),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = Color(0xFFA79FB8),
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prénom & Nom
            val displayName = if (user != null && user!!.fullName.isNotBlank()) {
                user!!.fullName
            } else {
                "Utilisateur VidéoCash"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                if (user?.emailVerified == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Compte vérifié",
                        tint = VideoCashGoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // @username
            val displayUsername = if (!user?.username.isNullOrBlank()) "@${user?.username}" else "@compte"
            Text(
                text = displayUsername,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = VideoCashGoldAccent,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(top = 2.dp)
            )

            // Bio
            if (!user?.bio.isNullOrBlank()) {
                Text(
                    text = user?.bio ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFC7BFDD),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Statistiques (Abonnés, Abonnements, Vidéos)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140D29)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(
                        count = user?.followersCount ?: 0,
                        label = "Abonnés"
                    )
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(Color(0xFF281F47))
                    )
                    StatColumn(
                        count = user?.followingCount ?: 0,
                        label = "Abonnements"
                    )
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(Color(0xFF281F47))
                    )
                    StatColumn(
                        count = user?.videosCount ?: 0,
                        label = "Vidéos"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Carte Portefeuille Rapide (Aperçu solde FCFA)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1238)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2D1854),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Portefeuille",
                                    tint = VideoCashGoldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Solde disponible",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
                            )
                            Text(
                                text = "${wallet?.availableBalance ?: 0L} FCFA",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VideoCashGoldAccent
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF281F47),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "Actif",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = VideoCashEmerald,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Boutons d'action du profil
            if (user?.role == "admin") {
                Button(
                    onClick = onNavigateToAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF831843)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_admin_dashboard")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Panneau d'Administration",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onNavigateToEditProfile,
                colors = ButtonDefaults.buttonColors(containerColor = VideoCashPurplePrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_edit_profile")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Modifier le profil",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section Réglementaire & Support (Conformité Google Play)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140D29)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Assistance & Conditions",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = VideoCashGoldAccent
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ProfileMenuRowItem(
                        icon = Icons.Default.SupportAgent,
                        title = "Contact / Propriétaire",
                        onClick = onNavigateToContact,
                        testTag = "menu_contact_owner"
                    )
                    ProfileMenuRowItem(
                        icon = Icons.Default.HelpOutline,
                        title = "Aide & Foire aux questions (FAQ)",
                        onClick = onNavigateToHelp,
                        testTag = "menu_help_support"
                    )
                    ProfileMenuRowItem(
                        icon = Icons.Default.Policy,
                        title = "Politique de confidentialité",
                        onClick = onNavigateToPrivacy,
                        testTag = "menu_privacy_policy"
                    )
                    ProfileMenuRowItem(
                        icon = Icons.Default.Description,
                        title = "Conditions d'utilisation (CGU)",
                        onClick = onNavigateToTerms,
                        testTag = "menu_terms_of_service"
                    )
                    ProfileMenuRowItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Supprimer définitivement mon compte",
                        titleColor = Color(0xFFFDA4AF),
                        iconTint = Color(0xFFFDA4AF),
                        onClick = onNavigateToDeleteAccount,
                        testTag = "menu_delete_account"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    viewModel.logout {
                        onLogoutSuccess()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFDA4AF)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B1D2F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_logout")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFFDA4AF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Déconnexion",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFDA4AF)
                    )
                )
            }
        }
    }
}

@Composable
fun ProfileMenuRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: Color = Color.White,
    iconTint: Color = VideoCashGoldAccent,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = titleColor
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFF8C82A8),
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun StatColumn(count: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFA59CBF)
            )
        )
    }
}
