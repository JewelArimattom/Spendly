package com.spendly.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "default_category_id")
    val defaultCategoryId: Long? = null,

    @ColumnInfo(name = "icon_name")
    val iconName: String = "store"
)
