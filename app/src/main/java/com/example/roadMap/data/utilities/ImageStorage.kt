package com.example.roadMap.data.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import androidx.core.net.toUri

object ImageStorageUtils {

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {

        val filename = "${UUID.randomUUID()}.jpeg"
        val imageFile = File(context.cacheDir, filename)

        try {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            return Uri.fromFile(imageFile).toString()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }


    fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = uriString.toUri()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun deleteImageFromInternalStorage(context: Context, uriString: String): Boolean {
        return try {
            val file = File(uriString.toUri().path ?: return false)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
