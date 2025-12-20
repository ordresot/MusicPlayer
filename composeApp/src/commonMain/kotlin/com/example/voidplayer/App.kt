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
import androidx.compose.ui.graphics.TileMode


// --- Cyberpunk Theme Colors ---
val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF00FF)
val DeepBlack = Color(0xFF050510)
val DarkSurface = Color(0xFF13131F)
val NeonYellow = Color(0xFFFFEE00)

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
    player: AudioPlayer,
    pickedFolderUri: State<String?> = mutableStateOf(null),
    statusMessage: String = "",
    onPickFolder: () -> Unit = {}
) {
    MaterialTheme(colorScheme = CyberpunkScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth > 600.dp
                AnimatedCyberBackground()
                MainContent(repository, player, isWideScreen, pickedFolderUri, statusMessage, onPickFolder)
            }
        }
    }
}

@Composable
fun AnimatedCyberBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(DeepBlack, DarkSurface, Color(0xFF0A0A15)),
                start = androidx.compose.ui.geometry.Offset(offset, offset),
                end = androidx.compose.ui.geometry.Offset(size.width + offset, size.height + offset),
                tileMode = TileMode.Mirror
            )
        )
    }
}

@Composable
fun MainContent(
    repository: SongRepository,
    player: AudioPlayer,
    isWideScreen: Boolean,
    pickedFolderUri: State<String?>,
    statusMessage: String,
    onPickFolder: () -> Unit
) {
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    val currentSong by player.currentSong.collectAsState()
    val error by player.error.collectAsState()

    // Load songs initially or when folder changes
    LaunchedEffect(pickedFolderUri.value) {
        try {
            if (pickedFolderUri.value != null) {
                // Load from specific folder
                songs = repository.loadFromFolder(pickedFolderUri.value!!)
                println("VoidPlayer: App loaded from folder: ${songs.size} songs")
            } else {
                // Default load
                songs = repository.getSongs()
                println("VoidPlayer: App loaded default: ${songs.size} songs")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Log on every recomposition
    println("VoidPlayer: App UI Recomposed. Songs size: ${songs.size}")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (currentSong != null) 70.dp else 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VOID PLAYER // V.DEBUG", // Visual check
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = NeonCyan
                )
                
                // Folder Icon Button
                IconButton(onClick = onPickFolder) {
                    Text("📂", fontSize = 24.sp)
                }
            }

            if (error != null) {
                ErrorBanner(error!!)
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (songs.isEmpty()) {
                    item {
                        EmptyState(onPickFolder, statusMessage)
                    }
                } else {
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
        }

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
fun EmptyState(onPickFolder: () -> Unit, statusMessage: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NO SONGS FOUND",
            color = Color.Gray,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ensure you have audio files > 10s and have granted permissions.",
            color = Color.DarkGray,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Show status message for debugging
        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                color = Color.Yellow,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onPickFolder,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack),
            shape = CutCornerShape(8.dp)
        ) {
            Text("CLICK ME (DEBUG MODE)", fontWeight = FontWeight.Bold)
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
    
    // Entrance Animation (Simple fade in for now as list builds)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
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
                LiveWaveform(color = Color.White, barCount = 3, heightRange = 5..20)
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
    
    val width by animateDpAsState(targetValue = if (isPlaying) 350.dp else 200.dp, animationSpec = spring(stiffness = Spring.StiffnessLow))
    val height by animateDpAsState(targetValue = if (isPlaying) 140.dp else 40.dp, animationSpec = spring(stiffness = Spring.StiffnessLow))
    val cornerRadius by animateDpAsState(targetValue = if (isPlaying) 28.dp else 20.dp)

    Box(
        modifier = modifier
            .padding(top = 10.dp)
            .size(width = width, height = height)
            .shadow(
                elevation = 20.dp, 
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
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
             
             LiveWaveform(color = NeonCyan, barCount = 5, heightRange = 8..24)
        }
        
        CustomProgressBar(
            current = currentPosition,
            total = song.duration,
            onSeek = { player.seekTo(it) }
        )
        
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
fun LiveWaveform(color: Color, barCount: Int, heightRange: IntRange) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        repeat(barCount) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = heightRange.first.toFloat(),
                targetValue = heightRange.last.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(300 + (index * 50), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
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
            .clickable { /* logic to seek on tap */ }
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
