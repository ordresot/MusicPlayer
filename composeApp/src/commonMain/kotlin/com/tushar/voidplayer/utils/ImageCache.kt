package com.tushar.voidplayer.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.tushar.voidplayer.toImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A bounded LRU cache for [ImageBitmap] instances.
 *
 * All public operations are @Synchronized to prevent data races when bitmap
 * loading coroutines (on Dispatchers.Default) and composition reads
 * (on the Main thread) access the cache concurrently.
 */
object ImageCache {
    private val MAX_ENTRIES = if (maxMemoryMB <= 256) 30 else if (maxMemoryMB <= 512) 60 else 100

    private val cache = object : java.util.LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    @Synchronized
    fun get(key: String): ImageBitmap? = cache[key]

    @Synchronized
    fun put(key: String, bitmap: ImageBitmap) { cache[key] = bitmap }

    @Synchronized
    fun clear() = cache.clear()
}

/**
 * A bounded LRU cache for dominant accent colors derived from album art.
 *
 * Synchronized for the same reasons as [ImageCache].
 */
object ColorCache {
    private const val MAX_ENTRIES = 50

    private val cache = object : java.util.LinkedHashMap<String, androidx.compose.ui.graphics.Color>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, androidx.compose.ui.graphics.Color>?
        ): Boolean = size > MAX_ENTRIES
    }

    @Synchronized
    fun get(key: String): androidx.compose.ui.graphics.Color? = cache[key]

    @Synchronized
    fun put(key: String, color: androidx.compose.ui.graphics.Color) { cache[key] = color }

    @Synchronized
    fun clear() = cache.clear()
}

// ------------------------------------------------------------------
// Composable helper
// ------------------------------------------------------------------

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
                    } catch (_: Throwable) {
                        // Art data may be corrupt — return null to keep UI stable with placeholder.
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
