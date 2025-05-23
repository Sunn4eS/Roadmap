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

@Composable
fun AttachFileButton(context: Context) {
    IconButton(
        onClick = {
            // Создаем Intent для выбора файла
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*" // Разрешаем выбирать любые типы файлов
                // type = "image/*" // Если нужны только изображения
               addCategory(Intent.CATEGORY_OPENABLE)
            }
            // В реальном приложении вам понадобится ActivityResultLauncher
            // для получения результата выбора файла
            context.startActivity(Intent.createChooser(intent, "Выберите файл для прикрепления"))
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_attach_file),
            contentDescription = "attach_file"
        )
    }
}