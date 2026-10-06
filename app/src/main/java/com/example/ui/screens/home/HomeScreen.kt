package com.example.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.Video
import com.example.ui.components.VerticalVideoPlayer
import com.example.util.ShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: (userId: String) -> Unit = {},
    onNavigateToPublish: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeCommentVideoId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Gestion des erreurs
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            homeViewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.videos.isEmpty()) {
            // État vide
            EmptyFeedView(
                selectedCategory = uiState.selectedCategory,
                onPublishClick = onNavigateToPublish,
                onRefresh = { homeViewModel.refreshFeed() }
            )
        } else {
            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { uiState.videos.size }
            )

            // Détection du changement de page pour le compteur de vue (seuil 3s)
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    if (page in uiState.videos.indices) {
                        val video = uiState.videos[page]
                        homeViewModel.onVideoVisible(video.id)
                    }
                    // Pagination automatique vers le bas
                    if (page >= uiState.videos.size - 3) {
                        homeViewModel.loadMoreVideos()
                    }
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    homeViewModel.onVideoHidden()
                }
            }

            // Lecteur vertical avec défilement
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().testTag("home_vertical_pager")
            ) { page ->
                val video = uiState.videos[page]
                val isCurrentPage = pagerState.currentPage == page

                VideoPageItem(
                    video = video,
                    isCurrentPage = isCurrentPage,
                    isMuted = uiState.isMuted,
                    isLiked = uiState.likedVideoIds.contains(video.id),
                    isFollowing = uiState.followedUserIds.contains(video.userId),
                    isCurrentUser = video.userId == homeViewModel.currentUserId,
                    onToggleLike = { homeViewModel.toggleLike(video) },
                    onToggleFollow = { homeViewModel.toggleFollow(video.userId) },
                    onOpenComments = { activeCommentVideoId = video.id },
                    onShare = { ShareHelper.shareVideo(context, video) },
                    onProfileClick = { onNavigateToProfile(video.userId) }
                )
            }
        }

        // Overlay Supérieur : Sélecteur de catégories & Bouton Son
        TopOverlayHeader(
            selectedCategory = uiState.selectedCategory,
            isMuted = uiState.isMuted,
            onSelectCategory = { homeViewModel.selectCategory(it) },
            onToggleMute = { homeViewModel.toggleMute() }
        )

        // Feuille de commentaires
        if (activeCommentVideoId != null) {
            CommentsSheet(
                videoId = activeCommentVideoId!!,
                sheetState = sheetState,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        activeCommentVideoId = null
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun VideoPageItem(
    video: Video,
    isCurrentPage: Boolean,
    isMuted: Boolean,
    isLiked: Boolean,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    onToggleLike: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onProfileClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Lecteur ExoPlayer
        VerticalVideoPlayer(
            videoUrl = video.videoUrl,
            isPlaying = isCurrentPage,
            isMuted = isMuted,
            modifier = Modifier.fillMaxSize()
        )

        // Dégradé sombre en bas pour garantir une lisibilité optimale des textes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Actions latérales à droite (Profil, Like, Commentaire, Partage, Vues)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar Créateur avec bouton Suivre
            CreatorAvatarWithFollow(
                username = video.username,
                photoUrl = video.userPhotoUrl,
                isFollowing = isFollowing,
                isCurrentUser = isCurrentUser,
                onProfileClick = onProfileClick,
                onToggleFollow = onToggleFollow
            )

            // Bouton Like
            val likeScale by animateFloatAsState(
                targetValue = if (isLiked) 1.2f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "likeScale"
            )
            val likeColor by animateColorAsState(
                targetValue = if (isLiked) Color(0xFFFF2D55) else Color.White,
                label = "likeColor"
            )

            ActionButton(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = video.formattedLikes,
                iconColor = likeColor,
                modifier = Modifier.scale(likeScale),
                testTag = "like_button_${video.id}",
                onClick = onToggleLike
            )

            // Bouton Commentaires
            ActionButton(
                icon = Icons.Default.Comment,
                label = video.formattedComments,
                testTag = "comment_button_${video.id}",
                onClick = onOpenComments
            )

            // Bouton Partager
            ActionButton(
                icon = Icons.Default.Share,
                label = "Partager",
                testTag = "share_button_${video.id}",
                onClick = onShare
            )

            // Indicateur de vues
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Nombre de vues",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = video.formattedViews,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Infos de la vidéo en bas à gauche (Créateur, Description, Hashtags, Catégorie)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            // Catégorie Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = video.category,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Nom du créateur
            Text(
                text = "@${video.username}",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onProfileClick() }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            var isExpanded by remember { mutableStateOf(false) }
            Text(
                text = video.description,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                maxLines = if (isExpanded) 10 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )

            // Hashtags
            if (video.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.hashtags.joinToString(" "),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CreatorAvatarWithFollow(
    username: String,
    photoUrl: String,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    onProfileClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Box(
        modifier = Modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        // Photo de profil
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = username,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Badge Follow (+)
        if (!isCurrentUser) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(
                        if (isFollowing) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                    .clickable { onToggleFollow() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (isFollowing) "Abonné" else "Suivre",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TopOverlayHeader(
    selectedCategory: String,
    isMuted: Boolean,
    onSelectCategory: (String) -> Unit,
    onToggleMute: () -> Unit
) {
    val categories = listOf("Tous") + Video.CATEGORIES

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barre de défilement horizontal des catégories
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.4f)
                        )
                        .clickable { onSelectCategory(category) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = category,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Bouton Muet / Son
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Activer le son" else "Couper le son",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyFeedView(
    selectedCategory: String,
    onPublishClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Aucune vidéo pour « $selectedCategory »",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Soyez le premier créateur à publier du contenu dans cette catégorie !",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(
            onClick = onPublishClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Publier une vidéo")
        }
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.material3.TextButton(
            onClick = onRefresh
        ) {
            Text("Rafraîchir", color = Color.White.copy(alpha = 0.8f))
        }
    }
}
