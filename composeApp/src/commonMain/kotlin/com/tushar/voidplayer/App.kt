package com.tushar.voidplayer

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
import com.tushar.voidplayer.data.SongRepository
import com.tushar.voidplayer.model.Song
import com.tushar.voidplayer.player.AudioPlayer
import com.tushar.voidplayer.ui.theme.*
import com.tushar.voidplayer.ui.components.*
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// Sort order — enum eliminates magic strings and enables exhaustive when
// ------------------------------------------------------------------

enum class SortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    DURATION("Duration")
}

@Composable
fun App(
    repository: SongRepository,
    player: AudioPlayer,
    pickedFolderUri: State<String?> = mutableStateOf(null),
    statusMessage: String = "",
    onPickFolder: () -> Unit = {}
) {
    val currentSong by player.currentSong.collectAsState()

    // Dynamic theme accent color
    var accentColor by remember { mutableStateOf(DefaultAccent) }
    var currentArt by remember { mutableStateOf<ByteArray?>(null) }

    // Step 1: when the song changes, load its art bytes (or read from color cache)
    LaunchedEffect(currentSong?.id) {
        val song = currentSong
        if (song == null) {
            currentArt = null
            accentColor = DefaultAccent
            return@LaunchedEffect
        }
        val cachedColor = com.tushar.voidplayer.utils.ColorCache.get(song.id.toString())
        if (cachedColor != null) {
            accentColor = cachedColor
            currentArt = null   // No need to decode art again
        } else {
            currentArt = repository.loadArt(song.uri)
        }
    }

    // Step 2: when art bytes change, extract the dominant color.
    // getDominantColor is a composable; calling it here (top of composition)
    // is safe and efficient — it only re-runs when currentArt changes.
    val extractedColor = currentArt?.let { getDominantColor(it) }
    LaunchedEffect(extractedColor) {
        if (extractedColor != null && extractedColor != Color.Unspecified) {
            currentSong?.let { song ->
                com.tushar.voidplayer.utils.ColorCache.put(song.id.toString(), extractedColor)
            }
            accentColor = extractedColor
        }
    }

    MaterialTheme(colorScheme = ModernScheme.copy(primary = accentColor)) {
        Box(modifier = Modifier.fillMaxSize().background(SurfaceBackground)) {
            // Animated background gradient
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.15f), SurfaceBackground)
                    )
                )
                // Ambient glow in top-right corner
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

    // Load songs whenever the picked folder changes
    LaunchedEffect(pickedFolderUri.value) {
        isLoading = true
        try {
            val loaded = if (pickedFolderUri.value != null) {
                repository.loadFromFolder(pickedFolderUri.value!!)
            } else {
                repository.getSongs()
            }
            songs = loaded
            // Set the playlist on first load so ExoPlayer knows the full queue
            if (player.currentSong.value == null && loaded.isNotEmpty()) {
                player.setPlaylist(loaded)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // When a song's favorite status is toggled, update it in the songs list
    // so the UI reflects the change immediately in both tabs.
    fun onToggleFavorite(updatedSong: Song) {
        songs = songs.map { if (it.id == updatedSong.id) updatedSong else it }
    }

    var selectedTab by remember { mutableStateOf(0) }       // 0 = All, 1 = Favorites
    var sortOrder by remember { mutableStateOf(SortOrder.TITLE) }

    // Derive the displayed list. `remember` with keys avoids recomputing on
    // every composition frame; only recalculates when inputs change.
    val filteredSongs by remember(songs, searchQuery, selectedTab, sortOrder) {
        derivedStateOf {
            var list = if (selectedTab == 1) songs.filter { it.isFavorite } else songs
            if (searchQuery.isNotBlank()) {
                list = list.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
                }
            }
            when (sortOrder) {
                SortOrder.ARTIST   -> list.sortedBy { it.artist.lowercase() }
                SortOrder.DURATION -> list.sortedByDescending { it.duration }
                SortOrder.TITLE    -> list.sortedBy { it.title.lowercase() }
            }
        }
    }

    val favoriteCount by remember(songs) { derivedStateOf { songs.count { it.isFavorite } } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                onPickFolder = onPickFolder,
                onOpenSettings = { showSettings = true },
                searchQuery = searchQuery,
                onSearchCb = { searchQuery = it }
            )

            // --- Category tabs & sort bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("All Songs (${songs.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Favorites ($favoriteCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { sortExpanded = true }) {
                        Text(
                            "Sort: ${sortOrder.label}",
                            color = accentColor,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                onClick = { sortOrder = order; sortExpanded = false }
                            )
                        }
                    }
                }
            }

            // --- Error banner ---
            AnimatedVisibility(
                visible = error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                error?.let { ErrorBanner(it) }
            }

            // --- Content area ---
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }
                    songs.isEmpty() -> EmptyState(onPickFolder, statusMessage)
                    filteredSongs.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (selectedTab == 1) "No favorite songs yet\nTap ♥ on any song to add it"
                                       else "No songs match \"$searchQuery\"",
                                color = SecondaryText,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    else -> {
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
                                    onToggleFavorite = ::onToggleFavorite,
                                    onClick = { player.play(song) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Player island ---
        if (currentSong != null) {
            DynamicIsland(
                modifier = Modifier.align(Alignment.BottomCenter),
                song = currentSong!!,
                player = player,
                repository = repository,
                isExpanded = isExpanded,
                accentColor = accentColor,
                onToggleExpand = { isExpanded = !isExpanded },
                onToggleFavorite = ::onToggleFavorite
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
