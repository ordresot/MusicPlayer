# VoidPlayer Release Notes

---

## What's New in Version 2.2 🚀

Welcome to **VoidPlayer 2.2**! This release introduces smart AI-driven music categorization, interactive synchronized LRC lyrics, local persistence for custom playlists and favorites, and expanded audio playback controls.

### 🌟 Highlights & New Features

1. **✨ AI Smart Categorization**
   - Automatically processes all library tracks and clusters them into intelligent mood and vibe collections:
     - 🌙 *Night Vibes & Lo-Fi* (chill, midnight, ambient)
     - 🔥 *High Energy & Workout* (fast tempo, high-drive beats)
     - 🎧 *Deep Focus & Study* (acoustic, instrumental, productivity)
     - 💖 *Romance & Heartfelt* (melodic, vocal expressions)
     - ⚡ *Quick Hits* (< 2.5 mins) & 🎼 *Extended Epics* (> 4.5 mins)
     - 🎙️ *Artist Spotlights*
   - Includes 1-tap "Play All" and tap-to-inspect category tracks.

2. **💾 Persistent Favorites & Custom Playlists**
   - Custom playlists and favorite songs are now saved to local persistent storage (`SharedPreferences`) and automatically loaded across app restarts.
   - 1-tap "Add to Playlist" directly from the song options menu (`⋮`).

3. **📜 Interactive Synced LRC Lyrics Engine**
   - Auto-detects matching `.lrc` / `.LRC` files in the song's folder.
   - Real-time timestamp sync with active line accent highlighting and smooth auto-scrolling.
   - Tap any lyric line to jump playback directly to that timestamp!

4. **⚡ Pitch-Corrected Playback Speed Control**
   - Choose between `0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, and `2.0x` speeds with native ExoPlayer pitch correction.

5. **🌙 Gentle Sleep Timer (Volume Fade-Out)**
   - Gradually reduces audio volume over the last 20 seconds before pause to prevent startling listeners awake.

6. **📋 Live Queue Viewer**
   - Toggle queue sheet in the full-screen player island to view upcoming tracks and jump between songs instantly.

7. **⚙️ Header & UI Polish**
   - Fixed header action buttons using clean Material 3 vector icons (`Settings`, `FolderOpen`, `Search`).
   - Clean 3-tab top navigation bar (`Tracks`, `AI Categories`, `Playlists`).

---

## VoidPlayer Version 2.1 Release Notes

Welcome to **VoidPlayer 2.1**! This update brings critical stability fixes, crash hardening across all components, and adaptive memory improvements to make the player run flawlessly on any device.

### 🧠 Architecture & Performance
- **Adaptive Memory Architecture**: Intelligently queries your device's exact RAM capabilities.
- **Bounded Image Cache**: A hardware-aware LRU cache for decoded album art bitmaps prevents memory exhaustion when scrolling large libraries.

### 🎧 Audio Engine
- **Proper Audio Attributes**: ExoPlayer is configured with `CONTENT_TYPE_MUSIC` and `USAGE_MEDIA` for correct audio focus behavior and best DAC routing.
- **Auto-Pause on Headphone Disconnect**: Full `AudioManager.ACTION_AUDIO_BECOMING_NOISY` integration.
- **Real-Time Dynamic Normalization**: Android's `DynamicsProcessing` DSP chip is used for multiband compression and normalization (API 28+ only).
- **Equalizer**: Full hardware equalizer bands exposed in Settings, initialized safely after audio session is ready.

### 🐛 Critical Crash Fixes
1. `ForegroundServiceStartNotAllowedException` (ANR on Android 12+) - fixed using proper startService lifecycle.
2. Native SIGSEGV crash in `libeffect` - fixed by gating DSP until non-zero audioSessionId.
3. DSP unavailable on custom ROMs - guarded with `catch(Throwable)`.
4. `SecurityException` from undeclared WAKE_LOCK - switched to `WAKE_MODE_NONE`.
5. `NullPointerException` from `BitmapFactory.decodeByteArray()` returning null - fixed with fallback bitmap and try-catch.
6. `OutOfMemoryError` in `loadArt()` - fixed with `catch(Throwable)` and safe retriever release.
7. OOM / Codec crash from aggressive ExoPlayer configuration - cleaned ExoPlayer builder.
