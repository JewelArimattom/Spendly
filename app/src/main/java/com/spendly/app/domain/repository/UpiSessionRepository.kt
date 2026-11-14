package com.spendly.app.domain.repository

import com.spendly.app.domain.model.UpiSession
import kotlinx.coroutines.flow.Flow

interface UpiSessionRepository {
    suspend fun insert(session: UpiSession)
    suspend fun update(session: UpiSession)
    suspend fun getById(id: String): UpiSession?
    suspend fun getLatestSessionForPackage(packageName: String): UpiSession?
    fun getUnreconciledSessions(): Flow<List<UpiSession>>
    fun getPendingReminderSessions(): Flow<List<UpiSession>>
    fun getAll(): Flow<List<UpiSession>>
    suspend fun deleteOlderThan(cutoffTime: Long)
}
