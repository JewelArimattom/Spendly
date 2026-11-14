package com.spendly.app.domain.repository

import com.spendly.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    suspend fun insertOrUpdate(settings: AppSettings)
    suspend fun saveSettings(settings: AppSettings)
    fun getSettingsFlow(): Flow<AppSettings?>
    suspend fun getSettingsSync(): AppSettings?
}
