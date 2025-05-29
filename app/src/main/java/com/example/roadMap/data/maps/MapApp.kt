
package com.example.roadMap.data.maps
import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
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
import com.example.roadMap.data.model.MapPoint
import com.example.roadMap.data.utilities.screenCenterPixels
import com.example.test.R
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.ui_view.ViewProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
    val mapObjects = remember(mapView) { mapView.map.mapObjects }
    val density = LocalDensity.current
    val userMapPoints by (loggedInUsername?.let { mapPointDao.getMapPointsForUser(it) }
        ?: flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList(), lifecycleOwner = LocalLifecycleOwner.current)
    val sharedPreferences = remember { context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE) }



//    LaunchedEffect(loggedInUsername) {
//        loggedInUsername.let { username ->
//            mapPointDao.getMapPointsForUser(username.toString()).collectLatest { points ->
//                userMapPoints = points
//            }
//        }
//    }




    MapInteractionHandler(mapView, showMenuAtLocation,coroutineScope, context, loggedInUsername)
    GetPointFromMap(
        userMapPoints = userMapPoints,
        context = context,
        mapView = mapView,
        mapObjects = mapObjects, // Используем mapObjects из remember
        loggedInUsername = loggedInUsername // Передаем currentUserId
    )

    LaunchedEffect(userMapPoints, mapView) {
        val mapObjects = mapView.map.mapObjects
        mapObjects.clear()
        userMapPoints.forEachIndexed { index, mapPoint ->
            val yandexPoint = mapPoint.toYandexPoint()

            val imageView = ImageView(context).apply {
                setImageResource(R.drawable.ic_map_flag)
                layoutParams = ViewGroup.LayoutParams(60.dp.toPx(density).toInt(), 60.dp.toPx(density).toInt())
            }

            val viewProvider = ViewProvider(imageView)
            val placemark = mapObjects.addPlacemark(yandexPoint, viewProvider)

            //  placemark.setText(mapPoint.label)
        }
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