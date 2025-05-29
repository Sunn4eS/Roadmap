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
import androidx.compose.foundation.lazy.items
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
import com.example.roadMap.data.model.MapPoint
import com.example.roadMap.data.utilities.AttachFileButton
import com.example.roadMap.data.utilities.ImageStorageUtils
import com.example.test.R

@Composable
fun CustomMapPointDialog(
    showDialog: Boolean,
    initialMapPoint: MapPoint?, // ИЗМЕНЕНО: Теперь принимает MapPoint?
    dialogTitle: String, // ИЗМЕНЕНО: Заголовок диалога
    onDismissRequest: () -> Unit,
    onSavePoint: (mapPoint: MapPoint) -> Unit,
    onDeletePoint: ((mapPoint: MapPoint) -> Unit)? = null
) {

    var pointLabel by remember(initialMapPoint) { mutableStateOf(initialMapPoint?.label ?: "") }
    var pointDescription by remember(initialMapPoint) { mutableStateOf(initialMapPoint?.description ?: "") }
    val selectedPhotoUris = remember(initialMapPoint) { mutableStateListOf<String>().apply {
        initialMapPoint?.photoUris?.let { addAll(it) }
    }}
    val context = LocalContext.current
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }

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
                            items(selectedPhotoUris.toList()) { uriString ->
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
                                                fullScreenImageUri = uriString.toString()
                                            }
                                    )
                                } ?: run {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(Color.LightGray, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Нет фото")
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        selectedPhotoUris.remove(uriString)
                                        ImageStorageUtils.deleteImageFromInternalStorage(context, uriString)
                                        Toast.makeText(context, "Фото удалено", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close_button),
                                            contentDescription = "Удалить фото",
                                            tint = Color.White,
                                            modifier = Modifier.size(8.dp) // <-- Размер иконки
                                        )
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
                            val mapPointToSave = initialMapPoint?.copy(
                                label = pointLabel,
                                description = pointDescription,
                                photoUris = selectedPhotoUris.toList()
                            ) ?: MapPoint(
                                userId = "",
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
                    if (initialMapPoint != null && onDeletePoint != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onDeletePoint.invoke(initialMapPoint) // Вызываем лямбду удаления
                                // onDismissRequest() // Диалог будет закрыт после выполнения onDeletePoint
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Удалить метку", color = Color.White)
                        }
                    }
                }
            }
        }
    }
    if (fullScreenImageUri != null) {
        Dialog(
            onDismissRequest = { fullScreenImageUri = null },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false // Позволяет диалогу занимать всю ширину
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                val bitmap: Bitmap? = ImageStorageUtils.loadBitmapFromUri(context, fullScreenImageUri!!)
                bitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: run {
                    Text("Не удалось загрузить изображение", color = Color.White)
                }

                // Кнопка закрытия полноэкранного просмотра
                IconButton(
                    onClick = { fullScreenImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_button),
                        contentDescription = "Закрыть просмотр",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun MapDialogCheck() {
    val initPoint = MapPoint(
        id = 1,
        userId = "preview_user",
        label = "Тестовая точка",
        description = "Это описание тестовой точки.",
        latitude = 53.9000,
        longitude = 27.5667,
        photoUris = listOf()
    )
    CustomMapPointDialog(
        showDialog = true,
        initialMapPoint = initPoint,
        onDismissRequest = {},
        onSavePoint = { mapPoint ->
            println("Сохранена/обновлена точка: ${mapPoint.label}")
        },
        dialogTitle = "Предварительный просмотр",
        onDeletePoint = { mapPoint ->
            println("Удалена точка: ${mapPoint.label}")
        }
    )
}