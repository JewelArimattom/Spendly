package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.MerchantDao
import com.spendly.app.domain.model.Merchant
import com.spendly.app.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantRepositoryImpl @Inject constructor(
    private val merchantDao: MerchantDao
) : MerchantRepository {

    override suspend fun insert(merchant: Merchant): Long =
        merchantDao.insert(merchant.toEntity())

    override suspend fun update(merchant: Merchant) =
        merchantDao.update(merchant.toEntity())

    override suspend fun delete(merchant: Merchant) =
        merchantDao.delete(merchant.toEntity())

    override suspend fun getById(id: Long): Merchant? =
        merchantDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Merchant>> =
        merchantDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Merchant>> =
        merchantDao.search(query).map { list -> list.map { it.toDomain() } }
}
