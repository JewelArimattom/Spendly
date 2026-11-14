package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.MerchantEntity
import com.spendly.app.domain.model.Merchant

fun MerchantEntity.toDomain(): Merchant = Merchant(
    id = id,
    name = name,
    defaultCategoryId = defaultCategoryId,
    iconName = iconName
)

fun Merchant.toEntity(): MerchantEntity = MerchantEntity(
    id = id,
    name = name,
    defaultCategoryId = defaultCategoryId,
    iconName = iconName
)
