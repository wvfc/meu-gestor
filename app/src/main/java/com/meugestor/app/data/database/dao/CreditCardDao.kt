package com.meugestor.app.data.database.dao

import androidx.room.*
import com.meugestor.app.data.database.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards WHERE is_active = 1 ORDER BY name")
    fun getAll(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getById(id: Long): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(creditCard: CreditCardEntity): Long

    @Update
    suspend fun update(creditCard: CreditCardEntity)

    @Delete
    suspend fun delete(creditCard: CreditCardEntity)

    @Query(
        """
        SELECT COALESCE(SUM(t.amount), 0.0) FROM transactions t
        WHERE t.credit_card_id = :creditCardId
        AND t.status != 'PAID'
        AND t.status != 'CANCELLED'
        """
    )
    fun getUsedLimit(creditCardId: Long): Flow<Double>

    @Query(
        """
        SELECT c.total_limit - COALESCE(SUM(t.amount), 0.0)
        FROM credit_cards c
        LEFT JOIN transactions t ON t.credit_card_id = c.id
            AND t.status != 'PAID'
            AND t.status != 'CANCELLED'
        WHERE c.id = :creditCardId
        GROUP BY c.id
        """
    )
    fun getAvailableLimit(creditCardId: Long): Flow<Double?>
}
