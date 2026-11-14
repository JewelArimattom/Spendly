package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.AppSettingsDao
import com.spendly.app.domain.model.AppSettings
import com.spendly.app.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) : AppSettingsRepository {

    override suspend fun insertOrUpdate(settings: AppSettings) =
        appSettingsDao.insertOrUpdate(settings.toEntity())

    override fun getSettingsFlow(): Flow<AppSettings?> =
        appSettingsDao.getSettingsFlow().map { it?.toDomain() }

    override suspend fun getSettingsSync(): AppSettings? =
        appSettingsDao.getSettingsSync()?.toDomain()
}
