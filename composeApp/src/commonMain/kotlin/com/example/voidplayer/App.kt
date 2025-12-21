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
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke

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
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedCyberBackground()
                MainContent(repository, player, pickedFolderUri, statusMessage, onPickFolder)
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
            animation = tween(40000, easing = LinearEasing),
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
        
        val gridSpacing = 50.dp.toPx()
        for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
            drawLine(
                color = NeonCyan.copy(alpha = 0.05f),
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                strokeWidth = 1f
            )
        }
        for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
            drawLine(
                color = NeonCyan.copy(alpha = 0.05f),
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 1f
            )
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

    val dominantColor = currentSong?.coverArt?.let { getDominantColor(it) } ?: NeonCyan
    val animatedDominantColor by animateColorAsState(dominantColor, animationSpec = tween(1000))

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
            Header(onPickFolder, animatedDominantColor)

            AnimatedVisibility(
                visible = error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                error?.let { ErrorBanner(it) }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (songs.isEmpty()) {
                    EmptyState(onPickFolder, statusMessage)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 140.dp, top = 10.dp)
                    ) {
                        items(songs, key = { it.id }) { song ->
                            val isPlaying = song.id == currentSong?.id
                            CyberListItem(
                                song = song,
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
                isExpanded = isExpanded,
                accentColor = animatedDominantColor,
                onToggleExpand = { isExpanded = !isExpanded }
            )
        }
    }
}

@Composable
fun Header(onPickFolder: () -> Unit, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "VOID PLAYER",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = accentColor
            )
            Text(
                text = "// SYSTEM ACTIVE",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = NeonMagenta.copy(alpha = 0.7f)
            )
        }
        
        IconButton(
            onClick = onPickFolder,
            modifier = Modifier
                .clip(CutCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.1f))
                .border(1.dp, accentColor.copy(alpha = 0.3f), CutCornerShape(8.dp))
        ) {
            Text("📂", fontSize = 20.sp)
        }
    }
}

