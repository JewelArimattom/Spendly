package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    suspend fun insert(tag: Tag): Long
    suspend fun update(tag: Tag)
    suspend fun delete(tag: Tag)
    suspend fun getById(id: Long): Tag?
    fun getAll(): Flow<List<Tag>>
    suspend fun getByIds(ids: List<Long>): List<Tag>
}
