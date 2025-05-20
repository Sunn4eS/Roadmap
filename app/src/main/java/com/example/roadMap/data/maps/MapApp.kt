package com.example.roadMap.data.maps

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.mapview.MapView

class MapApp : Application() {
    override fun onCreate() {
            super.onCreate()
            MapKitFactory.setApiKey("01b35dab-d1eb-436a-8c62-7dca91f1a3de")
            MapKitFactory.initialize(this)
    }
}

@Composable
fun YandexMapScreen() {
    val mapView = rememberMapViewWithLifeCycle()
    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )

}

//@Composable
//@Preview
//fun YandeMapScreen() {
//    val mapView = rememberMapViewWithLifeCycle()
//    AndroidView(
//        factory = { mapView },
//        modifier = Modifier.fillMaxSize()
//    )
//
//}

@Composable
fun rememberMapViewWithLifeCycle() : MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
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