package com.example.roadMap.data.utilities

import android.content.Context
import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.res.painterResource
import com.example.test.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun hashPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}
fun comparePassword(inputPassword: String, hashedPassword: String): Boolean {
    val inputHash = hashPassword(inputPassword) // Используйте ту же функцию хеширования, что и при регистрации
    return inputHash == hashedPassword
}

@Composable
fun screenDimensionsDisplay(): PointF {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Размеры в пикселях
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    return PointF(screenWidthPx, screenHeightPx)
}

@Composable
fun screenCenterPixels(): PointF {
    return PointF((screenDimensionsDisplay().x / 2), screenDimensionsDisplay().y / 2)
}

fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
}

fun uriToFile(uri: Uri, context: Context): File? {
    if (uri.scheme == "file") {
        return File(uri.path!!)
    }
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val tempFile = File.createTempFile("image_", null, context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}