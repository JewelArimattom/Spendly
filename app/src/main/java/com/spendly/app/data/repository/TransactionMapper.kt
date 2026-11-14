package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.TransactionEntity
import com.spendly.app.domain.model.PaymentMethod
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amountMinorUnits = amountMinorUnits,
    currency = currency,
    transactionType = TransactionType.valueOf(transactionType),
    merchantName = merchantName,
    categoryId = categoryId,
    purpose = purpose,
    paymentMethod = paymentMethod?.let {
        try { PaymentMethod.valueOf(it) } catch (e: Exception) { null }
    },
    source = try { TransactionSource.valueOf(source) } catch (e: Exception) { TransactionSource.MANUAL },
    sourceReference = sourceReference,
    confidence = confidence,
    status = try { TransactionStatus.valueOf(status) } catch (e: Exception) { TransactionStatus.DETECTED },
    transactionDate = transactionDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    note = note
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountMinorUnits = amountMinorUnits,
    currency = currency,
    transactionType = transactionType.name,
    merchantName = merchantName,
    categoryId = categoryId,
    purpose = purpose,
    paymentMethod = paymentMethod?.name,
    source = source.name,
    sourceReference = sourceReference,
    confidence = confidence,
    status = status.name,
    transactionDate = transactionDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    note = note
)
