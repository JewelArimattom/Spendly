package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY name ASC")
    fun getAllFlow(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId")
    fun getByCategoryId(categoryId: Long): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE start_date_millis <= :currentMillis AND (end_date_millis IS NULL OR end_date_millis >= :currentMillis)")
    fun getActiveBudgets(currentMillis: Long): Flow<List<BudgetEntity>>
}
