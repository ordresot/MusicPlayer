package com.example.voidplayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voidplayer.data.SongRepository
import com.example.voidplayer.model.Song
import com.example.voidplayer.player.AudioPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search

// --- Glassmorphism Theme Colors ---
val GlassBackground = Color(0xFF0F0F0F)
val GlassSurface = Color.White.copy(alpha = 0.08f)
val GlassBorder = Color.White.copy(alpha = 0.15f)
val PrimaryText = Color.White
val SecondaryText = Color.White.copy(alpha = 0.6f)
val AccentGradient = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)) // Purple-ish

val ModernScheme = darkColorScheme(
    primary = Color.White,
    background = GlassBackground,
    surface = Color(0xFF1E1E1E),
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
    MaterialTheme(colorScheme = ModernScheme) {
        Box(modifier = Modifier.fillMaxSize().background(GlassBackground)) {
            // Enhanced Background Gradient
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1A2E), Color(0xFF000000))
                    )
                )
                // Ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4A00E0).copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.1f),
                        radius = size.width * 0.6f
                    )
                )
            }
            
            MainContent(repository, player, pickedFolderUri, statusMessage, onPickFolder)
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
    var searchQuery by remember { mutableStateOf("") }
    val currentSong by player.currentSong.collectAsState()
    val error by player.error.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }


    LaunchedEffect(pickedFolderUri.value) {
        try {
            isLoading = true
            val loadedSongs = if (pickedFolderUri.value != null) {
                repository.loadFromFolder(pickedFolderUri.value!!)
            } else {
                repository.getSongs()
            }
            songs = loadedSongs
            if (player.currentSong.value == null && loadedSongs.isNotEmpty()) {
                player.setPlaylist(loadedSongs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }
    
    // Search Filtering
    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.artist.contains(searchQuery, ignoreCase = true) 
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                onPickFolder = onPickFolder, 
                onOpenSettings = { showSettings = true },
                searchQuery = searchQuery,
                onSearchCb = { searchQuery = it }
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (songs.isEmpty()) {
                    EmptyState(onPickFolder, statusMessage)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 160.dp, top = 8.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            val isPlaying = song.id == currentSong?.id
                            GlassSongItem(
                                song = song,
                                repository = repository,
                                isPlaying = isPlaying,
                                onClick = { player.play(song) }
                            )
                        }
                    }
                }
            }
        }

        if (currentSong != null) {
            DynamicGlassIsland(
                modifier = Modifier.align(Alignment.BottomCenter),
                song = currentSong!!,
                player = player,
                repository = repository,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded }
            )
        }
        
        if (showSettings) {
             SettingsDialog(
                 onDismiss = { showSettings = false }, 
                 player = player,
                 accentColor = Color.White
             )
        }
    }
}

@Composable
fun Header(
    onPickFolder: () -> Unit, 
    onOpenSettings: () -> Unit,
    searchQuery: String,
    onSearchCb: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
             if (isSearchActive) {
                 TextField(
                     value = searchQuery,
                     onValueChange = onSearchCb,
                     placeholder = { Text("Search...", color = SecondaryText) },
                     modifier = Modifier
                         .weight(1f)
                         .height(56.dp)
                         .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                         .background(GlassSurface, RoundedCornerShape(16.dp)),
                     colors = TextFieldDefaults.colors(
                         focusedContainerColor = Color.Transparent,
                         unfocusedContainerColor = Color.Transparent,
                         focusedTextColor = PrimaryText,
                         unfocusedTextColor = PrimaryText,
                         focusedIndicatorColor = Color.Transparent,
                         unfocusedIndicatorColor = Color.Transparent
                     ),
                     singleLine = true,
                     trailingIcon = {
                         IconButton(onClick = { 
                             isSearchActive = false 
                             onSearchCb("")
                         }) {
                             Icon(Icons.Filled.Close, null, tint = SecondaryText)
                         }
                     }
                 )
             } else {
                 Column(modifier = Modifier.weight(1f)) {
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
                        color = SecondaryText
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Text("⚙", fontSize = 24.sp, color = Color.White)
                    }
                    IconButton(onClick = onPickFolder) {
                        Text("📁", fontSize = 24.sp, color = Color.White)
                    }
                }
             }
        }
    }
}

