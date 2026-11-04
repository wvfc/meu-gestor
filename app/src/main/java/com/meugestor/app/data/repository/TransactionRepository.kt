package com.meugestor.app.data.repository

import androidx.room.withTransaction
import com.meugestor.app.data.database.AppDatabase
import com.meugestor.app.data.database.dao.AccountDao
import com.meugestor.app.data.database.dao.CategoryTotal
import com.meugestor.app.data.database.dao.MonthlyTotal
import com.meugestor.app.data.database.dao.TransactionDao
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.domain.BalanceRules
import com.meugestor.app.domain.seriesRootId
import com.meugestor.app.domain.withStatus
import kotlinx.coroutines.flow.Flow

data class MonthSummary(
    val income: Flow<Double?>,
    val expense: Flow<Double?>
)

/**
 * Ponto único de escrita de lançamentos. Toda inclusão, edição, troca de status e exclusão
 * passa por aqui e ajusta o saldo da conta dentro de uma transação do banco, usando o campo
 * `balance_applied` para saber exatamente o que já foi aplicado.
 */
class TransactionRepository(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAll()

    fun getByDateRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> =
        transactionDao.getByDateRange(startDate, endDate)

    fun getRecent(today: String, limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecent(today, limit)

    fun getByCategory(categoryId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getByCategory(categoryId)

    fun getByAccount(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getByAccount(accountId)

    fun getByCreditCard(creditCardId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getByCreditCard(creditCardId)

    fun getCreditCardTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getCreditCardTransactions()

    fun getByStatus(status: String): Flow<List<TransactionEntity>> =
        transactionDao.getByStatus(status)

    fun getByType(type: String): Flow<List<TransactionEntity>> =
        transactionDao.getByType(type)

    fun getByFixedVariable(isFixed: Boolean): Flow<List<TransactionEntity>> =
        transactionDao.getByFixedVariable(isFixed)

    fun getPendingExpenses(): Flow<List<TransactionEntity>> =
        transactionDao.getPendingExpenses()

    fun getPendingTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getPendingTransactions()

    fun getUpcomingDueItems(date: String): Flow<List<TransactionEntity>> =
        transactionDao.getUpcomingDueItems(date)

    fun search(query: String): Flow<List<TransactionEntity>> =
        transactionDao.search(query)

    fun getMonthlyTotals(year: String): Flow<List<MonthlyTotal>> =
        transactionDao.getMonthlyTotals(year)

    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)

    /**
     * Inclui um lançamento aplicando seu efeito no saldo somente se estiver realizado (PAID).
     * Compras no cartão seguem a mesma regra: saem da conta quando forem pagas.
     */
    suspend fun addTransaction(transaction: TransactionEntity): Long =
        database.withTransaction { insertApplyingBalance(transaction) }

    /**
     * Inclui uma série (parcelas ou repetição mensal). O primeiro item vira a origem e os
     * demais são ligados a ele por parent_transaction_id.
     */
    suspend fun addSeries(transactions: List<TransactionEntity>): Long? {
        if (transactions.isEmpty()) return null
        return database.withTransaction {
            val rootId = insertApplyingBalance(transactions.first())
            transactions.drop(1).forEach { item ->
                insertApplyingBalance(item.copy(parentTransactionId = rootId))
            }
            rootId
        }
    }

    /** Atualiza um lançamento revertendo o efeito antigo e aplicando o novo. */
    suspend fun update(transaction: TransactionEntity) {
        database.withTransaction { updateApplyingBalance(transaction) }
    }

    suspend fun updateAll(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) return
        database.withTransaction {
            transactions.forEach { updateApplyingBalance(it) }
        }
    }

    suspend fun setStatus(transaction: TransactionEntity, status: TransactionStatus) {
        update(transaction.withStatus(status))
    }

    suspend fun delete(transaction: TransactionEntity) {
        database.withTransaction {
            val stored = transactionDao.getById(transaction.id) ?: return@withTransaction
            reverseAppliedBalance(stored)
            transactionDao.delete(stored)
        }
    }

    /**
     * Exclui as parcelas/repetições ainda em aberto da série do lançamento informado.
     * Itens já pagos permanecem como histórico. Retorna quantos itens foram removidos.
     */
    suspend fun deleteOpenSeries(transaction: TransactionEntity): Int =
        database.withTransaction {
            val series = transactionDao.getSeries(transaction.seriesRootId())
            val toDelete = series.filter { it.status != TransactionStatus.PAID }
            toDelete.forEach { item ->
                reverseAppliedBalance(item)
                transactionDao.delete(item)
            }
            toDelete.size
        }

    /** Returns income and expense totals for a given date range. */
    fun getMonthSummary(startDate: String, endDate: String): MonthSummary {
        return MonthSummary(
            income = transactionDao.getIncomeByDateRange(startDate, endDate),
            expense = transactionDao.getExpenseByDateRange(startDate, endDate)
        )
    }

    /** Returns projected future transactions (recurring or planned). */
    fun getFutureProjections(fromDate: String): Flow<List<TransactionEntity>> =
        transactionDao.getFutureTransactions(fromDate)

    /** Returns the top expense categories for a given date range. */
    fun getTopCategories(
        startDate: String,
        endDate: String,
        limit: Int = 5
    ): Flow<List<CategoryTotal>> =
        transactionDao.getTopExpenseCategories(startDate, endDate, limit)

    private suspend fun insertApplyingBalance(transaction: TransactionEntity): Long {
        val apply = BalanceRules.shouldApply(transaction)
        val id = transactionDao.insert(transaction.copy(balanceApplied = apply))
        if (apply) {
            accountDao.updateBalance(transaction.accountId, BalanceRules.signedAmount(transaction))
        }
        return id
    }

    private suspend fun updateApplyingBalance(transaction: TransactionEntity) {
        val stored = transactionDao.getById(transaction.id) ?: return
        reverseAppliedBalance(stored)
        val apply = BalanceRules.shouldApply(transaction)
        if (apply) {
            accountDao.updateBalance(transaction.accountId, BalanceRules.signedAmount(transaction))
        }
        transactionDao.update(transaction.copy(balanceApplied = apply))
    }

    private suspend fun reverseAppliedBalance(stored: TransactionEntity) {
        val applied = BalanceRules.appliedEffect(stored)
        if (applied != 0.0) {
            accountDao.updateBalance(stored.accountId, -applied)
        }
    }
}
