package com.spendly.app.domain.model

data class Budget(
    val id: Long = 0L,
    val name: String,
    val amountMinor: Long,
    val currencyCode: String = "INR",
    val categoryId: Long? = null,
    val periodType: RecurrenceType = RecurrenceType.MONTHLY,
    val startDateMillis: Long,
    val endDateMillis: Long? = null,
    val spentMinor: Long = 0L
)
