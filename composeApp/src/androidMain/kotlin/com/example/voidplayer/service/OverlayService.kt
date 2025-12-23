package com.example.voidplayer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voidplayer.VoidPlayerApp
import com.example.voidplayer.LiveWaveform
import com.example.voidplayer.toImageBitmap
import com.example.voidplayer.formatTime
import com.example.voidplayer.getDominantColor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.example.voidplayer.model.Song

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            showOverlay()
        }
        return START_STICKY
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 60 

        val composeView = ComposeView(this).apply {
            setContent {
                val player = VoidPlayerApp.instance.player
                val repository = VoidPlayerApp.instance.repository
                val currentSong by player.currentSong.collectAsState()
                val isPlaying by player.isPlaying.collectAsState()
                val currentPosition by player.currentPosition.collectAsState()
                
                var isExpanded by remember { mutableStateOf(false) }
                
                // Art loading logic for overlay
                var art by remember(currentSong?.id) { mutableStateOf(currentSong?.coverArt) }
                LaunchedEffect(currentSong?.id) {
                    currentSong?.let { song ->
                        if (song.coverArt == null) {
                            art = repository.loadArt(song.uri)
                        } else {
                            art = song.coverArt
                        }
                    }
                }

                val accentColor = art?.let { getDominantColor(it) } ?: Color(0xFF00F0FF)

                LaunchedEffect(isExpanded) {
                    params.width = if (isExpanded) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT
                    windowManager.updateViewLayout(this@apply, params)
                }

                currentSong?.let { song ->
                    val width by animateDpAsState(if (isExpanded) 380.dp else 120.dp, animationSpec = spring(stiffness = Spring.StiffnessLow))
                    val height by animateDpAsState(if (isExpanded) 180.dp else 36.dp, animationSpec = spring(stiffness = Spring.StiffnessLow))
                    val cornerRadius by animateDpAsState(if (isExpanded) 28.dp else 18.dp)

                    Box(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            color = Color.Black,
                            modifier = Modifier
                                .size(width = width, height = height)
                                .shadow(elevation = 12.dp, shape = RoundedCornerShape(cornerRadius), spotColor = accentColor.copy(alpha=0.5f))
                                .clip(RoundedCornerShape(cornerRadius))
                                .clickable { isExpanded = !isExpanded },
                            shape = RoundedCornerShape(cornerRadius),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            AnimatedContent(
                                targetState = isExpanded,
                                transitionSpec = { 
                                    (fadeIn(tween(200)) + slideInVertically { it / 4 }) togetherWith 
                                    (fadeOut(tween(200)) + slideOutVertically { it / 4 })
                                }
                            ) { expanded ->
                                if (expanded) {
                                    ExpandedIsland(song, art, isPlaying, currentPosition, accentColor, 
                                        onPrev = { player.previous() },
                                        onNext = { player.next() },
                                        onPlayPause = { if (isPlaying) player.pause() else player.resume() }
                                    )
                                } else {
                                    CompactIsland(song, art, isPlaying, accentColor)
                                }
                            }
                        }
                    }
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        })
        composeView.setViewTreeSavedStateRegistryOwner(this)

        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    @Composable
    private fun CompactIsland(song: Song, art: ByteArray?, isPlaying: Boolean, accentColor: Color) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                art?.let {
                    Image(bitmap = it.toImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isPlaying) {
                LiveWaveform(color = accentColor, barCount = 4, heightRange = 6..14)
            } else {
                Text("II", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    private fun ExpandedIsland(
        song: Song, 
        art: ByteArray?,
        isPlaying: Boolean, 
        currentPosition: Long, 
        accentColor: Color,
        onPrev: () -> Unit,
        onNext: () -> Unit,
        onPlayPause: () -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                    art?.let {
                        Image(bitmap = it.toImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                }
                LiveWaveform(color = accentColor, barCount = 5, heightRange = 6..20)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column {
                LinearProgressIndicator(
                    progress = { if (song.duration > 0) currentPosition.toFloat() / song.duration else 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(currentPosition), color = Color.Gray, fontSize = 10.sp)
                    Text(formatTime(song.duration), color = Color.Gray, fontSize = 10.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceEvenly, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) { 
                    Text("⏮", color = Color.White, fontSize = 24.sp) 
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(10.dp, CircleShape, spotColor = accentColor)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶", 
                        color = Color.Black, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = onNext) { 
                    Text("⏭", color = Color.White, fontSize = 24.sp) 
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { 
            try {
                windowManager.removeView(it)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
