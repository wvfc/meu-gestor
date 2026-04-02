package com.meugestor.app.data.database.dao

import androidx.room.*
import com.meugestor.app.data.database.entity.TaxDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxDocumentDao {
    @Query("SELECT * FROM tax_documents WHERE year = :year ORDER BY category, document_date DESC")
    fun getByYear(year: Int): Flow<List<TaxDocumentEntity>>

    @Query("SELECT * FROM tax_documents WHERE year = :year AND category = :category ORDER BY document_date DESC")
    fun getByYearAndCategory(year: Int, category: String): Flow<List<TaxDocumentEntity>>

    @Query("SELECT * FROM tax_documents WHERE id = :id")
    suspend fun getById(id: Long): TaxDocumentEntity?

    @Query(
        """
        SELECT category AS categoryId, SUM(amount) AS total
        FROM tax_documents
        WHERE year = :year
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun getTotalGroupedByCategory(year: Int): Flow<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taxDocument: TaxDocumentEntity): Long

    @Update
    suspend fun update(taxDocument: TaxDocumentEntity)

    @Delete
    suspend fun delete(taxDocument: TaxDocumentEntity)

    @Query("SELECT DISTINCT year FROM tax_documents ORDER BY year DESC")
    fun getYears(): Flow<List<Int>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM tax_documents WHERE year = :year")
    fun getAnnualTotal(year: Int): Flow<Double?>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM tax_documents WHERE year = :year AND category = :category")
    fun getTotalByCategory(year: Int, category: String): Flow<Double?>
}
