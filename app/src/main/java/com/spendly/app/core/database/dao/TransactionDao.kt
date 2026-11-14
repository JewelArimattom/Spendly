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
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC")
    suspend fun getAllSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transaction_date BETWEEN :start AND :end ORDER BY transaction_date DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY transaction_date DESC")
    fun getByStatus(status: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING_CONFIRMATION' ORDER BY transaction_date DESC")
    fun getPendingTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId ORDER BY transaction_date DESC")
    fun getByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE source = :source ORDER BY transaction_date DESC")
    fun getBySource(source: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE merchant_name LIKE '%' || :query || '%' OR purpose LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY transaction_date DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount_minor_units) FROM transactions WHERE transaction_type = :type AND transaction_date BETWEEN :start AND :end")
    fun getTotalAmountByTypeAndDateRange(type: String, start: Long, end: Long): Flow<Long?>

    @Query("SELECT * FROM transactions WHERE source_reference = :sourceReference LIMIT 1")
    suspend fun getBySourceReference(sourceReference: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transaction_date BETWEEN :start AND :end ORDER BY transaction_date DESC")
    suspend fun getTransactionsBetween(start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE amount_minor_units = :amount AND transaction_date BETWEEN :start AND :end LIMIT 1")
    suspend fun findDuplicate(amount: Long, start: Long, end: Long): TransactionEntity?
}
