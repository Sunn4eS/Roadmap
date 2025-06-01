package com.example.roadMap.data.utilities

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.roadMap.MainActivity
import com.example.roadMap.data.dataBase.AppDatabase
import com.example.roadMap.data.model.User
import com.example.test.R
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import com.example.roadMap.data.dao.MapPointDao
import com.example.roadMap.data.dao.UserDao
import com.example.roadMap.data.dataBase.performDownload

@Composable
fun AttachFileButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_attach_file),
            contentDescription = "attach_file"
        )
    }
}

@Composable
fun RegisterButton(name: String, password: String) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val userDao = db.userDao()
    val passwordHash = hashPassword(password)
    Button(
        modifier = Modifier.offset((10).dp),
        onClick = {
            if (!name.isEmpty() && !password.isEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val existingUser = userDao.getUser(name)
                    if (existingUser == null) {
                        val passwordHash = hashPassword(password)
                        val newUser = User(username = name, password = passwordHash)
                        userDao.insertUser(newUser)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Пользователь зарегистрирован!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, MainActivity::class.java)
                            intent.putExtra("LOGGED_IN_USERNAME", newUser.username)
                            context.startActivity(intent)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Пользователь с таким именем уже существует", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            else {
                Toast.makeText(context, "Поля не заполнены!", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Регистрация")
    }
}

@Composable
fun LoginButton(name: String, password: String) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val userDao = database.userDao()
    val coroutineScope = rememberCoroutineScope()

    Button(
        modifier = Modifier.offset((-10).dp),
        onClick = {
            if (name.isNotEmpty() && password.isNotEmpty()) {
                coroutineScope.launch(Dispatchers.IO) {
                    val user = userDao.getUser(name)
                    if (user != null) {
                        val isPasswordCorrect = comparePassword(password, user.password)
                        if (isPasswordCorrect) {
                            withContext(Dispatchers.Main) { // Переключаемся на главный поток для UI
                                Toast.makeText(context, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(context, MainActivity::class.java)
                                intent.putExtra("LOGGED_IN_USERNAME", user.username) // Передаем имя пользователя
                                context.startActivity(intent)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Неверное имя пользователя или пароль", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Неверное имя пользователя или пароль", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Поля не заполнены", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Войти")
    }
}

@Composable
fun ExportAndUploadDataButton(coroutineScope: CoroutineScope) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val userDao = remember { database.userDao() }
    val mapPointDao = remember { database.mapPointDao() }
    val gson = remember { Gson() }
    val client = remember { OkHttpClient() }

    var showIpInput by remember { mutableStateOf(false) }
    var ipAddressInput by remember { mutableStateOf("192.168.0.105") } // IP-адрес по умолчанию

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        IconButton(
            onClick = {
                showIpInput = !showIpInput
            }
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sync_button),
                    contentDescription = "Синхронизация с сервером",
                    tint = Color.White,
                    modifier = Modifier.size(35.dp)
                )
            }
        }

        // Поле ввода IP-адреса, отображается только если showIpInput = true
        if (showIpInput) {
            OutlinedTextField(
                value = ipAddressInput,
                onValueChange = { ipAddressInput = it },
                label = { Text("IP-адрес сервера") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
        }
        AnimatedVisibility(
            visible = showIpInput, // Видимость контролируется состоянием
            enter = expandVertically(expandFrom = Alignment.Top), // Анимация появления (расширение сверху)
            exit = shrinkVertically(shrinkTowards = Alignment.Top) // Анимация исчезновения (сжатие к верху)
        ) {
            Row {
                SendButton(
                    ipAddressInput,
                    coroutineScope,
                    context,
                    userDao,
                    mapPointDao,
                    gson,
                    client

                )

                // Пробел между кнопками
                Spacer(modifier = Modifier.width(4.dp))

                // Вторая дополнительная кнопка
                DownloadButton(
                    ipAddressInput,
                    coroutineScope,
                    context,
                    userDao,
                    mapPointDao,
                    gson,
                    client,
                )
            }
        }
    }
}

@Composable
private fun DownloadButton(
    ipAddressInput: String,
    coroutineScope: CoroutineScope,
    context: Context,
    userDao: UserDao,
    mapPointDao: MapPointDao,
    gson: Gson,
    client: OkHttpClient,
) {
    IconButton(
        onClick = {
            val serverUploadUrl = "http://$ipAddressInput/get_data.php"
            val baseImageUrlFromServer = "http://$ipAddressInput/uploads_images/" // Замените на фактический URL

            coroutineScope.launch(Dispatchers.IO) {
                performDownload(
                    context,
                    userDao,
                    mapPointDao,
                    gson,
                    client,
                    serverUploadUrl,
                    baseImageUrlFromServer
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_download_button),
                contentDescription = "Получение данных",
                tint = Color.White,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@Composable
private fun SendButton(
    ipAddressInput: String,
    coroutineScope: CoroutineScope,
    context: Context,
    userDao: UserDao,
    mapPointDao: MapPointDao,
    gson: Gson,
    client: OkHttpClient,

) {
    IconButton(
        onClick = {
            val serverUploadUrl = "http://$ipAddressInput/process_data.php"
            coroutineScope.launch(Dispatchers.IO) {
                performUpload(
                    context,
                    userDao,
                    mapPointDao,
                    gson,
                    client,
                    serverUploadUrl
                )
            }
        }

        ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_send_button),
                contentDescription = "Отправка данных",
                tint = Color.White,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}


private suspend fun performUpload(
    context: Context,
    userDao: UserDao,
    mapPointDao: MapPointDao,
    gson: Gson,
    client: OkHttpClient,
    serverUploadUrl: String
) {
    try {
        val users = userDao.getAllUsers()
        val mapPoints = mapPointDao.getAllMapPoints()
        mapPoints.forEach {
            Log.d("Upload", "MapPoint ID: ${it.id}, PhotoUris: ${it.photoUris}")
        }
        val exportData = mapOf(
            "users" to users,
            "map_points" to mapPoints
        )
        val jsonString = gson.toJson(exportData)
        val fileName = "roadmap_export.json"

        var tempFile: File? = null

        try {
            tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { it.write(jsonString.toByteArray()) }

            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

            val jsonRequestBody = tempFile.asRequestBody("application/json".toMediaTypeOrNull())
            builder.addFormDataPart("json_file", tempFile.name, jsonRequestBody)

            for (mapPoint in mapPoints) {
                mapPoint.photoUris.forEach { uriString ->
                    try {
                        val uri = uriString.toUri()
                        val imageFile = uriToFile(uri, context)
                        imageFile?.let {
                            val imageRequestBody = it.asRequestBody("image/*".toMediaTypeOrNull())
                            builder.addFormDataPart("images[]", it.name, imageRequestBody)
                        }
                    } catch (e: Exception) {
                        Log.e("Upload", "Ошибка при обработке URI изображения: $uriString", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ошибка при обработке URI изображения.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            val requestBody = builder.build()

            val request = Request.Builder()
                .url(serverUploadUrl)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d("Upload", "Отправляемый JSON: $jsonString")
                        Toast.makeText(
                            context,
                            "Данные и изображения успешно отправлены!",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d("Upload", "Сервер ответил: $responseBody")
                    } else {
                        Toast.makeText(
                            context,
                            "Ошибка отправки на сервер: ${response.code} - $responseBody",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("Upload", "Ошибка сервера: ${response.code} - $responseBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Ошибка при отправке на сервер: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("Upload", "Ошибка сети: ${e.localizedMessage}", e)
                }
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e("ExportData", "Ошибка экспорта данных: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка экспорта данных: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Log.e("ExportData", "Ошибка при получении данных для экспорта: ${e.message}", e)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Ошибка при получении данных для экспорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

