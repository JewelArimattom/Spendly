package com.spendly.app.data.repository

import com.spendly.app.core.database.dao.TagDao
import com.spendly.app.domain.model.Tag
import com.spendly.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    override suspend fun insert(tag: Tag): Long =
        tagDao.insert(tag.toEntity())

    override suspend fun update(tag: Tag) =
        tagDao.update(tag.toEntity())

    override suspend fun delete(tag: Tag) =
        tagDao.delete(tag.toEntity())

    override suspend fun getById(id: Long): Tag? =
        tagDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Tag>> =
        tagDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getByIds(ids: List<Long>): List<Tag> =
        tagDao.getByIds(ids).map { it.toDomain() }
}
