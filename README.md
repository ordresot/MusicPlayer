# 🎵 Void Player

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

A modern, high-performance music player built with **Kotlin Multiplatform** and **Compose Multiplatform** targeting Android and Desktop.

## ✨ Features

- 🎨 **Dynamic UI Theming**: Automatically extracts dominant colors from album covers to create gorgeous, immersive backgrounds and UI accents via asynchronous coroutine processing.
- 📱 **Interactive Player Island**: A polished mini-player overlay that mimics a "Dynamic Island", smoothly expanding into a full-screen experience with physics-based spring animations.
- 🚀 **Blazing Fast Local Library**: Instantly scans and loads thousands of local music files, skipping slow synchronous metadata extraction in favor of UI-driven lazy loading.
- ⚡ **Highly Optimized Rendering**: Defers complex recompositions for sliders and waveform animations, guaranteeing smooth 60FPS UI transitions and maximizing battery life.
- 💽 **Cross-Platform Architecture**: Clean `commonMain` UI architecture that runs seamlessly on both Android and Desktop (JVM) targets.
- 🎵 **System Overlay Service**: Control your music from anywhere on Android via a system-wide floating Compose widget.

## 📥 Download

- [**Void Player v2.0 (APK)**](./Void_Player_v2.0.apk)

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose Multiplatform
- **Build System**: Gradle 
- **Audio Engine**: Media3 `ExoPlayer` (Android)
- **Architecture**: MVI/MVVM with `StateFlow` and heavily optimized `collectAsState` boundaries.

## 🚀 Getting Started

### Prerequisites
- JDK 17+
- Android Studio Ladybug or newer
- Gradle

### Build & Run

**Android:**
```bash
./gradlew :composeApp:installDebug
```

**Desktop:**
```bash
./gradlew :composeApp:run
```

## 📂 Project Structure

- `commonMain/`: Shared UI (`App.kt`, `PlayerIsland.kt`) and platform-agnostic models.
- `androidMain/`: Android-specific implementations, including `AndroidAudioPlayer`, `OverlayService`, and the optimized `AndroidSongRepository`.
- `desktopMain/`: Desktop-specific audio implementations.

## 📄 License
This project is open-source and free to modify.
