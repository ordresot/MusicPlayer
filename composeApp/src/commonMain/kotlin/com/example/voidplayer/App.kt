package com.example.voidplayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voidplayer.data.SongRepository
import com.example.voidplayer.model.Song
import com.example.voidplayer.player.AudioPlayer
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home

// --- Modern Dark Theme Colors ---
val PrimaryAccent = Color(0xFFFFFFFF) // Clean White
val SecondaryAccent = Color(0xFFB0B0B0) // Light Gray
val BackgroundColor = Color(0xFF121212) // Material Dark
val SurfaceColor = Color(0xFF1E1E1E) // Slightly lighter
val ErrorColor = Color(0xFFCF6679)

val ModernScheme = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = BackgroundColor,
    secondary = SecondaryAccent,
    background = BackgroundColor,
    surface = SurfaceColor,
    onSurface = PrimaryAccent
)

val SineEaseInOut = Easing { fraction ->
    (-(kotlin.math.cos(kotlin.math.PI * fraction) - 1f) / 2f).toFloat()
}

@Composable
fun App(
    repository: SongRepository,
    player: AudioPlayer,
    pickedFolderUri: State<String?> = mutableStateOf(null),
    statusMessage: String = "",
    onPickFolder: () -> Unit = {}
) {
    MaterialTheme(colorScheme = ModernScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Subtle Background Gradient
                 Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F0F0F), Color(0xFF1A1A1A))
                        )
                    )
                }
                
                MainContent(repository, player, pickedFolderUri, statusMessage, onPickFolder)
            }
        }
    }
}

