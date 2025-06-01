package com.example.roadMap.data.dataBase

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.example.roadMap.data.dao.MapPointDao
import com.example.roadMap.data.dao.UserDao
import com.example.roadMap.data.model.MapPoint
import com.example.roadMap.data.model.User
import com.example.roadMap.data.utilities.uriToFile
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlin.code
import kotlin.text.get
import kotlin.text.insert
import java.io.InputStream
import kotlin.jvm.java

suspend fun performUpload(
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

data class ExportResponse(
    val status: String,
    val message: String,
    val data: ExportedData
)

data class ExportedData(
    val users: List<User>,
    @SerializedName("map_points")
    val mapPoints: List<MapPoint>,
    @SerializedName("image_files_on_server")
    val imageFilesOnServer: List<String>
)

suspend fun downloadAndSaveImage(
    context: Context,
    client: OkHttpClient,
    imageUrl: String,
    filenameWithExtension: String
): Uri? {
    return withContext(Dispatchers.IO) {
        val imagesDir = File(context.cacheDir, "downloaded_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val outputFile = File(imagesDir, filenameWithExtension)

        if (outputFile.exists()) {
            Log.d("Download", "Изображение уже существует локально: ${outputFile.absolutePath}")
            return@withContext outputFile.toUri()
        }

        try {
            val request = Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.byteStream()?.use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Log.d("Download", "Изображение скачано и сохранено в: ${outputFile.absolutePath}")
                    return@withContext outputFile.toUri()
                }
            } else {
                Log.e("Download", "Не удалось скачать изображение с $imageUrl: ${response.code} - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e("Download", "Ошибка при скачивании или сохранении изображения с $imageUrl", e)
        }
        return@withContext null
    }
}

suspend fun performDownload(
    context: Context,
    userDao: UserDao,
    mapPointDao: MapPointDao,
    gson: Gson,
    client: OkHttpClient,
    serverDownloadUrl: String,
    baseImageUrl: String
) {
    try {
        val request = Request.Builder()
            .url(serverDownloadUrl)
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        withContext(Dispatchers.Main) {
            if (response.isSuccessful) {
                Log.d("Download", "Сервер ответил: $responseBody")
                Toast.makeText(
                    context,
                    "Данные успешно получены с сервера!",
                    Toast.LENGTH_SHORT
                ).show()

                if (responseBody == null) {
                    Toast.makeText(context, "Пустой ответ от сервера.", Toast.LENGTH_LONG).show()
                    Log.e("Download", "Пустой ответ от сервера.")
                    return@withContext
                }

                try {
                    val exportResponse = gson.fromJson(responseBody, ExportResponse::class.java)

                    if (exportResponse.status == "success") {
                        // --- 1. Обработка пользователей ---
                        withContext(Dispatchers.IO) { // <--- Переключаемся в IO поток для работы с базой данных
                            exportResponse.data.users.forEach { user ->
                                try {
                                    val existingUser = userDao.getUser(user.username)
                                    if (existingUser == null) {
                                        userDao.insertUser(user)
                                    Log.d("Download", "Пользователь ${user.username} обработан.") }
                                    else {
                                        Log.d("Download", "Пользователь ${user.username} существует.")

                                }
                                } catch (e: Exception) {
                                    Log.e("Download", "Ошибка при обработке пользователя ${user.username}: ${e.message}", e)
                                    withContext(Dispatchers.Main) { // Возвращаемся в Main для Toast
                                        Toast.makeText(context, "Ошибка обработки пользователя ${user.username}.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            Log.d("Download", "Все пользователи обработаны.")
                        }
                        // --- 2. Обработка точек карты и изображений ---
                        val downloadedImageMap = mutableMapOf<String, Uri>() // serverImagePath -> localUri

                        exportResponse.data.mapPoints.forEach { serverMapPoint ->
                            val newPhotoUris = mutableListOf<String>()
                            serverMapPoint.photoUris.forEach { serverImagePath ->
                                try {
                                    val filename = serverImagePath.substringAfterLast('/')
                                    val imageUrl = "$baseImageUrl$filename"

                                    val localUri = downloadedImageMap[imageUrl] ?: run {
                                        downloadAndSaveImage(context, client, imageUrl, filename)
                                    }

                                    localUri?.let {
                                        newPhotoUris.add(it.toString())
                                        downloadedImageMap[imageUrl] = it
                                    } ?: run {
                                        Log.e("Download", "Не удалось получить локальный URI для изображения: $serverImagePath")
                                    }
                                } catch (e: Exception) {
                                    Log.e("Download", "Ошибка при обработке URI изображения ${serverImagePath}: ${e.message}", e)
                                }
                            }

                            val mapPointToSave = serverMapPoint.copy(photoUris = newPhotoUris)

                            try {
                                mapPointDao.insertMapPoint(mapPointToSave)
                                Log.d("Download", "Точка карты ${mapPointToSave.label} (ID: ${mapPointToSave.id}) обработана.")
                            } catch (e: Exception) {
                                Log.e("Download", "Ошибка при обработке точки карты ${mapPointToSave.id}: ${e.message}", e)
                                Toast.makeText(context, "Ошибка обработки точки карты ${mapPointToSave.id}.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        Log.d("Download", "Все точки карты и изображения обработаны.")

                        Toast.makeText(context, "Все данные успешно скачаны и обновлены!", Toast.LENGTH_LONG).show()

                    } else {
                        Toast.makeText(
                            context,
                            "Сервер вернул ошибку: ${exportResponse.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("Download", "Сервер вернул статус ошибки: ${exportResponse.message}")
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка парсинга JSON: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("Download", "Ошибка парсинга JSON: ${e.message}", e)
                }

            } else {
                Toast.makeText(
                    context,
                    "Ошибка скачивания с сервера: ${response.code} - $responseBody",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("Download", "Ошибка сервера при скачивании: ${response.code} - $responseBody")
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Ошибка при скачивании данных: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
            Log.e("Download", "Ошибка сети или общая ошибка при скачивании: ${e.localizedMessage}", e)
        }
    }
}


