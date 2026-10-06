package com.example.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.AppNotification
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    object Loading : NotificationsUiState
    data class Success(
        val notifications: List<AppNotification>,
        val unreadCount: Int,
        val activeFilter: String = "ALL"
    ) : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var currentFilter = "ALL"

    init {
        loadNotifications()
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        val current = _uiState.value
        if (current is NotificationsUiState.Success) {
            _uiState.value = current.copy(activeFilter = filter)
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"

            notificationRepository.getNotificationsFlow(userId)
                .catch { e ->
                    _uiState.value = NotificationsUiState.Error("Impossible de charger les notifications : ${e.message}")
                }
                .collect { list ->
                    val unread = list.count { !it.read }
                    _uiState.value = NotificationsUiState.Success(
                        notifications = list,
                        unreadCount = unread,
                        activeFilter = currentFilter
                    )
                }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"
            notificationRepository.markAllAsRead(userId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun simulateNotification(type: String) {
        viewModelScope.launch {
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"
            val (title, body) = when (type) {
                AppNotification.TYPE_WITHDRAWAL_CREATED ->
                    "Demande de retrait reçue" to "Votre demande de retrait de 5 000 FCFA via Wave a été reçue et est en cours d'examen."
                AppNotification.TYPE_WITHDRAWAL_APPROVED ->
                    "Retrait approuvé !" to "Votre demande de retrait de 5 000 FCFA a été validée par l'administrateur."
                AppNotification.TYPE_WITHDRAWAL_REJECTED ->
                    "Retrait refusé" to "Votre demande de retrait n'a pas pu aboutir. Les fonds ont été restitués à votre solde."
                AppNotification.TYPE_WITHDRAWAL_PAID ->
                    "Paiement envoyé !" to "Votre paiement de 5 000 FCFA a été confirmé avec succès par l'opérateur Mobile Money."
                AppNotification.TYPE_REWARD_APPROVED ->
                    "Gain validé !" to "Félicitations, +10 FCFA ont été crédités sur votre solde pour votre visionnage qualifié."
                AppNotification.TYPE_NEW_COMMENT ->
                    "Nouveau commentaire" to "Kouamé a commenté : « Superbe vidéo, continue comme ça ! »"
                AppNotification.TYPE_NEW_LIKE ->
                    "Nouveau J'aime" to "Aya et 3 autres personnes ont aimé votre vidéo."
                AppNotification.TYPE_ADMIN ->
                    "Message de l'administration" to "Maintenance programmée ce soir à minuit pour optimisation des serveurs."
                else ->
                    "Notification Système" to "Bienvenue sur VidéoCash ! Complétez votre profil pour plus de visibilité."
            }

            notificationRepository.createSimulatedNotification(
                userId = userId,
                title = title,
                body = body,
                type = type,
                referenceId = "SIM-REF-${System.currentTimeMillis() % 1000}"
            )
        }
    }
}