@Composable
fun GlassSongItem(
    song: Song,
    repository: SongRepository,
    isPlaying: Boolean,
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPlaying) Color.White.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray.copy(alpha = 0.5f))
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
                    Text("♪", color = SecondaryText, fontSize = 20.sp)
                }
            }
            
            if (isPlaying) {
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    LiveWaveform(color = Color.White, barCount = 3, heightRange = 6..18)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DynamicGlassIsland(
    modifier: Modifier = Modifier,
    song: Song,
    player: AudioPlayer,
    repository: SongRepository,
    isExpanded: Boolean,
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
        targetValue = if (isExpanded) 350.dp else 300.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val height by animateDpAsState(
        targetValue = if (isExpanded) 650.dp else 70.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val cornerRadius by animateDpAsState(if (isExpanded) 40.dp else 35.dp)

    Box(
        modifier = modifier
            .padding(bottom = 20.dp)
            .size(width = width, height = height)
            .shadow(20.dp, RoundedCornerShape(cornerRadius), spotColor = Color.Black)
            .clip(RoundedCornerShape(cornerRadius))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleExpand() }
    ) {
        // Blur background simulation
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.6f))
        }

        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (fadeIn(tween(400))).togetherWith(fadeOut(tween(200)))
            }
        ) { expanded ->
            if (expanded) {
                FullScreenPlayer(song, art, player, Color.White, onToggleExpand)
            } else {
                val currentPosition by player.currentPosition.collectAsState()
                CompactIsland(song, art, isPlaying, Color.White, 
                    onPlayPause = { if (isPlaying) player.pause() else player.resume() },
                    currentPosition = currentPosition
                )
            }
        }
        
    }
}

// Re-using exiting components but ensuring they fit the glass theme
// Copied and adapted helper components

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
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
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
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(song.title, color = Color.White, fontSize = 15.sp, maxLines = 1, fontWeight = FontWeight.SemiBold, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            IconButton(onClick = onPlayPause) {
                Text(if (isPlaying) "II" else "▶", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                    .background(Color.White)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                color = SecondaryText
            )
            IconButton(onClick = { }) {
                Text("⋮", color = Color.White, fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Box(
            modifier = Modifier
                .size(260.dp)
                .shadow(40.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF222222))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
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
                    Text("♫", color = Color.DarkGray, fontSize = 64.sp)
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
            color = SecondaryText,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Control Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = if (song.duration > 0) currentPosition.toFloat() / song.duration else 0f,
                onValueChange = { player.seekTo((it * song.duration).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(3.dp),
                         colors = SliderDefaults.colors(
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                },
                thumb = {
                    Box(modifier = Modifier.size(12.dp).background(Color.White, CircleShape))
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentPosition), color = SecondaryText, fontSize = 12.sp)
                Text(formatTime(song.duration), color = SecondaryText, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 IconButton(onClick = { player.toggleShuffle() }) {
                    Text("🔀", color = if(isShuffle) Color.White else Color.White.copy(alpha = 0.3f), fontSize = 20.sp)
                 }
                 
                 IconButton(onClick = { player.previous() }) {
                    Text("⏮", color = Color.White, fontSize = 32.sp)
                 }

                 Box(
                     modifier = Modifier
                         .size(72.dp)
                         .background(Color.White, CircleShape)
                         .clickable { if (isPlaying) player.pause() else player.resume() },
                     contentAlignment = Alignment.Center
                 ) {
                    Text(
                        if (isPlaying) "II" else "▶", 
                        color = Color.Black, 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Black
                    )
                 }

                 IconButton(onClick = { player.next() }) {
                    Text("⏭", color = Color.White, fontSize = 32.sp)
                 }

                 IconButton(onClick = { player.toggleRepeat() }) {
                    val icon = when(repeatMode) {
                        AudioPlayer.RepeatMode.ONE -> "🔂"
                        else -> "🔁"
                    }
                    Text(icon, color = if(repeatMode != AudioPlayer.RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.3f), fontSize = 20.sp)
                 }
            }
        }
        
        Spacer(modifier = Modifier.weight(0.1f))
    }
}

// Helpers
fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
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
fun ErrorBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFFCF6679), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(message, color = Color.White)
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
            tint = SecondaryText,
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
            color = SecondaryText,
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
            Text(statusMessage, color = SecondaryText, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit, player: AudioPlayer, accentColor: Color) {
    var showEqualizer by remember { mutableStateOf(false) }
    
    if (showEqualizer) {
        EqualizerDialog(onDismiss = { showEqualizer = false }, player = player, accentColor = accentColor)
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text("SETTINGS", color = accentColor, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { showEqualizer = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("EQUALIZER", color = Color.White)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio Normalization", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = true,
                            onCheckedChange = { },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
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
}

@Composable
fun EqualizerDialog(onDismiss: () -> Unit, player: AudioPlayer, accentColor: Color) {
    val bands by player.equalizerBands.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Text("EQUALIZER", color = accentColor, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (bands.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Start music to use equalizer", color = Color.Gray)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        bands.forEachIndexed { index, band ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(60.dp)
                            ) {
                                Text(
                                    text = "${band.level / 100}dB", 
                                    color = Color.Gray, 
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = band.level.toFloat(),
                                    onValueChange = { player.setEqualizerBandLevel(index, it.toInt()) },
                                    valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                                    modifier = Modifier.height(180.dp).graphicsLayer {
                                        rotationZ = -90f
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if(band.frequency >= 1000) "${band.frequency/1000}k" else "${band.frequency}", 
                                    color = Color.White, 
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = accentColor)
            }
        }
    )
}
