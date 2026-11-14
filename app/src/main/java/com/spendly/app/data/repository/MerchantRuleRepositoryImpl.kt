package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.MerchantRuleDao
import com.spendly.app.domain.model.MerchantRule
import com.spendly.app.domain.repository.MerchantRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantRuleRepositoryImpl @Inject constructor(
    private val merchantRuleDao: MerchantRuleDao
) : MerchantRuleRepository {

    override suspend fun insert(rule: MerchantRule) =
        merchantRuleDao.insert(rule.toEntity())

    override suspend fun insertAll(rules: List<MerchantRule>) =
        merchantRuleDao.insertAll(rules.map { it.toEntity() })

    override suspend fun update(rule: MerchantRule) =
        merchantRuleDao.update(rule.toEntity())

    override suspend fun delete(rule: MerchantRule) =
        merchantRuleDao.delete(rule.toEntity())

    override suspend fun getById(id: String): MerchantRule? =
        merchantRuleDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<MerchantRule>> =
        merchantRuleDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override fun findMatchingPattern(pattern: String): Flow<List<MerchantRule>> =
        merchantRuleDao.findMatchingPattern(pattern).map { list -> list.map { it.toDomain() } }

    override fun getUserDefinedRules(): Flow<List<MerchantRule>> =
        merchantRuleDao.getUserDefinedRules().map { list -> list.map { it.toDomain() } }
}
