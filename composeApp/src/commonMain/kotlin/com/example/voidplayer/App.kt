package com.example.voidplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voidplayer.data.SongRepository
import com.example.voidplayer.model.Song
import com.example.voidplayer.player.AudioPlayer

// Theme
val CyberColor = Color(0xFF00FFFF)
val DarkScheme = darkColorScheme(
    primary = CyberColor,
    onPrimary = Color.Black,
    surface = Color(0xFF121212),
    background = Color.Black
)

@Composable
fun App(
    repository: SongRepository,
    player: AudioPlayer
) {
    MaterialTheme(colorScheme = DarkScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AudioAppContent(repository, player)
        }
    }
}

@Composable
fun AudioAppContent(
    repository: SongRepository,
    player: AudioPlayer
) {
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val currentPosition by player.currentPosition.collectAsState()

    // Load songs
    LaunchedEffect(Unit) {
        try {
            songs = repository.getSongs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Void Player",
            style = MaterialTheme.typography.headlineMedium,
            color = CyberColor,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(songs) { song ->
                SongItem(
                    song = song,
                    isPlaying = (song.id == currentSong?.id),
                    onClick = { player.play(song) }
                )
            }
        }

        if (currentSong != null) {
            PlayerControl(
                song = currentSong!!,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                onPlayPause = { if (isPlaying) player.pause() else player.resume() },
                onSeek = { player.seekTo(it) }
            )
        }
    }
}

@Composable
fun SongItem(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPlaying) CyberColor else Color.White,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PlayerControl(
    song: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        Text(text = song.title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(text = song.artist, color = Color.Gray)

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..song.duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = CyberColor,
                activeTrackColor = CyberColor
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(currentPosition), color = Color.Gray)
            Text(text = formatTime(song.duration), color = Color.Gray)
        }

        Button(
            onClick = onPlayPause,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(containerColor = CyberColor, contentColor = Color.Black)
        ) {
            Text(if (isPlaying) "Pause" else "Play")
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${if (seconds < 10) "0" else ""}${seconds}"
}
