package com.example.roadMap.data.maps

import androidx.constraintlayout.utils.widget.MotionLabel
import com.example.roadMap.data.dataBase.UserEntity

data class MapPoint(
    val user: UserEntity,
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val description: String,
    val photoPaths: List<String>
)