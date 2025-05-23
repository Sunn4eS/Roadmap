
package com.example.roadMap.data.maps
import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roadMap.data.dataBase.AppDatabase
import com.example.roadMap.data.module.MapPoint
import com.example.roadMap.data.utilities.screenCenterPixels
import com.example.test.R
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.ui_view.ViewProvider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf


class MapApp : Application() {
    override fun onCreate() {
            super.onCreate()
            MapKitFactory.setApiKey("01b35dab-d1eb-436a-8c62-7dca91f1a3de")
            MapKitFactory.initialize(this)
    }
}

@Composable
fun YandexMapScreen(loggedInUsername: String?) {
    val context = LocalContext.current;
    val mapView = rememberMapViewWithLifeCycle()
    val screenCenter = screenCenterPixels()
    val mapKit = remember { MapKitFactory.getInstance() }
    val userLocationLayer = remember(mapView) { mapKit.createUserLocationLayer(mapView.mapWindow) }
    val coroutineScope = rememberCoroutineScope()
    val showMenuAtLocation = remember { mutableStateOf<Point?>(null) }
    val mapPointDao = remember { AppDatabase.getDatabase(context).mapPointDao() } // Получаем DAO для MapPoint

    var userMapPoints by remember { mutableStateOf(emptyList<MapPoint>()) }

    // ИСПРАВЛЕНО: Ручной сбор Flow в LaunchedEffect
    LaunchedEffect(loggedInUsername) {
        loggedInUsername?.let { username ->
            mapPointDao.getMapPointsForUser(username).collectLatest { points ->
                userMapPoints = points
            }
        } ?: run {
            userMapPoints = emptyList() // Очищаем список, если пользователь не авторизован
        }
    }
    val sharedPreferences = remember { context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE) }
    MapInteractionHandler(mapView, showMenuAtLocation,coroutineScope, context, loggedInUsername)

//    LaunchedEffect(userMapPoints, mapView) {
//        val mapObjects = mapView.map.mapObjects
//        mapObjects.clear() // Очищаем старые объекты перед добавлением новых
//
//        userMapPoints.forEach { mapPoint ->
//            val placemark = mapObjects.addPlacemark() // Создаем пустую метку
//            placemark.geometry = mapPoint.toYandexPoint() // Устанавливаем геометрию
//            placemark.setIcon(ImageProvider.fromResource(context, R.drawable.my_pos_pointer)) // Устанавливаем иконку
//                // Например:
//                // it.setIcon(ImageProvider.fromResource(context, R.drawable.your_point_icon))
//                // it.setText(mapPoint.name) // Отобразить имя точки
//
//        }
//    }


    LaunchedEffect(loggedInUsername) {
        loggedInUsername?.let { username ->
            mapPointDao.getMapPointsForUser(username).collectLatest { points ->
                userMapPoints = points
                Log.d("YandexMapScreen", "Fetched ${points.size} map points for user: $username")
            }
        } ?: run {
            userMapPoints = emptyList()
            Log.d("YandexMapScreen", "No loggedInUsername, setting userMapPoints to empty.")
        }
    }
    val density = LocalDensity.current
    // Эффект для отображения точек на карте
    LaunchedEffect(userMapPoints, mapView) {
        val mapObjects = mapView.map.mapObjects
        mapObjects.clear() // Очищаем старые объекты перед добавлением новых

        Log.d("YandexMapScreen", "Attempting to display ${userMapPoints.size} map points on map.")

        if (userMapPoints.isEmpty()) {
            Log.d("YandexMapScreen", "No map points to display for loggedInUsername: $loggedInUsername")
        }

        userMapPoints.forEachIndexed { index, mapPoint ->
            val yandexPoint = mapPoint.toYandexPoint()
            Log.d("YandexMapScreen", "Displaying point $index (ViewProvider): Name=${mapPoint.label}, Lat=${yandexPoint.latitude}, Lon=${yandexPoint.longitude}")

            val imageView = ImageView(context).apply {
                setImageResource(R.drawable.my_pos_pointer)

                layoutParams = ViewGroup.LayoutParams(60.dp.toPx(density).toInt(), 60.dp.toPx(density).toInt())
            }

            val viewProvider = ViewProvider(imageView)
            val placemark = mapObjects.addPlacemark(yandexPoint, viewProvider)

            placemark.setText(mapPoint.label)
            // TODO: Вы можете настроить внешний вид метки здесь (цвет, размер и т.д.)
        }
    }

    fun saveLastKnownLocation(point: Point) {
        sharedPreferences.edit()
            .putFloat("last_lat", point.latitude.toFloat())
            .putFloat("last_lon", point.longitude.toFloat())
            .apply()
    }
    fun loadLastKnownLocation(): Point? {
        if (sharedPreferences.contains("last_lat") && sharedPreferences.contains("last_lon")) {
            val lat = sharedPreferences.getFloat("last_lat", 0f).toDouble()
            val lon = sharedPreferences.getFloat("last_lon", 0f).toDouble()
            return Point(lat, lon)
        }
        return null
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                userLocationLayer.setHeadingEnabled(false)
                userLocationLayer.isVisible = true
                userLocationLayer.setAnchor(
                    screenCenter,
                    screenCenter
                )

                userLocationLayer.cameraPosition()?.let { cameraPosition ->
                    mapView.map.move(
                        CameraPosition(cameraPosition.target, 13.0f, 0f,0f))
                }


            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                userLocationLayer.isVisible = true


            }
            else -> {
                android.widget.Toast.makeText(context, "Разрешение на местоположение отклонено", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }


    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        userLocationLayer.isVisible = true


                userLocationLayer.setHeadingEnabled(false)




            } else -> {
                requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            }
        }

    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
        }
        Button(
            onClick = {
                userLocationLayer.cameraPosition()?.let { cameraPosition ->
                    mapView.map.move(
                        CameraPosition(cameraPosition.target, 13.0f, 0f,0f))
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Transparent
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
            .then(Modifier.padding(0.dp)),
            contentPadding = PaddingValues(0.dp)

        ) {

            Image(
                painter = painterResource(R.drawable.my_pos_pointer),
                contentDescription = "Моё местоположение",
                modifier = Modifier.fillMaxSize()
            )
        }

    }



}

private fun Dp.toPx(density: Density): Float {
    return this.value * density.density
}

@Composable
fun rememberMapViewWithLifeCycle() : MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    DisposableEffect(Unit) {
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        onDispose {
            mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
    }
    return mapView
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    YandexMapScreen(loggedInUsername = "preview_user")
}