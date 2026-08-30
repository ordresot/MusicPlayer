import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.tushar.voidplayer.App
import com.tushar.voidplayer.data.SongRepository
import com.tushar.voidplayer.model.Playlist
import com.tushar.voidplayer.model.Song
import com.tushar.voidplayer.player.AudioPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File
import java.util.Properties
import javax.swing.JFileChooser
import javax.swing.UIManager

fun main() = application {
    val repository = remember { DesktopSongRepository() }
    val player = remember { DesktopAudioPlayer() }

    val pickedFolderUri = remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("Ready") }

    // Load last used folder on startup
    LaunchedEffect(Unit) {
        val lastFolder = repository.getLastUsedFolder()
        if (!lastFolder.isNullOrBlank() && File(lastFolder).exists()) {
            pickedFolderUri.value = lastFolder
            statusMessage = "Loaded: $lastFolder"
        }
    }

    Window(
        onCloseRequest = {
            player.cleanUp()
            exitApplication()
        },
        title = "Void Player",
        icon = painterResource("icon.png")
    ) {
        App(
            repository = repository,
            player = player,
            pickedFolderUri = pickedFolderUri,
            statusMessage = statusMessage,
            onPickFolder = {
                val selectedPath = pickDesktopFolder()
                if (!selectedPath.isNullOrBlank()) {
                    pickedFolderUri.value = selectedPath
                    repository.saveLastUsedFolder(selectedPath)
                    statusMessage = "Folder selected: $selectedPath"
                }
            }
        )
    }
}

/**
 * Native Windows / Desktop directory chooser dialog
 */
fun pickDesktopFolder(): String? {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Throwable) {}

    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select Music Folder"
        isAcceptAllFileFilterUsed = false
    }

    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}

class DesktopSongRepository : SongRepository {

    private val dataDir = File(System.getProperty("user.home"), ".voidplayer").apply { mkdirs() }
    private val prefsFile = File(dataDir, "preferences.properties")
    private val playlistsFile = File(dataDir, "playlists.properties")

    fun getLastUsedFolder(): String? {
        if (!prefsFile.exists()) return null
        val props = Properties()
        prefsFile.inputStream().use { props.load(it) }
        return props.getProperty("last_folder")
    }

    fun saveLastUsedFolder(path: String) {
        val props = Properties()
        if (prefsFile.exists()) {
            prefsFile.inputStream().use { props.load(it) }
        }
        props.setProperty("last_folder", path)
        prefsFile.outputStream().use { props.store(it, "Void Player Preferences") }
    }

    private fun getFavorites(): Set<String> {
        if (!prefsFile.exists()) return emptySet()
        val props = Properties()
        prefsFile.inputStream().use { props.load(it) }
        val raw = props.getProperty("favorites", "")
        return if (raw.isNotBlank()) raw.split(",").toSet() else emptySet()
    }

    override suspend fun toggleFavorite(songId: Long, isFav: Boolean) = withContext(Dispatchers.IO) {
        val favs = getFavorites().toMutableSet()
        if (isFav) favs.add(songId.toString()) else favs.remove(songId.toString())
        val props = Properties()
        if (prefsFile.exists()) {
            prefsFile.inputStream().use { props.load(it) }
        }
        props.setProperty("favorites", favs.joinToString(","))
        prefsFile.outputStream().use { props.store(it, "Void Player Preferences") }
    }

