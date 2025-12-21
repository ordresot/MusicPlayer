package com.example.voidplayer.data

import com.example.voidplayer.model.Song

interface SongRepository {
    suspend fun getSongs(): List<Song>
    suspend fun loadFromFolder(uriString: String): List<Song>
    suspend fun loadArt(uriString: String): ByteArray?
}
