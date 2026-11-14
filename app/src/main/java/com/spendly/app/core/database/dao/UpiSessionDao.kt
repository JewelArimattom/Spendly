package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.core.database.entity.UpiSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UpiSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: UpiSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<UpiSessionEntity>)

    @Update
    suspend fun update(session: UpiSessionEntity)

    @Query("DELETE FROM upi_sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM upi_sessions WHERE id = :id")
    suspend fun getById(id: String): UpiSessionEntity?

    @Query("SELECT * FROM upi_sessions WHERE package_name = :packageName ORDER BY started_at DESC LIMIT 1")
    suspend fun getLatestSessionForPackage(packageName: String): UpiSessionEntity?

    @Query("SELECT * FROM upi_sessions WHERE session_status = 'ENDED' AND associated_transaction_ids IS NULL ORDER BY started_at DESC")
    fun getUnreconciledSessions(): Flow<List<UpiSessionEntity>>

    @Query("SELECT * FROM upi_sessions WHERE reminder_status = 'SCHEDULED' ORDER BY started_at DESC")
    fun getPendingReminderSessions(): Flow<List<UpiSessionEntity>>

    @Query("SELECT * FROM upi_sessions WHERE reminder_status = 'SCHEDULED' AND session_status = 'ENDED' ORDER BY started_at DESC")
    suspend fun getPendingScheduledReminders(): List<UpiSessionEntity>

    @Query("SELECT * FROM upi_sessions WHERE package_name = :packageName AND session_status = 'ACTIVE' ORDER BY started_at DESC LIMIT 1")
    suspend fun getActiveSession(packageName: String): UpiSessionEntity?

    @Query("SELECT * FROM upi_sessions WHERE session_status = 'ACTIVE'")
    suspend fun getAllActiveSessions(): List<UpiSessionEntity>

    @Query("SELECT * FROM upi_sessions WHERE package_name = :packageName AND (ended_at >= :sinceTime OR started_at >= :sinceTime) ORDER BY started_at DESC LIMIT 1")
    suspend fun getRecentSession(packageName: String, sinceTime: Long): UpiSessionEntity?

    @Query("SELECT * FROM upi_sessions WHERE ended_at >= :sinceTime OR started_at >= :sinceTime ORDER BY started_at DESC LIMIT 1")
    suspend fun getAnyRecentSession(sinceTime: Long): UpiSessionEntity?

    @Query("SELECT COUNT(*) FROM upi_sessions WHERE package_name = :packageName AND reminder_status IN ('SHOWN', 'CONFIRMED_PAID', 'CONFIRMED_NO_PAYMENT') AND started_at >= :sinceTime")
    suspend fun countRemindersSentSince(packageName: String, sinceTime: Long): Int

    @Query("SELECT MAX(started_at) FROM upi_sessions WHERE package_name = :packageName AND reminder_status IN ('SHOWN', 'CONFIRMED_PAID', 'CONFIRMED_NO_PAYMENT')")
    suspend fun getLastReminderTime(packageName: String): Long?

    @Query("SELECT * FROM upi_sessions ORDER BY started_at DESC")
    fun getAllFlow(): Flow<List<UpiSessionEntity>>

    @Query("SELECT * FROM upi_sessions ORDER BY started_at DESC")
    suspend fun getAllSync(): List<UpiSessionEntity>

    @Query("DELETE FROM upi_sessions WHERE started_at < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}
