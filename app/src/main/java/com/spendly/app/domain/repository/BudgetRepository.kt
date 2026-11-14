package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    suspend fun insert(budget: Budget)
    suspend fun update(budget: Budget)
    suspend fun delete(budget: Budget)
    suspend fun getById(id: String): Budget?
    fun getAll(): Flow<List<Budget>>
    fun getActiveBudgetsForDate(date: Long): Flow<List<Budget>>
}
