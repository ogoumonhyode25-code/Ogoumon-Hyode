package com.example.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Transaction
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface TransactionHistoryUiState {
    object Loading : TransactionHistoryUiState
    data class Success(val transactions: List<Transaction>, val selectedFilter: String) : TransactionHistoryUiState
    data class Error(val message: String) : TransactionHistoryUiState
}

class TransactionHistoryViewModel(
    private val transactionRepository: TransactionRepository = TransactionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionHistoryUiState>(TransactionHistoryUiState.Loading)
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    private var currentFilter: String = "all"

    init {
        loadTransactions(currentFilter)
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        loadTransactions(filter)
    }

    private fun loadTransactions(filter: String) {
        viewModelScope.launch {
            _uiState.value = TransactionHistoryUiState.Loading
            val userId = FirebaseManager.currentUser?.uid ?: "user_demo_123"

            transactionRepository.getTransactionsFlow(userId, if (filter == "all") null else filter)
                .catch { e ->
                    _uiState.value = TransactionHistoryUiState.Error("Erreur de chargement: ${e.message}")
                }
                .collect { list ->
                    _uiState.value = TransactionHistoryUiState.Success(
                        transactions = list,
                        selectedFilter = currentFilter
                    )
                }
        }
    }
}
