package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY transaction_date_millis DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transaction_date_millis BETWEEN :startMillis AND :endMillis ORDER BY transaction_date_millis DESC")
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId ORDER BY transaction_date_millis DESC")
    fun getByCategoryId(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY transaction_date_millis DESC")
    fun getByAccountId(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY transaction_date_millis DESC")
    fun getByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount_minor) FROM transactions WHERE type = :type AND transaction_date_millis BETWEEN :startMillis AND :endMillis")
    fun getSumByTypeAndDateRange(type: String, startMillis: Long, endMillis: Long): Flow<Long?>

    @Query("SELECT SUM(amount_minor) FROM transactions WHERE type = :type AND category_id = :categoryId AND transaction_date_millis BETWEEN :startMillis AND :endMillis")
    fun getSumByCategoryAndDateRange(type: String, categoryId: Long, startMillis: Long, endMillis: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getCount(): Flow<Int>
}
