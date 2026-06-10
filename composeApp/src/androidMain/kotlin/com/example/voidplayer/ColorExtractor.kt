package com.example.voidplayer

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun getDominantColor(imageBytes: ByteArray): Color {
    var dominantColor by remember(imageBytes) { mutableStateOf(Color.Unspecified) }

    LaunchedEffect(imageBytes) {
        dominantColor = withContext(Dispatchers.Default) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val palette = Palette.from(bitmap).generate()
                val argb = palette.getVibrantColor(
                    palette.getDominantColor(Color.Cyan.toArgb())
                )
                Color(argb)
            } catch (e: Exception) {
                Color.Cyan
            }
        }
    }

    return dominantColor
}
