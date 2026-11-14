package com.spendly.app.data.repository

import com.spendly.app.core.database.entity.UpiSessionEntity
import com.spendly.app.domain.model.ReminderStatus
import com.spendly.app.domain.model.UpiSession
import com.spendly.app.domain.model.UpiSessionStatus

fun UpiSessionEntity.toDomain(): UpiSession = UpiSession(
    id = id,
    packageName = packageName,
    startedAt = startedAt,
    endedAt = endedAt,
    sessionStatus = try { UpiSessionStatus.valueOf(sessionStatus) } catch (e: Exception) { UpiSessionStatus.ACTIVE },
    reminderStatus = try { ReminderStatus.valueOf(reminderStatus) } catch (e: Exception) { ReminderStatus.NOT_NEEDED },
    associatedTransactionIds = associatedTransactionIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    createdAt = createdAt
)

fun UpiSession.toEntity(): UpiSessionEntity = UpiSessionEntity(
    id = id,
    packageName = packageName,
    startedAt = startedAt,
    endedAt = endedAt,
    sessionStatus = sessionStatus.name,
    reminderStatus = reminderStatus.name,
    associatedTransactionIds = if (associatedTransactionIds.isEmpty()) null else associatedTransactionIds.joinToString(","),
    createdAt = createdAt
)
