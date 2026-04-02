package com.meugestor.app.data.repository

import com.meugestor.app.data.database.dao.AccountDao
import com.meugestor.app.data.database.dao.CategoryTotal
import com.meugestor.app.data.database.dao.MonthlyTotal
import com.meugestor.app.data.database.dao.TransactionDao
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow

data class MonthSummary(
    val income: Flow<Double?>,
    val expense: Flow<Double?>
)

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAll()

    fun getByDateRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> =
        transactionDao.getByDateRange(startDate, endDate)

    fun getByCategory(categoryId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getByCategory(categoryId)

    fun getByAccount(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getByAccount(accountId)

    fun getByCreditCard(creditCardId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getByCreditCard(creditCardId)

    fun getByStatus(status: String): Flow<List<TransactionEntity>> =
        transactionDao.getByStatus(status)

    fun getByType(type: String): Flow<List<TransactionEntity>> =
        transactionDao.getByType(type)

    fun getByFixedVariable(isFixed: Boolean): Flow<List<TransactionEntity>> =
        transactionDao.getByFixedVariable(isFixed)

    fun getPendingExpenses(): Flow<List<TransactionEntity>> =
        transactionDao.getPendingExpenses()

    fun getUpcomingDueItems(date: String): Flow<List<TransactionEntity>> =
        transactionDao.getUpcomingDueItems(date)

    fun search(query: String): Flow<List<TransactionEntity>> =
        transactionDao.search(query)

    fun getMonthlyTotals(year: String): Flow<List<MonthlyTotal>> =
        transactionDao.getMonthlyTotals(year)

    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)

    suspend fun update(transaction: TransactionEntity) = transactionDao.update(transaction)

    suspend fun delete(transaction: TransactionEntity) {
        // Reverse the balance impact before deleting
        if (transaction.creditCardId == null) {
            val balanceChange = when (transaction.type) {
                TransactionType.INCOME -> -transaction.amount
                TransactionType.EXPENSE -> transaction.amount
                else -> 0.0
            }
            if (balanceChange != 0.0) {
                accountDao.updateBalance(transaction.accountId, balanceChange)
            }
        }
        transactionDao.delete(transaction)
    }

    /**
     * Inserts a transaction and updates the associated account balance.
     * Income increases the balance; Expense decreases it.
     * Credit card transactions do not affect account balance directly.
     */
    suspend fun addTransaction(transaction: TransactionEntity): Long {
        val transactionId = transactionDao.insert(transaction)

        // Only update account balance for non-credit-card transactions
        if (transaction.creditCardId == null) {
            val balanceChange = when (transaction.type) {
                TransactionType.INCOME -> transaction.amount
                TransactionType.EXPENSE -> -transaction.amount
                TransactionType.TRANSFER -> -transaction.amount // source account
                TransactionType.ADJUSTMENT -> transaction.amount
            }
            accountDao.updateBalance(transaction.accountId, balanceChange)
        }

        return transactionId
    }

    /**
     * Returns income and expense totals for a given date range.
     */
    fun getMonthSummary(startDate: String, endDate: String): MonthSummary {
        return MonthSummary(
            income = transactionDao.getIncomeByDateRange(startDate, endDate),
            expense = transactionDao.getExpenseByDateRange(startDate, endDate)
        )
    }

    /**
     * Returns projected future transactions (recurring or planned).
     */
    fun getFutureProjections(fromDate: String): Flow<List<TransactionEntity>> =
        transactionDao.getFutureTransactions(fromDate)

    /**
     * Returns the top expense categories for a given date range.
     */
    fun getTopCategories(
        startDate: String,
        endDate: String,
        limit: Int = 5
    ): Flow<List<CategoryTotal>> =
        transactionDao.getTopExpenseCategories(startDate, endDate, limit)
}
