package com.tushar.voidplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushar.voidplayer.data.SongRepository
import com.tushar.voidplayer.model.Song
import com.tushar.voidplayer.player.AudioPlayer
import com.tushar.voidplayer.toImageBitmap
import com.tushar.voidplayer.ui.theme.PrimaryText
import com.tushar.voidplayer.ui.theme.SecondaryText
import com.tushar.voidplayer.ui.theme.SurfaceElevated
import com.tushar.voidplayer.utils.LrcLine
import com.tushar.voidplayer.utils.LrcParser

@Composable
fun DynamicIsland(
    modifier: Modifier = Modifier,
    song: Song,
    player: AudioPlayer,
    repository: SongRepository,
    isExpanded: Boolean,
    accentColor: Color,
    onToggleExpand: () -> Unit,
    onToggleFavorite: ((Song) -> Unit)? = null
) {
    val isPlaying by player.isPlaying.collectAsState()
    val bitmap = com.tushar.voidplayer.utils.rememberSongImage(song, repository)

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
            .shadow(24.dp, RoundedCornerShape(cornerRadius), spotColor = accentColor.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceElevated)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleExpand() }
    ) {
        // Dynamic gradient background for the Island
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isExpanded) 0.3f else 0.1f),
                            SurfaceElevated
                        )
                    )
                )
        )

        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = { fadeIn(tween(400)).togetherWith(fadeOut(tween(200))) }
        ) { expanded ->
            if (expanded) {
                FullScreenPlayer(
                    song = song,
                    bitmap = bitmap,
                    player = player,
                    repository = repository,
                    accentColor = accentColor,
                    onCollapse = onToggleExpand,
                    onToggleFavorite = onToggleFavorite
                )
            } else {
                CompactIsland(
                    song = song,
                    bitmap = bitmap,
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    onPlayPause = { if (isPlaying) player.pause() else player.resume() },
                    player = player
                )
            }
        }
    }
}

