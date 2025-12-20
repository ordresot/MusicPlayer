package com.example.voidplayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voidplayer.data.SongRepository
import com.example.voidplayer.model.Song
import com.example.voidplayer.player.AudioPlayer
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.vector.ImageVector


// --- Cyberpunk Theme Colors ---
val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF00FF)
val DeepBlack = Color(0xFF050510)
val DarkSurface = Color(0xFF13131F)
val NeonYellow = Color(0xFFFNEE00)

val CyberpunkScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepBlack,
    secondary = NeonMagenta,
    background = DeepBlack,
    surface = DarkSurface,
    onSurface = Color.White
)

@Composable
fun App(
    repository: SongRepository,
    player: AudioPlayer
) {
    MaterialTheme(colorScheme = CyberpunkScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth > 600.dp
                MainContent(repository, player, isWideScreen)
            }
        }
    }
}

@Composable
fun MainContent(
    repository: SongRepository,
    player: AudioPlayer,
    isWideScreen: Boolean
) {
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    val currentSong by player.currentSong.collectAsState()
    val error by player.error.collectAsState()

    // Load songs once
    LaunchedEffect(Unit) {
        try {
            songs = repository.getSongs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (currentSong != null) 60.dp else 20.dp) // Space for Island
        ) {
            // Header
            Text(
                text = "VOID PLAYER // V.1.0",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = NeonCyan,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .align(if (isWideScreen) Alignment.CenterHorizontally else Alignment.Start)
            )

            // Error Banner
            if (error != null) {
                ErrorBanner(error!!)
            }

            // Song List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    val isPlaying = song.id == currentSong?.id
                    CyberListItem(
                        song = song,
                        isPlaying = isPlaying,
                        onClick = { player.play(song) }
                    )
                }
            }
        }

        // Dynamic Island Overlay
        if (currentSong != null) {
            DynamicIsland(
                modifier = Modifier.align(Alignment.TopCenter),
                song = currentSong!!,
                player = player
            )
        }
    }
}

@Composable
fun ErrorBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color.Red, CutCornerShape(8.dp))
            .background(Color(0x33FF0000))
            .padding(12.dp)
    ) {
        Text(text = "ERROR: $message", color = Color.Red, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun CyberListItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isPlaying) NeonCyan else Color.Transparent
    val backgroundColor = if (isPlaying) Color(0xFF0F2022) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(CutCornerShape(bottomEnd = 16.dp))
            .border(1.dp, borderColor, CutCornerShape(bottomEnd = 16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isPlaying) NeonMagenta else Color.DarkGray, CutCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                // Animated Indicator could go here
                Text("▶", color = Color.White)
            } else {
                Text("♫", color = Color.LightGray)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color = if (isPlaying) NeonCyan else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Text(
            text = formatTime(song.duration),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = if (isPlaying) NeonYellow else Color.DarkGray
        )
    }
}

@Composable
fun DynamicIsland(
    modifier: Modifier = Modifier,
    song: Song,
    player: AudioPlayer
) {
    val isPlaying by player.isPlaying.collectAsState()
    
    // Animate expansion
    val width by animateDpAsState(targetValue = if (isPlaying) 350.dp else 200.dp)
    val height by animateDpAsState(targetValue = if (isPlaying) 120.dp else 40.dp)
    val cornerRadius by animateDpAsState(targetValue = if (isPlaying) 24.dp else 20.dp)

    Box(
        modifier = modifier
            .padding(top = 10.dp)
            .size(width = width, height = height)
            .shadow(
                elevation = 16.dp, 
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = NeonCyan,
                spotColor = NeonMagenta
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black)
            .border(1.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta)), RoundedCornerShape(cornerRadius))
    ) {
        if (isPlaying) {
            ExpandedIslandContent(song, player)
        } else {
            CompactIslandContent(song)
        }
    }
}

@Composable
fun CompactIslandContent(song: Song) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
         Text(
            text = "PAUSED",
            color = NeonMagenta,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
        )
    }
}

@Composable
fun ExpandedIslandContent(song: Song, player: AudioPlayer) {
    val currentPosition by player.currentPosition.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Song Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
             // Scrolling text if needed
             Column(modifier = Modifier.weight(1f)) {
                 Text(
                    text = song.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                     maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
             }
             
             // Visualizer Bars (Static Simulation)
             Row(verticalAlignment = Alignment.Bottom) {
                 repeat(4) {
                     Box(
                         modifier = Modifier
                             .padding(horizontal = 2.dp)
                             .width(4.dp)
                             .height((10..20).random().dp) // Simulate movement
                             .background(NeonCyan)
                     )
                 }
             }
        }
        
        // Progress
        CustomProgressBar(
            current = currentPosition,
            total = song.duration,
            onSeek = { player.seekTo(it) }
        )
        
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
             ControlIcon(text = "⏮") { /* Prev */ }
             ControlIcon(text = "⏸", isHighlight = true) { player.pause() }
             ControlIcon(text = "⏭") { /* Next */ }
        }
    }
}

@Composable
fun CustomProgressBar(current: Long, total: Long, onSeek: (Long) -> Unit) {
    val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.DarkGray, RoundedCornerShape(2.dp))
            .clickable { /* logic to seek on tap could be complex without slider width knowledge */ }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta)), RoundedCornerShape(2.dp))
        )
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(formatTime(current), color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(formatTime(total), color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ControlIcon(text: String, isHighlight: Boolean = false, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 24.sp,
        color = if (isHighlight) NeonCyan else Color.White,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${if (seconds < 10) "0" else ""}${seconds}"
}
