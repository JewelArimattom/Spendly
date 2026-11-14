package com.spendly.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "balance_minor")
    val balanceMinor: Long = 0L,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String = "INR",

    @ColumnInfo(name = "icon_name")
    val iconName: String = "account_balance",

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#FF03DAC5",

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "include_in_total")
    val includeInTotal: Boolean = true
)
