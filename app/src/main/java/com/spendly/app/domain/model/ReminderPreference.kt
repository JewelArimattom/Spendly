package com.spendly.app.domain.model

import java.util.UUID

data class ReminderPreference(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val enabled: Boolean = true,
    val cooldownMinutes: Int = 30,
    val maximumDailyReminders: Int = 5,
    val quietHoursStart: Int? = null,
    val quietHoursEnd: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
