package com.meugestor.app.data.repository

import com.meugestor.app.data.database.dao.AttachmentDao
import com.meugestor.app.data.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

class AttachmentRepository(private val attachmentDao: AttachmentDao) {

    fun getAll(): Flow<List<AttachmentEntity>> = attachmentDao.getAll()

    fun getByTransactionId(transactionId: Long): Flow<List<AttachmentEntity>> =
        attachmentDao.getByTransactionId(transactionId)

    fun getByTaxDocumentId(taxDocumentId: Long): Flow<List<AttachmentEntity>> =
        attachmentDao.getByTaxDocumentId(taxDocumentId)

    suspend fun insert(attachment: AttachmentEntity): Long = attachmentDao.insert(attachment)

    suspend fun delete(attachment: AttachmentEntity) = attachmentDao.delete(attachment)
}
