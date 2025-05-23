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
import androidx.compose.runtime.*
import com.example.roadMap.data.dataBase.AppDatabase
import com.example.roadMap.data.module.MapPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Composable-функция для обработки взаимодействий пользователя с картой (InputListener).
 * В частности, обрабатывает долгое нажатие для отображения меню.
 *
 * @param mapView Экземпляр MapView, к которому будет добавлен слушатель.
 * @param showMenuAtLocation Состояние, управляющее видимостью и позицией меню по долгому нажатию.
 * @param coroutineScope CoroutineScope для запуска корутин, например, для задержек.
 */
@Composable
fun MapInteractionHandler(
    mapView: MapView,
    showMenuAtLocation: MutableState<Point?>,
    coroutineScope: CoroutineScope,
    context: Context,
    loggedInUsername: String?
) {
    val mapPointDao = remember { AppDatabase.getDatabase(context).mapPointDao() } // Получаем DAO для MapPoint

    var userId by remember { mutableStateOf<String?>(null) }

    val longPressMapListener = remember {
        object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                // При касании карты, закрываем меню, если оно открыто
                showMenuAtLocation.value = null
            }
            override fun onMapLongTap(map: Map, point: Point) {
                // При долгом нажатии, показываем меню с координатами
                showMenuAtLocation.value = point


            }
        }
    }

    // Добавляем InputListener к карте при создании Composable и удаляем при его уничтожении
    DisposableEffect(mapView) {
        mapView.map.addInputListener(longPressMapListener)
        onDispose {
            mapView.map.removeInputListener(longPressMapListener)
        }
    }
    if (showMenuAtLocation.value != null) { // Используем .value для MutableState
        CustomMapPointDialog(
            showDialog = true,
            point = showMenuAtLocation.value,
            onDismissRequest = { showMenuAtLocation.value = null },
            userId = loggedInUsername,
            onSavePoint = { name, description, photoUris ->

                val newMapPoint = MapPoint(
                    userId = loggedInUsername,
                    label = name,
                    description = description,
                    latitude = showMenuAtLocation.value!!.latitude,
                    longitude = showMenuAtLocation.value!!.longitude,
                    photoUris = photoUris
                )
                coroutineScope.launch(Dispatchers.IO) {
                    mapPointDao.insertMapPoint(newMapPoint)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Точка '${name}' сохранена!", Toast.LENGTH_SHORT).show()
                        showMenuAtLocation.value = null
                    }
                }
            }
        )
    }

}


//@Composable
//fun LongTapMenu(showMenuAtLocation: MutableState<Point?>) {
//    if (showMenuAtLocation.value != null) {
//        AlertDialog(
//
//            onDismissRequest = { showMenuAtLocation.value = null },
//            title = { Text("Меню по долгому нажатию") },
//            text = {
//                Column {
//                    Text("Координаты:")
//                    Text("Широта: ${String.format("%.4f", showMenuAtLocation.value!!.latitude)}")
//                    Text("Долгота: ${String.format("%.4f", showMenuAtLocation.value!!.longitude)}")
//                }
//            },
//            confirmButton = {
//                Button(onClick = { showMenuAtLocation.value = null }) {
//                    Text("Закрыть")
//                }
//            },
//
//
//        )
//    }
//}

@Preview
@Composable
fun defPrev() {
    val initialLocation = Point(55.75, 37.62)
    val showMenuAtLocation = remember { mutableStateOf<Point?>(initialLocation) }
   // LongTapMenu(showMenuAtLocation)
}