@Composable
fun CompactIsland(
    song: Song,
    bitmap: androidx.compose.ui.graphics.ImageBitmap?,
    isPlaying: Boolean,
    accentColor: Color,
    onPlayPause: () -> Unit,
    player: AudioPlayer
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.DarkGray)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    song.title,
                    color = PrimaryText,
                    fontSize = 15.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    color = SecondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        CompactProgress(song, player, accentColor, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    song: Song,
    bitmap: androidx.compose.ui.graphics.ImageBitmap?,
    player: AudioPlayer,
    repository: SongRepository,
    accentColor: Color,
    onCollapse: () -> Unit,
    onToggleFavorite: ((Song) -> Unit)? = null
) {
    val isPlaying by player.isPlaying.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val currentPosition by player.currentPosition.collectAsState()
    val queue by player.currentQueue.collectAsState()

    var isFav by remember(song.id, song.isFavorite) { mutableStateOf(song.isFavorite) }
    var activeViewMode by remember { mutableStateOf(0) } // 0 = Album Art, 1 = Lyrics, 2 = Queue

    var lrcLines by remember(song.id) { mutableStateOf<List<LrcLine>>(emptyList()) }
    var isLoadingLyrics by remember(song.id) { mutableStateOf(false) }

    LaunchedEffect(song.id) {
        isLoadingLyrics = true
        try {
            val lyrics = repository.loadLyrics(song.uri)
            if (lyrics != null) {
                lrcLines = LrcParser.parse(lyrics)
            } else {
                lrcLines = emptyList()
            }
        } catch (_: Throwable) {
            lrcLines = emptyList()
        } finally {
            isLoadingLyrics = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Top bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = PrimaryText,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = when (activeViewMode) {
                    1 -> "LYRICS"
                    2 -> "PLAYING QUEUE"
                    else -> "NOW PLAYING"
                },
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
                color = SecondaryText
            )
            IconButton(onClick = { activeViewMode = if (activeViewMode == 2) 0 else 2 }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue",
                    tint = if (activeViewMode == 2) accentColor else PrimaryText,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.08f))

        // --- Center View: Art / Lyrics / Queue ---
        Box(
            modifier = Modifier
                .size(280.dp)
                .shadow(40.dp, RoundedCornerShape(24.dp), spotColor = accentColor.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            when (activeViewMode) {
                1 -> {
                    // Synced Lyrics View
                    if (isLoadingLyrics) {
                        CircularProgressIndicator(color = accentColor)
                    } else if (lrcLines.isEmpty()) {
                        Text(
                            text = "♪ No synced lyrics (.lrc)\nfound for this track ♪",
                            color = SecondaryText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        val activeIndex = remember(currentPosition, lrcLines) {
                            LrcParser.getCurrentLineIndex(lrcLines, currentPosition)
                        }
                        val listState = rememberLazyListState()

                        LaunchedEffect(activeIndex) {
                            if (activeIndex >= 0 && activeIndex < lrcLines.size) {
                                listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(vertical = 40.dp)
                        ) {
                            itemsIndexed(lrcLines) { idx, line ->
                                val isCurrent = idx == activeIndex
                                Text(
                                    text = line.text,
                                    color = if (isCurrent) accentColor else Color.Gray,
                                    fontSize = if (isCurrent) 17.sp else 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable { player.seekTo(line.timestampMs) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Live Queue View
                    if (queue.isEmpty()) {
                        Text("Queue is empty", color = SecondaryText)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(queue, key = { it.id }) { item ->
                                val isItemPlaying = item.id == song.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isItemPlaying) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { player.play(item) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = if (isItemPlaying) accentColor else PrimaryText,
                                            fontSize = 13.sp,
                                            fontWeight = if (isItemPlaying) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.artist,
                                            color = SecondaryText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Album Art
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.05f))

        // --- Song info + action buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = PrimaryText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = SecondaryText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = {
                isFav = !isFav
                onToggleFavorite?.invoke(song.copy(isFavorite = isFav))
            }) {
                Icon(
                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                    tint = if (isFav) Color(0xFFFF4081) else SecondaryText,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = { activeViewMode = if (activeViewMode == 1) 0 else 1 }) {
                Icon(
                    imageVector = Icons.Filled.Subtitles,
                    contentDescription = if (activeViewMode == 1) "Hide lyrics" else "Show lyrics",
                    tint = if (activeViewMode == 1) accentColor else SecondaryText,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Controls ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FullScreenProgress(song, player, accentColor)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { player.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) accentColor else SecondaryText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { player.previous() }) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = PrimaryText,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(accentColor, CircleShape)
                        .clickable { if (isPlaying) player.pause() else player.resume() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = { player.next() }) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = PrimaryText,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(onClick = { player.toggleRepeat() }) {
                    val icon = when (repeatMode) {
                        AudioPlayer.RepeatMode.ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != AudioPlayer.RepeatMode.OFF) accentColor else SecondaryText,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))
    }
}

// ------------------------------------------------------------------
// Progress composables
// ------------------------------------------------------------------

@Composable
fun CompactProgress(
    song: Song,
    player: AudioPlayer,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val currentPosition by player.currentPosition.collectAsState()
    val progress by remember { derivedStateOf {
        if (song.duration > 0) (currentPosition.toFloat() / song.duration).coerceIn(0f, 1f) else 0f
    }}

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(accentColor)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenProgress(song: Song, player: AudioPlayer, accentColor: Color) {
    val currentPosition by player.currentPosition.collectAsState()
    val safeProgress by remember { derivedStateOf {
        if (song.duration > 0) (currentPosition.toFloat() / song.duration).coerceIn(0f, 1f) else 0f
    }}

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = safeProgress,
            onValueChange = { player.seekTo((it * song.duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            },
            thumb = {
                Box(modifier = Modifier.size(14.dp).background(accentColor, CircleShape))
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition), color = SecondaryText, fontSize = 12.sp)
            Text(formatTime(song.duration), color = SecondaryText, fontSize = 12.sp)
        }
    }
}

fun formatTime(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
