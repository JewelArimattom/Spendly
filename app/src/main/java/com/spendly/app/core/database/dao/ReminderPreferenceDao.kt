package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.core.database.entity.ReminderPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderPreferenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preference: ReminderPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(preferences: List<ReminderPreferenceEntity>)

    @Update
    suspend fun update(preference: ReminderPreferenceEntity)

    @Query("DELETE FROM reminder_preferences")
    suspend fun deleteAll()

    @Query("SELECT * FROM reminder_preferences ORDER BY app_name ASC")
    suspend fun getAllSync(): List<ReminderPreferenceEntity>

    @Query("SELECT * FROM reminder_preferences WHERE package_name = :packageName LIMIT 1")
    fun getByPackageName(packageName: String): Flow<ReminderPreferenceEntity?>

    @Query("SELECT * FROM reminder_preferences WHERE package_name = :packageName LIMIT 1")
    suspend fun getPreferenceForPackage(packageName: String): ReminderPreferenceEntity?

    @Query("SELECT * FROM reminder_preferences ORDER BY app_name ASC")
    fun getAllFlow(): Flow<List<ReminderPreferenceEntity>>

    @Query("SELECT * FROM reminder_preferences WHERE enabled = 1")
    fun getAllEnabled(): Flow<List<ReminderPreferenceEntity>>
}
