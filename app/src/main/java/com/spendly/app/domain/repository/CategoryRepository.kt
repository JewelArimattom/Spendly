package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun insert(category: Category): Long
    suspend fun update(category: Category)
    suspend fun delete(category: Category)
    suspend fun getById(id: Long): Category?
    fun getAll(): Flow<List<Category>>
    fun getByType(type: String): Flow<List<Category>>
    fun getRootCategories(): Flow<List<Category>>
    fun getSubCategories(parentId: Long): Flow<List<Category>>
}
