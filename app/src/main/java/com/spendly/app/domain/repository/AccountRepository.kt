package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    suspend fun insert(account: Account): Long
    suspend fun update(account: Account)
    suspend fun delete(account: Account)
    suspend fun getById(id: Long): Account?
    fun getAll(): Flow<List<Account>>
    suspend fun getDefault(): Account?
    fun getTotalBalance(): Flow<Long>
    suspend fun updateBalance(id: Long, deltaMinor: Long)
}
