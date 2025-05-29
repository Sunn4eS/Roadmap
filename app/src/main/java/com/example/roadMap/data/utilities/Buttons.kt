package com.example.roadMap.data.utilities

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.tv.AdRequest
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
import java.io.OutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import androidx.core.net.toUri

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
    val serverUploadUrl = "http://192.168.0.106/process_data.php" // Убедитесь, что IP правильный!

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val users = userDao.getAllUsers()
                        val mapPoints = mapPointDao.getAllMapPoints()
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

                            // Собираем файлы изображений из mapPoints
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
                                tempFile?.delete()
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
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Экспорт и отправка данных")
        }
    }
}

