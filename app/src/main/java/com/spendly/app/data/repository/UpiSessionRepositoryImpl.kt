package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.UpiSessionDao
import com.spendly.app.domain.model.UpiSession
import com.spendly.app.domain.repository.UpiSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpiSessionRepositoryImpl @Inject constructor(
    private val upiSessionDao: UpiSessionDao
) : UpiSessionRepository {

    override suspend fun insert(session: UpiSession) =
        upiSessionDao.insert(session.toEntity())

    override suspend fun update(session: UpiSession) =
        upiSessionDao.update(session.toEntity())

    override suspend fun getById(id: String): UpiSession? =
        upiSessionDao.getById(id)?.toDomain()

    override suspend fun getLatestSessionForPackage(packageName: String): UpiSession? =
        upiSessionDao.getLatestSessionForPackage(packageName)?.toDomain()

    override fun getUnreconciledSessions(): Flow<List<UpiSession>> =
        upiSessionDao.getUnreconciledSessions().map { list -> list.map { it.toDomain() } }

    override fun getPendingReminderSessions(): Flow<List<UpiSession>> =
        upiSessionDao.getPendingReminderSessions().map { list -> list.map { it.toDomain() } }

    override fun getAll(): Flow<List<UpiSession>> =
        upiSessionDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun deleteOlderThan(cutoffTime: Long) =
        upiSessionDao.deleteOlderThan(cutoffTime)
}
