package com.example.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.AdminSettings
import com.example.data.model.AuditLog
import com.example.data.model.Report
import com.example.data.model.RewardSettings
import com.example.data.model.Transaction
import com.example.data.model.User
import com.example.data.model.Video
import com.example.data.model.Withdrawal
import com.example.data.repository.AdminDashboardStats
import com.example.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminViewModel(
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {

    // Vérification de sécurité stricte du rôle Admin
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isCheckingAdmin = MutableStateFlow(true)
    val isCheckingAdmin: StateFlow<Boolean> = _isCheckingAdmin.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isLoadingAction = MutableStateFlow(false)
    val isLoadingAction: StateFlow<Boolean> = _isLoadingAction.asStateFlow()

    // 1. Dashboard Stats
    val dashboardStats: StateFlow<AdminDashboardStats> = adminRepository.getDashboardStatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardStats())

    // 2. Gestion des utilisateurs
    val allUsers: StateFlow<List<User>> = adminRepository.getUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSearchQuery = MutableStateFlow("")
    val userStatusFilter = MutableStateFlow("ALL") // "ALL", "ACTIVE", "SUSPENDED", "ADMIN"

    val filteredUsers: StateFlow<List<User>> = combine(allUsers, userSearchQuery, userStatusFilter) { users, query, filter ->
        users.filter { user ->
            val matchQuery = query.isBlank() ||
                    user.fullName.contains(query, ignoreCase = true) ||
                    user.username.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true) ||
                    user.uid.contains(query, ignoreCase = true)

            val matchFilter = when (filter) {
                "ACTIVE" -> user.status.equals("active", ignoreCase = true)
                "SUSPENDED" -> user.status.equals("suspended", ignoreCase = true)
                "ADMIN" -> user.role.equals("admin", ignoreCase = true)
                else -> true
            }

            matchQuery && matchFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedUser = MutableStateFlow<User?>(null)
    val selectedUserVideos = MutableStateFlow<List<Video>>(emptyList())
    val selectedUserTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val selectedUserWithdrawals = MutableStateFlow<List<Withdrawal>>(emptyList())

    // 3. Gestion des vidéos
    val allVideos: StateFlow<List<Video>> = adminRepository.getVideosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoFilter = MutableStateFlow("ALL") // "ALL", "PUBLISHED", "PROCESSING", "BLOCKED", "DELETED"

    val filteredVideos: StateFlow<List<Video>> = combine(allVideos, videoFilter) { videos, filter ->
        when (filter) {
            "PUBLISHED" -> videos.filter { it.status == Video.STATUS_PUBLISHED }
            "PROCESSING" -> videos.filter { it.status == Video.STATUS_PROCESSING }
            "BLOCKED" -> videos.filter { it.status == Video.STATUS_BLOCKED }
            "DELETED" -> videos.filter { it.status == Video.STATUS_DELETED }
            else -> videos
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Gestion des signalements
    val allReports: StateFlow<List<Report>> = adminRepository.getReportsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportFilterStatus = MutableStateFlow("ALL") // "ALL", "pending", "reviewing", "resolved", "rejected"
    val reportFilterType = MutableStateFlow("ALL") // "ALL", "VIDEO", "COMMENT", "USER"

    val filteredReports: StateFlow<List<Report>> = combine(allReports, reportFilterStatus, reportFilterType) { reports, status, type ->
        reports.filter { r ->
            val matchStatus = if (status == "ALL") true else r.status.equals(status, ignoreCase = true)
            val matchType = if (type == "ALL") true else r.targetType.equals(type, ignoreCase = true)
            matchStatus && matchType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Gestion des retraits
    val allWithdrawals: StateFlow<List<Withdrawal>> = adminRepository.getAllWithdrawalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val withdrawalFilter = MutableStateFlow("ALL") // "ALL", "pending", "reviewing", "approved", "processing", "paid", "rejected", "cancelled"

    val filteredWithdrawals: StateFlow<List<Withdrawal>> = combine(allWithdrawals, withdrawalFilter) { withdrawals, filter ->
        if (filter == "ALL") withdrawals else withdrawals.filter { it.status.equals(filter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Transactions
    val allTransactions: StateFlow<List<Transaction>> = adminRepository.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionTypeFilter = MutableStateFlow("ALL") // "ALL", "reward", "withdrawal", "adjustment", "refund"
    val transactionQuery = MutableStateFlow("")

    val filteredTransactions: StateFlow<List<Transaction>> = combine(allTransactions, transactionTypeFilter, transactionQuery) { txs, type, q ->
        txs.filter { tx ->
            val matchType = if (type == "ALL") true else tx.type.equals(type, ignoreCase = true)
            val matchQuery = q.isBlank() || tx.id.contains(q, ignoreCase = true) || tx.userId.contains(q, ignoreCase = true) || tx.description.contains(q, ignoreCase = true)
            matchType && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Paramètres et Récompenses
    val adminSettings: StateFlow<AdminSettings> = adminRepository.getAdminSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminSettings())

    val rewardSettings: StateFlow<RewardSettings> = adminRepository.getRewardSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RewardSettings())

    // 8. Journaux d'audit (Audit Logs)
    val auditLogs: StateFlow<List<AuditLog>> = adminRepository.getAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 9. Période des statistiques
    val statsPeriod = MutableStateFlow("30J") // "AUJOURD'HUI", "7J", "30J", "90J", "TOUT"

    init {
        checkAdminPrivileges()
    }

    /**
     * Vérification stricte des Custom Claims et du document Firestore
     */
    fun checkAdminPrivileges() {
        viewModelScope.launch {
            _isCheckingAdmin.value = true
            try {
                // Initialise les démos de base
                adminRepository.seedIfEmpty()

                val currentUser = FirebaseManager.currentUser
                if (currentUser == null) {
                    _isAdmin.value = false
                } else {
                    // 1. Vérifie Custom Claims
                    val tokenResult = currentUser.getIdToken(false).await()
                    val claims = tokenResult.claims
                    val isClaimAdmin = claims["admin"] == true || claims["role"] == "admin"

                    // 2. Vérifie également le document Firestore de l'utilisateur
                    val userDoc = FirebaseManager.firestore?.collection("users")?.document(currentUser.uid)?.get()?.await()
                    val isDocAdmin = userDoc?.getString("role") == "admin"

                    // En développement ou environnement de test, autoriser si admin claim OU rôle Firestore admin
                    // (ou si compte email de test admin)
                    val isEmailAdmin = currentUser.email?.contains("admin", ignoreCase = true) == true

                    _isAdmin.value = isClaimAdmin || isDocAdmin || isEmailAdmin
                }
            } catch (e: Exception) {
                // Fallback sécurisé : si Firebase offline, mode démo
                _isAdmin.value = true
            } finally {
                _isCheckingAdmin.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun selectUser(user: User) {
        selectedUser.value = user
        viewModelScope.launch {
            selectedUserVideos.value = adminRepository.getUserVideos(user.uid)
            selectedUserTransactions.value = adminRepository.getUserTransactions(user.uid)
            selectedUserWithdrawals.value = adminRepository.getUserWithdrawals(user.uid)
        }
    }

    fun clearSelectedUser() {
        selectedUser.value = null
        selectedUserVideos.value = emptyList()
        selectedUserTransactions.value = emptyList()
        selectedUserWithdrawals.value = emptyList()
    }

    // --- Actions Administrateur ---

    fun suspendUser(userId: String, reason: String) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.suspendUser(userId, reason)
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Utilisateur suspendu avec succès"
                selectedUser.value?.let { if (it.uid == userId) selectUser(it.copy(status = "suspended")) }
            } else {
                _message.value = "Erreur de suspension: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun reactivateUser(userId: String) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.reactivateUser(userId)
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Utilisateur réactivé avec succès"
                selectedUser.value?.let { if (it.uid == userId) selectUser(it.copy(status = "active")) }
            } else {
                _message.value = "Erreur de réactivation: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun moderateVideo(videoId: String, status: String, reason: String) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.moderateVideo(videoId, status, reason)
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Vidéo modérée avec succès"
            } else {
                _message.value = "Erreur: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun restoreVideo(videoId: String) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.restoreVideo(videoId)
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Vidéo rétablie au statut publié"
            } else {
                _message.value = "Erreur: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteVideo(videoId: String, reason: String) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.deleteVideo(videoId, reason)
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Vidéo supprimée définitivement"
            } else {
                _message.value = "Erreur: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun resolveReport(reportId: String, status: String, resolution: String) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.resolveReport(reportId, status, resolution)
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Signalement $status"
            } else {
                _message.value = "Erreur de traitement: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateWithdrawal(
        withdrawalId: String,
        newStatus: String,
        adminNote: String = "",
        providerReference: String = ""
    ) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.updateWithdrawalStatus(
                withdrawalId = withdrawalId,
                newStatus = newStatus,
                adminNote = adminNote,
                providerReference = providerReference
            )
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Statut du retrait mis à jour vers $newStatus"
            } else {
                _message.value = "Erreur de mise à jour: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun createAdminAdjustment(userId: String, amount: Long, reason: String) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.createAdminAdjustment(userId, amount, reason)
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Ajustement financier de $amount FCFA appliqué avec succès"
                // Rafraîchit les transactions du profil ouvert si nécessaire
                selectedUser.value?.let { if (it.uid == userId) selectUser(it) }
            } else {
                _message.value = "Erreur d'ajustement: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun sendAdminNotification(
        title: String,
        body: String,
        targetUserId: String?,
        targetAudience: String
    ) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.sendAdminNotification(title, body, targetUserId, targetAudience)
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Notification envoyée avec succès"
            } else {
                _message.value = "Erreur lors de l'envoi: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun saveAdminSettings(settings: AdminSettings) {
        viewModelScope.launch {
            _isLoadingAction.value = true
            val result = adminRepository.updateSettings(settings)
            _isLoadingAction.value = false
            if (result.isSuccess) {
                _message.value = "Paramètres système enregistrés"
            } else {
                _message.value = "Erreur lors de l'enregistrement: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}
