package com.example.roadMap.data.maps

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.roadMap.data.dataBase.AppDatabase
import com.example.roadMap.data.model.MapPoint
import com.example.roadMap.data.utilities.distance
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.forEach

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
            },
            onDeletePoint = { mapPointToDelete -> // <-- ДОБАВЬТЕ ЭТОТ БЛОК
                coroutineScope.launch(Dispatchers.IO) {
                    mapPointDao.deleteMapPoint(mapPointToDelete) // Вызываем метод удаления
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Точка '${mapPointToDelete.label}' удалена!", Toast.LENGTH_SHORT).show()
                        onDismissDialog() // Закрываем диалог после удаления
                    }
                }
            }
        )
    }
}