@Composable
fun MainContent(
    repository: SongRepository,
    player: AudioPlayer,
    pickedFolderUri: State<String?>,
    statusMessage: String,
    onPickFolder: () -> Unit
) {
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    val currentSong by player.currentSong.collectAsState()
    val error by player.error.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val animatedDominantColor = Color.White 

    LaunchedEffect(pickedFolderUri.value) {
        try {
            isLoading = true
            val loadedSongs = if (pickedFolderUri.value != null) {
                repository.loadFromFolder(pickedFolderUri.value!!)
            } else {
                repository.getSongs()
            }
            songs = loadedSongs
            if (player.currentSong.value == null) {
                player.setPlaylist(loadedSongs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                onPickFolder = onPickFolder, 
                onOpenSettings = { showSettings = true }
            )

            AnimatedVisibility(
                visible = error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                error?.let { ErrorBanner(it) }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else if (songs.isEmpty()) {
                    EmptyState(onPickFolder, statusMessage)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 160.dp, top = 8.dp)
                    ) {
                        items(songs, key = { it.id }) { song ->
                            val isPlaying = song.id == currentSong?.id
                            SongListItem(
                                song = song,
                                repository = repository,
                                isPlaying = isPlaying,
                                activeColor = animatedDominantColor,
                                onClick = { player.play(song) }
                            )
                        }
                    }
                }
            }
        }

        if (currentSong != null) {
            DynamicIsland(
                modifier = Modifier.align(Alignment.BottomCenter),
                song = currentSong!!,
                player = player,
                repository = repository,
                isExpanded = isExpanded,
                accentColor = animatedDominantColor,
                onToggleExpand = { isExpanded = !isExpanded }
            )
        }
        
        if (showSettings) {
             SettingsDialog(
                 onDismiss = { showSettings = false }, 
                 player = player, 
                 accentColor = animatedDominantColor
             )
        }
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit, player: AudioPlayer, accentColor: Color) {
    var disableNormalization by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundColor,
        title = {
            Text("SETTINGS", color = accentColor, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Disable Normalization", color = Color.White, fontSize = 14.sp)
                    Switch(
                        checked = disableNormalization,
                        onCheckedChange = { disableNormalization = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor,
                            checkedTrackColor = accentColor.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
                
                Button(
                    onClick = { player.openEqualizer() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OPEN EQUALIZER", color = Color.White)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = accentColor)
            }
        }
    )
}

@Composable
fun Header(onPickFolder: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Library", 
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Text(
                text = "My Music",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onOpenSettings) {
                Text("⚙", fontSize = 24.sp, color = Color.White)
            }
            IconButton(onClick = onPickFolder) {
                Text("📁", fontSize = 24.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    repository: SongRepository,
    isPlaying: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    var art by remember(song.id) { mutableStateOf(song.coverArt) }
    LaunchedEffect(song.id) {
        if (art == null) {
            art = repository.loadArt(song.uri)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF252525))
        ) {
            if (art != null) {
                Image(
                    bitmap = art!!.toImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("♪", color = Color.Gray, fontSize = 24.sp)
                }
            }
            
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    LiveWaveform(color = activeColor, barCount = 3, heightRange = 6..18)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPlaying) activeColor else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = formatTime(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray
        )
    }
}

@Composable
fun DynamicIsland(
    modifier: Modifier = Modifier,
    song: Song,
    player: AudioPlayer,
    repository: SongRepository,
    isExpanded: Boolean,
    accentColor: Color,
    onToggleExpand: () -> Unit
) {
    val isPlaying by player.isPlaying.collectAsState()
    
    var art by remember(song.id) { mutableStateOf(song.coverArt) }
    LaunchedEffect(song.id) {
        if (art == null) {
            art = repository.loadArt(song.uri)
        }
    }

    val width by animateDpAsState(
        targetValue = if (isExpanded) 350.dp else 280.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val height by animateDpAsState(
        targetValue = if (isExpanded) 600.dp else 64.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val cornerRadius by animateDpAsState(if (isExpanded) 40.dp else 32.dp)

    Surface(
        modifier = modifier
            .padding(bottom = 20.dp)
            .size(width = width, height = height)
            .shadow(
                elevation = if (isExpanded) 50.dp else 10.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = accentColor
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleExpand() },
        color = Color.Black.copy(alpha = 0.95f),
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (fadeIn(tween(400)) + slideInVertically(initialOffsetY = { it / 2 })).togetherWith(
                (fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }))
            )
        }
        ) { expanded ->
            if (expanded) {
                FullScreenPlayer(song, art, player, accentColor, onToggleExpand)
            } else {
                val currentPosition by player.currentPosition.collectAsState()
                CompactIsland(song, art, isPlaying, accentColor, 
                    onPlayPause = { if (isPlaying) player.pause() else player.resume() },
                    currentPosition = currentPosition
                )
            }
        }
    }
}

@Composable
fun FullScreenPlayer(
    song: Song,
    art: ByteArray?,
    player: AudioPlayer,
    accentColor: Color,
    onCollapse: () -> Unit
) {
    val isPlaying by player.isPlaying.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val currentPosition by player.currentPosition.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Text("▼", color = Color.White, fontSize = 20.sp)
            }
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp),
                color = Color.Gray
            )
            IconButton(onClick = { }) {
                Text("⋮", color = Color.White, fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(40.dp, RoundedCornerShape(24.dp), spotColor = accentColor)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceColor)
        ) {
            if (art != null) {
                Image(
                    bitmap = art.toImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("♫", color = Color.DarkGray, fontSize = 48.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Text(
            text = song.title,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = accentColor,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = if (song.duration > 0) currentPosition.toFloat() / song.duration else 0f,
            onValueChange = { player.seekTo((it * song.duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.DarkGray
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Text(formatTime(song.duration), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
             IconButton(onClick = { player.toggleShuffle() }) {
                Text("🔀", color = if(isShuffle) accentColor else Color.Gray, fontSize = 20.sp)
             }
             IconButton(onClick = { player.previous() }) {
                Text("⏮", color = Color.White, fontSize = 32.sp)
             }
             IconButton(
                 onClick = { if (isPlaying) player.pause() else player.resume() },
                 modifier = Modifier.size(64.dp).background(accentColor, CircleShape)
             ) {
                Text(if (isPlaying) "⏸" else "▶", color = Color.Black, fontSize = 28.sp)
             }
             IconButton(onClick = { player.next() }) {
                Text("⏭", color = Color.White, fontSize = 32.sp)
             }
             IconButton(onClick = { player.toggleRepeat() }) {
                Text(if (repeatMode == AudioPlayer.RepeatMode.ONE) "🔂" else "🔁", color = if(repeatMode != AudioPlayer.RepeatMode.OFF) accentColor else Color.Gray, fontSize = 20.sp)
             }
        }
        
        Spacer(modifier = Modifier.weight(0.1f))
    }
}

@Composable
fun CompactIsland(
    song: Song, 
    art: ByteArray?,
    isPlaying: Boolean, 
    accentColor: Color,
    onPlayPause: () -> Unit,
    currentPosition: Long
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                if (art != null) {
                    Image(
                        bitmap = art.toImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("♪", color = Color.Gray, fontSize = 20.sp)
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(song.title, color = Color.White, fontSize = 14.sp, maxLines = 1, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            if (isPlaying) {
                LiveWaveform(color = accentColor, barCount = 4, heightRange = 6..18)
            } else {
                Text("II", color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            val progress = if (song.duration > 0) currentPosition.toFloat() / song.duration else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun LiveWaveform(color: Color, barCount: Int, heightRange: IntRange) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        repeat(barCount) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = heightRange.first.toFloat(),
                targetValue = heightRange.last.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + (index * 100), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun EmptyState(onPickFolder: () -> Unit, statusMessage: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Home, 
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your Library is Empty",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a folder to start listening",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onPickFolder,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(50.dp).fillMaxWidth(0.7f)
        ) {
            Text("Select Music Folder", fontWeight = FontWeight.Bold)
        }
        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusMessage, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ErrorBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, ErrorColor, RoundedCornerShape(12.dp))
            .background(ErrorColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
             Text("⚠️", modifier = Modifier.padding(end = 8.dp))
             Text(
                 text = message, 
                 color = ErrorColor, 
                 style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
             )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val secStr = if (seconds < 10) "0$seconds" else "$seconds"
    return "$minutes:$secStr"
}
