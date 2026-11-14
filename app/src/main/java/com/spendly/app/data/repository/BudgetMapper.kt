package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.BudgetEntity
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.RecurrenceType

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    name = name,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    categoryId = categoryId,
    periodType = RecurrenceType.valueOf(periodType),
    startDateMillis = startDateMillis,
    endDateMillis = endDateMillis
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    categoryId = categoryId,
    periodType = periodType.name,
    startDateMillis = startDateMillis,
    endDateMillis = endDateMillis
)
