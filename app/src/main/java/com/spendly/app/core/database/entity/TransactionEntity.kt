package com.spendly.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["account_id"]),
        Index(value = ["transaction_date_millis"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String = "INR",

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "merchant_name")
    val merchantName: String = "",

    @ColumnInfo(name = "source")
    val source: String = "MANUAL",

    @ColumnInfo(name = "transaction_date_millis")
    val transactionDateMillis: Long,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "recurrence_type")
    val recurrenceType: String = "NONE"
)
