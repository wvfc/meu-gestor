package com.meugestor.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.dao.MonthlyTotal
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.data.repository.CategoryRepository
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class RankingItem(val label: String, val amount: Double, val percentage: Float, val color: String? = null)

data class ReportsUiState(
    val monthlyTotals: List<MonthlyTotal> = emptyList(),
    val topExpenses: List<TransactionEntity> = emptyList(),
    val categoryRanking: List<RankingItem> = emptyList(),
    val totalYearIncome: Double = 0.0,
    val totalYearExpense: Double = 0.0,
    /** Totais do período selecionado (semana, mês ou ano). */
    val totalMonthExpense: Double = 0.0,
    val totalMonthIncome: Double = 0.0,
    val averageMonthlyExpense: Double = 0.0,
    val selectedPeriod: String = PERIOD_MONTH,
    val periodLabel: String = "",
    val selectedTab: Int = 0,
    val isLoading: Boolean = true
)

const val PERIOD_WEEK = "Semana"
const val PERIOD_MONTH = "Mês"
const val PERIOD_YEAR = "Ano"

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(PERIOD_MONTH)
    private val selectedTab = MutableStateFlow(0)

    private val periodTransactions = selectedPeriod.flatMapLatest { period ->
        val (start, end) = periodRange(period)
        transactionRepository.getByDateRange(start, end).map { list -> period to list }
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        transactionRepository.getMonthlyTotals(DateUtils.currentYear().toString()),
        periodTransactions,
        categoryRepository.getAllCategories(),
        selectedTab
    ) { totals, periodData, categories, tab ->
        val (period, list) = periodData
        val active = list.filter { it.status != TransactionStatus.CANCELLED }
        val expenses = active.filter { it.type == TransactionType.EXPENSE }
        val income = active.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = expenses.sumOf { it.amount }
        val totalYearExpense = totals.sumOf { it.expense }
        val monthsWithData = totals.count { it.income > 0 || it.expense > 0 }

        val categoriesById = categories.associateBy { it.id }
        val ranking = expenses
            .groupBy { it.categoryId }
            .map { (categoryId, items) ->
                val total = items.sumOf { it.amount }
                val category = categoryId?.let { categoriesById[it] }
                RankingItem(
                    label = category?.name ?: "Sem categoria",
                    amount = total,
                    percentage = if (expense > 0) (total / expense * 100).toFloat() else 0f,
                    color = category?.color
                )
            }
            .sortedByDescending { it.amount }

        ReportsUiState(
            monthlyTotals = totals,
            topExpenses = expenses.sortedByDescending { it.amount }.take(10),
            categoryRanking = ranking,
            totalYearIncome = totals.sumOf { it.income },
            totalYearExpense = totalYearExpense,
            totalMonthExpense = expense,
            totalMonthIncome = income,
            averageMonthlyExpense = if (monthsWithData > 0) totalYearExpense / monthsWithData else 0.0,
            selectedPeriod = period,
            periodLabel = periodLabel(period),
            selectedTab = tab,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun selectPeriod(period: String) {
        selectedPeriod.value = period
    }

    fun selectTab(tab: Int) {
        selectedTab.value = tab
    }

    private fun periodRange(period: String): Pair<String, String> = when (period) {
        PERIOD_WEEK -> DateUtils.getStartOfWeek() to DateUtils.getEndOfWeek()
        PERIOD_YEAR -> DateUtils.getStartOfYear() to DateUtils.getEndOfYear()
        else -> DateUtils.getStartOfMonth() to DateUtils.getEndOfMonth()
    }

    private fun periodLabel(period: String): String = when (period) {
        PERIOD_WEEK -> "desta semana"
        PERIOD_YEAR -> "deste ano"
        else -> "deste mês"
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReportsViewModel(transactionRepository, categoryRepository) as T
    }
}
