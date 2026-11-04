package com.meugestor.app.ui.screens.future

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.data.repository.AccountRepository
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.domain.effectiveDueDate
import com.meugestor.app.ui.common.TransactionActionsViewModel
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class MonthProjection(
    val label: String,
    val income: Double,
    val expense: Double,
    /** Saldo estimado ao final do mês, acumulando as pendências até ele. */
    val endBalance: Double = 0.0
) {
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
    /** Primeiro mês em que o saldo estimado fica negativo, se houver. */
    val firstNegativeMonth: String? = null,
    val monthlyProjections: List<MonthProjection> = emptyList(),
    val currentBalance: Double = 0.0,
    val isLoading: Boolean = true
)

class FutureEntriesViewModel(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository
) : TransactionActionsViewModel(transactionRepository) {

    // Saldo e pendências combinados: antes o saldo previsto usava o saldo ainda não carregado.
    val uiState: StateFlow<FutureEntriesUiState> = combine(
        accountRepository.getTotalBalance(),
        transactionRepository.getPendingTransactions()
    ) { balance, pending ->
        buildState(balance ?: 0.0, pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FutureEntriesUiState())

    private fun buildState(currentBalance: Double, pending: List<TransactionEntity>): FutureEntriesUiState {
        val today = DateUtils.todayDate()
        val incomes = pending.filter { it.type == TransactionType.INCOME }
        val expenses = pending.filter { it.type == TransactionType.EXPENSE }
        val totalReceivable = incomes.sumOf { it.amount }
        val totalPayable = expenses.sumOf { it.amount }
        val projected = currentBalance + totalReceivable - totalPayable

        // Projeção dos próximos 6 meses; pendências atrasadas entram no mês atual.
        val firstMonth = YearMonth.from(today)
        val months = (0L until PROJECTION_MONTHS).map { firstMonth.plusMonths(it) }
        val lastMonth = months.last()
        val byMonth = pending.groupBy { tx ->
            val due = tx.effectiveDueDate() ?: today
            val month = YearMonth.from(due)
            if (month.isBefore(firstMonth)) firstMonth else month
        }

        var running = currentBalance
        var firstNegative: String? = null
        val projections = months.map { month ->
            val items = byMonth[month].orEmpty()
            val income = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            running += income - expense
            if (running < 0 && firstNegative == null) firstNegative = DateUtils.formatYearMonth(month)
            MonthProjection(
                label = DateUtils.formatShortMonth(month),
                income = income,
                expense = expense,
                endBalance = running
            )
        }
        // Pendências além do horizonte do gráfico também contam para o risco.
        val beyondHorizon = byMonth.filterKeys { it.isAfter(lastMonth) }.values.flatten()
        val finalBalance = running +
            beyondHorizon.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } -
            beyondHorizon.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val limitDate = today.plusDays(7)
        return FutureEntriesUiState(
            futureIncomes = incomes,
            futureExpenses = expenses,
            upcomingItems = pending.filter { tx -> tx.effectiveDueDate()?.isAfter(limitDate) != true },
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            projectedBalance = projected,
            negativeBalanceRisk = firstNegative != null || finalBalance < 0,
            firstNegativeMonth = firstNegative,
            monthlyProjections = projections,
            currentBalance = currentBalance,
            isLoading = false
        )
    }

    companion object {
        private const val PROJECTION_MONTHS = 6L
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
