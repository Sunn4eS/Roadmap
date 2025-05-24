package com.example.roadMap.data.database // Убедитесь, что это ваш пакет

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type Converters для преобразования List<String> (для URI) в String (JSON) и обратно для Room.
 * Использует библиотеку Gson для сериализации и десериализации списка строк.
 */
class UriConverters {
    private val gson = Gson()

    /**
     * Преобразует List<String> (список URI в виде строк) в JSON-строку.
     * Этот метод используется Room при сохранении List<String> в базу данных.
     * @param value Список строк, который нужно преобразовать в JSON.
     * @return JSON-строка, представляющая список.
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        // Если список null, возвращаем пустую JSON-строку или null, в зависимости от логики.
        // Здесь возвращаем JSON-представление null, что корректно обрабатывается Gson.
        return gson.toJson(value)
    }

    /**
     * Преобразует JSON-строку обратно в List<String>.
     * Этот метод используется Room при чтении строки из базы данных и преобразовании ее в List<String>.
     * @param value JSON-строка, которую нужно преобразовать в список строк.
     * @return Список строк, полученный из JSON.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        // Если строка null или пустая, возвращаем null или пустой список.
        if (value == null || value.isEmpty()) {
            return null
        }
        // Определяем тип List<String> для корректной десериализации Gson.
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
