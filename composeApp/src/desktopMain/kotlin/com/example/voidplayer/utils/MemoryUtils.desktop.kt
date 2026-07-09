package com.example.voidplayer.utils

actual val maxMemoryMB: Int
    get() = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
