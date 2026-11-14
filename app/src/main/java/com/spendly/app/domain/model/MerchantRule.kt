package com.spendly.app.domain.model

import java.util.UUID

data class MerchantRule(
    val id: String = UUID.randomUUID().toString(),
    val merchantPattern: String,
    val categoryId: String? = null,
    val preferredName: String? = null,
    val confidence: Float = 1.0f,
    val isUserDefined: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
