# 🎵 Void Player

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/version-2.2-blue?style=for-the-badge)

A modern, high-performance, privacy-focused music player built with **Kotlin Multiplatform** and **Compose Multiplatform** targeting Android and Windows Desktop. 100% Free & Open Source (FOSS) with zero ads, tracking, or telemetry.

---

## ✨ Features

- 🎨 **Dynamic UI Theming**: Automatically extracts dominant colors from album covers to create immersive backgrounds and UI accents via asynchronous coroutine processing.
- ✨ **AI Smart Categorization**: Automatically analyzes your music collection metadata to group songs into intelligent mood and vibe collections (Night Vibes, High Energy, Deep Focus, Romance, Quick Hits, and Artist Spotlights).
- 📁 **Custom Playlists & Favorites**: Create and manage custom playlists and favorite tracks, fully persisted locally across app restarts.
- 📜 **Interactive Synced LRC Lyrics**: Automatic local `.lrc` file detection with real-time timestamp synchronization, active line accent highlighting, smooth auto-scrolling, and tap-to-seek.
- 📱 **Interactive Player Island**: A polished mini-player overlay that mimics a "Dynamic Island", smoothly expanding into a full-screen experience with physics-based spring animations.
- ⚡ **Playback Speed Controller**: Adjust tempo from `0.5x` to `2.0x` with native ExoPlayer pitch correction.
- 🌙 **Gentle Sleep Timer**: Automatically pause playback with optional 20-second volume fade-out so you drift off peacefully.
- 📋 **Live Queue Viewer**: View and jump between upcoming tracks in your playlist queue directly from the full-screen player.
- 🎵 **System Overlay Service**: Control your music from anywhere on Android via a floating system-wide Compose widget.
- 🧠 **Adaptive Memory Architecture**: Hardware-aware LRU image caches ensure buttery-smooth 60fps scrolling on both 2GB budget phones and 12GB flagships.
- 🎧 **Hardware Equalizer & Normalization**: Full multi-band equalizer and dynamic real-time normalization via Android's `DynamicsProcessing` DSP chip.
- 🔊 **Auto-Pause & Audio Focus**: Seamlessly pauses playback when headphones/Bluetooth disconnect or incoming calls take focus.
- 🖥️ **Desktop Native Support**: Fully functional on Windows Desktop with portable standalone `.exe` and `.msi` installers.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin (100% pure KMP) |
| **UI Framework** | Jetpack Compose Multiplatform (Material 3) |
| **Audio Engine** | Media3 ExoPlayer |
| **State Management** | MVVM with `StateFlow` + `collectAsState` |
| **Build System** | Gradle (KMP) with JDK 21 |
| **Min / Target SDK** | Android 8.0 (API 26) / Android 14 (API 34) |

---

## 🚀 Getting Started

### Build & Run

**Android (Debug):**
```bash
./gradlew :composeApp:installDebug
```

**Android (Release APK):**
```bash
./gradlew :composeApp:assembleRelease
```

**Desktop (Run locally):**
```bash
./gradlew :composeApp:run
```

**Desktop (Package Standalone .exe / .msi):**
```bash
./gradlew :composeApp:createDistributable
./gradlew :composeApp:packageMsi
```

---

## 📂 Project Structure

```
composeApp/
├── commonMain/          # Shared UI, models, utilities & interfaces
│   ├── App.kt           # Root composable & Navigation Tabs
│   ├── model/           # Song, Playlist, AiCategory
│   ├── player/          # Platform-agnostic AudioPlayer interface
│   ├── data/            # SongRepository interface
│   ├── utils/           # AiCategorizer, LrcParser, SleepTimer, ImageCache
│   └── ui/components/   # PlayerIsland, AiCategoriesScreen, PlaylistsScreen, etc.
├── androidMain/         # Android Media3 & SAF implementations
│   ├── MainActivity.kt
│   ├── VoidPlayerApp.kt
│   ├── player/AndroidAudioPlayer.kt
│   ├── service/OverlayService.kt
│   └── data/AndroidSongRepository.kt
└── desktopMain/         # Desktop JVM implementations & entrypoint
```

---

## 📄 License
This project is open-source under the **MIT License**. See [LICENSE](./LICENSE) for details.
