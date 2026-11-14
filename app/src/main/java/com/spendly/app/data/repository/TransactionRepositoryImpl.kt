package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
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

    override suspend fun insertAll(transactions: List<Transaction>) =
        transactionDao.insertAll(transactions.map { it.toEntity() })

    override suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction.toEntity())

    override suspend fun delete(transaction: Transaction) =
        transactionDao.delete(transaction.toEntity())

    override suspend fun deleteById(id: String) =
        transactionDao.deleteById(id)

    override suspend fun deleteAll() =
        transactionDao.deleteAll()

    override suspend fun getById(id: String): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Transaction>> =
        transactionDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override fun getRecent(limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(start, end).map { list -> list.map { it.toDomain() } }

    override fun getByStatus(status: TransactionStatus): Flow<List<Transaction>> =
        transactionDao.getByStatus(status.name).map { list -> list.map { it.toDomain() } }

    override fun getPendingTransactions(): Flow<List<Transaction>> =
        transactionDao.getPendingTransactions().map { list -> list.map { it.toDomain() } }

    override fun getByCategory(categoryId: String): Flow<List<Transaction>> =
        transactionDao.getByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override fun getBySource(source: String): Flow<List<Transaction>> =
        transactionDao.getBySource(source).map { list -> list.map { it.toDomain() } }

    override fun searchTransactions(query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions(query).map { list -> list.map { it.toDomain() } }

    override fun getTotalAmountByTypeAndDateRange(type: TransactionType, start: Long, end: Long): Flow<Long> =
        transactionDao.getTotalAmountByTypeAndDateRange(type.name, start, end).map { it ?: 0L }

    override suspend fun getBySourceReference(sourceReference: String): Transaction? =
        transactionDao.getBySourceReference(sourceReference)?.toDomain()
}
