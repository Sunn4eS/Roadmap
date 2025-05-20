package com.example.roadMap.data.maps

import androidx.constraintlayout.utils.widget.MotionLabel

data class MapPoint(
  //  val user: User,
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val description: String,
    val photoPaths: List<String>
)