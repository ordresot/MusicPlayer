package com.tushar.voidplayer.data

import com.tushar.voidplayer.model.Playlist
import com.tushar.voidplayer.model.Song

interface SongRepository {
    suspend fun getSongs(): List<Song>
    suspend fun loadFromFolder(uriString: String): List<Song>
    suspend fun loadArt(uriString: String): ByteArray?
    suspend fun toggleFavorite(songId: Long, isFav: Boolean)
    suspend fun loadLyrics(uriString: String): String?
    suspend fun getPlaylists(): List<Playlist>
    suspend fun savePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlistId: String)
}
