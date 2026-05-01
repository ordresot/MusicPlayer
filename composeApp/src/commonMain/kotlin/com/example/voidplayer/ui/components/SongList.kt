package com.example.voidplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voidplayer.data.SongRepository
import com.example.voidplayer.model.Song
import com.example.voidplayer.toImageBitmap
import com.example.voidplayer.ui.theme.PrimaryText
import com.example.voidplayer.ui.theme.SecondaryText

@Composable
fun SongItem(
    song: Song,
    repository: SongRepository,
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    // Memory Management Optimization: Don't just keep byte array, but this is simple for now. 
    // Ideally repository caches ImageBitmaps.
    var bitmap by remember(song.id) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(com.example.voidplayer.utils.ImageCache.get(song.id.toString())) }
    LaunchedEffect(song.id) {
        if (bitmap == null) {
            val artBytes = repository.loadArt(song.uri)
            if (artBytes != null) {
                val imgBitmap = artBytes.toImageBitmap()
                com.example.voidplayer.utils.ImageCache.put(song.id.toString(), imgBitmap)
                bitmap = imgBitmap
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray.copy(alpha = 0.5f))
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
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
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    LiveWaveform(color = accentColor, barCount = 3, heightRange = 6..18)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (isPlaying) accentColor else PrimaryText,
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
        
        IconButton(onClick = { /* TODO Options menu */ }) {
             Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = SecondaryText)
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
            color = PrimaryText,
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
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryText, contentColor = Color.Black),
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
