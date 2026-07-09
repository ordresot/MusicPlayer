package com.example.voidplayer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.graphics.Bitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return try {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(this, 0, this.size, options)

        val maxMem = com.example.voidplayer.utils.maxMemoryMB
        val reqWidth = if (maxMem <= 128) 300 else if (maxMem <= 256) 500 else 800
        val reqHeight = reqWidth
        var inSampleSize = 1

        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        // Use RGB_565 to cut memory by 50% vs ARGB_8888
        options.inPreferredConfig = Bitmap.Config.RGB_565

        // decodeByteArray can return null if data is corrupt or not a valid image.
        // Falling back to a 1x1 transparent bitmap prevents a NullPointerException
        // which would crash the entire app via an uncaught exception in LaunchedEffect.
        val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size, options)
            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        bitmap.asImageBitmap()
    } catch (e: Throwable) {
        // Last-resort fallback: return a 1x1 bitmap rather than crashing the UI
        Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565).asImageBitmap()
    }
}
