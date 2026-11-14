package com.spendly.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "upi_sessions",
    indices = [
        Index(value = ["package_name"]),
        Index(value = ["started_at"])
    ]
)
data class UpiSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "ended_at")
    val endedAt: Long? = null,

    @ColumnInfo(name = "session_status")
    val sessionStatus: String = "ACTIVE",

    @ColumnInfo(name = "reminder_status")
    val reminderStatus: String = "NOT_NEEDED",

    @ColumnInfo(name = "associated_transaction_ids")
    val associatedTransactionIds: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
