package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Merchant
import kotlinx.coroutines.flow.Flow

interface MerchantRepository {
    suspend fun insert(merchant: Merchant): Long
    suspend fun update(merchant: Merchant)
    suspend fun delete(merchant: Merchant)
    suspend fun getById(id: Long): Merchant?
    fun getAll(): Flow<List<Merchant>>
    fun search(query: String): Flow<List<Merchant>>
}
