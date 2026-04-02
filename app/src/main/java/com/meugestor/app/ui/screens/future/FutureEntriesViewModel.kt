package com.meugestor.app.ui.screens.future

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.data.repository.AccountRepository
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MonthProjection(val label: String, val income: Double, val expense: Double) {
    val balance: Double get() = income - expense
}

data class FutureEntriesUiState(
    val futureIncomes: List<TransactionEntity> = emptyList(),
    val futureExpenses: List<TransactionEntity> = emptyList(),
    val upcomingItems: List<TransactionEntity> = emptyList(),
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val projectedBalance: Double = 0.0,
    val negativeBalanceRisk: Boolean = false,
    val monthlyProjections: List<MonthProjection> = emptyList(),
    val currentBalance: Double = 0.0,
    val isLoading: Boolean = true
)

class FutureEntriesViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FutureEntriesUiState())
    val uiState: StateFlow<FutureEntriesUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        val today = DateUtils.today()
        viewModelScope.launch {
            accountRepository.getTotalBalance().collect { balance ->
                _uiState.update { it.copy(currentBalance = balance ?: 0.0) }
            }
        }
        viewModelScope.launch {
            transactionRepository.getPendingExpenses().collect { expenses ->
                val incomes = expenses.filter { it.type == TransactionType.INCOME }
                val exps = expenses.filter { it.type == TransactionType.EXPENSE }
                val totalRec = incomes.sumOf { it.amount }
                val totalPay = exps.sumOf { it.amount }
                _uiState.update {
                    it.copy(
                        futureIncomes = incomes,
                        futureExpenses = exps,
                        totalReceivable = totalRec,
                        totalPayable = totalPay,
                        projectedBalance = it.currentBalance + totalRec - totalPay,
                        negativeBalanceRisk = it.currentBalance + totalRec - totalPay < 0,
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            transactionRepository.getUpcomingDueItems(today).collect { items ->
                _uiState.update { it.copy(upcomingItems = items) }
            }
        }
        buildMonthlyProjections()
    }

    private fun buildMonthlyProjections() {
        val months = listOf("Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez")
        val projections = months.mapIndexed { i, label ->
            MonthProjection(label, income = 0.0, expense = 0.0)
        }
        _uiState.update { it.copy(monthlyProjections = projections) }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FutureEntriesViewModel(transactionRepository, accountRepository) as T
    }
}
