package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendly.app.core.database.entity.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: TransactionTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(crossRefs: List<TransactionTagCrossRef>)

    @Query("DELETE FROM transaction_tag_cross_ref WHERE transaction_id = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Long)

    @Query("DELETE FROM transaction_tag_cross_ref WHERE transaction_id = :transactionId AND tag_id = :tagId")
    suspend fun delete(transactionId: Long, tagId: Long)

    @Query("SELECT tag_id FROM transaction_tag_cross_ref WHERE transaction_id = :transactionId")
    fun getTagIdsForTransaction(transactionId: Long): Flow<List<Long>>

    @Query("SELECT transaction_id FROM transaction_tag_cross_ref WHERE tag_id = :tagId")
    fun getTransactionIdsForTag(tagId: Long): Flow<List<Long>>
}
