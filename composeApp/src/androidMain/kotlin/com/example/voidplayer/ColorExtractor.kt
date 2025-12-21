package com.example.voidplayer

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

@Composable
actual fun getDominantColor(imageBytes: ByteArray): Color {
    return remember(imageBytes) {
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
