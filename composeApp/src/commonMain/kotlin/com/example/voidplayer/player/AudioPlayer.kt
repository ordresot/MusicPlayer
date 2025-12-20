package com.example.voidplayer.player

import com.example.voidplayer.model.Song
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val currentSong: StateFlow<Song?>

    fun play(song: Song)
    fun pause()
    fun resume()
    fun seekTo(position: Long)
    fun cleanUp()
}
