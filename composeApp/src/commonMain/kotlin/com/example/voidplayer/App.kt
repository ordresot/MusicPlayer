package com.example.voidplayer

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.voidplayer.data.SongRepository
import com.example.voidplayer.model.Song
import com.example.voidplayer.player.AudioPlayer
import com.example.voidplayer.ui.theme.*
import com.example.voidplayer.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun App(
    repository: SongRepository,
    player: AudioPlayer,
    pickedFolderUri: State<String?> = mutableStateOf(null),
    statusMessage: String = "",
    onPickFolder: () -> Unit = {}
) {
    val currentSong by player.currentSong.collectAsState()
    
    // Manage dynamic theme color
    var accentColor by remember { mutableStateOf(DefaultAccent) }
    var currentArt by remember { mutableStateOf<ByteArray?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(currentSong?.id) {
        currentSong?.let { song ->
            val cachedColor = com.example.voidplayer.utils.ColorCache.get(song.id.toString())
            if (cachedColor != null) {
                accentColor = cachedColor
                currentArt = null
            } else {
                currentArt = repository.loadArt(song.uri)
            }
        } ?: run {
            currentArt = null
            accentColor = DefaultAccent
        }
    }
    
    if (currentArt != null && currentSong != null) {
        // getDominantColor is a composable that returns a Color
        val extractedColor = getDominantColor(currentArt!!)
        LaunchedEffect(extractedColor) {
             if (extractedColor != Color.Unspecified) {
                 com.example.voidplayer.utils.ColorCache.put(currentSong!!.id.toString(), extractedColor)
                 accentColor = extractedColor
             }
        }
    }

    MaterialTheme(colorScheme = ModernScheme.copy(primary = accentColor)) {
        Box(modifier = Modifier.fillMaxSize().background(SurfaceBackground)) {
            // Enhanced Background Dynamic Gradient
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.15f), 
                            SurfaceBackground
                        )
                    )
                )
                // Ambient glow in top right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f),
                        radius = size.width * 0.7f
                    )
                )
            }
            
            MainContent(repository, player, pickedFolderUri, statusMessage, onPickFolder, accentColor)
        }
    }
}

@Composable
fun MainContent(
    repository: SongRepository,
    player: AudioPlayer,
    pickedFolderUri: State<String?>,
    statusMessage: String,
    onPickFolder: () -> Unit,
    accentColor: Color
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
                        CircularProgressIndicator(color = accentColor)
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
                            SongItem(
                                song = song,
                                repository = repository,
                                isPlaying = isPlaying,
                                accentColor = accentColor,
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
                accentColor = accentColor,
                onToggleExpand = { isExpanded = !isExpanded }
            )
        }
        
        if (showSettings) {
             AudioSettingsScreen(
                 onDismiss = { showSettings = false }, 
                 player = player,
                 accentColor = accentColor
             )
        }
    }
}
