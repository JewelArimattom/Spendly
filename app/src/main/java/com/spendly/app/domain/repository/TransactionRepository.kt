package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insert(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): Transaction?
    fun getAll(): Flow<List<Transaction>>
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>>
    fun getByCategoryId(categoryId: Long): Flow<List<Transaction>>
    fun getByAccountId(accountId: Long): Flow<List<Transaction>>
    fun getByType(type: String): Flow<List<Transaction>>
    fun getSumByTypeAndDateRange(type: String, startMillis: Long, endMillis: Long): Flow<Long>
    fun getSumByCategoryAndDateRange(type: String, categoryId: Long, startMillis: Long, endMillis: Long): Flow<Long>
}
