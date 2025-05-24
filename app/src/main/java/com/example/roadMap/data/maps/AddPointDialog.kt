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
import com.example.roadMap.data.utilities.AttachFileButton
import com.example.roadMap.data.utilities.ImageStorageUtils
import com.example.test.R
import com.yandex.mapkit.geometry.Point

@Composable
fun CustomMapPointDialog(
    showDialog: Boolean,
    point: Point?,
    onDismissRequest: () -> Unit,
    onSavePoint: (name: String, description: String, photoUris: List<String>) -> Unit, // Новый callback
    userId: String?

) {
    var pointName by remember { mutableStateOf("") }
    var pointDescription by remember { mutableStateOf("") } // Добавлено состояние для описания
    val selectedPhotoUris = remember { mutableStateListOf<String>() } // Список URI выбранных фото
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

    if (showDialog && point != null) {
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
                        text = "Новая точка",
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
                        value = pointName,
                        onValueChange = { pointName = it },
                        label = {Text("Название: ")},
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    Spacer(
                        modifier = Modifier
                            .height(8.dp)
                    )
                    OutlinedTextField(
                        value = pointName,
                        onValueChange = { pointName = it },
                        label = {Text("Описание: ")},
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    Spacer(
                        modifier = Modifier
                            .height(8.dp)
                    )

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

                        Button(onClick = {onSavePoint(pointName, pointDescription, selectedPhotoUris.toList()) }) {
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
    val init = Point(23.2, 23.2)
    CustomMapPointDialog(true, init, onDismissRequest = {}, onSavePoint = { _, _, _ -> }, userId = "test_user_id")
}