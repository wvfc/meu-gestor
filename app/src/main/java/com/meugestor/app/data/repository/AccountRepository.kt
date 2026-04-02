package com.meugestor.app.data.repository

import com.meugestor.app.data.database.dao.AccountDao
import com.meugestor.app.data.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {

    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAll()

    fun getTotalBalance(): Flow<Double?> = accountDao.getTotalBalance()

    fun getTotalByType(type: String): Flow<Double?> = accountDao.getTotalByType(type)

    suspend fun getById(id: Long): AccountEntity? = accountDao.getById(id)

    suspend fun insert(account: AccountEntity): Long = accountDao.insert(account)

    suspend fun update(account: AccountEntity) = accountDao.update(account)

    suspend fun delete(account: AccountEntity) = accountDao.delete(account)

    suspend fun updateBalance(accountId: Long, amount: Double) =
        accountDao.updateBalance(accountId, amount)
}
