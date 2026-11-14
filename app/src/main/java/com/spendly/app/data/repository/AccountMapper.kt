package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.AccountEntity
import com.spendly.app.domain.model.Account
import com.spendly.app.domain.model.AccountType

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    balanceMinor = balanceMinor,
    currencyCode = currencyCode,
    iconName = iconName,
    colorHex = colorHex,
    isDefault = isDefault,
    includeInTotal = includeInTotal
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    balanceMinor = balanceMinor,
    currencyCode = currencyCode,
    iconName = iconName,
    colorHex = colorHex,
    isDefault = isDefault,
    includeInTotal = includeInTotal
)
