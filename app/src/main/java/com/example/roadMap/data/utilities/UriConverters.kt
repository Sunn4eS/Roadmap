package com.example.roadMap.data.utilities // Убедитесь, что это ваш пакет

import androidx.room.TypeConverter

class StringListConverter {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toString(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}
