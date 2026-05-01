package com.example.voidplayer.utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A simple bounded cache for ImageBitmaps to improve memory management and scroll performance.
 */
object ImageCache {
    private const val MAX_ENTRIES = 50
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
