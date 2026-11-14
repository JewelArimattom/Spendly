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

    @Update
    suspend fun update(session: UpiSessionEntity)

    @Query("SELECT * FROM upi_sessions WHERE id = :id")
    suspend fun getById(id: String): UpiSessionEntity?

    @Query("SELECT * FROM upi_sessions WHERE package_name = :packageName ORDER BY started_at DESC LIMIT 1")
    suspend fun getLatestSessionForPackage(packageName: String): UpiSessionEntity?

    @Query("SELECT * FROM upi_sessions WHERE session_status = 'ENDED' AND associated_transaction_ids IS NULL ORDER BY started_at DESC")
    fun getUnreconciledSessions(): Flow<List<UpiSessionEntity>>

    @Query("SELECT * FROM upi_sessions WHERE reminder_status = 'SCHEDULED' ORDER BY started_at DESC")
    fun getPendingReminderSessions(): Flow<List<UpiSessionEntity>>

    @Query("SELECT * FROM upi_sessions ORDER BY started_at DESC")
    fun getAllFlow(): Flow<List<UpiSessionEntity>>

    @Query("DELETE FROM upi_sessions WHERE started_at < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}
