package com.spendly.app.domain.model

data class Category(
    val id: Long = 0L,
    val name: String,
    val iconName: String = "category",
    val colorHex: String = "#FF6200EE",
    val type: TransactionType,
    val isDefault: Boolean = false,
    val parentId: Long? = null
)
