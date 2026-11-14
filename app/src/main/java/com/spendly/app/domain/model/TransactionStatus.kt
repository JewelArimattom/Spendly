package com.spendly.app.domain.model

enum class TransactionStatus {
    DETECTED,
    PENDING_CONFIRMATION,
    CONFIRMED,
    FAILED,
    REVERSED,
    REFUNDED,
    DUPLICATE_CANDIDATE
}
