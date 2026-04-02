package com.meugestor.app.data.database.dao

import androidx.room.*
import com.meugestor.app.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val categoryId: Long,
    val total: Double
)

data class MonthlyTotal(
    val month: Int,
    val income: Double,
    val expense: Double
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId ORDER BY date DESC")
    fun getByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY date DESC")
    fun getByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE credit_card_id = :creditCardId ORDER BY date DESC")
    fun getByCreditCard(creditCardId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY date DESC")
    fun getByStatus(status: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: String): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE type = 'INCOME'
        AND date BETWEEN :startDate AND :endDate
        AND status != 'CANCELLED'
        """
    )
    fun getIncomeByDateRange(startDate: String, endDate: String): Flow<Double?>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE type = 'EXPENSE'
        AND date BETWEEN :startDate AND :endDate
        AND status != 'CANCELLED'
        """
    )
    fun getExpenseByDateRange(startDate: String, endDate: String): Flow<Double?>

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'EXPENSE'
        AND status = 'PENDING'
        ORDER BY due_date ASC
        """
    )
    fun getPendingExpenses(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE status = 'PENDING'
        AND due_date IS NOT NULL
        AND due_date BETWEEN :date AND date(:date, '+7 days')
        ORDER BY due_date ASC
        """
    )
    fun getUpcomingDueItems(date: String): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE date > :fromDate
        AND is_projection = 1
        ORDER BY date ASC
        """
    )
    fun getFutureTransactions(fromDate: String): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT category_id AS categoryId, SUM(amount) AS total
        FROM transactions
        WHERE type = 'EXPENSE'
        AND date BETWEEN :startDate AND :endDate
        AND status != 'CANCELLED'
        AND category_id IS NOT NULL
        GROUP BY category_id
        ORDER BY total DESC
        LIMIT :limit
        """
    )
    fun getTopExpenseCategories(startDate: String, endDate: String, limit: Int): Flow<List<CategoryTotal>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE description LIKE '%' || :query || '%'
        ORDER BY date DESC
        """
    )
    fun search(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query(
        """
        SELECT
            CAST(SUBSTR(date, 6, 2) AS INTEGER) AS month,
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0.0) AS income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0.0) AS expense
        FROM transactions
        WHERE SUBSTR(date, 1, 4) = :year
        AND status != 'CANCELLED'
        GROUP BY SUBSTR(date, 6, 2)
        ORDER BY month
        """
    )
    fun getMonthlyTotals(year: String): Flow<List<MonthlyTotal>>

    @Query("SELECT * FROM transactions WHERE is_fixed = :isFixed ORDER BY date DESC")
    fun getByFixedVariable(isFixed: Boolean): Flow<List<TransactionEntity>>
}
