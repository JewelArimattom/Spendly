package com.spendly.app.domain.repository

import com.spendly.app.domain.model.MerchantRule
import kotlinx.coroutines.flow.Flow

interface MerchantRuleRepository {
    suspend fun insert(rule: MerchantRule)
    suspend fun insertAll(rules: List<MerchantRule>)
    suspend fun update(rule: MerchantRule)
    suspend fun delete(rule: MerchantRule)
    suspend fun getById(id: String): MerchantRule?
    fun getAll(): Flow<List<MerchantRule>>
    fun findMatchingPattern(pattern: String): Flow<List<MerchantRule>>
    fun getUserDefinedRules(): Flow<List<MerchantRule>>
}
