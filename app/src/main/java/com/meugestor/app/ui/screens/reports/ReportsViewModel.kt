package com.meugestor.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.dao.MonthlyTotal
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RankingItem(val label: String, val amount: Double, val percentage: Float)

data class ReportsUiState(
    val monthlyTotals: List<MonthlyTotal> = emptyList(),
    val topExpenses: List<TransactionEntity> = emptyList(),
    val totalYearIncome: Double = 0.0,
    val totalYearExpense: Double = 0.0,
    val totalMonthExpense: Double = 0.0,
    val totalMonthIncome: Double = 0.0,
    val averageMonthlyExpense: Double = 0.0,
    val selectedPeriod: String = "Mês",
    val selectedTab: Int = 0,
    val isLoading: Boolean = true
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        val year = DateUtils.currentYear().toString()
        val startMonth = DateUtils.getStartOfMonth()
        val endMonth = DateUtils.getEndOfMonth()
        val startYear = DateUtils.getStartOfYear()
        val endYear = DateUtils.getEndOfYear()

        viewModelScope.launch {
            transactionRepository.getMonthlyTotals(year).collect { totals ->
                val totalIncome = totals.sumOf { it.income }
                val totalExpense = totals.sumOf { it.expense }
                val avgExpense = if (totals.isNotEmpty()) totalExpense / totals.size else 0.0
                _uiState.update {
                    it.copy(
                        monthlyTotals = totals,
                        totalYearIncome = totalIncome,
                        totalYearExpense = totalExpense,
                        averageMonthlyExpense = avgExpense,
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            transactionRepository.getByDateRange(startMonth, endMonth).collect { list ->
                val income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val topExp = list.filter { it.type == TransactionType.EXPENSE }
                    .sortedByDescending { it.amount }.take(10)
                _uiState.update {
                    it.copy(totalMonthIncome = income, totalMonthExpense = expense, topExpenses = topExp)
                }
            }
        }
    }

    fun selectPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    class Factory(
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReportsViewModel(transactionRepository) as T
    }
}
