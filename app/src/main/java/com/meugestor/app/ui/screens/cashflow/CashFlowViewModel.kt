package com.meugestor.app.ui.screens.cashflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.ui.common.TransactionActionsViewModel
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth

enum class CashFlowFilter { ALL, INCOME, EXPENSE, PENDING }

data class CashFlowUiState(
    val month: YearMonth = YearMonth.now(),
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val currentFilter: CashFlowFilter = CashFlowFilter.ALL,
    val searchQuery: String = "",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val pendingCount: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class CashFlowViewModel(
    transactionRepository: TransactionRepository
) : TransactionActionsViewModel(transactionRepository) {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val filter = MutableStateFlow(CashFlowFilter.ALL)
    private val query = MutableStateFlow("")

    private val monthTransactions = selectedMonth.flatMapLatest { month ->
        transactionRepository
            .getByDateRange(DateUtils.startOfMonth(month), DateUtils.endOfMonth(month))
            .map { list -> month to list }
    }

    val uiState: StateFlow<CashFlowUiState> = combine(monthTransactions, filter, query) { monthData, currentFilter, currentQuery ->
        val (month, list) = monthData
        val active = list.filter { it.status != TransactionStatus.CANCELLED }
        val income = active.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = active.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        CashFlowUiState(
            month = month,
            transactions = list,
            filteredTransactions = applyFilters(list, currentFilter, currentQuery),
            currentFilter = currentFilter,
            searchQuery = currentQuery,
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense,
            pendingCount = list.count { it.status == TransactionStatus.PENDING || it.status == TransactionStatus.OVERDUE },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashFlowUiState())

    fun previousMonth() = selectedMonth.update { it.minusMonths(1) }

    fun nextMonth() = selectedMonth.update { it.plusMonths(1) }

    fun goToCurrentMonth() {
        selectedMonth.value = YearMonth.now()
    }

    fun setFilter(newFilter: CashFlowFilter) {
        filter.value = newFilter
    }

    fun setSearchQuery(newQuery: String) {
        query.value = newQuery
    }

    private fun applyFilters(list: List<TransactionEntity>, filter: CashFlowFilter, query: String): List<TransactionEntity> {
        return list.filter { tx ->
            val matchesFilter = when (filter) {
                CashFlowFilter.ALL -> true
                CashFlowFilter.INCOME -> tx.type == TransactionType.INCOME
                CashFlowFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                CashFlowFilter.PENDING -> tx.status == TransactionStatus.PENDING || tx.status == TransactionStatus.OVERDUE
            }
            val matchesQuery = query.isBlank() ||
                tx.description.contains(query, ignoreCase = true) ||
                tx.notes?.contains(query, ignoreCase = true) == true
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
