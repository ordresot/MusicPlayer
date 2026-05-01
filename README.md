# Void Player 🎵

A modern, professional music player built with **Kotlin Multiplatform** and **Compose Multiplatform** for Android and Desktop.

## ✨ Features

- **Dynamic Gradient Theme**: Extracts the dominant color from the currently playing track's album cover and dynamically applies it to the app's background and player controls.
- **Modern Solid Dark Mode**: A sleek, elevated dark surface design with professional Material vector icons that feels fast and looks premium.
- **Cross-Platform**: Runs natively on Android and Desktop (JVM).
- **Redesigned Player Island**: Interactive mini-player that expands into a full-screen experience with a large album cover display and smooth spring animations.
- **Visualizer**: Real-time mock waveform visualization for an immersive playback experience.
- **Smart Image Caching**: Highly efficient LRU (Least Recently Used) cache for album art for smoother scrolling and reduced memory bloat.
- **Local Library**: Efficiently scans and plays music from your local device folders.
- **Seamless Playback Sync**: High-frequency progress updates for real-time track synchronization.

## 📥 Download

- [**Void Player v2.0 (APK)**](./Void_Player_v2.0.apk)

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose Multiplatform
- **Build System**: Gradle
- **Architecture**: MVVM-like state management with `StateFlow` and `collectAsState`.
- **Image Loading**: Platform-specific implementations using `BitmapFactory` (Android) and `Skia` (Desktop).

## 🚀 Getting Started

### Prerequisites
- JDK 17 or higher
- Android Studio (for Android build)
- Gradle

### Building the Project

**Android:**
```bash
./gradlew :composeApp:installDebug
```

**Desktop:**
```bash
./gradlew :composeApp:run
```

## 📂 Project Structure

- `commonMain`: Shared UI and logic code (App.kt, generic repositories).
- `androidMain`: Android-specific implementations (AudioPlayer, SongRepository, ImageUtils).
- `desktopMain`: Desktop-specific implementations (AudioPlayer, ImageUtils).

## 📄 License

This project is open-source.
