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
fun TermsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag("terms_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Conditions d'Utilisation",
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
                    text = "Conditions Générales d'Utilisation",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Application VidéoCash • Édition Septembre 2026",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = VideoCashGoldAccent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                LegalSectionCard(
                    title = "1. Objet et Adhésion",
                    content = "VidéoCash est une plateforme communautaire de partage et de visionnage de vidéos courtes. En créant un compte, vous acceptez sans réserve les présentes Conditions Générales d'Utilisation."
                )

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionCard(
                    title = "2. Règles Relatives aux Contenus",
                    content = "Sont strictement prohibés sur VidéoCash :\n" +
                            "• Tout contenu violent, haineux, diffamatoire ou menaçant.\n" +
                            "• La pornographie, la nudité explicite ou les contenus à caractère sexuel non sollicités.\n" +
                            "• La diffusion d'œuvres protégées par des droits d'auteur sans autorisation légale.\n" +
                            "• Les arnaques, le phishing, ou toute forme de promotion trompeuse.\n" +
                            "Tout contenu contrevenant fera l'objet d'une suppression immédiate et pourra entraîner la suspension irrévocable du compte de son auteur."
                )

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionCard(
                    title = "3. Programme de Récompenses Communautaires",
                    content = "• VidéoCash propose un mécanisme d'encouragement aux spectateurs et créateurs loyaux en FCFA.\n" +
                            "• MENTION DE TRANSPARENCE : VidéoCash ne promet aucun gain financier systématique ou garanti. Les récompenses dépendent strictement du respect de la durée minimale de visionnage (15 secondes réelles), de l'absence de comportement automatisé et des fonds disponibles du programme.\n" +
                            "• Toute tentative de fraude (bots, scripts, visionnages simultanés, création de multiples faux comptes) entraînera la confiscation des avoirs et le bannissement du contrevenant."
                )

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionCard(
                    title = "4. Modalités de Retrait Mobile Money",
                    content = "• Les retraits en FCFA sont soumis à un seuil minimum clairement indiqué sur l'écran du portefeuille (ex. 1 000 FCFA).\n" +
                            "• Les demandes sont vérifiées par notre système et nos équipes administratives avant validation définitive et exécution vers votre compte Mobile Money (Wave, Orange Money, MTN MoMo).\n" +
                            "• L'utilisateur est seul responsable de l'exactitude du numéro de téléphone et de l'opérateur sélectionnés."
                )

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionCard(
                    title = "5. Modification des Conditions",
                    content = "VidéoCash se réserve le droit d'ajuster les règles du programme, les plafonds journaliers et les barèmes de rétribution afin d'assurer l'équilibre économique et la pérennité du service."
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