    override suspend fun getPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        if (!playlistsFile.exists()) return@withContext emptyList<Playlist>()
        val props = Properties()
        playlistsFile.inputStream().use { props.load(it) }
        val list = mutableListOf<Playlist>()
        for (key in props.stringPropertyNames()) {
            if (key.startsWith("pl_")) {
                val value = props.getProperty(key, "")
                val parts = value.split(":::")
                val name = parts.getOrNull(0) ?: "Playlist"
                val idsStr = parts.getOrNull(1) ?: ""
                val songIds = if (idsStr.isNotBlank()) {
                    idsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
                } else {
                    emptyList()
                }
                list.add(Playlist(id = key.removePrefix("pl_"), name = name, songIds = songIds))
            }
        }
        list
    }

    override suspend fun savePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        val props = Properties()
        if (playlistsFile.exists()) {
            playlistsFile.inputStream().use { props.load(it) }
        }
        props.setProperty("pl_${playlist.id}", "${playlist.name}:::${playlist.songIds.joinToString(",")}")
        playlistsFile.outputStream().use { props.store(it, "Void Player Playlists") }
    }

    override suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        if (!playlistsFile.exists()) return@withContext
        val props = Properties()
        playlistsFile.inputStream().use { props.load(it) }
        props.remove("pl_$playlistId")
        playlistsFile.outputStream().use { props.store(it, "Void Player Playlists") }
    }

    override suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        val lastFolder = getLastUsedFolder()
        if (!lastFolder.isNullOrBlank()) {
            loadFromFolder(lastFolder)
        } else {
            // Check default user music folder
            val userMusic = File(System.getProperty("user.home"), "Music")
            if (userMusic.exists()) loadFromFolder(userMusic.absolutePath) else emptyList()
        }
    }

    override suspend fun loadFromFolder(uriString: String): List<Song> = withContext(Dispatchers.IO) {
        val folder = File(uriString)
        if (!folder.exists() || !folder.isDirectory) return@withContext emptyList()

        val audioFiles = mutableListOf<File>()
        folder.walkTopDown().filter { it.isFile && isAudioFile(it.name) }.forEach {
            audioFiles.add(it)
        }

        val favs = getFavorites()
        audioFiles.map { file ->
            val id = file.absolutePath.hashCode().toLong() and 0x7FFF_FFFF_FFFF_FFFFL
            val fileName = file.nameWithoutExtension
            val (artist, title) = if (fileName.contains(" - ")) {
                val p = fileName.split(" - ", limit = 2)
                p[0].trim() to p[1].trim()
            } else {
                "Unknown Artist" to fileName
            }

            Song(
                id = id,
                title = title,
                artist = artist,
                album = file.parentFile?.name ?: "Local Audio",
                duration = estimateAudioDuration(file),
                uri = file.absolutePath,
                coverArt = null,
                isFavorite = favs.contains(id.toString())
            )
        }
    }

    override suspend fun loadArt(uriString: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val file = File(uriString)
            val parent = file.parentFile
            if (parent != null) {
                val cover = parent.listFiles { _, name ->
                    val lower = name.lowercase()
                    lower == "cover.jpg" || lower == "cover.png" || lower == "folder.jpg" || lower == "album.jpg"
                }?.firstOrNull()
                if (cover != null && cover.exists()) {
                    return@withContext cover.readBytes()
                }
            }
        } catch (_: Throwable) {}
        null
    }

    override suspend fun loadLyrics(uriString: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(uriString)
            val parent = file.parentFile
            if (parent != null) {
                val base = file.nameWithoutExtension
                val lrc = File(parent, "$base.lrc").takeIf { it.exists() }
                    ?: File(parent, "$base.LRC").takeIf { it.exists() }
                if (lrc != null) {
                    return@withContext lrc.readText()
                }
            }
        } catch (_: Throwable) {}
        null
    }

    private fun isAudioFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac") ||
               lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".ogg") ||
               lower.endsWith(".opus") || lower.endsWith(".wma")
    }

    private fun estimateAudioDuration(file: File): Long {
        // Java Sound SPI only supports WAV/AIFF — for MP3/FLAC/etc. we estimate
        // from file size using typical bitrate ranges.
        // Average bitrate ~192 kbps = 24 KB/s
        val avgBytesPerSec = 192 * 1024 / 8 // 24 KB/s
        val approxSecs = (file.length() / avgBytesPerSec.toDouble()).coerceIn(15.0, 1800.0)
        return (approxSecs * 1000).toLong()
    }
}

