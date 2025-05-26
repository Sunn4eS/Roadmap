package com.example.roadMap.data.maps

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
//
//
//@Composable
//fun GetPointFromMap(
//    userMapPoints: List<MapPoint>,
//    context: Context,
//    tapRadiusMeters: Float,
//    mapView: MapView,
//    mapObjects: MapObjectCollection,
//    loggedInUsername: String?,
//) {
//    val mapPointDao = remember { AppDatabase.getDatabase(context).mapPointDao() }
//
//    val tapListener = remember {
//        MapObjectTapListener { mapObject, tapPoint ->
//            if (mapObject is PlacemarkMapObject) {
//                // Стандартное касание метки
//                val tappedPoint = userMapPoints.find { it.toYandexPoint() == mapObject.geometry }
//                tappedPoint?.let {
//                    Toast.makeText(context, "Нажата метка (точно): ${it.label}", Toast.LENGTH_SHORT)
//                        .show()
//                    Log.d("MapTap", "Точное нажатие на метку: ${it.label}")
//                    return@MapObjectTapListener true
//                }
//            }
//
//            // Проверяем, находится ли нажатие в радиусе от какой-либо метки
//            userMapPoints.forEach { mapPoint ->
//                var point = mapPoint.toYandexPoint()
//                val dist = distance(tapPoint.latitude, tapPoint.longitude, point.latitude, point.longitude)
//                val showMenuAtLocation : MutableState<Point?>
//                if (dist <= tapRadiusMeters) {
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
//                    return@MapObjectTapListener true
//                }
//            }
//            return@MapObjectTapListener false
//        }
//    }
//    DisposableEffect(mapView) {
//        mapObjects.addTapListener(tapListener)
//        onDispose {
//            mapObjects.removeTapListener(tapListener)
//        }
//    }
//
//
//}

@Composable
fun GetPointFromMap(
    userMapPoints: List<MapPoint>,
    context: Context,
    mapView: MapView,
    mapObjects: MapObjectCollection,
    loggedInUsername: String?,
) {
    val mapPointDao = remember { AppDatabase.getDatabase(context).mapPointDao() }
    val coroutineScope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var selectedMapPoint by remember { mutableStateOf<MapPoint?>(null) }

    val onDismissDialog = { showDialog = false
        selectedMapPoint = null}

    val _currentMapPoints = remember { mutableStateOf(emptyList<MapPoint>()) }

    // ИСПРАВЛЕНО: LaunchedEffect для обновления _currentMapPoints при изменении userMapPoints
    LaunchedEffect(userMapPoints) {
        _currentMapPoints.value = userMapPoints
        Log.d("GetPointFromMap", "Internal _currentMapPoints updated to size: ${_currentMapPoints.value.size}")
    }
    Log.d("MapTap", "Fetch ${userMapPoints.size} map points for user: $loggedInUsername")

    val tapListener = remember(_currentMapPoints) {

        MapObjectTapListener { mapObject, tapPoint ->
            Log.d("MapTap", "Fetched ${_currentMapPoints.value.size} map points for user: $loggedInUsername")

            var tapped = false
            if (mapObject is PlacemarkMapObject) {
                val tappedPoint = _currentMapPoints.value.find { it.toYandexPoint() == mapObject.geometry }
                tappedPoint?.let {
                    Log.d("MapTap", "Точное нажатие на метку: ${tappedPoint.label}")
                    selectedMapPoint = tappedPoint
                    showDialog = true
                    tapped = true
                    return@MapObjectTapListener true
                }
            }

            _currentMapPoints.value.forEach { mapPoint ->
                val point = mapPoint.toYandexPoint()
                val dist = distance(tapPoint.latitude, tapPoint.longitude, point.latitude, point.longitude)
                if (dist <= 0.002f) {
                    Log.d("MapTap", "Нажатие в радиусе метки: ${mapPoint.label}, расстояние: $dist метров")
                    selectedMapPoint = mapPoint
                    showDialog = true
                    tapped = true
                    return@MapObjectTapListener true
                }
            }
            return@MapObjectTapListener false
        }
    }

    DisposableEffect(mapView, mapObjects) {
        mapObjects.addTapListener(tapListener)
        onDispose {
            mapObjects.removeTapListener(tapListener)
        }
    }
    Log.d("GetPointFromMap", "Checking dialog condition: showDialog=$showDialog, selectedMapPoint=$selectedMapPoint")


    if (showDialog && selectedMapPoint != null) {
        CustomMapPointDialog(
            showDialog = showDialog,
            initialMapPoint = selectedMapPoint,
            dialogTitle = "Изменение точки",
            onDismissRequest = onDismissDialog,
            onSavePoint = { updatedMapPoint ->
                loggedInUsername?.let { userId ->
                    val finalMapPoint = updatedMapPoint.copy(userId = userId, id = selectedMapPoint?.id
                        ?: -1)
                    coroutineScope.launch(Dispatchers.IO) {
                        finalMapPoint.id.let {
                            mapPointDao.updateMapPoint(finalMapPoint)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Точка '${finalMapPoint.label}' изменена!", Toast.LENGTH_SHORT).show()
                                showDialog = false
                                selectedMapPoint = null
                            }
                        }
                    }
                }
            }
        )
//        CustomMapPointDialog(
//            showDialog = true,
//            initialMapPoint = null,
//            dialogTitle = "Новая точка",
//            onDismissRequest = { showMenuAtLocation.value = null },
//            onSavePoint = {mapPointFromDialog ->
//                val newMapPoint = mapPointFromDialog.copy(
//                    userId = loggedInUsername,
//                    latitude = showMenuAtLocation.value!!.latitude,
//                    longitude = showMenuAtLocation.value!!.longitude
//                )
//                coroutineScope.launch(Dispatchers.IO) {
//                    mapPointDao.insertMapPoint(newMapPoint)
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(context, "Точка '${newMapPoint.label}' сохранена!", Toast.LENGTH_SHORT).show()
//                        showMenuAtLocation.value = null
//                    }
//                }
//            }
//        )
    }
}