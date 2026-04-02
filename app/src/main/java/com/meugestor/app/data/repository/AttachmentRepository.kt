package com.meugestor.app.data.repository

import com.meugestor.app.data.database.dao.AttachmentDao
import com.meugestor.app.data.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

class AttachmentRepository(private val attachmentDao: AttachmentDao) {

    fun getByTransaction(transactionId: Long): Flow<List<AttachmentEntity>> =
        attachmentDao.getByTransaction(transactionId)

    fun getByTaxDocument(taxDocumentId: Long): Flow<List<AttachmentEntity>> =
        attachmentDao.getByTaxDocument(taxDocumentId)

    suspend fun getById(id: Long): AttachmentEntity? = attachmentDao.getById(id)

    suspend fun insert(attachment: AttachmentEntity): Long = attachmentDao.insert(attachment)

    suspend fun update(attachment: AttachmentEntity) = attachmentDao.update(attachment)

    suspend fun delete(attachment: AttachmentEntity) = attachmentDao.delete(attachment)
}
