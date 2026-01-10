package com.example.voidplayer.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.voidplayer.MainActivity
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
    
    private var mediaSession: MediaSession? = null
    private var androidEqualizer: Equalizer? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    override val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(AudioPlayer.RepeatMode.OFF)
    override val repeatMode: StateFlow<AudioPlayer.RepeatMode> = _repeatMode.asStateFlow()

    private val _equalizerBands = MutableStateFlow<List<AudioPlayer.EqualizerBand>>(emptyList())
    override val equalizerBands: StateFlow<List<AudioPlayer.EqualizerBand>> = _equalizerBands.asStateFlow()

    private var playlist: List<Song> = emptyList()
    private var progressJob: Job? = null

    init {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        mediaSession = MediaSession.Builder(context, player)
            .setSessionActivity(pendingIntent)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressUpdate()
                    startPlaybackService()
                    ensureEqualizer()
                } else {
                    stopProgressUpdate()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                if (index in playlist.indices) {
                    _currentSong.value = playlist[index]
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _error.value = "Playback Error: ${error.message}"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    updateCurrentSongMetadata()
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                updateCurrentSongMetadata()
            }
        })
    }

    private fun updateCurrentSongMetadata() {
        val current = _currentSong.value ?: return
        val duration = if (player.duration != androidx.media3.common.C.TIME_UNSET) player.duration else current.duration
        val artist = player.mediaMetadata.artist?.toString() ?: current.artist
        // Only title if it's not empty? Usually filename is a decent fallback, but metadata title is better.
        val title = player.mediaMetadata.title?.toString() ?: current.title
        
        if (duration != current.duration || artist != current.artist || title != current.title) {
            _currentSong.value = current.copy(
                duration = duration,
                artist = if(artist == "Unknown Artist") "Unknown Artist" else artist, // Keep Unknown if unknown
                title = title
            )
        }
    }

    private fun ensureEqualizer() {
        if (androidEqualizer == null) {
            try {
                androidEqualizer = Equalizer(0, player.audioSessionId).apply {
                    enabled = true
                }
                updateEqualizerBandsState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateEqualizerBandsState() {
        val eq = androidEqualizer ?: return
        val bands = mutableListOf<AudioPlayer.EqualizerBand>()
        val minMax = eq.bandLevelRange
        for (i in 0 until eq.numberOfBands) {
            bands.add(
                AudioPlayer.EqualizerBand(
                    frequency = eq.getCenterFreq(i.toShort()) / 1000,
                    level = eq.getBandLevel(i.toShort()).toInt(),
                    minLevel = minMax[0].toInt(),
                    maxLevel = minMax[1].toInt()
                )
            )
        }
        _equalizerBands.value = bands
    }

    override fun setEqualizerBandLevel(bandIndex: Int, level: Int) {
        ensureEqualizer()
        androidEqualizer?.let { eq ->
            eq.setBandLevel(bandIndex.toShort(), level.toShort())
            updateEqualizerBandsState()
        }
    }

    override fun updateSongArt(songId: Long, art: ByteArray) {
        val index = playlist.indexOfFirst { it.id == songId }
        if (index != -1) {
            val updatedSong = playlist[index].copy(coverArt = art)
            playlist = playlist.toMutableList().apply { set(index, updatedSong) }
            
            if (_currentSong.value?.id == songId) {
                _currentSong.value = updatedSong
            }
        }
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMediaSession(): MediaSession? = mediaSession

    override fun setPlaylist(songs: List<Song>) {
        this.playlist = songs
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkData(song.coverArt, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems)
        player.prepare()
        
        if (_currentSong.value == null && songs.isNotEmpty()) {
            _currentSong.value = songs[0]
        }
    }

    override fun play(song: Song) {
        val index = playlist.indexOfFirst { it.id == song.id }
        if (index != -1) {
            _currentSong.value = song
            player.seekTo(index, 0)
            player.play()
        } else {
            setPlaylist(listOf(song))
            _currentSong.value = song
            player.play()
        }
        startPlaybackService()
    }

    override fun pause() { player.pause() }
    override fun resume() { player.play() }

    override fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }

    override fun previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    override fun toggleShuffle() {
        val newValue = !_isShuffle.value
        _isShuffle.value = newValue
        player.shuffleModeEnabled = newValue
    }

    override fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            AudioPlayer.RepeatMode.OFF -> AudioPlayer.RepeatMode.ALL
            AudioPlayer.RepeatMode.ALL -> AudioPlayer.RepeatMode.ONE
            AudioPlayer.RepeatMode.ONE -> AudioPlayer.RepeatMode.OFF
        }
        _repeatMode.value = nextMode
        player.repeatMode = when (nextMode) {
            AudioPlayer.RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            AudioPlayer.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            AudioPlayer.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    override fun seekTo(position: Long) {
        player.seekTo(position)
        _currentPosition.value = player.currentPosition
    }

    override fun seekForward(millis: Long) {
        val newPos = (player.currentPosition + millis).coerceAtMost(player.duration)
        seekTo(newPos)
    }

    override fun seekBackward(millis: Long) {
        val newPos = (player.currentPosition - millis).coerceAtLeast(0L)
        seekTo(newPos)
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressJob = scope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun cleanUp() {
        mediaSession?.release()
        mediaSession = null
        androidEqualizer?.release()
        androidEqualizer = null
        player.release()
        stopProgressUpdate()
    }
}
