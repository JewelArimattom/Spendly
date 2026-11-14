package com.spendly.app.domain.repository

import com.spendly.app.domain.model.ReminderPreference
import kotlinx.coroutines.flow.Flow

interface ReminderPreferenceRepository {
    suspend fun insert(preference: ReminderPreference)
    suspend fun insertAll(preferences: List<ReminderPreference>)
    suspend fun update(preference: ReminderPreference)
    fun getByPackageName(packageName: String): Flow<ReminderPreference?>
    fun getAll(): Flow<List<ReminderPreference>>
    fun getAllEnabled(): Flow<List<ReminderPreference>>
}
