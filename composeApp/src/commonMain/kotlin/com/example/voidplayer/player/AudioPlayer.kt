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
    val equalizerBands: StateFlow<List<EqualizerBand>>
    val isNormalizationEnabled: StateFlow<Boolean>

    fun play(song: Song)
    fun setPlaylist(songs: List<Song>)
    fun pause()
    fun resume()
    fun next()
    fun previous()
    fun toggleShuffle()
    fun toggleRepeat()
    fun seekTo(position: Long)
    fun seekForward(millis: Long = 10000)
    fun seekBackward(millis: Long = 10000)
    fun cleanUp()
    fun setEqualizerBandLevel(bandIndex: Int, level: Int)
    fun resetEqualizer()
    fun toggleNormalization()
    fun updateSongArt(songId: Long, art: ByteArray)

    enum class RepeatMode {
        OFF, ONE, ALL
    }

    data class EqualizerBand(
        val frequency: Int,
        val level: Int,
        val minLevel: Int,
        val maxLevel: Int
    )
}
