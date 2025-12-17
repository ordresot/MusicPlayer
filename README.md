# 🎵 Void Player

A lightweight, fully offline music player built with **Flutter**, featuring a modern **Cyberpunk / Neon-Dark UI**.
Designed for performance and aesthetics, running natively on **Android** and **Windows**.

For the motivation and story behind this project, read [ABOUT.md](ABOUT.md).

## ✨ Features

*   **Offline Playback**: Plays local audio files (MP3, FLAC, WAV, M4A) directly from your device.
*   **Cyberpunk Aesthetics**: Custom-built Dark Mode with Neon Accents (Cyan/Purple), Glassmorphism, and futuristic typography.
*   **Dynamic Island Player**: A floating "pill" player (Spotify-style) that animates and follows you across screens.
*   **Efficient Database**: Uses **Hive** (NoSQL) for instant library loading (no need to rescan every startup).
*   **Background Playback**: Full support for background audio and notification controls via `audio_service`.
*   **Cross-Platform**:
    *   **Android**: Auto-scans standard Music folders.
    *   **Windows**: Drag-and-drop or select any folder to scan.

## 🛠️ Tech Stack

*   **Framework**: Flutter (Dart)
*   **Audio Engine**: `just_audio` + `audio_service`
*   **Database**: `hive` (High-performance Key-Value store)
*   **State Management**: `provider`
*   **UI/Animations**: `flutter_animate`, `google_fonts` (Orbitron)
*   **Window Management**: `window_manager` (Custom title bars on Desktop)

## 🚀 Getting Started

### Prerequisites

*   [Flutter SDK](https://flutter.dev/docs/get-started/install) (3.x or later)
*   **Android**: Android SDK & Emulator/Device.
*   **Windows**: Visual Studio 2022 with "Desktop development with C++" workload.

### Installation

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/cyber-music-player.git
    cd cyber-music-player
    ```

2.  **Install Dependencies**:
    ```bash
    flutter pub get
    ```

3.  **Run the App**:

    *   **Android**:
        ```bash
        flutter run -d android
        ```
    *   **Windows**:
        ```bash
        flutter run -d windows
        ```

### 📱 Android Notes
*   On first launch, grant **Storage/Audio permissions** when prompted.
*   The app automatically scans `/storage/emulated/0/Music` (standard Music folder).

### 💻 Windows Notes
*   If you encounter "Visual Studio not found", ensure you have VS 2022 installed with C++ tools.
*   The generic file picker allows you to select any folder to scan.

## 🔧 Roadmap

- [x] Basic Playback & Cyberpunk UI
- [x] Background Audio Support
- [x] Hive Database Integration
- [ ] Metadata Extraction (Artist, Title, Album Art)
- [ ] Shuffle & Repeat Modes
- [ ] Smart Playlists

## 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

## 📄 License

MIT License. Free to use and modify.
