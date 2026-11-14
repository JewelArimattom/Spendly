package com.spendly.app.domain.model

data class Merchant(
    val id: Long = 0L,
    val name: String,
    val defaultCategoryId: Long? = null,
    val iconName: String = "store"
)
