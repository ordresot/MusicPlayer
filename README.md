# Void Player 🎵

A modern, professional music player built with **Kotlin Multiplatform** and **Compose Multiplatform** for Android and Desktop.

## ✨ Features

- **Modern Dark UI**: A sleek, professional aesthetic with a clean dark theme and white accents.
- **Cross-Platform**: Runs natively on Android and Desktop (JVM).
- **Dynamic Island Player**: Interactive mini-player that expands into a full-screen experience with smooth spring animations.
- **Visualizer**: Real-time mock waveform visualization for an immersive playback experience.
- **Local Library**: Efficiently scans and plays music from your local device folders.
- **Smart Features**: Lazy loading album art, shuffle/repeat modes, and system media control integration.

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

## 📸 Screenshots

*(Add screenshots here)*

## 📄 License

This project is open-source.
