package com.example.roadMap.data.utilities // Убедитесь, что это ваш пакет

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID // Для генерации уникальных имен файлов

/**
 * Утилитарный класс для сохранения и загрузки изображений в приватное хранилище приложения.
 */
object ImageStorageUtils {

    private const val TAG = "ImageStorageUtils"

    /**
     * Сохраняет Bitmap в приватный файл JPEG в кэше приложения.
     *
     * @param context Контекст приложения.
     * @param bitmap Bitmap, который нужно сохранить.
     * @return Uri сохраненного файла в виде строки, или null в случае ошибки.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        val filename = "${UUID.randomUUID()}.jpeg"
        val imageFile = File(context.cacheDir, filename) // Сохраняем в кэш-директорию

        try {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val uriString = Uri.fromFile(imageFile).toString()
            Log.d(TAG, "Bitmap saved to: $uriString")
            return uriString
        } catch (e: IOException) {
            Log.e(TAG, "Error saving bitmap to internal storage: ${e.message}", e)
            return null
        }
    }

    /**
     * Загружает Bitmap из URI, хранящегося в приватном хранилище приложения.
     * Улучшена обработка URI, включая file:// схемы.
     *
     * @param context Контекст приложения.
     * @param uriString URI файла изображения в виде строки.
     * @return Загруженный Bitmap, или null, если файл не найден или произошла ошибка.
     */
    fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
        if (uriString.isEmpty()) {
            Log.w(TAG, "Attempted to load bitmap from empty URI string.")
            return null
        }

        return try {
            val uri = Uri.parse(uriString)
            Log.d(TAG, "Attempting to load bitmap from URI: $uri")

            val inputStream = when (uri.scheme) {
                "file" -> {
                    // Для file:// URI, получаем путь и открываем FileInputStream
                    uri.path?.let { File(it) }?.let { file ->
                        if (file.exists() && file.canRead()) {
                            FileInputStream(file)
                        } else {
                            Log.e(TAG, "File does not exist or is not readable: $file")
                            null
                        }
                    }
                }
                "content" -> {
                    // Для content:// URI, используем ContentResolver
                    context.contentResolver.openInputStream(uri)
                }
                else -> {
                    Log.e(TAG, "Unsupported URI scheme: ${uri.scheme}")
                    null
                }
            }

            inputStream?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI: $uriString - ${e.message}", e)
            null
        }
    }

    /**
     * Удаляет файл изображения из приватного хранилища по его URI.
     *
     * @param context Контекст приложения.
     * @param uriString URI файла изображения в виде строки.
     * @return true, если файл успешно удален, false в противном случае.
     */
    fun deleteImageFromInternalStorage(context: Context, uriString: String): Boolean {
        if (uriString.isEmpty()) {
            Log.w(TAG, "Attempted to delete bitmap from empty URI string.")
            return false
        }
        return try {
            val uri = Uri.parse(uriString)
            val file = when (uri.scheme) {
                "file" -> File(uri.path ?: return false)
                else -> {
                    Log.e(TAG, "Deletion of unsupported URI scheme: ${uri.scheme}")
                    return false
                }
            }
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Successfully deleted file: $uriString")
            } else {
                Log.w(TAG, "Failed to delete file: $uriString (file might not exist or permissions issue)")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image from internal storage: $uriString - ${e.message}", e)
            false
        }
    }
}
