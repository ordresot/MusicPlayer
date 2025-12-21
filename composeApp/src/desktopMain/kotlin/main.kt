import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.voidplayer.App
import com.example.voidplayer.data.SongRepository
import com.example.voidplayer.model.Song
import com.example.voidplayer.player.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.compose.ui.res.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Void Player",
        icon = painterResource("icon.png")
    ) {
        App(DesktopSongRepository(), DesktopAudioPlayer())
    }
}

class DesktopSongRepository : SongRepository {
    override suspend fun getSongs(): List<Song> {
        return emptyList()
    }

    override suspend fun loadFromFolder(uriString: String): List<Song> {
        return emptyList()
    }

    override suspend fun loadArt(uriString: String): ByteArray? {
        return null
    }
}

class DesktopAudioPlayer : AudioPlayer {
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    override val error: StateFlow<String?> = MutableStateFlow(null)

    override val isShuffle: StateFlow<Boolean> = MutableStateFlow(false)
    override val repeatMode: StateFlow<AudioPlayer.RepeatMode> = MutableStateFlow(AudioPlayer.RepeatMode.OFF)

    override fun play(song: Song) {}
    override fun setPlaylist(songs: List<Song>) {}
    override fun pause() {}
    override fun resume() {}
    override fun next() {}
    override fun previous() {}
    override fun toggleShuffle() {}
    override fun toggleRepeat() {}
    override fun seekTo(position: Long) {}
    override fun seekForward(millis: Long) {}
    override fun seekBackward(millis: Long) {}
    override fun cleanUp() {}
    override fun openEqualizer() {}
}
