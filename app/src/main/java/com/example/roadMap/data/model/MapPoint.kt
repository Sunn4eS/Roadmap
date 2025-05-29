package com.example.roadMap.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yandex.mapkit.geometry.Point

@Entity(tableName = "map_points")
data class MapPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // ID точки (будет генерироваться Room или приходить с сервера)
    @ColumnInfo(name = "userId")
    val userId: String?, // ID пользователя, которому принадлежит точка
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    @ColumnInfo(name = "photo_uris")
    val photoUris: List<String>, // Список URI фотографий

) {
    // Функция-расширение для MapPoint в Yandex Point
    fun toYandexPoint(): Point {
        return Point(this.latitude, this.longitude)
    }
}