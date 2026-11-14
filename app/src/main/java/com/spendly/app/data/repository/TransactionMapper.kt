package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.TransactionEntity
import com.spendly.app.domain.model.RecurrenceType
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionType

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    type = TransactionType.valueOf(type),
    categoryId = categoryId ?: 0L,
    accountId = accountId,
    note = note,
    merchantName = merchantName,
    source = TransactionSource.valueOf(source),
    transactionDateMillis = transactionDateMillis,
    createdAtMillis = createdAtMillis,
    isRecurring = isRecurring,
    recurrenceType = RecurrenceType.valueOf(recurrenceType)
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    type = type.name,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    merchantName = merchantName,
    source = source.name,
    transactionDateMillis = transactionDateMillis,
    createdAtMillis = createdAtMillis,
    isRecurring = isRecurring,
    recurrenceType = recurrenceType.name
)
