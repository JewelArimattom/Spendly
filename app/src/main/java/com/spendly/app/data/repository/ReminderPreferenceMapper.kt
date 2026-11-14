package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.ReminderPreferenceEntity
import com.spendly.app.domain.model.ReminderPreference

fun ReminderPreferenceEntity.toDomain(): ReminderPreference = ReminderPreference(
    id = id,
    packageName = packageName,
    appName = appName,
    enabled = enabled,
    cooldownMinutes = cooldownMinutes,
    maximumDailyReminders = maximumDailyReminders,
    quietHoursStart = quietHoursStart,
    quietHoursEnd = quietHoursEnd,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ReminderPreference.toEntity(): ReminderPreferenceEntity = ReminderPreferenceEntity(
    id = id,
    packageName = packageName,
    appName = appName,
    enabled = enabled,
    cooldownMinutes = cooldownMinutes,
    maximumDailyReminders = maximumDailyReminders,
    quietHoursStart = quietHoursStart,
    quietHoursEnd = quietHoursEnd,
    createdAt = createdAt,
    updatedAt = updatedAt
)
