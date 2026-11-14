package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    suspend fun insert(budget: Budget): Long
    suspend fun update(budget: Budget)
    suspend fun delete(budget: Budget)
    suspend fun getById(id: Long): Budget?
    fun getAll(): Flow<List<Budget>>
    fun getByCategoryId(categoryId: Long): Flow<List<Budget>>
    fun getActiveBudgets(currentMillis: Long): Flow<List<Budget>>
}
