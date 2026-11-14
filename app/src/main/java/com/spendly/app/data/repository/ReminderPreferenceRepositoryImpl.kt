package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.ReminderPreferenceDao
import com.spendly.app.domain.model.ReminderPreference
import com.spendly.app.domain.repository.ReminderPreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderPreferenceRepositoryImpl @Inject constructor(
    private val reminderPreferenceDao: ReminderPreferenceDao
) : ReminderPreferenceRepository {

    override suspend fun insert(preference: ReminderPreference) =
        reminderPreferenceDao.insert(preference.toEntity())

    override suspend fun insertAll(preferences: List<ReminderPreference>) =
        reminderPreferenceDao.insertAll(preferences.map { it.toEntity() })

    override suspend fun update(preference: ReminderPreference) =
        reminderPreferenceDao.update(preference.toEntity())

    override fun getByPackageName(packageName: String): Flow<ReminderPreference?> =
        reminderPreferenceDao.getByPackageName(packageName).map { it?.toDomain() }

    override fun getAll(): Flow<List<ReminderPreference>> =
        reminderPreferenceDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override fun getAllEnabled(): Flow<List<ReminderPreference>> =
        reminderPreferenceDao.getAllEnabled().map { list -> list.map { it.toDomain() } }
}
