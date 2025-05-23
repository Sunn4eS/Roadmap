
package com.example.roadMap.data.maps
import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.roadMap.data.utilities.screenCenterPixels
import com.example.test.R
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.Job


class MapApp : Application() {
    override fun onCreate() {
            super.onCreate()
            MapKitFactory.setApiKey("01b35dab-d1eb-436a-8c62-7dca91f1a3de")
            MapKitFactory.initialize(this)
    }
}

@Composable
fun YandexMapScreen() {
    val context = LocalContext.current;
    val mapView = rememberMapViewWithLifeCycle()
    val screenCenter = screenCenterPixels()
    val mapKit = remember { MapKitFactory.getInstance() }
    val userLocationLayer = remember(mapView) { mapKit.createUserLocationLayer(mapView.mapWindow) }
    val coroutineScope = rememberCoroutineScope()
    val showMenuAtLocation = remember { mutableStateOf<Point?>(null) }

    val sharedPreferences = remember { context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE) }
    MapInteractionHandler(mapView, showMenuAtLocation,coroutineScope)

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
    YandexMapScreen()
}
