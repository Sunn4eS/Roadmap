package com.example.roadMap.data.module

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yandex.mapkit.geometry.Point

@Entity(tableName = "map_points")
data class MapPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String?,
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val description: String,
    val photoUris: List<String>
) {
    fun toYandexPoint(): Point {
        return Point(latitude, longitude)
    }
}