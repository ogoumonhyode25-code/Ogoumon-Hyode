package com.example.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Withdrawal
import com.example.data.repository.WithdrawalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface WithdrawalHistoryUiState {
    object Loading : WithdrawalHistoryUiState
    data class Success(val withdrawals: List<Withdrawal>) : WithdrawalHistoryUiState
    data class Error(val message: String) : WithdrawalHistoryUiState
}

class WithdrawalHistoryViewModel(
    private val withdrawalRepository: WithdrawalRepository = WithdrawalRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WithdrawalHistoryUiState>(WithdrawalHistoryUiState.Loading)
    val uiState: StateFlow<WithdrawalHistoryUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadWithdrawals()
    }

    fun loadWithdrawals() {
        viewModelScope.launch {
            _uiState.value = WithdrawalHistoryUiState.Loading
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"

            withdrawalRepository.getWithdrawalsFlow(userId)
                .catch { e ->
                    _uiState.value = WithdrawalHistoryUiState.Error("Impossible de charger l'historique: ${e.message}")
                }
                .collect { list ->
                    _uiState.value = WithdrawalHistoryUiState.Success(list)
                }
        }
    }

    fun cancelWithdrawal(withdrawalId: String) {
        viewModelScope.launch {
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"
            val result = withdrawalRepository.cancelWithdrawal(userId, withdrawalId)
            result.onSuccess {
                _message.value = "Demande de retrait annulée. Le montant a été restitué à votre solde."
            }.onFailure { err ->
                _message.value = "Erreur: ${err.message}"
            }
        }
    }

    /**
     * Pour les tests en simulation : permet de simuler l'approbation, le refus ou le paiement
     */
    fun simulateStatusChange(withdrawalId: String, newStatus: String) {
        viewModelScope.launch {
            val result = withdrawalRepository.simulateAdminAction(
                withdrawalId = withdrawalId,
                newStatus = newStatus,
                adminNote = when (newStatus) {
                    Withdrawal.STATUS_PAID -> "Paiement simulé validé avec succès (Wave/Orange/MTN)"
                    Withdrawal.STATUS_REJECTED -> "Refus simulation : coordonnées incorrectes"
                    Withdrawal.STATUS_APPROVED -> "Demande approuvée par l'administrateur"
                    else -> "Mise à jour d'état"
                }
            )
            result.onSuccess {
                _message.value = "Statut simulé mis à jour: $newStatus"
            }.onFailure { err ->
                _message.value = "Erreur simulation: ${err.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
