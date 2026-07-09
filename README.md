# 🎵 Void Player

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/version-2.1-blue?style=for-the-badge)

A modern, crash-hardened music player built with **Kotlin Multiplatform** and **Compose Multiplatform** targeting Android and Desktop.

## ✨ Features

- 🎨 **Dynamic UI Theming**: Automatically extracts dominant colors from album covers to create immersive backgrounds and UI accents via asynchronous coroutine processing.
- 📱 **Interactive Player Island**: A polished mini-player overlay that mimics a "Dynamic Island", smoothly expanding into a full-screen experience with physics-based spring animations.
- 🎵 **System Overlay Service**: Control your music from anywhere on Android via a system-wide floating Compose widget (requires overlay permission).
- 🚀 **Fast Local Library**: Scans local music files with lazy metadata extraction and a bounded LRU image cache for smooth scrolling.
- 🧠 **Adaptive Memory Architecture**: Queries the device's hardware memory class to dynamically scale image caches and album art resolutions. Smooth on 2GB budget phones, gorgeous on 12GB flagships.
- 🎧 **Equalizer & Normalization**: Full hardware equalizer bands and real-time multiband dynamic normalization via Android's `DynamicsProcessing` DSP (API 28+).
- 🔊 **Audio Focus & Noisy Handling**: Auto-pauses when headphones are disconnected or another app takes audio focus.
- 💽 **Cross-Platform Architecture**: Clean `commonMain` UI architecture that runs on both Android and Desktop (JVM) targets.

## 📥 Download

- [**Void Player v2.1 (APK)**](./VoidPlayer-2.1-release.apk)

> ⚠️ Must uninstall v2.0 before installing v2.1 (signing key changed).

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose Multiplatform |
| Audio Engine | Media3 ExoPlayer (Android) |
| Architecture | MVVM with `StateFlow` + `collectAsState` |
| Build System | Gradle (KMP) |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |

## 🚀 Getting Started

### Prerequisites
- Android device running Android 8.0 (API 26) or higher
- JDK 21+
- Android Studio Ladybug or newer

### Build & Run

**Android (Debug):**
```bash
./gradlew :composeApp:installDebug
```

**Android (Release APK):**
```bash
./gradlew :composeApp:assembleRelease
```

**Desktop:**
```bash
./gradlew :composeApp:run
```

## 📂 Project Structure

```
composeApp/
├── commonMain/          # Shared UI, models, interfaces
│   ├── App.kt           # Root composable
│   ├── model/Song.kt    # Data model
│   ├── player/AudioPlayer.kt     # Platform-agnostic interface
│   ├── data/SongRepository.kt   # Platform-agnostic interface
│   └── ui/components/           # All UI components
├── androidMain/         # Android implementations
│   ├── MainActivity.kt
│   ├── VoidPlayerApp.kt          # Application class (singleton)
│   ├── player/AndroidAudioPlayer.kt  # ExoPlayer wrapper
│   ├── player/PlaybackService.kt     # MediaSessionService
│   ├── service/OverlayService.kt     # System overlay
│   └── data/AndroidSongRepository.kt
└── desktopMain/         # Desktop stub implementations
```

## 🐛 Crash History & Developer Notes

See [release.md](./release.md) for the full changelog and [chat.md](./chat.md) for the complete crash audit with root causes, fixes, and prevention rules for future development.

## 📄 License
This project is open-source under the MIT License. See [LICENSE](./LICENSE) for details.
