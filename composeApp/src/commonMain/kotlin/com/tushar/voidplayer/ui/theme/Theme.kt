package com.tushar.voidplayer.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// --- Solid Modern Dark Theme Colors ---
val SurfaceBackground = Color(0xFF121212)
val SurfaceElevated = Color(0xFF1E1E1E)
val SurfaceVariant = Color(0xFF2C2C2C)
val PrimaryText = Color.White
val SecondaryText = Color(0xFFAAAAAA)
val DefaultAccent = Color(0xFF1DB954) // A nice green default, will be overridden dynamically
val DividerColor = Color.White.copy(alpha = 0.1f)

val ModernScheme = darkColorScheme(
    primary = DefaultAccent,
    background = SurfaceBackground,
    surface = SurfaceElevated,
    onSurface = PrimaryText,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = SecondaryText
)
