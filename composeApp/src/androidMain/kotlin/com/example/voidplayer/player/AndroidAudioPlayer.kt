package com.example.voidplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.voidplayer.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {

    private val player = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private var progressJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    stopProgressUpdate()
                    // Optional: Auto-next logic could go here
                }
            }
        })
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressJob = scope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition
                delay(1000) // Update every second
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun play(song: Song) {
        if (_currentSong.value?.id != song.id) {
            _currentSong.value = song
            val mediaItem = MediaItem.fromUri(song.uri)
            player.setMediaItem(mediaItem)
            player.prepare()
        }
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        player.play()
    }

    override fun seekTo(position: Long) {
        player.seekTo(position)
        _currentPosition.value = position
    }

    override fun cleanUp() {
        player.release()
        stopProgressUpdate()
    }
}
