package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parent_id IS NULL ORDER BY name ASC")
    fun getRootCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parent_id = :parentId ORDER BY name ASC")
    fun getSubCategories(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_default = 1")
    fun getDefaults(): Flow<List<CategoryEntity>>
}
