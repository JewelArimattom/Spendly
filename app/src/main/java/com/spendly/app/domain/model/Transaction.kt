package com.spendly.app.domain.model

import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amountMinorUnits: Long,
    val currency: String = "INR",
    val transactionType: TransactionType,
    val merchantName: String? = null,
    val categoryId: String? = null,
    val purpose: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val source: TransactionSource,
    val sourceReference: String? = null,
    val confidence: Float = 1.0f,
    val status: TransactionStatus,
    val transactionDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val note: String? = null
)
