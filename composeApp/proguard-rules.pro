# R8 / ProGuard Rules for VoidPlayer

# Keep Koin
-keep class org.koin.** { *; }

# Keep ExoPlayer
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn androidx.media3.**

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep App Models for Reflection/Serialization (if needed)
-keep class com.tushar.voidplayer.model.** { *; }
