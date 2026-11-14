package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.MerchantRuleEntity
import com.spendly.app.domain.model.MerchantRule

fun MerchantRuleEntity.toDomain(): MerchantRule = MerchantRule(
    id = id,
    merchantPattern = merchantPattern,
    categoryId = categoryId,
    preferredName = preferredName,
    confidence = confidence,
    isUserDefined = isUserDefined,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MerchantRule.toEntity(): MerchantRuleEntity = MerchantRuleEntity(
    id = id,
    merchantPattern = merchantPattern,
    categoryId = categoryId,
    preferredName = preferredName,
    confidence = confidence,
    isUserDefined = isUserDefined,
    createdAt = createdAt,
    updatedAt = updatedAt
)
