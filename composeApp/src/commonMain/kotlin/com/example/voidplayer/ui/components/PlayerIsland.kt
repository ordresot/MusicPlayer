package com.example.voidplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voidplayer.data.SongRepository
import com.example.voidplayer.model.Song
import com.example.voidplayer.player.AudioPlayer
import com.example.voidplayer.toImageBitmap
import com.example.voidplayer.ui.theme.PrimaryText
import com.example.voidplayer.ui.theme.SecondaryText
import com.example.voidplayer.ui.theme.SurfaceElevated

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

    val bitmap = com.example.voidplayer.utils.rememberSongImage(song, repository)

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
            .shadow(24.dp, RoundedCornerShape(cornerRadius), spotColor = accentColor.copy(alpha=0.5f))
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceElevated)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleExpand() }
    ) {
        // Dynamic Gradient Background for the Island
        Box(
            modifier = Modifier.fillMaxSize()
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
            transitionSpec = {
                (fadeIn(tween(400))).togetherWith(fadeOut(tween(200)))
            }
        ) { expanded ->
            if (expanded) {
                FullScreenPlayer(song, bitmap, player, accentColor, onToggleExpand)
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
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
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
                    contentDescription = null,
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
    accentColor: Color,
    onCollapse: () -> Unit
) {
    val isPlaying by player.isPlaying.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()

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
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse", tint = PrimaryText, modifier = Modifier.size(32.dp))
            }
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp),
                color = SecondaryText
            )
            IconButton(onClick = { }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = PrimaryText, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Box(
            modifier = Modifier
                .size(280.dp) // Increased size
                .shadow(40.dp, RoundedCornerShape(24.dp), spotColor = accentColor.copy(alpha=0.6f))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF222222))
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
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(80.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

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
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Control Section
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
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = PrimaryText, modifier = Modifier.size(48.dp))
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
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = { player.next() }) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = PrimaryText, modifier = Modifier.size(48.dp))
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

@Composable
fun CompactProgress(song: Song, player: AudioPlayer, accentColor: Color, modifier: Modifier = Modifier) {
    val currentPosition by player.currentPosition.collectAsState()
    Box(
        modifier = modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenProgress(song: Song, player: AudioPlayer, accentColor: Color) {
    val currentPosition by player.currentPosition.collectAsState()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = if (song.duration > 0) currentPosition.toFloat() / song.duration else 0f,
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition), color = SecondaryText, fontSize = 12.sp)
            Text(formatTime(song.duration), color = SecondaryText, fontSize = 12.sp)
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
