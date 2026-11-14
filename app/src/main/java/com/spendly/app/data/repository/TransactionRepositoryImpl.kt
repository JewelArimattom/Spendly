package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override suspend fun insert(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())

    override suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction.toEntity())

    override suspend fun delete(id: Long) =
        transactionDao.deleteById(id)

    override suspend fun getById(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Transaction>> =
        transactionDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startMillis, endMillis).map { list -> list.map { it.toDomain() } }

    override fun getByCategoryId(categoryId: Long): Flow<List<Transaction>> =
        transactionDao.getByCategoryId(categoryId).map { list -> list.map { it.toDomain() } }

    override fun getByAccountId(accountId: Long): Flow<List<Transaction>> =
        transactionDao.getByAccountId(accountId).map { list -> list.map { it.toDomain() } }

    override fun getByType(type: String): Flow<List<Transaction>> =
        transactionDao.getByType(type).map { list -> list.map { it.toDomain() } }

    override fun getSumByTypeAndDateRange(type: String, startMillis: Long, endMillis: Long): Flow<Long> =
        transactionDao.getSumByTypeAndDateRange(type, startMillis, endMillis).map { it ?: 0L }

    override fun getSumByCategoryAndDateRange(type: String, categoryId: Long, startMillis: Long, endMillis: Long): Flow<Long> =
        transactionDao.getSumByCategoryAndDateRange(type, categoryId, startMillis, endMillis).map { it ?: 0L }
}
