package com.example.roadMap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment

import androidx.compose.ui.unit.sp
import com.example.roadMap.data.maps.YandexMapScreen
import com.yandex.mapkit.MapKitFactory


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("01b35dab-d1eb-436a-8c62-7dca91f1a3de")
        enableEdgeToEdge()
        setContent {
            val loggedInUsername = intent.getStringExtra("LOGGED_IN_USERNAME")
            YandexMapScreen(loggedInUsername = loggedInUsername)
        }
    }
}

@Composable
fun MainScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = "Добро пожаловать на главный экран!", fontSize = 24.sp)
        }
    }
}


