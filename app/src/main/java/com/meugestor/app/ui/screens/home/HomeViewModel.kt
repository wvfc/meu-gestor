package com.meugestor.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.repository.AccountRepository
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalBalance: Double = 0.0,
    val totalInAccounts: Double = 0.0,
    val totalCash: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val totalPayable: Double = 0.0,
    val projectedBalance: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val upcomingDueItems: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()
        val today = DateUtils.today()

        viewModelScope.launch {
            accountRepository.getTotalBalance().collect { total ->
                _uiState.update { it.copy(totalBalance = total ?: 0.0) }
            }
        }
        viewModelScope.launch {
            accountRepository.getTotalByType("CHECKING").collect { total ->
                _uiState.update { it.copy(totalInAccounts = total ?: 0.0) }
            }
        }
        viewModelScope.launch {
            accountRepository.getTotalByType("CASH").collect { total ->
                _uiState.update { it.copy(totalCash = total ?: 0.0) }
            }
        }
        viewModelScope.launch {
            transactionRepository.getByDateRange(startOfMonth, endOfMonth).collect { list ->
                val income = list.filter { it.type.name == "INCOME" }.sumOf { it.amount }
                val expense = list.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
                _uiState.update {
                    it.copy(
                        monthlyIncome = income,
                        monthlyExpenses = expense,
                        recentTransactions = list.take(10),
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            transactionRepository.getPendingExpenses().collect { items ->
                _uiState.update { it.copy(totalPayable = items.sumOf { tx -> tx.amount }) }
            }
        }
        viewModelScope.launch {
            transactionRepository.getUpcomingDueItems(today).collect { items ->
                _uiState.update { it.copy(upcomingDueItems = items.take(5)) }
            }
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(transactionRepository, accountRepository) as T
        }
    }
}
