package com.spendly.app.domain.model

data class Account(
    val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val balanceMinor: Long = 0L,
    val currencyCode: String = "INR",
    val iconName: String = "account_balance",
    val colorHex: String = "#FF03DAC5",
    val isDefault: Boolean = false,
    val includeInTotal: Boolean = true
)
