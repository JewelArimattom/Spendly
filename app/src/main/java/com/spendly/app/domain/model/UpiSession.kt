package com.spendly.app.domain.model

enum class UpiSessionStatus {
    ACTIVE,
    ENDED,
    DISCARDED
}

enum class ReminderStatus {
    NOT_NEEDED,
    SCHEDULED,
    SHOWN,
    CONFIRMED_PAID,
    CONFIRMED_NO_PAYMENT,
    DISMISSED
}

data class UpiSession(
    val id: String,
    val packageName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val sessionStatus: UpiSessionStatus = UpiSessionStatus.ACTIVE,
    val reminderStatus: ReminderStatus = ReminderStatus.NOT_NEEDED,
    val associatedTransactionIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
