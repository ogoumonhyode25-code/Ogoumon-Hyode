package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Comment
import com.example.data.repository.CommentRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommentsUiState(
    val videoId: String = "",
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val inputText: String = "",
    val errorMessage: String? = null
)

class CommentsViewModel(
    private val commentRepository: CommentRepository = CommentRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    val currentUserId: String
        get() = FirebaseManager.auth?.currentUser?.uid.orEmpty()

    fun loadCommentsForVideo(videoId: String) {
        if (_uiState.value.videoId == videoId && observeJob?.isActive == true) return

        observeJob?.cancel()
        _uiState.update { it.copy(videoId = videoId, isLoading = true, errorMessage = null) }

        observeJob = viewModelScope.launch {
            commentRepository.observeComments(videoId).collect { list ->
                _uiState.update { it.copy(comments = list, isLoading = false) }
            }
        }
    }

    fun onInputTextChange(text: String) {
        if (text.length <= 250) {
            _uiState.update { it.copy(inputText = text) }
        }
    }

    fun sendComment() {
        val text = _uiState.value.inputText.trim()
        val videoId = _uiState.value.videoId
        val uid = currentUserId

        if (text.isBlank() || videoId.isBlank() || uid.isBlank()) return

        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        viewModelScope.launch {
            val user = userRepository.getUserProfile(uid).getOrNull()
            val username = user?.username ?: FirebaseManager.auth?.currentUser?.displayName ?: "Utilisateur"
            val photoUrl = user?.photoUrl ?: FirebaseManager.auth?.currentUser?.photoUrl?.toString().orEmpty()

            val result = commentRepository.addComment(
                videoId = videoId,
                userId = uid,
                username = username,
                userPhotoUrl = photoUrl,
                text = text
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSending = false, inputText = "") }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.message ?: "Échec de l'envoi du commentaire"
                        )
                    }
                }
            )
        }
    }

    fun deleteComment(commentId: String) {
        val videoId = _uiState.value.videoId
        viewModelScope.launch {
            commentRepository.deleteComment(commentId, videoId)
        }
    }

    fun flagComment(commentId: String) {
        viewModelScope.launch {
            commentRepository.flagComment(commentId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
