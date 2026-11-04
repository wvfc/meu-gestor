package com.meugestor.app.data.repository

import com.meugestor.app.data.database.dao.TaxChecklistDao
import com.meugestor.app.data.database.dao.TaxDocumentDao
import com.meugestor.app.data.database.entity.TaxChecklistItemEntity
import com.meugestor.app.data.database.entity.TaxDocumentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AnnualTaxSummary(
    val year: Int,
    val documents: Flow<List<TaxDocumentEntity>>,
    val totalAmount: Flow<Double?>,
    val checklist: Flow<List<TaxChecklistItemEntity>>
)

class TaxRepository(
    private val taxDocumentDao: TaxDocumentDao,
    private val taxChecklistDao: TaxChecklistDao
) {
    private val checklistSeedMutex = Mutex()

    // Tax Documents

    fun getDocumentsByYear(year: Int): Flow<List<TaxDocumentEntity>> =
        taxDocumentDao.getByYear(year)

    suspend fun getDocumentById(id: Long): TaxDocumentEntity? = taxDocumentDao.getById(id)

    fun getTotalByCategory(year: Int, category: String): Flow<Double?> =
        taxDocumentDao.getTotalByCategory(year, category)

    suspend fun addDocument(document: TaxDocumentEntity): Long =
        taxDocumentDao.insert(document)

    suspend fun updateDocument(document: TaxDocumentEntity) = taxDocumentDao.update(document)

    suspend fun deleteDocument(document: TaxDocumentEntity) = taxDocumentDao.delete(document)

    // Tax Checklist

    fun getChecklist(year: Int): Flow<List<TaxChecklistItemEntity>> =
        taxChecklistDao.getByYear(year)

    suspend fun addChecklistItem(item: TaxChecklistItemEntity): Long =
        taxChecklistDao.insert(item)

    suspend fun addChecklistItems(items: List<TaxChecklistItemEntity>) =
        taxChecklistDao.insertAll(items)

    /**
     * Cria o checklist padrão do ano somente se ele ainda não existir.
     * O mutex evita duplicação quando o usuário troca de ano rapidamente.
     */
    suspend fun ensureChecklist(year: Int, defaultItems: List<String>) {
        checklistSeedMutex.withLock {
            if (taxChecklistDao.countByYear(year) > 0) return
            taxChecklistDao.insertAll(
                defaultItems.map { description ->
                    TaxChecklistItemEntity(year = year, description = description, category = "Geral")
                }
            )
        }
    }

    suspend fun updateChecklistItem(item: TaxChecklistItemEntity) =
        taxChecklistDao.update(item)

    suspend fun toggleChecklistItem(id: Long, isCompleted: Boolean) =
        taxChecklistDao.updateCompletionStatus(id, isCompleted)

    suspend fun deleteChecklistItem(item: TaxChecklistItemEntity) =
        taxChecklistDao.delete(item)

    /**
     * Returns a combined annual summary with documents, total amount, and checklist.
     */
    fun getAnnualSummary(year: Int): AnnualTaxSummary {
        return AnnualTaxSummary(
            year = year,
            documents = taxDocumentDao.getByYear(year),
            totalAmount = taxDocumentDao.getAnnualTotal(year),
            checklist = taxChecklistDao.getByYear(year)
        )
    }
}
