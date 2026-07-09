# VoidPlayer Version 2.1 Release Notes

Welcome to **VoidPlayer 2.1**! This update brings critical stability fixes, crash hardening across all components, and adaptive memory improvements to make the player run flawlessly on any device.

> [!WARNING]
> **Important Installation Note:** Because we upgraded to a new signing key for this release, **you must completely uninstall Version 2.0 before installing Version 2.1**. If you try to update directly, Android will block the installation with a "Signature Mismatch" error.

---

## What's New in 2.1 🚀

### 🧠 Architecture & Performance
- **Adaptive Memory Architecture**: Intelligently queries your device's exact RAM capabilities. Flagship phones get high-fidelity album art; budget devices get downscaled images to maintain speed.
- **Bounded Image Cache**: A hardware-aware LRU cache for decoded album art bitmaps prevents memory exhaustion when scrolling large libraries.

### 🎧 Audio Engine
- **Proper Audio Attributes**: ExoPlayer is configured with `CONTENT_TYPE_MUSIC` and `USAGE_MEDIA` for correct audio focus behavior and best DAC routing.
- **Auto-Pause on Headphone Disconnect**: Full `AudioManager.ACTION_AUDIO_BECOMING_NOISY` integration — music pauses automatically when you unplug headphones or disconnect Bluetooth.
- **Real-Time Dynamic Normalization**: Android's `DynamicsProcessing` DSP chip is used for multiband compression and normalization (API 28+ only). Fully guarded and safe.
- **Equalizer**: Full hardware equalizer bands exposed in Settings, initialized safely after audio session is ready.

### 🐛 Critical Crash Fixes

All seven confirmed crash causes have been fixed:

1. **ForegroundServiceStartNotAllowedException (ANR on Android 12+)**
   - `PlaybackService` is a `MediaSessionService`. Manually calling `startForegroundService()` on it crashes the app on Android 12+ if the service doesn't post a notification within 5 seconds.
   - **Fix**: Now uses `context.startService()` only. Media3 handles foreground elevation internally.

2. **Native SIGSEGV crash in `libeffect` (Equalizer / DynamicsProcessing)**
   - `Equalizer` and `DynamicsProcessing` were initialized when `audioSessionId` was still `0`. Passing `0` to native audio effects causes an uncatchable C++ crash.
   - **Fix**: DSP initialization moved to `onAudioSessionIdChanged()` callback with a non-zero guard.

3. **DSP unavailable on device / custom ROM**
   - `DynamicsProcessing` requires API 28+ and may be absent on custom ROMs even when the SDK is satisfied.
   - **Fix**: API check + `catch(Throwable)` on all DSP code (not just `catch(Exception)`).

4. **SecurityException from undeclared WAKE_LOCK permission**
   - `WAKE_MODE_NETWORK` and `WAKE_MODE_LOCAL` both require `android.permission.WAKE_LOCK`, which was not declared in the manifest.
   - **Fix**: Changed to `WAKE_MODE_NONE` — correct for local file playback.

5. **NullPointerException from `BitmapFactory.decodeByteArray()` returning null**
   - Songs with missing or corrupt embedded album art cause `BitmapFactory.decodeByteArray()` to return `null`. Calling `.asImageBitmap()` on null throws NPE inside a Compose `LaunchedEffect`, crashing the entire app.
   - **Fix**: Added null fallback (`1×1 transparent bitmap`) and wrapped `toImageBitmap()` in `try-catch(Throwable)`.

6. **OutOfMemoryError in `loadArt()` from large embedded artwork**
   - Very large album art (3000×3000px) can cause `OutOfMemoryError` when loaded via `MediaMetadataRetriever`. `OOM` is an `Error`, not an `Exception`, so `catch(Exception)` missed it.
   - **Fix**: Changed to `catch(Throwable)` with safe `retriever.release()` in nested try-catch.

7. **OOM / Codec crash from aggressive ExoPlayer configuration**
   - Custom `DefaultLoadControl` with 50–100MB buffer + `EXTENSION_RENDERER_MODE_PREFER` caused OOM on low-RAM phones and codec crashes on devices without extension libraries.
   - **Fix**: Removed all custom ExoPlayer configuration. Using clean `ExoPlayer.Builder(context).build()`.

### 🛡️ Additional Hardening

- **OverlayService — `windowManager.updateViewLayout()` guarded**: Wrapped in `try-catch` to prevent `IllegalArgumentException` if the overlay was detached before the `LaunchedEffect` fired.
- **OverlayService — Album art rendering guarded**: Both `toImageBitmap()` calls inside the overlay are now wrapped in `try-catch` for safe rendering.
- **PlaybackService — instance null-safety**: `VoidPlayerApp.instance` access is guarded with `try-catch` in case Android restarts the service process without calling `Application.onCreate()` first.
- **OverlayService — instance null-safety**: `onStartCommand` is guarded with `try-catch` for the same edge case.
- **Dynamic Island**: Fixed permission prompt appearing on every launch. Now shown exactly once.
- **Settings Layout**: Fixed "Audio Normalization" text pushing the toggle off-screen on narrow displays.

### ⚙️ Under The Hood
- Minimum SDK raised to Android 8.0 (API 26).
- Added `commonTest` test source set with Kotlin test dependency.
- Strengthened `.gitignore` to prevent keystore and APK files from being committed.

---

## 🛠 Developer Bug Reference

See [chat.md](./chat.md) for the full crash audit with root causes, fixes, and prevention rules.

Quick summary of all-time crash causes:

| # | Crash Type | Location | Fixed In |
|---|-----------|----------|----------|
| 1 | `ForegroundServiceStartNotAllowedException` | `AndroidAudioPlayer.startPlaybackService()` | v2.1 |
| 2 | Native SIGSEGV (`libeffect`) | `AndroidAudioPlayer.ensureEqualizer()` | v2.1 |
| 3 | DSP `RuntimeException` / `NoClassDefFoundError` | `AndroidAudioPlayer.ensureNormalization()` | v2.1 |
| 4 | `SecurityException` (WAKE_LOCK) | `AndroidAudioPlayer` ExoPlayer init | v2.1 |
| 5 | `NullPointerException` (null bitmap) | `ImageUtils.toImageBitmap()` | v2.1 |
| 6 | `OutOfMemoryError` (large art) | `AndroidSongRepository.loadArt()` | v2.1 |
| 7 | OOM / Codec crash | ExoPlayer `LoadControl` + `RenderersFactory` | v2.1 |

---

*Thank you for using VoidPlayer! This release marks a major stability milestone.*
