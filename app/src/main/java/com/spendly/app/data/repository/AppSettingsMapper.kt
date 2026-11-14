package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.AppSettingsEntity
import com.spendly.app.domain.model.AppSettings

fun AppSettingsEntity.toDomain(): AppSettings = AppSettings(
    id = id,
    currency = currency,
    currencySymbol = currencySymbol,
    theme = theme,
    biometricEnabled = biometricEnabled,
    pinHash = pinHash,
    autoLockOnBackground = autoLockOnBackground,
    usageMonitoringEnabled = usageMonitoringEnabled,
    notificationMonitoringEnabled = notificationMonitoringEnabled,
    onboardingCompleted = onboardingCompleted,
    updatedAt = updatedAt
)

fun AppSettings.toEntity(): AppSettingsEntity = AppSettingsEntity(
    id = id,
    currency = currency,
    currencySymbol = currencySymbol,
    theme = theme,
    biometricEnabled = biometricEnabled,
    pinHash = pinHash,
    autoLockOnBackground = autoLockOnBackground,
    usageMonitoringEnabled = usageMonitoringEnabled,
    notificationMonitoringEnabled = notificationMonitoringEnabled,
    onboardingCompleted = onboardingCompleted,
    updatedAt = updatedAt
)
