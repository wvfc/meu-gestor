package com.meugestor.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.AccountType
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
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

data class HomeUiState(
    val totalBalance: Double = 0.0,
    val totalInAccounts: Double = 0.0,
    val totalCash: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val totalPayable: Double = 0.0,
    val totalReceivable: Double = 0.0,
    val projectedBalance: Double = 0.0,
    val overdueCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val upcomingDueItems: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository
) : TransactionActionsViewModel(transactionRepository) {

    private val today = DateUtils.todayDate()

    val uiState: StateFlow<HomeUiState> = combine(
        accountRepository.getAllAccounts(),
        transactionRepository.getByDateRange(DateUtils.getStartOfMonth(), DateUtils.getEndOfMonth()),
        transactionRepository.getPendingTransactions(),
        transactionRepository.getRecent(DateUtils.today(), limit = 5)
    ) { accounts, monthTransactions, pending, recent ->
        val active = monthTransactions.filter { it.status != TransactionStatus.CANCELLED }
        val payable = pending.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val receivable = pending.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val balance = accounts.sumOf { it.balance }
        val limitDate = today.plusDays(7)

        HomeUiState(
            totalBalance = balance,
            totalInAccounts = accounts
                .filter { it.accountType == AccountType.CHECKING || it.accountType == AccountType.SAVINGS }
                .sumOf { it.balance },
            totalCash = accounts.filter { it.accountType == AccountType.CASH }.sumOf { it.balance },
            monthlyIncome = active.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            monthlyExpenses = active.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            totalPayable = payable,
            totalReceivable = receivable,
            projectedBalance = balance + receivable - payable,
            overdueCount = pending.count { tx -> tx.effectiveDueDate()?.isBefore(today) == true },
            recentTransactions = recent,
            upcomingDueItems = pending
                .filter { tx -> tx.effectiveDueDate()?.isAfter(limitDate) != true }
                .take(5),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

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
