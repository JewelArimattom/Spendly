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
    suspend fun insert(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY created_at DESC")
    suspend fun getAllSync(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE start_date <= :date AND (end_date IS NULL OR end_date >= :date)")
    fun getActiveBudgetsForDate(date: Long): Flow<List<BudgetEntity>>
}
