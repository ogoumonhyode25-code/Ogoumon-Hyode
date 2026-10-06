package com.example.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.RewardSettings
import com.example.data.model.Wallet
import com.example.data.model.Withdrawal
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WithdrawUiState {
    data class Input(
        val availableBalance: Long = 0L,
        val settings: RewardSettings = RewardSettings(),
        val amount: String = "",
        val selectedMethod: String = Withdrawal.METHOD_WAVE,
        val account: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) : WithdrawUiState

    data class Success(val withdrawal: Withdrawal) : WithdrawUiState
}

class WithdrawalViewModel(
    private val walletRepository: WalletRepository = WalletRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WithdrawUiState>(WithdrawUiState.Input())
    val uiState: StateFlow<WithdrawUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"
            val settings = walletRepository.getRewardSettings()

            walletRepository.getWalletFlow(userId).collect { wallet ->
                val current = _uiState.value
                if (current is WithdrawUiState.Input) {
                    _uiState.value = current.copy(
                        availableBalance = wallet.availableBalance,
                        settings = settings
                    )
                }
            }
        }
    }

    fun onAmountChanged(newAmount: String) {
        val filtered = newAmount.filter { it.isDigit() }
        val current = _uiState.value as? WithdrawUiState.Input ?: return
        _uiState.value = current.copy(amount = filtered, errorMessage = null)
    }

    fun onMethodSelected(method: String) {
        val current = _uiState.value as? WithdrawUiState.Input ?: return
        _uiState.value = current.copy(selectedMethod = method)
    }

    fun onAccountChanged(newAccount: String) {
        val current = _uiState.value as? WithdrawUiState.Input ?: return
        _uiState.value = current.copy(account = newAccount, errorMessage = null)
    }

    fun setQuickAmount(amount: Long) {
        val current = _uiState.value as? WithdrawUiState.Input ?: return
        _uiState.value = current.copy(amount = amount.toString(), errorMessage = null)
    }

    fun submitWithdrawal() {
        val current = _uiState.value as? WithdrawUiState.Input ?: return
        val amountLong = current.amount.toLongOrNull() ?: 0L

        // Validation 1 : Montant minimum
        if (amountLong < current.settings.minimumWithdrawal) {
            _uiState.value = current.copy(
                errorMessage = "Le montant minimum de retrait est de ${Wallet.formatCurrency(current.settings.minimumWithdrawal)}."
            )
            return
        }

        // Validation 2 : Solde disponible suffisant
        if (amountLong > current.availableBalance) {
            _uiState.value = current.copy(
                errorMessage = "Solde insuffisant. Vous disposez de ${Wallet.formatCurrency(current.availableBalance)}."
            )
            return
        }

        // Validation 3 : Numéro de paiement valide
        if (current.account.trim().length < 8) {
            _uiState.value = current.copy(
                errorMessage = "Veuillez saisir un numéro de téléphone ou compte Mobile Money valide."
            )
            return
        }

        // Exécution de la demande de retrait
        _uiState.value = current.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"
            val result = walletRepository.requestWithdrawal(
                userId = userId,
                amount = amountLong,
                method = current.selectedMethod,
                account = current.account.trim(),
                isSimulationMode = true
            )

            result.onSuccess { withdrawal ->
                _uiState.value = WithdrawUiState.Success(withdrawal)
            }.onFailure { exception ->
                _uiState.value = current.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Une erreur est survenue lors de la demande."
                )
            }
        }
    }

    fun reset() {
        loadInitialData()
    }
}
