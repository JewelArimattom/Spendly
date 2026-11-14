package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.BudgetDao
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override suspend fun insert(budget: Budget): Long =
        budgetDao.insert(budget.toEntity())

    override suspend fun update(budget: Budget) =
        budgetDao.update(budget.toEntity())

    override suspend fun delete(budget: Budget) =
        budgetDao.delete(budget.toEntity())

    override suspend fun getById(id: Long): Budget? =
        budgetDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Budget>> =
        budgetDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override fun getByCategoryId(categoryId: Long): Flow<List<Budget>> =
        budgetDao.getByCategoryId(categoryId).map { list -> list.map { it.toDomain() } }

    override fun getActiveBudgets(currentMillis: Long): Flow<List<Budget>> =
        budgetDao.getActiveBudgets(currentMillis).map { list -> list.map { it.toDomain() } }
}
