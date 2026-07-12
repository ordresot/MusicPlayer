package com.tushar.voidplayer.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.tushar.voidplayer.toImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A simple bounded cache for ImageBitmaps to improve memory management and scroll performance.
 */
object ImageCache {
    private val MAX_ENTRIES = if (maxMemoryMB <= 256) 30 else if (maxMemoryMB <= 512) 60 else 100
    private val cache = object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    fun get(key: String): ImageBitmap? {
        return cache[key]
    }

    fun put(key: String, bitmap: ImageBitmap) {
        cache[key] = bitmap
    }
    
    fun clear() {
        cache.clear()
    }
}

/**
 * A cache for dominant colors to avoid recalculating them on song changes.
 */
object ColorCache {
    private const val MAX_ENTRIES = 50
    private val cache = object : LinkedHashMap<String, androidx.compose.ui.graphics.Color>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, androidx.compose.ui.graphics.Color>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    fun get(key: String): androidx.compose.ui.graphics.Color? = cache[key]

    fun put(key: String, color: androidx.compose.ui.graphics.Color) {
        cache[key] = color
    }
    
    fun clear() {
        cache.clear()
    }
}

@androidx.compose.runtime.Composable
fun rememberSongImage(
    song: com.tushar.voidplayer.model.Song,
    repository: com.tushar.voidplayer.data.SongRepository
): ImageBitmap? {
    var bitmap by androidx.compose.runtime.remember(song.id) { 
        androidx.compose.runtime.mutableStateOf(ImageCache.get(song.id.toString())) 
    }
    
    androidx.compose.runtime.LaunchedEffect(song.id) {
        if (bitmap == null) {
            val artBytes = repository.loadArt(song.uri)
            if (artBytes != null) {
                val imgBitmap = withContext(Dispatchers.Default) {
                    try {
                        artBytes.toImageBitmap()
                    } catch (e: Throwable) {
                        // toImageBitmap() can throw if the art data is corrupt.
                        // Returning null here keeps the UI stable with the placeholder.
                        null
                    }
                }
                if (imgBitmap != null) {
                    ImageCache.put(song.id.toString(), imgBitmap)
                    bitmap = imgBitmap
                }
            }
        }
    }
    return bitmap
}
