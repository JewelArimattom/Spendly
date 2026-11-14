package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.AccountDao
import com.spendly.app.domain.model.Account
import com.spendly.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override suspend fun insert(account: Account): Long =
        accountDao.insert(account.toEntity())

    override suspend fun update(account: Account) =
        accountDao.update(account.toEntity())

    override suspend fun delete(account: Account) =
        accountDao.delete(account.toEntity())

    override suspend fun getById(id: Long): Account? =
        accountDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Account>> =
        accountDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getDefault(): Account? =
        accountDao.getDefault()?.toDomain()

    override fun getTotalBalance(): Flow<Long> =
        accountDao.getTotalBalance().map { it ?: 0L }

    override suspend fun updateBalance(id: Long, deltaMinor: Long) =
        accountDao.updateBalance(id, deltaMinor)
}