class DesktopAudioPlayer : AudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // vlcj: full-featured media player backed by LibVLC
    // Supports MP3, FLAC, OGG, AAC, OPUS, WMA, M4A, WAV and many more
    private val mediaPlayerFactory = MediaPlayerFactory()
    private val mediaPlayer: MediaPlayer = mediaPlayerFactory.mediaPlayers().newMediaPlayer()

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

    override val equalizerBands: StateFlow<List<AudioPlayer.EqualizerBand>> = MutableStateFlow(emptyList())
    override val isNormalizationEnabled: StateFlow<Boolean> = MutableStateFlow(false)

    private val _playbackSpeed = MutableStateFlow(1.0f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    override val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    private var currentPlaylist: List<Song> = emptyList()
    private var positionJob: Job? = null

    init {
        // Listen to playback events for proper UI updates
        mediaPlayer.events().addMediaPlayerEventListener(
            object : MediaPlayerEventAdapter() {
                override fun playing(player: MediaPlayer) {
                    _isPlaying.value = true
                    startPositionTracker()
                }

                override fun paused(player: MediaPlayer) {
                    _isPlaying.value = false
                    positionJob?.cancel()
                }

                override fun stopped(player: MediaPlayer) {
                    _isPlaying.value = false
                    positionJob?.cancel()
                }

                override fun finished(player: MediaPlayer) {
                    positionJob?.cancel()
                    handleSongCompletion()
                }

                override fun error(player: MediaPlayer) {
                    _isPlaying.value = false
                    positionJob?.cancel()
                    _error.value = "Playback Error"
                }
            }
        )
    }

    override fun setPlaylist(songs: List<Song>) {
        currentPlaylist = songs
        _currentQueue.value = songs
    }

    override fun play(song: Song) {
        cleanUp()
        _currentSong.value = song
        _currentPosition.value = 0L
        _error.value = null

        try {
            val file = File(song.uri)
            if (!file.exists()) {
                _error.value = "File not found: ${song.title}"
                return
            }

            // vlcj handles ALL formats natively via LibVLC
            // media().play(path) prepares and starts playback
            mediaPlayer.media().play(file.absolutePath)
            startPositionTracker()
        } catch (e: Throwable) {
            _error.value = "Playback Error: ${e.message}"
            _isPlaying.value = false
        }
    }

    private fun startPositionTracker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (_isPlaying.value) {
                delay(300)
                try {
                    // status().time() returns current position in ms
                    // status().length() returns total duration in ms
                    val time = mediaPlayer.status().time()
                    _currentPosition.value = time
                } catch (e: Throwable) {
                    // Player might not be ready yet
                }
            }
        }
    }

    private fun handleSongCompletion() {
        when (_repeatMode.value) {
            AudioPlayer.RepeatMode.ONE -> _currentSong.value?.let { play(it) }
            AudioPlayer.RepeatMode.ALL -> next()
            AudioPlayer.RepeatMode.OFF -> {
                val currentIndex = currentPlaylist.indexOfFirst { it.id == _currentSong.value?.id }
                if (currentIndex >= 0 && currentIndex < currentPlaylist.size - 1) {
                    next()
                } else {
                    pause()
                }
            }
        }
    }

    override fun pause() {
        mediaPlayer.controls().pause()
        positionJob?.cancel()
    }

    override fun resume() {
        mediaPlayer.controls().play()
        startPositionTracker()
    }

    override fun next() {
        if (currentPlaylist.isEmpty()) return

        val currentId = _currentSong.value?.id
        if (_isShuffle.value) {
            val candidates = currentPlaylist.filter { it.id != currentId }
            val nextSong = candidates.randomOrNull() ?: currentPlaylist.randomOrNull()
            nextSong?.let { play(it) }
        } else {
            val idx = currentPlaylist.indexOfFirst { it.id == currentId }
            val nextIdx = if (idx >= 0 && idx < currentPlaylist.size - 1) idx + 1 else 0
            currentPlaylist.getOrNull(nextIdx)?.let { play(it) }
        }
    }

    override fun previous() {
        if (currentPlaylist.isEmpty()) return

        val currentId = _currentSong.value?.id
        if (_currentPosition.value > 3000) {
            _currentSong.value?.let { play(it) }
            return
        }

        if (_isShuffle.value) {
            val prevSong = currentPlaylist.randomOrNull()
            prevSong?.let { play(it) }
        } else {
            val idx = currentPlaylist.indexOfFirst { it.id == currentId }
            val prevIdx = if (idx > 0) idx - 1 else currentPlaylist.size - 1
            currentPlaylist.getOrNull(prevIdx)?.let { play(it) }
        }
    }

    override fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    override fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            AudioPlayer.RepeatMode.OFF -> AudioPlayer.RepeatMode.ALL
            AudioPlayer.RepeatMode.ALL -> AudioPlayer.RepeatMode.ONE
            AudioPlayer.RepeatMode.ONE -> AudioPlayer.RepeatMode.OFF
        }
    }

    override fun seekTo(position: Long) {
        mediaPlayer.controls().setTime(position)
        _currentPosition.value = position
    }

    override fun seekForward(millis: Long) {
        val newPos = (_currentPosition.value + millis).coerceAtMost(_currentSong.value?.duration ?: 0L)
        seekTo(newPos)
    }

    override fun seekBackward(millis: Long) {
        val newPos = (_currentPosition.value - millis).coerceAtLeast(0L)
        seekTo(newPos)
    }

    override fun cleanUp() {
        positionJob?.cancel()
        positionJob = null
        try {
            mediaPlayer.controls().stop()
        } catch (_: Throwable) {}
        _isPlaying.value = false
    }

    override fun setEqualizerBandLevel(bandIndex: Int, level: Int) {}
    override fun resetEqualizer() {}
    override fun toggleNormalization() {}
    override fun updateSongArt(songId: Long, art: ByteArray) {}

    override fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 3.0f)
        _playbackSpeed.value = safeSpeed
        try {
            mediaPlayer.controls().setRate(safeSpeed)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun setVolume(volume: Float) {
        try {
            val vlcVolume = (volume * 200).toInt().coerceIn(0, 200)
            mediaPlayer.audio().setVolume(vlcVolume.toInt())
        } catch (_: Throwable) {}
    }
}
