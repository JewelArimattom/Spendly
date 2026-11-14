package com.spendly.app.domain.model

import java.util.UUID

enum class BudgetPeriod {
    MONTHLY,
    WEEKLY,
    YEARLY
}

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String? = null,
    val amountMinorUnits: Long,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long,
    val endDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
