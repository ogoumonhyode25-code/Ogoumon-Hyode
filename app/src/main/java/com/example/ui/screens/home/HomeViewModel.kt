package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Video
import com.example.data.repository.FollowRepository
import com.example.data.repository.LikeRepository
import com.example.data.repository.VideoRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val videos: List<Video> = emptyList(),
    val selectedCategory: String = "Tous",
    val likedVideoIds: Set<String> = emptySet(),
    val followedUserIds: Set<String> = emptySet(),
    val isMuted: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val videoRepository: VideoRepository = VideoRepository(),
    private val likeRepository: LikeRepository = LikeRepository(),
    private val followRepository: FollowRepository = FollowRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastVisibleSnapshot: DocumentSnapshot? = null
    private var hasMoreVideos = true
    private var isLoadingMore = false

    // Anti-spam pour les vues dans la session courante
    private val viewedVideosInSession = mutableSetOf<String>()
    private var viewTimerJob: Job? = null

    val currentUserId: String
        get() = FirebaseManager.auth?.currentUser?.uid.orEmpty()

    init {
        loadFeed(isRefresh = false)
    }

    fun selectCategory(category: String) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.update { it.copy(selectedCategory = category) }
        loadFeed(isRefresh = true)
    }

    fun refreshFeed() {
        loadFeed(isRefresh = true)
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun loadFeed(isRefresh: Boolean) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                lastVisibleSnapshot = null
                hasMoreVideos = true
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val category = if (_uiState.value.selectedCategory == "Tous") null else _uiState.value.selectedCategory
            val result = videoRepository.getFeedVideos(category = category, lastVisible = null)

            result.fold(
                onSuccess = { (fetchedVideos, newLastSnapshot) ->
                    lastVisibleSnapshot = newLastSnapshot
                    hasMoreVideos = fetchedVideos.isNotEmpty()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            videos = fetchedVideos
                        )
                    }

                    // Charger les statuts de Like et Follow pour les vidéos chargées
                    checkInteractionsForVideos(fetchedVideos)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "Impossible de charger les vidéos"
                        )
                    }
                }
            )
        }
    }

    fun loadMoreVideos() {
        if (isLoadingMore || !hasMoreVideos) return
        isLoadingMore = true

        viewModelScope.launch {
            val category = if (_uiState.value.selectedCategory == "Tous") null else _uiState.value.selectedCategory
            val result = videoRepository.getFeedVideos(category = category, lastVisible = lastVisibleSnapshot)

            result.fold(
                onSuccess = { (newVideos, newLastSnapshot) ->
                    lastVisibleSnapshot = newLastSnapshot
                    hasMoreVideos = newVideos.isNotEmpty()

                    _uiState.update { current ->
                        current.copy(videos = current.videos + newVideos)
                    }
                    checkInteractionsForVideos(newVideos)
                },
                onFailure = {
                    // Échec silencieux pour pagination fluide
                }
            )
            isLoadingMore = false
        }
    }

    private fun checkInteractionsForVideos(videos: List<Video>) {
        val uid = currentUserId
        if (uid.isBlank()) return

        viewModelScope.launch {
            val liked = _uiState.value.likedVideoIds.toMutableSet()
            val followed = _uiState.value.followedUserIds.toMutableSet()

            for (video in videos) {
                if (likeRepository.isVideoLiked(video.id, uid)) {
                    liked.add(video.id)
                }
                if (video.userId != uid && followRepository.isFollowing(uid, video.userId)) {
                    followed.add(video.userId)
                }
            }

            _uiState.update {
                it.copy(
                    likedVideoIds = liked,
                    followedUserIds = followed
                )
            }
        }
    }

    /**
     * Bascule le statut de Like d'une vidéo avec mise à jour optimiste.
     */
    fun toggleLike(video: Video) {
        val uid = currentUserId
        if (uid.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez vous connecter pour aimer une vidéo") }
            return
        }

        val wasLiked = _uiState.value.likedVideoIds.contains(video.id)
        val newLikedSet = if (wasLiked) {
            _uiState.value.likedVideoIds - video.id
        } else {
            _uiState.value.likedVideoIds + video.id
        }

        // Mise à jour optimiste
        _uiState.update { current ->
            current.copy(
                likedVideoIds = newLikedSet,
                videos = current.videos.map { v ->
                    if (v.id == video.id) {
                        val newCount = if (wasLiked) (v.likesCount - 1).coerceAtLeast(0) else v.likesCount + 1
                        v.copy(likesCount = newCount)
                    } else v
                }
            )
        }

        // Appel repository Firebase en arrière-plan
        viewModelScope.launch {
            val result = likeRepository.toggleLike(video.id, uid)
            if (result.isFailure) {
                // Revert en cas d'erreur
                _uiState.update { current ->
                    current.copy(
                        likedVideoIds = if (wasLiked) current.likedVideoIds + video.id else current.likedVideoIds - video.id,
                        videos = current.videos.map { v ->
                            if (v.id == video.id) video else v
                        },
                        errorMessage = "Échec de l'action de mention J'aime"
                    )
                }
            }
        }
    }

    /**
     * Bascule le statut de Follow du créateur.
     */
    fun toggleFollow(creatorId: String) {
        val uid = currentUserId
        if (uid.isBlank() || uid == creatorId) return

        val wasFollowing = _uiState.value.followedUserIds.contains(creatorId)
        val newFollowSet = if (wasFollowing) {
            _uiState.value.followedUserIds - creatorId
        } else {
            _uiState.value.followedUserIds + creatorId
        }

        _uiState.update { it.copy(followedUserIds = newFollowSet) }

        viewModelScope.launch {
            val result = followRepository.toggleFollow(uid, creatorId)
            if (result.isFailure) {
                // Revert
                _uiState.update { current ->
                    current.copy(
                        followedUserIds = if (wasFollowing) current.followedUserIds + creatorId else current.followedUserIds - creatorId,
                        errorMessage = "Échec du suivi de l'utilisateur"
                    )
                }
            }
        }
    }

    /**
     * Enregistre une vue uniquement après 3 secondes de visionnage effectif.
     */
    fun onVideoVisible(videoId: String) {
        viewTimerJob?.cancel()

        if (viewedVideosInSession.contains(videoId)) {
            // Déjà comptabilisé pour cette session
            return
        }

        viewTimerJob = viewModelScope.launch {
            delay(3000L) // Seuil antifraude 3 secondes de lecture continue
            viewedVideosInSession.add(videoId)
            videoRepository.incrementViewCount(videoId)

            // Mise à jour locale du compteur de vue
            _uiState.update { current ->
                current.copy(
                    videos = current.videos.map { v ->
                        if (v.id == videoId) v.copy(viewsCount = v.viewsCount + 1) else v
                    }
                )
            }
        }
    }

    fun onVideoHidden() {
        viewTimerJob?.cancel()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
