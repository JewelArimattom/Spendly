package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.core.database.entity.MerchantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(merchant: MerchantEntity): Long

    @Update
    suspend fun update(merchant: MerchantEntity)

    @Delete
    suspend fun delete(merchant: MerchantEntity)

    @Query("SELECT * FROM merchants WHERE id = :id")
    suspend fun getById(id: Long): MerchantEntity?

    @Query("SELECT * FROM merchants ORDER BY name ASC")
    fun getAllFlow(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<MerchantEntity>>
}
