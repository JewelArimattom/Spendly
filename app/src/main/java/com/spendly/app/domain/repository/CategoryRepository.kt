package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun insert(category: Category)
    suspend fun insertAll(categories: List<Category>)
    suspend fun update(category: Category)
    suspend fun delete(category: Category)
    suspend fun getById(id: String): Category?
    fun getAll(): Flow<List<Category>>
    fun getDefaultCategories(): Flow<List<Category>>
    suspend fun getByName(name: String): Category?
}
