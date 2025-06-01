package com.example.roadMap.data.maps // Измените на ваш базовый пакет

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.roadMap.data.dataBase.AppDatabase
import com.example.roadMap.data.model.MapPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MapInteractionHandler(
    mapView: MapView,
    showMenuAtLocation: MutableState<Point?>,
    coroutineScope: CoroutineScope,
    context: Context,
    loggedInUsername: String?
) {
    val mapPointDao = remember { AppDatabase.getDatabase(context).mapPointDao() }
    val longPressMapListener = remember {
        object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                showMenuAtLocation.value = null
            }
            override fun onMapLongTap(map: Map, point: Point) {
                showMenuAtLocation.value = point
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.map.addInputListener(longPressMapListener)
        onDispose {
            mapView.map.removeInputListener(longPressMapListener)
        }
    }
    if (showMenuAtLocation.value != null) {
        CustomMapPointDialog(
            showDialog = true,
            initialMapPoint = null,
            dialogTitle = "Новая точка",
            onDismissRequest = { showMenuAtLocation.value = null },
            onSavePoint = {mapPointFromDialog ->
                val newMapPoint = MapPoint(
                    userId = loggedInUsername,
                    label = mapPointFromDialog.label,
                    description = mapPointFromDialog.description,
                    latitude = showMenuAtLocation.value!!.latitude,
                    longitude = showMenuAtLocation.value!!.longitude,
                    photoUris = mapPointFromDialog.photoUris
                )
                coroutineScope.launch(Dispatchers.IO) {
                    mapPointDao.insertMapPoint(newMapPoint)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Точка '${newMapPoint}' сохранена!", Toast.LENGTH_SHORT).show()
                        showMenuAtLocation.value = null
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun defPrev() {
    val initialLocation = Point(55.75, 37.62)
    val showMenuAtLocation = remember { mutableStateOf<Point?>(initialLocation) }
}