# 🤝 Contributing to Void Player

Thank you for your interest in contributing to **Void Player**! We welcome all contributions from bug reports, UI improvements, audio DSP optimizations, to new feature implementations.

---

## 📋 Table of Contents
1. [Code of Conduct](#code-of-conduct)
2. [How Can I Contribute?](#how-can-i-contribute)
   - [Reporting Bugs](#reporting-bugs)
   - [Suggesting Features](#suggesting-features)
   - [Pull Requests](#pull-requests)
3. [Development Setup](#development-setup)
   - [Prerequisites](#prerequisites)
   - [Building & Running](#building--running)
4. [Architecture & Guidelines](#architecture--guidelines)
5. [Git & Commit Conventions](#git--commit-conventions)
6. [F-Droid Compatibility Policy](#f-droid-compatibility-policy)

---

## 📜 Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](./CODE_OF_CONDUCT.md). Please treat fellow contributors with respect and kindness.

---

## 💡 How Can I Contribute?

### 🐞 Reporting Bugs
Before submitting an issue, please search existing [GitHub Issues](https://github.com/TUSHAR91316/Void-Player/issues) to ensure it hasn't already been reported.

When filing a bug report, please include:
- **Device & OS Version** (e.g. Pixel 8, Android 14 / Windows 11).
- **App Version** (e.g. `v2.2`).
- **Steps to Reproduce**: Clear, numbered steps to reproduce the issue.
- **Expected vs Actual Behavior**.
- **Logcat / Terminal Logs** (if applicable).

### 💡 Suggesting Features
Feature requests are always welcome! Open an issue describing:
- The problem you are trying to solve.
- Your proposed solution or user experience.
- Any alternative approaches considered.

### 🔀 Pull Requests
1. **Fork the repository** on GitHub.
2. **Clone your fork**:
   ```bash
   git clone https://github.com/<your-username>/Void-Player.git
   cd Void-Player
   ```
3. **Create a descriptive feature branch**:
   ```bash
   git checkout -b feat/my-new-feature
   ```
4. **Implement your changes** and test thoroughly on Android and Desktop.
5. **Commit your changes** following our [Commit Conventions](#git--commit-conventions).
6. **Push to your fork** and submit a **Pull Request** against the `v2` branch.

---

## 🛠️ Development Setup

### Prerequisites
- **JDK 21** (e.g., OpenJDK 21 / Temurin 21)
- **Android Studio** (Ladybug / Iguana or later) with Android SDK API 35
- **Git**

### Building & Running

**Android (Debug on Device/Emulator):**
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

**Desktop (Package MSI Installer & Portable EXE):**
```bash
./gradlew :composeApp:packageMsi :composeApp:createDistributable
```

**Run Unit Tests:**
```bash
./gradlew check
```

---

## 📐 Architecture & Guidelines

Void Player is built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**:

```
composeApp/
├── commonMain/          # Shared Compose UI, ViewModels, Audio models, and utils
│   ├── ui/              # Compose screens, components, theme, and navigation
│   ├── model/           # Song, Playlist, AiCategory data models
│   ├── player/          # Common AudioPlayer interface & state definitions
│   ├── data/            # SongRepository interface
│   └── utils/           # AiEngine, LrcParser, AudioMetadataUtils, SleepTimer
├── androidMain/         # Android implementations (ExoPlayer Media3, SAF, Palette)
└── desktopMain/         # Desktop JVM implementations (Java Sound, JFileChooser)
```

### Key Development Rules:
- **Clean Architecture**: Keep platform-specific code isolated in `androidMain` and `desktopMain`. Common UI and business logic must reside in `commonMain`.
- **Material 3 Design**: Use Material 3 tokens, harmonious dark palettes, and fluid spring animations.
- **Privacy First**: Void Player is 100% offline. **Never** add network dependencies that send analytics, telemetry, or user data.

---

## 📝 Git & Commit Conventions

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

- `feat:` A new feature (e.g., `feat(ui): add dedicated now playing screen`)
- `fix:` A bug fix (e.g., `fix(nav): keep bottom nav bar visible on player`)
- `docs:` Documentation only changes (e.g., `docs: update README with F-Droid badge`)
- `style:` Changes that do not affect the meaning of the code (formatting, whitespaces)
- `refactor:` Code change that neither fixes a bug nor adds a feature
- `perf:` A code change that improves performance
- `build:` Changes that affect the build system or dependencies

---

## 🛡️ F-Droid Compatibility Policy

Void Player is distributed on **F-Droid**. To maintain F-Droid inclusion:
1. **No Proprietary Dependencies**: All libraries must be 100% open source and free of non-free binary blobs.
2. **No Tracking / Ads**: Zero ad networks, tracking SDKs, or proprietary analytics.
3. **Reproducible Builds**: All release builds must compile cleanly in F-Droid's build environment.

---

Thank you for helping make Void Player awesome! 🎧🚀
