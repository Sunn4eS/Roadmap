package com.example.roadMap.data.utilities // Убедитесь, что это ваш пакет

import androidx.room.TypeConverter

/**
 * Type Converters для преобразования List<String> (для URI) в String (JSON) и обратно для Room.
 * Использует библиотеку Gson для сериализации и десериализации списка строк.
 */
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
