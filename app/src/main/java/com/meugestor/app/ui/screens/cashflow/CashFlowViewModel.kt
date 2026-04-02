package com.meugestor.app.ui.screens.cashflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CashFlowFilter { ALL, INCOME, EXPENSE, TRANSFER }

data class CashFlowUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val currentFilter: CashFlowFilter = CashFlowFilter.ALL,
    val searchQuery: String = "",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false
)

class CashFlowViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashFlowUiState())
    val uiState: StateFlow<CashFlowUiState> = _uiState.asStateFlow()

    init { loadTransactions() }

    private fun loadTransactions() {
        val start = DateUtils.getStartOfMonth()
        val end = DateUtils.getEndOfMonth()
        viewModelScope.launch {
            transactionRepository.getByDateRange(start, end).collect { list ->
                val income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                _uiState.update { state ->
                    state.copy(
                        transactions = list,
                        filteredTransactions = applyFilters(list, state.currentFilter, state.searchQuery),
                        totalIncome = income,
                        totalExpense = expense,
                        balance = income - expense,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setFilter(filter: CashFlowFilter) {
        _uiState.update { state ->
            state.copy(
                currentFilter = filter,
                filteredTransactions = applyFilters(state.transactions, filter, state.searchQuery)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredTransactions = applyFilters(state.transactions, state.currentFilter, query)
            )
        }
    }

    fun toggleAddDialog() {
        _uiState.update { it.copy(showAddDialog = !it.showAddDialog) }
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.addTransaction(transaction)
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { transactionRepository.delete(transaction) }
    }

    private fun applyFilters(list: List<TransactionEntity>, filter: CashFlowFilter, query: String): List<TransactionEntity> {
        return list.filter { tx ->
            val matchesFilter = when (filter) {
                CashFlowFilter.ALL -> true
                CashFlowFilter.INCOME -> tx.type == TransactionType.INCOME
                CashFlowFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                CashFlowFilter.TRANSFER -> tx.type == TransactionType.TRANSFER
            }
            val matchesQuery = query.isBlank() || tx.description.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CashFlowViewModel(transactionRepository) as T
        }
    }
}
