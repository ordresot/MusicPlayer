package com.example.voidplayer.player

import com.example.voidplayer.model.Song
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val currentSong: StateFlow<Song?>
    val error: StateFlow<String?>
    val isShuffle: StateFlow<Boolean>
    val repeatMode: StateFlow<RepeatMode>

    fun play(song: Song)
    fun setPlaylist(songs: List<Song>)
    fun pause()
    fun resume()
    fun next()
    fun previous()
    fun toggleShuffle()
    fun toggleRepeat()
    fun seekTo(position: Long)
    fun cleanUp()

    enum class RepeatMode {
        OFF, ONE, ALL
    }
}
