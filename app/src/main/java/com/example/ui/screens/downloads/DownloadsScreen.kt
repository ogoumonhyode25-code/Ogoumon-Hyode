package com.example.ui.screens.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadedVideoItem
import com.example.ui.components.VerticalVideoPlayer
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary
import com.example.ui.theme.VideoCashPurpleSecondary
import com.example.util.MobileDownloadManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val downloadedList by MobileDownloadManager.downloadsList.collectAsState()
    val activeProgressMap by MobileDownloadManager.downloadProgressMap.collectAsState()

    var selectedVideoForPlayback by remember { mutableStateOf<DownloadedVideoItem?>(null) }
    var showConfirmClearAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Téléchargements Mobiles",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${downloadedList.size} vidéo(s) hors-ligne",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = VideoCashGoldAccent,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_downloads")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (downloadedList.isNotEmpty()) {
                        IconButton(
                            onClick = { showConfirmClearAll = true },
                            modifier = Modifier.testTag("btn_clear_all_downloads")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Tout supprimer",
                                tint = Color(0xFFFDA4AF)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0D081E)
                )
            )
        },
        containerColor = Color(0xFF070414)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (downloadedList.isEmpty() && activeProgressMap.isEmpty()) {
                // Vue vide
                EmptyDownloadsPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Carte d'informations / mode hors-ligne
                    item {
                        OfflineInfoBanner(downloadCount = downloadedList.size)
                    }

                    // Téléchargements en cours
                    if (activeProgressMap.isNotEmpty()) {
                        item {
                            Text(
                                text = "Téléchargements en cours",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VideoCashGoldAccent
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(activeProgressMap.entries.toList(), key = { it.key }) { entry ->
                            DownloadingItemCard(
                                videoId = entry.key,
                                progress = entry.value
                            )
                        }
                    }

                    // Liste des vidéos téléchargées
                    if (downloadedList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Disponibles hors-ligne",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(downloadedList, key = { it.videoId }) { item ->
                            DownloadedVideoCard(
                                item = item,
                                onPlayClick = {
                                    selectedVideoForPlayback = item
                                },
                                onDeleteClick = {
                                    MobileDownloadManager.deleteDownloadedVideo(context, item.videoId)
                                }
                            )
                        }
                    }
                }
            }

            // Lecteur modal plein écran pour vidéo téléchargée hors-ligne
            selectedVideoForPlayback?.let { videoItem ->
                OfflineVideoPlayerDialog(
                    videoItem = videoItem,
                    onDismiss = { selectedVideoForPlayback = null }
                )
            }

            // Dialogue de confirmation pour tout supprimer
            if (showConfirmClearAll) {
                AlertDialog(
                    onDismissRequest = { showConfirmClearAll = false },
                    title = {
                        Text(
                            text = "Supprimer tous les téléchargements ?",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "Cette action supprimera toutes les vidéos téléchargées de la mémoire de votre téléphone pour libérer de l'espace.",
                            color = Color(0xFFD1D5DB)
                        )
                    },
                    containerColor = Color(0xFF1E1438),
                    confirmButton = {
                        TextButton(
                            onClick = {
                                MobileDownloadManager.clearAllDownloads(context)
                                showConfirmClearAll = false
                            }
                        ) {
                            Text("Supprimer tout", color = Color(0xFFFDA4AF), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmClearAll = false }) {
                            Text("Annuler", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OfflineInfoBanner(downloadCount: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1238)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = VideoCashEmerald.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = VideoCashEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Mode Visionnage Hors-Ligne",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Lisez ces vidéos sans connexion internet ni consommation de données mobiles.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFA59DBF)
                    )
                )
            }
        }
    }
}

@Composable
private fun DownloadingItemCard(
    videoId: String,
    progress: Float
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF231647)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(42.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(36.dp),
                    color = VideoCashGoldAccent,
                    trackColor = Color(0xFF382963),
                    strokeWidth = 3.dp
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Téléchargement en cours...",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Progression : ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = VideoCashGoldAccent
                    )
                )
            }
        }
    }
}

@Composable
private fun DownloadedVideoCard(
    item: DownloadedVideoItem,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateStr = remember(item.downloadedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy à HH:mm", Locale.FRENCH)
        sdf.format(Date(item.downloadedAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF160E2E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .testTag("download_card_${item.videoId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature avec icône Play
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(VideoCashPurpleSecondary, VideoCashPurplePrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Lire la vidéo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Infos vidéo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@${item.username} • ${item.category}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = VideoCashGoldAccent,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF281F47)
                    ) {
                        Text(
                            text = item.formattedSize,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFE0E7FF),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF7E769B),
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Bouton supprimer de l'appareil
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("btn_delete_download_${item.videoId}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer de la mémoire",
                    tint = Color(0xFFFDA4AF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyDownloadsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E1438),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FolderOff,
                    contentDescription = null,
                    tint = VideoCashGoldAccent,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Aucune vidéo téléchargée",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Dans le fil d'actualité, cliquez sur l'icône de téléchargement sous chaque vidéo pour l'enregistrer sur votre mobile et la regarder à tout moment sans internet.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF9E95B8),
                fontSize = 13.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun OfflineVideoPlayerDialog(
    videoItem: DownloadedVideoItem,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Lecteur utilisant le chemin local du fichier vidéo
        VerticalVideoPlayer(
            videoUrl = videoItem.localFilePath,
            isPlaying = true,
            isMuted = false,
            modifier = Modifier.fillMaxSize()
        )

        // Bouton Fermer en haut à droite
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .testTag("btn_close_offline_player")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fermer le lecteur",
                tint = Color.White
            )
        }

        // Titre et info en bas
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp, top = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = VideoCashEmerald.copy(alpha = 0.9f)
            ) {
                Text(
                    text = "Lecture Hors-Ligne",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = videoItem.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = "@${videoItem.username} • Fichier local (${videoItem.formattedSize})",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = VideoCashGoldAccent
                )
            )
        }
    }
}
