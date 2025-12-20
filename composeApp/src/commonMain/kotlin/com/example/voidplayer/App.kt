package com.example.voidplayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset


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
                start = Offset(offset, offset),
                end = Offset(size.width + offset, size.height + offset),
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

    LaunchedEffect(pickedFolderUri.value) {
        try {
            val loadedSongs = if (pickedFolderUri.value != null) {
                repository.loadFromFolder(pickedFolderUri.value!!)
            } else {
                repository.getSongs()
            }
            songs = loadedSongs
            player.setPlaylist(loadedSongs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VOID PLAYER",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = NeonCyan
                )
                
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
                contentPadding = PaddingValues(bottom = 120.dp)
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
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
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
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onPickFolder,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack),
            shape = CutCornerShape(8.dp)
        ) {
            Text("SELECT MUSIC FOLDER", fontWeight = FontWeight.Bold)
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
                .size(50.dp)
                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverArt != null) {
                 Image(
                    bitmap = song.coverArt.toImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("♫", color = Color.LightGray)
            }
            
            if (isPlaying) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                LiveWaveform(color = NeonCyan, barCount = 3, heightRange = 5..20)
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
    var isExpanded by remember { mutableStateOf(false) }
    val isPlaying by player.isPlaying.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    
    val width by animateDpAsState(targetValue = if (isExpanded) 360.dp else 260.dp, animationSpec = spring(stiffness = Spring.StiffnessLow))
    val height by animateDpAsState(targetValue = if (isExpanded) 140.dp else 60.dp, animationSpec = spring(stiffness = Spring.StiffnessLow))
    val cornerRadius by animateDpAsState(targetValue = 30.dp)

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shadow(20.dp, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black)
            .border(1.dp, Color.DarkGray, RoundedCornerShape(cornerRadius))
            .clickable { isExpanded = !isExpanded }
            .padding(12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                ) {
                    if (song.coverArt != null) {
                        Image(
                            bitmap = song.coverArt.toImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                if (!isExpanded) {
                    LiveWaveform(color = NeonCyan, barCount = 3, heightRange = 4..16)
                } else {
                    IconButton(onClick = { player.toggleShuffle() }) {
                        Text("🔀", color = if (isShuffle) NeonCyan else Color.Gray, fontSize = 16.sp)
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { player.toggleRepeat() }) {
                         Text(
                             text = when(repeatMode) {
                                 AudioPlayer.RepeatMode.OFF -> "🔁"
                                 AudioPlayer.RepeatMode.ALL -> "🔁"
                                 AudioPlayer.RepeatMode.ONE -> "🔂"
                             },
                             color = if (repeatMode != AudioPlayer.RepeatMode.OFF) NeonCyan else Color.Gray,
                             fontSize = 18.sp
                         )
                    }
                    IconButton(onClick = { player.previous() }) {
                        Text("⏮", color = Color.White, fontSize = 24.sp)
                    }
                    IconButton(onClick = { if (isPlaying) player.pause() else player.resume() }) {
                        Text(if (isPlaying) "⏸" else "▶", color = NeonCyan, fontSize = 28.sp)
                    }
                    IconButton(onClick = { player.next() }) {
                        Text("⏭", color = Color.White, fontSize = 24.sp)
                    }
                    IconButton(onClick = { isExpanded = false }) {
                        Text("▼", color = Color.Gray, fontSize = 16.sp)
                    }
                }
                
                val currentPosition by player.currentPosition.collectAsState()
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.DarkGray)
                ) {
                    val progress = if (song.duration > 0) currentPosition.toFloat() / song.duration else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(NeonCyan)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveWaveform(color: Color, barCount: Int, heightRange: IntRange) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
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
                    .width(3.dp)
                    .height(height.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

fun ByteArray.toImageBitmap(): ImageBitmap {
    return android.graphics.BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap()
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${if (seconds < 10) "0" else ""}${seconds}"
}
