package com.meugestor.app.data.database.dao

import androidx.room.*
import com.meugestor.app.data.database.entity.TaxChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxChecklistDao {
    @Query("SELECT * FROM tax_checklist_items WHERE year = :year ORDER BY category, description")
    fun getByYear(year: Int): Flow<List<TaxChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TaxChecklistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TaxChecklistItemEntity>)

    @Update
    suspend fun update(item: TaxChecklistItemEntity)

    @Query("UPDATE tax_checklist_items SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean)

    @Delete
    suspend fun delete(item: TaxChecklistItemEntity)

    @Query("SELECT COUNT(*) FROM tax_checklist_items WHERE year = :year AND is_completed = 1")
    fun getCompletionCount(year: Int): Flow<Int>
}
