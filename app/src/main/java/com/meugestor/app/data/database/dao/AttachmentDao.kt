package com.meugestor.app.data.database.dao

import androidx.room.*
import com.meugestor.app.data.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments ORDER BY created_at DESC")
    fun getAll(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE transaction_id = :transactionId ORDER BY created_at DESC")
    fun getByTransactionId(transactionId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE tax_document_id = :taxDocumentId ORDER BY created_at DESC")
    fun getByTaxDocumentId(taxDocumentId: Long): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: AttachmentEntity)
}