@Composable
fun CyberListItem(
    song: Song,
    isPlaying: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = SineEaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isPlaying) activeColor.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(400)
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) activeColor.copy(alpha = glowAlpha) else Color.Transparent,
        animationSpec = tween(400)
    )
    
    val scale by animateFloatAsState(if (isPlaying) 1.02f else 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CutCornerShape(bottomEnd = 16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, CutCornerShape(bottomEnd = 16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
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
                Text("♫", color = Color.LightGray, fontSize = 24.sp)
            }
            
            if (isPlaying) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                LiveWaveform(color = activeColor, barCount = 3, heightRange = 8..24)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isPlaying) activeColor else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
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
    player: AudioPlayer,
    isExpanded: Boolean,
    accentColor: Color,
    onToggleExpand: () -> Unit
) {
    val isPlaying by player.isPlaying.collectAsState()
    
    val width by animateDpAsState(
        targetValue = if (isExpanded) 400.dp else 280.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val height by animateDpAsState(
        targetValue = if (isExpanded) 800.dp else 64.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val bottomPadding by animateDpAsState(if (isExpanded) 0.dp else 32.dp)
    val cornerRadius by animateDpAsState(if (isExpanded) 0.dp else 32.dp)

    Box(
        modifier = modifier
            .padding(bottom = bottomPadding)
            .size(width = width, height = height)
            .shadow(if (isExpanded) 0.dp else 24.dp, RoundedCornerShape(cornerRadius), spotColor = accentColor)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black)
            .then(if (!isExpanded) Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(cornerRadius)) else Modifier)
            .clickable { onToggleExpand() }
            .padding(if (isExpanded) 0.dp else 12.dp)
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (fadeIn(tween(400, delayMillis = 100)) + slideInVertically(initialOffsetY = { it / 2 })) togetherWith 
                (fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }))
            }
        ) { expanded ->
            if (expanded) {
                FullScreenPlayer(song, player, accentColor, onToggleExpand)
            } else {
                val currentPosition by player.currentPosition.collectAsState()
                CompactIsland(song, isPlaying, accentColor, 
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
    player: AudioPlayer,
    accentColor: Color,
    onCollapse: () -> Unit
) {
    val isPlaying by player.isPlaying.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val currentPosition by player.currentPosition.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        
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
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp
                    ),
                    color = Color.Gray
                )
                IconButton(onClick = { }) {
                    Text("⋮", color = Color.White, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            val artSize = if (screenHeight < 600.dp) (screenWidth * 0.5f) else (screenWidth * 0.8f)
            Box(
                modifier = Modifier
                    .size(artSize)
                    .shadow(40.dp, RoundedCornerShape(24.dp), spotColor = accentColor)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                if (song.coverArt != null) {
                    Image(
                        bitmap = song.coverArt.toImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("♫", color = Color.DarkGray, fontSize = (artSize.value / 3).sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = accentColor,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { }) {
                        Text("♡", color = Color.Gray, fontSize = 24.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column {
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatTime(currentPosition), color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(formatTime(song.duration), color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CyberActionButton(
                    icon = "🔀",
                    isActive = isShuffle,
                    accentColor = accentColor,
                    onClick = { player.toggleShuffle() }
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { player.previous() }) {
                        Text("⏮", color = Color.White, fontSize = 32.sp, modifier = Modifier.glow(Color.White))
                    }
                    
                    GlowPlayButton(
                        isPlaying = isPlaying,
                        color = accentColor,
                        onClick = { if (isPlaying) player.pause() else player.resume() }
                    )
                    
                    IconButton(onClick = { player.next() }) {
                        Text("⏭", color = Color.White, fontSize = 32.sp, modifier = Modifier.glow(Color.White))
                    }
                }

                CyberActionButton(
                    icon = if (repeatMode == AudioPlayer.RepeatMode.ONE) "🔂" else "🔁",
                    isActive = repeatMode != AudioPlayer.RepeatMode.OFF,
                    accentColor = accentColor,
                    onClick = { player.toggleRepeat() }
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}

@Composable
fun CyberActionButton(
    icon: String,
    isActive: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isActive) 1.2f else 1f)
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CutCornerShape(12.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.1f) else Color.Transparent)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    if (isActive) listOf(accentColor, Color.Transparent) else listOf(Color.Gray.copy(alpha = 0.3f), Color.Transparent)
                ),
                shape = CutCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            color = if (isActive) accentColor else Color.White.copy(alpha = 0.7f),
            fontSize = 20.sp,
            modifier = Modifier.then(if (isActive) Modifier.glow(accentColor) else Modifier)
        )
    }
}

@Composable
fun GlowPlayButton(
    isPlaying: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f)

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(if (isPressed) 10.dp else 30.dp, CircleShape, spotColor = color)
            .background(Color.Black, CircleShape)
            .border(2.dp, color.copy(alpha = 0.5f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = size.minDimension / 2 + 8.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        Text(
            text = if (isPlaying) "⏸" else "▶",
            color = color,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun Modifier.glow(color: Color): Modifier = this.drawBehind {
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = size.maxDimension / 1.5f,
        center = center
    )
}

@Composable
fun CompactIsland(
    song: Song, 
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
                if (song.coverArt != null) {
                    Image(
                        bitmap = song.coverArt.toImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = accentColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onPlayPause) {
                Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 20.sp)
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
        Text(
            text = "NO DATA DETECTED",
            color = Color.Gray,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onPickFolder,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack),
            shape = CutCornerShape(8.dp),
            modifier = Modifier.height(56.dp).fillMaxWidth()
        ) {
            Text("INITIALIZE DISK SCAN", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusMessage, color = NeonMagenta.copy(alpha = 0.6f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
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
        Text(text = "CRITICAL ERROR: $message", color = Color.Red, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

fun ByteArray.toImageBitmap(): ImageBitmap {
    return android.graphics.BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap()
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${if (seconds < 10) \"0\" else \"\"}${seconds}"
}

val SineEaseInOut = Easing { fraction ->
    (-(kotlin.math.cos(kotlin.math.PI * fraction) - 1f) / 2f).toFloat()
}
