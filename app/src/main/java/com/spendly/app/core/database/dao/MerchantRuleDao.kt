package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.core.database.entity.MerchantRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MerchantRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<MerchantRuleEntity>)

    @Update
    suspend fun update(rule: MerchantRuleEntity)

    @Delete
    suspend fun delete(rule: MerchantRuleEntity)

    @Query("SELECT * FROM merchant_rules WHERE id = :id")
    suspend fun getById(id: String): MerchantRuleEntity?

    @Query("SELECT * FROM merchant_rules ORDER BY confidence DESC")
    fun getAllFlow(): Flow<List<MerchantRuleEntity>>

    @Query("SELECT * FROM merchant_rules WHERE :pattern LIKE '%' || merchant_pattern || '%' OR merchant_pattern LIKE '%' || :pattern || '%'")
    fun findMatchingPattern(pattern: String): Flow<List<MerchantRuleEntity>>

    @Query("SELECT * FROM merchant_rules WHERE is_user_defined = 1 ORDER BY updated_at DESC")
    fun getUserDefinedRules(): Flow<List<MerchantRuleEntity>>
}
