package com.spendly.app.domain.model

data class TransactionCandidate(
    val amountMinorUnits: Long,
    val currency: String = "INR",
    val transactionType: TransactionType = TransactionType.DEBIT,
    val merchantName: String? = null,
    val purpose: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val sourcePackage: String,
    val referenceId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val isFailed: Boolean = false,
    val rawTitle: String? = null,
    val rawText: String? = null
)
