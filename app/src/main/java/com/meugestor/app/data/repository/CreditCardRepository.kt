package com.meugestor.app.data.repository

import com.meugestor.app.data.database.dao.CreditCardDao
import com.meugestor.app.data.database.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

class CreditCardRepository(private val creditCardDao: CreditCardDao) {

    fun getAllCards(): Flow<List<CreditCardEntity>> = creditCardDao.getAll()

    suspend fun getById(id: Long): CreditCardEntity? = creditCardDao.getById(id)

    suspend fun insert(creditCard: CreditCardEntity): Long = creditCardDao.insert(creditCard)

    suspend fun update(creditCard: CreditCardEntity) = creditCardDao.update(creditCard)

    suspend fun delete(creditCard: CreditCardEntity) = creditCardDao.delete(creditCard)

    fun getUsedLimit(creditCardId: Long): Flow<Double> = creditCardDao.getUsedLimit(creditCardId)

    fun getAvailableLimit(creditCardId: Long): Flow<Double?> =
        creditCardDao.getAvailableLimit(creditCardId)
}
