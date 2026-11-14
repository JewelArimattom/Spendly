package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<TagEntity>
}
