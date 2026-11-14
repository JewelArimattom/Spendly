package com.spendly.app.core.database

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringList(value: String?): List<Long> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").mapNotNull { it.toLongOrNull() }
    }

    @TypeConverter
    fun toStringList(list: List<Long>?): String {
        return list?.joinToString(",") ?: ""
    }
}
