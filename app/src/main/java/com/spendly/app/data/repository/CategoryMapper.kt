package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.CategoryEntity
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.TransactionType

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    type = TransactionType.valueOf(type),
    isDefault = isDefault,
    parentId = parentId
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    type = type.name,
    isDefault = isDefault,
    parentId = parentId
)
