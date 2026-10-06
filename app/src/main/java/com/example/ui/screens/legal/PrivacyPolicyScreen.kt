package com.example.ui.screens.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VideoCashGoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag("privacy_policy_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Politique de Confidentialité",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090614))
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Politique de Protection des Données Personnelles",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Dernière mise à jour : 12 Septembre 2026",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = VideoCashGoldAccent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                LegalSectionCard(
                    title = "1. Données collectées",
                    content = "Pour faire fonctionner la plateforme VidéoCash, nous collectons les données strictement nécessaires lors de votre inscription et votre utilisation :\n" +
                            "• Identité de compte : Nom, prénom, nom d'utilisateur, adresse e-mail, numéro de téléphone.\n" +
                            "• Contenus publiés : Fichiers vidéo, titres, descriptions, hashtags et miniatures.\n" +
                            "• Données d'activité : Historique des visionnages qualifiés, mentions 'J'aime', commentaires déposés et abonnements.\n" +
                            "• Opérations de portefeuille : Historique des récompenses distribuées et demandes de retrait Mobile Money (numéro de bénéficiaire, opérateur choisi, identifiant de transaction)."
                )

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionCard(
                    title = "2. Finalités du traitement des données",
                    content = "Vos données personnelles sont traitées pour :\n" +
                            "• Assurer la diffusion et le partage de vos vidéos auprès de la communauté.\n" +
                            "• Détecter et bloquer les comportements frauduleux (robots, fermes de visionnage artificiel, multi-comptes).\n" +
                            "• Traiter loyalement les demandes de retrait Mobile Money (Wave, Orange Money, MTN MoMo).\n" +
                            "• Envoyer des notifications essentielles d'activité (statut des retraits, récompenses obtenues, sécurité)."
                )

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionCard(
                    title = "3. Sécurité & Stockage",
                    content = "L'ensemble des transferts de données et des flux financiers est chiffré selon les normes HTTPS/TLS les plus strictes. Les règles de sécurité Firebase Firestore et Storage interdisent tout accès non autorisé à vos informations de compte et à votre solde."
                )

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionCard(
                    title = "4. Suppression du compte & Droit d'accès",
                    content = "Conformément aux exigences de Google Play et aux lois sur la vie privée, vous disposez d'un droit inconditionnel de suppression de votre compte directement depuis l'application (Mon Profil > Paramètres & Sécurité > Supprimer mon compte).\n" +
                            "La suppression désactive immédiatement votre profil et supprime vos identifiants d'authentification. Seules les données de transactions comptables strictement obligatoires sont archivées pour satisfaire aux obligations légales de lutte contre la fraude."
                )

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionCard(
                    title = "5. Contact & Délégué aux données",
                    content = "Pour toute question relative à vos données personnelles ou pour exercer vos droits, vous pouvez contacter notre équipe à : support@videocash.app"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun LegalSectionCard(
    title: String,
    content: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF140D29)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFCCC5E3),
                    lineHeight = 22.sp
                )
            )
        }
    }
}
