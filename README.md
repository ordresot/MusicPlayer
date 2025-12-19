# 🎵 Void Player (Phoenix Edition)

> **"The music player that never sleeps."**

A robust, fully offline music player built with **Flutter** and **Native C++** (via MediaKit).
Designed for extreme stability, background endurance, and cyberpunk aesthetics.

![Void Player Banner](https://via.placeholder.com/1200x400/000000/00FFFF?text=VOID+PLAYER)

---

## 🏗️ System Architecture ("The Phoenix Protocol")

Void Player operates on a custom-built architecture designed to solve the "40-second background crash" notorious in Android development.

### 1. The Core Engine (`CyberAudioHandler`)
Unlike standard Flutter players that run on the UI thread, Void Player utilizes a **Foreground Service** with a dedicated **C++ Audio Back-end**.
*   **MediaKit**: Uses `libmpv` (C++) for audio decoding. This bypasses the Android MediaPlayer framework, preventing codec-specific crashes.
*   **Smart Wakelock**: Implements a "VLC-style" Partial Wakelock.
    *   **Playing**: CPU is locked Awake.
    *   **Paused**: CPU is released to sleep.
*   **Audio Session**: Manages focus (pauses on calls, ducks on notifications).

### 2. The Nervous System (`PlayerProvider`)
A passive state machine that mirrors the Engine. It has zero logic of its own regarding playback state, eliminating "Split-Brain" bugs where the UI thinks music is playing but the engine stopped.

### 3. The Black Box (`CrashLogger`)
A forensic subsystem that writes fatal errors to the device's internal storage (`crash_logs.txt`). Even if the UI vanishes, the Black Box survives to report the cause.

---

## 📂 Project Structure

```bash
lib/
├── main.dart                  # Entry Point (Crash Guard + Dependency Injection)
├── screens/
│   ├── home_screen.dart       # Dashboard (Dynamic Island + Library)
│   └── player_screen.dart     # Now Playing UI (Animations + visualizer)
├── services/
│   ├── audio_handler.dart     # THE PHOENIX ENGINE (Native Audio + Service)
│   ├── crash_logger.dart      # "The Black Box" implementation
│   ├── db_service.dart        # Hive Database (Track metadata)
│   ├── file_scanner.dart      # Permission & FileWalker logic
│   └── battery_optimization_service.dart # (Deprecated) OEM specific checks
├── providers/
│   └── player_provider.dart   # State Machine (Connects UI to Engine)
└── widgets/
    └── dynamic_island.dart    # Floating Mini-Player
```

---

## 🛠️ Tech Stack

| Component | Technology | Reasoning |
| :--- | :--- | :--- |
| **Framework** | Flutter 3.16+ | Cross-platform UI |
| **Audio Engine** | **MediaKit** (C++) | Industrial-strength stability (mpv based) |
| **Service** | `audio_service` | Android Foreground Service guarantees |
| **Database** | `Hive` | Instant startup (NoSQL Key-Value) |
| **State** | `Provider` | Efficient reactive UI |
| **Logging** | `dart:io` | Raw file logging for independent diagnostics |

---

## 🚀 Installation & Build

### Prerequisites
- Flutter SDK 3.x
- **Windows**: Visual Studio 2022 (C++ Desktop Workload)
- **Android**: SDK 34 (UpsideDownCake)

### Running
```bash
# 1. Get packages
flutter pub get

# 2. Run (Release mode recommended for performance)
flutter run --release
```

## 🐛 Troubleshooting (The Black Box)

If the app crashes, connect your phone and run:
`adb shell run-as com.example.cyber_music_player cat app_flutter/crash_logs.txt`

---

## 📄 License
MIT License. Open Source.
