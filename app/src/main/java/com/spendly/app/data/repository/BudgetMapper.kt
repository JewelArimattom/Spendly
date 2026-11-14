package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.BudgetEntity
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.BudgetPeriod

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    amountMinorUnits = amountMinorUnits,
    period = try { BudgetPeriod.valueOf(period) } catch (e: Exception) { BudgetPeriod.MONTHLY },
    startDate = startDate,
    endDate = endDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    categoryId = categoryId,
    amountMinorUnits = amountMinorUnits,
    period = period.name,
    startDate = startDate,
    endDate = endDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)
