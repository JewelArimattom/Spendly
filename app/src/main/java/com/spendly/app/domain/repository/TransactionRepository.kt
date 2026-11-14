package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insert(transaction: Transaction): Long
    suspend fun insertAll(transactions: List<Transaction>)
    suspend fun update(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    suspend fun deleteById(id: String)
    suspend fun deleteAll()
    suspend fun getById(id: String): Transaction?
    fun getAll(): Flow<List<Transaction>>
    fun getRecent(limit: Int): Flow<List<Transaction>>
    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>>
    fun getByStatus(status: TransactionStatus): Flow<List<Transaction>>
    fun getPendingTransactions(): Flow<List<Transaction>>
    fun getByCategory(categoryId: String): Flow<List<Transaction>>
    fun getBySource(source: String): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    fun getTotalAmountByTypeAndDateRange(type: TransactionType, start: Long, end: Long): Flow<Long>
    suspend fun getBySourceReference(sourceReference: String): Transaction?
}
