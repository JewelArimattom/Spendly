package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.TagEntity
import com.spendly.app.domain.model.Tag

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    colorHex = colorHex
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    colorHex = colorHex
)
