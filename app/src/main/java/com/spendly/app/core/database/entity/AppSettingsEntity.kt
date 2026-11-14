package com.spendly.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: String = "default",

    @ColumnInfo(name = "currency")
    val currency: String = "INR",

    @ColumnInfo(name = "currency_symbol")
    val currencySymbol: String = "₹",

    @ColumnInfo(name = "theme")
    val theme: String = "system",

    @ColumnInfo(name = "biometric_enabled")
    val biometricEnabled: Boolean = false,

    @ColumnInfo(name = "pin_hash")
    val pinHash: String? = null,

    @ColumnInfo(name = "auto_lock_on_background")
    val autoLockOnBackground: Boolean = false,

    @ColumnInfo(name = "usage_monitoring_enabled")
    val usageMonitoringEnabled: Boolean = true,

    @ColumnInfo(name = "notification_monitoring_enabled")
    val notificationMonitoringEnabled: Boolean = true,

    @ColumnInfo(name = "onboarding_completed")
    val onboardingCompleted: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
