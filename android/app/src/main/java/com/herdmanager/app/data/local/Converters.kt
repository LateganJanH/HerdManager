package com.herdmanager.app.data.local

import androidx.room.TypeConverter

class Converters {
    private val delimiter = "||"

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        if (value.isEmpty()) "" else value.joinToString(delimiter)

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(delimiter).filter { it.isNotBlank() }
    }
}
