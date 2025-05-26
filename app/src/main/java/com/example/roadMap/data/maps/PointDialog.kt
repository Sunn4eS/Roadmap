package com.example.roadMap.data.maps

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roadMap.data.module.MapPoint
import com.example.roadMap.data.utilities.AttachFileButton
import com.example.roadMap.data.utilities.ImageStorageUtils
import com.example.test.R

@Composable
fun CustomMapPointDialog(
    showDialog: Boolean,
    initialMapPoint: MapPoint?, // ИЗМЕНЕНО: Теперь принимает MapPoint?
    dialogTitle: String, // ИЗМЕНЕНО: Заголовок диалога
    onDismissRequest: () -> Unit,
    onSavePoint: (mapPoint: MapPoint) -> Unit
) {

    var pointLabel by remember(initialMapPoint) { mutableStateOf(initialMapPoint?.label ?: "") }
    var pointDescription by remember(initialMapPoint) { mutableStateOf(initialMapPoint?.description ?: "") }
    val selectedPhotoUris = remember(initialMapPoint) { mutableStateListOf<String>().apply {
        initialMapPoint?.photoUris?.let { addAll(it) }
    }}
    val context = LocalContext.current


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) {
        uri: Uri? ->
        uri?.let {
            selectedUri ->
            val bitmap: Bitmap? = try  {
                val source = ImageDecoder.createSource(context.contentResolver, selectedUri)
                ImageDecoder.decodeBitmap(source)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Ошибка загрузки изображения: ${e.message}", Toast.LENGTH_SHORT).show()
                null
            }
            bitmap?.let { bmp ->
                val savedUriString = ImageStorageUtils.saveBitmapToInternalStorage(context, bmp)
                if (savedUriString != null) {
                    selectedPhotoUris.add(savedUriString) // Добавляем сохраненный URI в список
                } else {
                    Toast.makeText(context, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show()
                }
            }

        }

    }

    if (showDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)

                    .wrapContentHeight()
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp, start = 16.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Spacer(
                        Modifier.weight(1f)
                    )
                    Text(
                        text = dialogTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_button),
                            contentDescription = "close_button"
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()

                        .padding(24.dp)
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OutlinedTextField(
                        singleLine = true,
                        value = pointLabel,
                        onValueChange = { pointLabel = it },
                        label = {Text("Имя: ")},
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    Spacer(
                        modifier = Modifier
                            .height(8.dp)
                    )
                    OutlinedTextField(
                        value = pointDescription,
                        onValueChange = { pointDescription = it },
                        label = {Text("Описание: ")},
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    Spacer(
                        modifier = Modifier
                            .height(8.dp)
                    )
                    if (initialMapPoint != null) {
                        Text("Широта: ${String.format("%.4f", initialMapPoint.latitude)}")
                        Text("Долгота: ${String.format("%.4f", initialMapPoint.longitude)}")
                    } else {
                        // Для новой точки, координаты будут взяты из showMenuAtLocation в YandexMapScreen
                        Text("Координаты будут определены при сохранении")
                    }

                    if (selectedPhotoUris.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedPhotoUris.count()) { uriString ->
                                val bitmap: Bitmap? = ImageStorageUtils.loadBitmapFromUri(context,
                                    uriString.toString()
                                )
                                bitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clickable {
                                                // TODO: Возможно, добавить просмотр/удаление фото по клику
                                            }
                                    )
                                } ?: run {
                                    // Placeholder if image fails to load
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(Color.LightGray, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Нет фото")
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = {
                            // Создаем или копируем MapPoint с обновленными данными
                            val mapPointToSave = initialMapPoint?.copy(
                                label = pointLabel,
                                description = pointDescription,
                                photoUris = selectedPhotoUris.toList()
                            ) ?: MapPoint(
                                //id = 0, // ID будет сгенерирован Room для новой точки
                                userId = "", // userId и координаты будут заполнены в YandexMapScreen
                                label = pointLabel,
                                description = pointDescription,
                                latitude = 0.0,
                                longitude = 0.0,
                                photoUris = selectedPhotoUris.toList()
                            )
                            onSavePoint(mapPointToSave)
                        }) {
                            Text("Сохранить")
                        }
                        AttachFileButton(onClick = {imagePickerLauncher.launch("image/*")})
                    }



                }
            }
        }
    }
}

@Preview
@Composable
fun mapDialogCheck() {
    val initPoint = MapPoint(id = 1, userId = "preview_user", label = "Тестовая точка", description = "Это описание тестовой точки.", latitude = 53.9000, longitude = 27.5667, photoUris = listOf("uri1", "uri2"))
    CustomMapPointDialog(
        showDialog = true,
        initialMapPoint = initPoint,
        onDismissRequest = {},
        onSavePoint = { mapPoint ->
            // Логика сохранения/обновления для предпросмотра
            println("Сохранена/обновлена точка: ${mapPoint.label}")
        },
        dialogTitle = "Предварительный просмотр"
    )
}