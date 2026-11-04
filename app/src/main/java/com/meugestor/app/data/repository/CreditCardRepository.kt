package com.meugestor.app.data.repository

import androidx.room.withTransaction
import com.meugestor.app.data.database.AppDatabase
import com.meugestor.app.data.database.dao.CreditCardDao
import com.meugestor.app.data.database.dao.TransactionDao
import com.meugestor.app.data.database.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

class CreditCardRepository(
    private val database: AppDatabase,
    private val creditCardDao: CreditCardDao,
    private val transactionDao: TransactionDao
) {

    fun getAllCards(): Flow<List<CreditCardEntity>> = creditCardDao.getAll()

    suspend fun getById(id: Long): CreditCardEntity? = creditCardDao.getById(id)

    suspend fun insert(creditCard: CreditCardEntity): Long = creditCardDao.insert(creditCard)

    suspend fun update(creditCard: CreditCardEntity) = creditCardDao.update(creditCard)

    /**
     * Exclui o cartão e as compras ainda em aberto dele. Compras já pagas continuam no
     * histórico da conta (o vínculo com o cartão é removido pela foreign key SET NULL).
     */
    suspend fun delete(creditCard: CreditCardEntity) {
        database.withTransaction {
            transactionDao.deleteOpenByCreditCard(creditCard.id)
            creditCardDao.delete(creditCard)
        }
    }

    fun getUsedLimit(creditCardId: Long): Flow<Double> = creditCardDao.getUsedLimit(creditCardId)

    fun getAvailableLimit(creditCardId: Long): Flow<Double?> =
        creditCardDao.getAvailableLimit(creditCardId)
}
