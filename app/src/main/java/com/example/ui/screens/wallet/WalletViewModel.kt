package com.example.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.RewardSettings
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.data.repository.RewardRepository
import com.example.data.repository.TransactionRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface WalletUiState {
    object Loading : WalletUiState
    data class Success(
        val wallet: Wallet,
        val recentTransactions: List<Transaction>,
        val settings: RewardSettings,
        val isSimulationMode: Boolean = true
    ) : WalletUiState
    data class Error(val message: String) : WalletUiState
}

class WalletViewModel(
    private val walletRepository: WalletRepository = WalletRepository(),
    private val transactionRepository: TransactionRepository = TransactionRepository(),
    private val rewardRepository: RewardRepository = RewardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(true)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadWalletData()
    }

    fun loadWalletData() {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"

            // S'assurer que le wallet existe
            walletRepository.ensureWalletExists(userId)
            val settings = walletRepository.getRewardSettings()

            // Écoute combinée du wallet et des transactions récentes
            walletRepository.getWalletFlow(userId)
                .catch { e ->
                    _uiState.value = WalletUiState.Error("Impossible de charger le portefeuille: ${e.message}")
                }
                .collect { wallet ->
                    transactionRepository.getTransactionsFlow(userId)
                        .collect { transactions ->
                            _uiState.value = WalletUiState.Success(
                                wallet = wallet,
                                recentTransactions = transactions.take(5),
                                settings = settings,
                                isSimulationMode = _isSimulationMode.value
                            )
                        }
                }
        }
    }

    fun toggleSimulationMode() {
        _isSimulationMode.value = !_isSimulationMode.value
        val current = _uiState.value
        if (current is WalletUiState.Success) {
            _uiState.value = current.copy(isSimulationMode = _isSimulationMode.value)
        }
    }

    /**
     * Action de test rapide pour simuler le gain d'une récompense pour vue qualifiée
     */
    fun simulateRewardView() {
        viewModelScope.launch {
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"
            val dummyVideoId = "video_sample_${System.currentTimeMillis() % 100}"
            val result = rewardRepository.recordQualifiedView(
                userId = userId,
                videoId = dummyVideoId,
                watchDurationSeconds = 20, // 20s > 15s requis
                isSimulationMode = _isSimulationMode.value
            )

            result.onSuccess { amount ->
                _actionMessage.value = "+${Wallet.formatCurrency(amount)} crédités via vue qualifiée !"
            }.onFailure { err ->
                _actionMessage.value = "Erreur récompense: ${err.message}"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
