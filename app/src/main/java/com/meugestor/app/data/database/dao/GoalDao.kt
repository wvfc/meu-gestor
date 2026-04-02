package com.meugestor.app.data.database.dao

import androidx.room.*
import com.meugestor.app.data.database.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY name")
    fun getAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE is_active = 1 ORDER BY name")
    fun getActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Query("SELECT * FROM goals WHERE category_id = :categoryId ORDER BY name")
    fun getByCategoryId(categoryId: Long): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE category_id = :categoryId
        AND type = 'EXPENSE'
        AND status != 'CANCELLED'
        AND date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun getSpentAmount(categoryId: Long, startDate: String, endDate: String): Double
}
