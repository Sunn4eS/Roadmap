package com.example.roadMap.data.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog // Импортируем Dialog
import androidx.compose.ui.window.DialogProperties // Импортируем DialogProperties
import com.example.roadMap.data.utilities.AttachFileButton
import com.yandex.mapkit.geometry.Point

/**
 * Кастомное диалоговое окно для отображения информации о точке на карте.
 *
 * @param showDialog Состояние, управляющее видимостью диалога.
 * @param point Точка, для которой отображается информация.
 * @param onDismissRequest Лямбда, вызываемая при запросе закрытия диалога (например, по клику вне).
 */
@Composable
fun CustomMapPointDialog(
    showDialog: Boolean,
    point: Point?,
    onDismissRequest: () -> Unit
) {
    val pointName: String
    val pointDescription: String

    if (showDialog && point != null) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .wrapContentWidth()
                    .background(Color.White, shape = RoundedCornerShape(12.dp)) // Фон вашего окна
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Добавление новой точки",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

//                OutlinedTextField()
////                    value = name,
////                    onValueChange = { name = it },
////                    label = {Text("Введите ваше имя: ")},
////                    modifier = Modifier
////                        .fillMaxWidth(),
////
////                    )
                Spacer(
                    modifier = Modifier
                        .height(8.dp))
                Text("Широта: ${String.format("%.4f", point.latitude)}")
                Text("Долгота: ${String.format("%.4f", point.longitude)}")
                Spacer(modifier = Modifier.height(24.dp))
                Row() {
//                    Button(onClick = onDismissRequest) {
//                        Text("Закрыть")
//                    }
//                    Button(onClick = onDismissRequest) {
//                        Text("Сохранить")
//                    }
                   AttachFileButton(context = LocalContext.current)
                }

            }
        }
    }
}

@Preview
@Composable
fun mapDialogCheck() {
    val init = Point(23.2,23.2)
    CustomMapPointDialog(true, init) { }
}