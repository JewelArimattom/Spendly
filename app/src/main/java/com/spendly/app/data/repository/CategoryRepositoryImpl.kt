package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.CategoryDao
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override suspend fun insert(category: Category): Long =
        categoryDao.insert(category.toEntity())

    override suspend fun update(category: Category) =
        categoryDao.update(category.toEntity())

    override suspend fun delete(category: Category) =
        categoryDao.delete(category.toEntity())

    override suspend fun getById(id: Long): Category? =
        categoryDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Category>> =
        categoryDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override fun getByType(type: String): Flow<List<Category>> =
        categoryDao.getByType(type).map { list -> list.map { it.toDomain() } }

    override fun getRootCategories(): Flow<List<Category>> =
        categoryDao.getRootCategories().map { list -> list.map { it.toDomain() } }

    override fun getSubCategories(parentId: Long): Flow<List<Category>> =
        categoryDao.getSubCategories(parentId).map { list -> list.map { it.toDomain() } }
}
