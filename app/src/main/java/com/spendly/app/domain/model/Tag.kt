package com.spendly.app.domain.model

data class Tag(
    val id: Long = 0L,
    val name: String,
    val colorHex: String = "#FFBB86FC"
)
