package com.spendly.app.domain.model

data class Transaction(
    val id: Long = 0L,
    val amountMinor: Long,
    val currencyCode: String = "INR",
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long,
    val note: String = "",
    val merchantName: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
    val transactionDateMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val tagIds: List<Long> = emptyList()
)
