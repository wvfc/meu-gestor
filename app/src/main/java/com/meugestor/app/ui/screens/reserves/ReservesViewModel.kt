package com.meugestor.app.ui.screens.reserves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.AccountEntity
import com.meugestor.app.data.database.entity.AccountType
import com.meugestor.app.data.repository.AccountRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReservesUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalChecking: Double = 0.0,
    val totalSavings: Double = 0.0,
    val totalInvestments: Double = 0.0,
    val totalCash: Double = 0.0,
    val totalEmergency: Double = 0.0,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false
)

class ReservesViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservesUiState())
    val uiState: StateFlow<ReservesUiState> = _uiState.asStateFlow()

    init { loadAccounts() }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                val total = accounts.sumOf { it.balance }
                val checking = accounts.filter { it.accountType == AccountType.CHECKING }.sumOf { it.balance }
                val savings = accounts.filter { it.accountType == AccountType.SAVINGS }.sumOf { it.balance }
                val investments = accounts.filter { it.accountType == AccountType.INVESTMENT }.sumOf { it.balance }
                val cash = accounts.filter { it.accountType == AccountType.CASH }.sumOf { it.balance }
                val emergency = accounts.filter { it.accountType == AccountType.EMERGENCY }.sumOf { it.balance }
                _uiState.update {
                    it.copy(
                        accounts = accounts,
                        totalBalance = total,
                        totalChecking = checking,
                        totalSavings = savings,
                        totalInvestments = investments,
                        totalCash = cash,
                        totalEmergency = emergency,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun addAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.insert(account)
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch { accountRepository.delete(account) }
    }

    fun toggleAddDialog() {
        _uiState.update { it.copy(showAddDialog = !it.showAddDialog) }
    }

    class Factory(
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReservesViewModel(accountRepository) as T
    }
}
