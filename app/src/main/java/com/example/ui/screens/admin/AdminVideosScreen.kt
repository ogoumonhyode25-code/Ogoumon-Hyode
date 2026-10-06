package com.example.ui.screens.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Video
import com.example.ui.theme.VideoCashEmerald
import com.example.ui.theme.VideoCashPurplePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminVideosScreen(
    viewModel: AdminViewModel,
    onViewReportsForVideo: ((String) -> Unit)? = null
) {
    val videos by viewModel.filteredVideos.collectAsStateWithLifecycle()
    val currentFilter by viewModel.videoFilter.collectAsStateWithLifecycle()

    var videoToModerate by remember { mutableStateOf<Video?>(null) }
    var moderationReason by remember { mutableStateOf("") }
    var videoToDelete by remember { mutableStateOf<Video?>(null) }
    var deleteReason by remember { mutableStateOf("") }
    var previewVideo by remember { mutableStateOf<Video?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_videos_screen")
    ) {
        Text(
            text = "Gestion des Vidéos",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = "${videos.size} vidéo(s) affichée(s)",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filtres
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filterList = listOf(
                "ALL" to "Toutes",
                "PUBLISHED" to "Publiées",
                "BLOCKED" to "Masquées / Bloquées",
                "DELETED" to "Supprimées"
            )
            items(filterList) { (key, label) ->
                val selected = currentFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.videoFilter.value = key },
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

        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucune vidéo trouvée pour ce filtre.",
                    color = Color(0xFF9E95B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    AdminVideoCard(
                        video = video,
                        onPreview = { previewVideo = video },
                        onModerate = { videoToModerate = video },
                        onRestore = { viewModel.restoreVideo(video.id) },
                        onDelete = { videoToDelete = video },
                        onViewReports = { onViewReportsForVideo?.invoke(video.id) }
                    )
                }
            }
        }
    }

    // Dialogue pour modérer (masquer)
    videoToModerate?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToModerate = null; moderationReason = "" },
            title = { Text("Masquer la vidéo", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Voulez-vous masquer cette vidéo du flux public ?", color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = moderationReason,
                        onValueChange = { moderationReason = it },
                        label = { Text("Motif de modération") },
                        placeholder = { Text("Ex: Propos injurieux, copyright...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moderateVideo(video.id, Video.STATUS_BLOCKED, moderationReason)
                        videoToModerate = null
                        moderationReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Masquer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToModerate = null; moderationReason = "" }) {
                    Text("Annuler", color = Color(0xFF9E95B8))
                }
            },
            containerColor = Color(0xFF191136)
        )
    }

    // Dialogue pour supprimer définitivement
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null; deleteReason = "" },
            title = { Text("Supprimer la vidéo", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Cette action marquera la vidéo comme supprimée définitivement.", color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Motif de suppression") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVideo(video.id, deleteReason)
                        videoToDelete = null
                        deleteReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null; deleteReason = "" }) {
                    Text("Annuler", color = Color(0xFF9E95B8))
                }
            },
            containerColor = Color(0xFF191136)
        )
    }

    // Modal de prévisualisation de la vidéo
    previewVideo?.let { v ->
        AlertDialog(
            onDismissRequest = { previewVideo = null },
            title = { Text("Aperçu de la vidéo", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Titre / Description :", color = Color(0xFF9E95B8), fontSize = 12.sp)
                    Text(v.description.ifBlank { "Aucune description" }, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Auteur : @${v.username} (${v.userId})", color = Color.White, fontSize = 12.sp)
                    Text("Catégorie : ${v.category}", color = Color.White, fontSize = 12.sp)
                    Text("Statut : ${v.status.uppercase()}", color = VideoCashEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Stats : ${v.viewsCount} vues • ${v.likesCount} likes • ${v.commentsCount} com.", color = Color(0xFF9E95B8), fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { previewVideo = null }, colors = ButtonDefaults.buttonColors(containerColor = VideoCashPurplePrimary)) {
                    Text("Fermer")
                }
            },
            containerColor = Color(0xFF191136)
        )
    }
}

@Composable
fun AdminVideoCard(
    video: Video,
    onPreview: () -> Unit,
    onModerate: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onViewReports: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.FRENCH) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF281F47)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_video_card_${video.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Miniature vidéo
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 96.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF281F47))
                        .clickable { onPreview() },
                    contentAlignment = Alignment.Center
                ) {
                    if (video.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Aperçu",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
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
                                text = video.category,
                                color = Color(0xFFD8B4FE),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Badge Statut
                        val statusColor = when (video.status) {
                            Video.STATUS_PUBLISHED -> VideoCashEmerald
                            Video.STATUS_BLOCKED -> Color(0xFFF59E0B)
                            Video.STATUS_DELETED -> Color(0xFFF43F5E)
                            else -> Color(0xFF9E95B8)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = video.status.uppercase(),
                                color = statusColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = video.description.ifBlank { "Sans description" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Par @${video.username} • ${dateFormat.format(Date(video.createdAt))}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E95B8), fontSize = 11.sp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Métriques : Vues, Likes, Commentaires
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF9E95B8), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${video.viewsCount}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${video.likesCount}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Boutons d'actions de modération
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Détails", color = Color.White, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (video.status == Video.STATUS_BLOCKED || video.status == Video.STATUS_DELETED) {
                    Button(
                        onClick = onRestore,
                        colors = ButtonDefaults.buttonColors(containerColor = VideoCashEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Restaurer", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onModerate,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Masquer", color = Color(0xFFF59E0B), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (video.status != Video.STATUS_DELETED) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
