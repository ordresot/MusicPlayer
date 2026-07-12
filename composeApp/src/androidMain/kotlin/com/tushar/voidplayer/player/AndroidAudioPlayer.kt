package com.tushar.voidplayer.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.tushar.voidplayer.MainActivity
import com.tushar.voidplayer.model.Song
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

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    // Simple, default ExoPlayer â€” no custom buffers or renderer factories
    // which caused OOM and codec-loading crashes on many devices.
    private val player = ExoPlayer.Builder(context)
        .build()
        .apply {
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            // WAKE_MODE_NONE: no WAKE_LOCK or WifiLock is acquired.
            // WAKE_MODE_LOCAL and WAKE_MODE_NETWORK both require android.permission.WAKE_LOCK
            // which is not declared in AndroidManifest.xml. Using them would throw
            // SecurityException on strict devices. For local file playback, WAKE_MODE_NONE is
            // correct â€” the screen stays on while the user is interacting with the app.
            setWakeMode(C.WAKE_MODE_NONE)
        }

    private val scope = CoroutineScope(Dispatchers.Main)
    
    private var mediaSession: MediaSession? = null
    private var androidEqualizer: Equalizer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null

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
    
    private val _isNormalizationEnabled = MutableStateFlow(false)
    override val isNormalizationEnabled: StateFlow<Boolean> = _isNormalizationEnabled.asStateFlow()

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
                } else {
                    stopProgressUpdate()
                }
            }

            // Only attach DSP effects once ExoPlayer has a valid audio session.
            // Attaching with audioSessionId == 0 causes a native SIGSEGV crash
            // in libeffect that bypasses all Kotlin try-catch blocks.
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0 && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    ensureEqualizer()
                    ensureNormalization()
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
        val duration = if (player.duration != C.TIME_UNSET) player.duration else current.duration
        val artist = player.mediaMetadata.artist?.toString() ?: current.artist
        val title = player.mediaMetadata.title?.toString() ?: current.title
        
        if (duration != current.duration || artist != current.artist || title != current.title) {
            _currentSong.value = current.copy(
                duration = duration,
                artist = if (artist == "Unknown Artist") "Unknown Artist" else artist,
                title = title
            )
        }
    }

    private fun ensureEqualizer() {
        val sessionId = player.audioSessionId
        if (sessionId == 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (androidEqualizer != null) return
        
        try {
            androidEqualizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
            updateEqualizerBandsState()
        } catch (e: Throwable) {
            // Thrown on devices without hardware DSP support or on custom ROMs
            // that strip the audio effect libraries. Silently ignore.
            e.printStackTrace()
        }
    }

    private fun updateEqualizerBandsState() {
        val eq = androidEqualizer ?: return
        try {
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
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun setEqualizerBandLevel(bandIndex: Int, level: Int) {
        ensureEqualizer()
        try {
            androidEqualizer?.let { eq ->
                eq.setBandLevel(bandIndex.toShort(), level.toShort())
                updateEqualizerBandsState()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun resetEqualizer() {
        try {
            androidEqualizer?.let { eq ->
                for (i in 0 until eq.numberOfBands) {
                    eq.setBandLevel(i.toShort(), 0)
                }
                updateEqualizerBandsState()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun ensureNormalization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return // DynamicsProcessing requires API 28+
        val sessionId = player.audioSessionId
        if (sessionId == 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (dynamicsProcessing != null) return
        
        try {
            val builder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2, // 2 channels (Stereo)
                false, 0, // no pre-eq
                true, 1,  // multiband compressor (1 band)
                false, 0, // no post-eq
                true      // limiter enabled
            )
            dynamicsProcessing = DynamicsProcessing(0, sessionId, builder.build()).apply {
                enabled = _isNormalizationEnabled.value
            }
        } catch (e: Throwable) {
            // DynamicsProcessing may not exist on older/custom ROM devices even if API >= 28.
            // Silently ignore to preserve playback.
            e.printStackTrace()
        }
    }

    override fun toggleNormalization() {
        val newValue = !_isNormalizationEnabled.value
        _isNormalizationEnabled.value = newValue
        ensureNormalization()
        try {
            dynamicsProcessing?.enabled = newValue
        } catch (e: Throwable) {
            e.printStackTrace()
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
        // PlaybackService is a MediaSessionService. Media3 manages its own foreground
        // lifecycle internally. We only need a regular startService() to bind to it â€”
        // calling startForegroundService() from outside would cause a
        // ForegroundServiceStartNotAllowedException on Android 12+ if the app is
        // in the background, as the service won't call startForeground() fast enough.
        try {
            val intent = Intent(context, PlaybackService::class.java)
            context.startService(intent)
        } catch (e: Throwable) {
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
                delay(250)
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
        try {
            androidEqualizer?.release()
        } catch (e: Throwable) { e.printStackTrace() }
        androidEqualizer = null
        try {
            dynamicsProcessing?.release()
        } catch (e: Throwable) { e.printStackTrace() }
        dynamicsProcessing = null
        player.release()
        stopProgressUpdate()
    }
}
