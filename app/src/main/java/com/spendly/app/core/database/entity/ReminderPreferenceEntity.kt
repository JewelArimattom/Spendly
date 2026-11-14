package com.spendly.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "reminder_preferences",
    indices = [
        Index(value = ["package_name"], unique = true)
    ]
)
data class ReminderPreferenceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "cooldown_minutes")
    val cooldownMinutes: Int = 30,

    @ColumnInfo(name = "maximum_daily_reminders")
    val maximumDailyReminders: Int = 5,

    @ColumnInfo(name = "quiet_hours_start")
    val quietHoursStart: Int? = null,

    @ColumnInfo(name = "quiet_hours_end")
    val quietHoursEnd: Int? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
