package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary
import com.example.ui.theme.VideoCashPurpleSecondary

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val navState by viewModel.navigationState.collectAsStateWithLifecycle()

    LaunchedEffect(navState) {
        if (navState is SplashNavigationState.Navigate) {
            val destination = (navState as SplashNavigationState.Navigate).destination
            onNavigate(destination)
        }
    }

    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen")
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090614),
                        Color(0xFF140D2B),
                        Color(0xFF07040E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Logo Container with Ambient Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Outer Glow ring
                Box(
                    modifier = Modifier
                        .size(120.dp * glowPulse)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    VideoCashPurplePrimary.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Logo Graphic
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF1C133A),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .size(96.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    VideoCashGoldAccent,
                                    VideoCashPurplePrimary
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_videocash_logo),
                            contentDescription = "Logo VidéoCash",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(32.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Titre VidéoCash
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Vidéo",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "Cash",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = VideoCashGoldAccent
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badge de devise / slogan
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF221644),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = VideoCashPurpleSecondary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = VideoCashGoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Vidéos • Récompenses en FCFA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFFE0DAF5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Indicateur de chargement
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = VideoCashGoldAccent,
                strokeWidth = 2.5.dp,
                trackColor = Color(0xFF2B1F4F)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Vérification de la session...",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E95B8),
                textAlign = TextAlign.Center
            )
        }
    }
}
