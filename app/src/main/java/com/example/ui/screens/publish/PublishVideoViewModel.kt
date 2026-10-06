package com.example.ui.screens.publish

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Video
import com.example.data.repository.UserRepository
import com.example.data.repository.VideoRepository
import com.example.util.MediaHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PublishUiState {
    data object Idle : PublishUiState
    data class Uploading(val progress: Int) : PublishUiState
    data class Success(val video: Video) : PublishUiState
    data class Error(val message: String) : PublishUiState
}

data class PublishFormState(
    val videoUri: Uri? = null,
    val videoSizeMb: Double = 0.0,
    val thumbnailBitmap: Bitmap? = null,
    val customThumbnailUri: Uri? = null,
    val description: String = "",
    val hashtagInput: String = "",
    val hashtags: List<String> = emptyList(),
    val category: String = Video.CATEGORIES.first(),
    val isFormValid: Boolean = false
)

class PublishVideoViewModel(
    private val videoRepository: VideoRepository = VideoRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _publishState = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val publishState: StateFlow<PublishUiState> = _publishState.asStateFlow()

    private val _formState = MutableStateFlow(PublishFormState())
    val formState: StateFlow<PublishFormState> = _formState.asStateFlow()

    fun onVideoSelected(context: Context, uri: Uri) {
        val sizeBytes = MediaHelper.getFileSize(context, uri)
        val sizeMb = sizeBytes / (1024.0 * 1024.0)

        if (sizeBytes > MediaHelper.MAX_VIDEO_SIZE_BYTES) {
            _publishState.value = PublishUiState.Error("La vidéo dépasse la taille maximale autorisée de 100 Mo.")
            return
        }

        val thumbnail = MediaHelper.extractVideoThumbnail(context, uri)

        _formState.update {
            it.copy(
                videoUri = uri,
                videoSizeMb = sizeMb,
                thumbnailBitmap = thumbnail
            )
        }
        validateForm()
    }

    fun onCustomThumbnailSelected(uri: Uri) {
        _formState.update { it.copy(customThumbnailUri = uri) }
    }

    fun onDescriptionChange(text: String) {
        if (text.length <= 300) {
            _formState.update { it.copy(description = text) }
            validateForm()
        }
    }

    fun onHashtagInputChange(text: String) {
        _formState.update { it.copy(hashtagInput = text) }
    }

    fun addHashtag() {
        val raw = _formState.value.hashtagInput.trim()
        if (raw.isBlank()) return

        val tag = if (raw.startsWith("#")) raw else "#$raw"
        if (!_formState.value.hashtags.contains(tag) && _formState.value.hashtags.size < 8) {
            _formState.update {
                it.copy(
                    hashtags = it.hashtags + tag,
                    hashtagInput = ""
                )
            }
        }
    }

    fun removeHashtag(tag: String) {
        _formState.update {
            it.copy(hashtags = it.hashtags - tag)
        }
    }

    fun onCategoryChange(category: String) {
        _formState.update { it.copy(category = category) }
    }

    private fun validateForm() {
        val current = _formState.value
        val isValid = current.videoUri != null && current.description.isNotBlank()
        _formState.update { it.copy(isFormValid = isValid) }
    }

    fun publishVideo(context: Context) {
        val current = _formState.value
        val videoUri = current.videoUri
        if (videoUri == null || current.description.isBlank()) {
            _publishState.value = PublishUiState.Error("Veuillez sélectionner une vidéo et saisir une description.")
            return
        }

        val currentUser = FirebaseManager.auth?.currentUser
        if (currentUser == null) {
            _publishState.value = PublishUiState.Error("Veuillez vous connecter pour publier.")
            return
        }

        _publishState.value = PublishUiState.Uploading(progress = 0)

        viewModelScope.launch {
            // Récupérer le nom et la photo actuels de l'utilisateur
            val userProfileResult = userRepository.getUserProfile(currentUser.uid)
            val username = userProfileResult.getOrNull()?.username
                ?: currentUser.displayName
                ?: "créateur"
            val photoUrl = userProfileResult.getOrNull()?.photoUrl
                ?: currentUser.photoUrl?.toString().orEmpty()

            val result = videoRepository.publishVideo(
                context = context,
                userId = currentUser.uid,
                username = username,
                userPhotoUrl = photoUrl,
                videoUri = videoUri,
                customThumbnailUri = current.customThumbnailUri,
                description = current.description,
                hashtags = current.hashtags,
                category = current.category,
                onProgress = { percent ->
                    _publishState.value = PublishUiState.Uploading(progress = percent)
                }
            )

            result.fold(
                onSuccess = { video ->
                    _publishState.value = PublishUiState.Success(video)
                    resetForm()
                },
                onFailure = { error ->
                    _publishState.value = PublishUiState.Error(
                        error.message ?: "Échec de la publication de la vidéo"
                    )
                }
            )
        }
    }

    fun resetState() {
        _publishState.value = PublishUiState.Idle
    }

    private fun resetForm() {
        _formState.value = PublishFormState()
    }
}
