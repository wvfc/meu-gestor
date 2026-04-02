package com.meugestor.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meugestor.app.data.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments ORDER BY created_at DESC")
    fun getAll(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE transaction_id = :transactionId ORDER BY created_at DESC")
    fun getByTransaction(transactionId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE tax_document_id = :taxDocumentId ORDER BY created_at DESC")
    fun getByTaxDocument(taxDocumentId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Update
    suspend fun update(attachment: AttachmentEntity)

    @Delete
    suspend fun delete(attachment: AttachmentEntity)
}
