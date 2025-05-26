package com.example.roadMap.data.maps

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import com.example.roadMap.data.dataBase.AppDatabase
import com.example.roadMap.data.module.MapPoint
import com.example.roadMap.data.utilities.distance
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.forEach


@Composable
fun GetPointFromMap(
    userMapPoints: List<MapPoint>,
    context: Context,
    tapRadiusMeters: Float,
    mapView: MapView,
    mapObjects: MapObjectCollection,
    loggedInUsername: String?,
) {
    val mapPointDao = remember { AppDatabase.getDatabase(context).mapPointDao() }

    val tapListener = remember {
        MapObjectTapListener { mapObject, tapPoint ->
            if (mapObject is PlacemarkMapObject) {
                // Стандартное касание метки
                val tappedPoint = userMapPoints.find { it.toYandexPoint() == mapObject.geometry }
                tappedPoint?.let {
                    Toast.makeText(context, "Нажата метка (точно): ${it.label}", Toast.LENGTH_SHORT)
                        .show()
                    Log.d("MapTap", "Точное нажатие на метку: ${it.label}")
                    return@MapObjectTapListener true
                }
            }

            // Проверяем, находится ли нажатие в радиусе от какой-либо метки
            userMapPoints.forEach { mapPoint ->
                var point = mapPoint.toYandexPoint()
                val dist = distance(tapPoint.latitude, tapPoint.longitude, point.latitude, point.longitude)
                val showMenuAtLocation : MutableState<Point?>
                if (dist <= tapRadiusMeters) {
//                    CustomMapPointDialog(
//                        showDialog = true,
//                        point = point,
//                        onDismissRequest = {  },
//                        onSavePoint = { name, description, photoUris ->
//
//                            val newMapPoint = MapPoint(
//                                userId = loggedInUsername,
//                                label = mapPoint.label,
//                                description = mapPoint.description,
//                                latitude = point.latitude,
//                                longitude = point.longitude,
//                                photoUris = mapPoint.photoUris
//                            )
//                            coroutineScope.launch(Dispatchers.IO) {
//                                mapPointDao.insertMapPoint(newMapPoint)
//                                withContext(Dispatchers.Main) {
//                                    Toast.makeText(context, "Точка '${name}' измененена!", Toast.LENGTH_SHORT).show()
//                                    showMenuAtLocation.value = null
//                                }
//                            }
//                        },
//                        dialogLabel = "Изменение точки"
//                    )
                    return@MapObjectTapListener true
                }
            }
            return@MapObjectTapListener false
        }
    }
    DisposableEffect(mapView) {
        mapObjects.addTapListener(tapListener)
        onDispose {
            mapObjects.removeTapListener(tapListener)
        }
    }


}