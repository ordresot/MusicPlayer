# VoidPlayer Version 2.0 Release Notes

Welcome to **VoidPlayer 2.0**! This major update brings a massive overhaul to the user interface, significant user experience enhancements, and important under-the-hood memory optimizations.

## What's New in 2.0 🚀

### 🎨 Complete UI/UX Overhaul
We've moved away from the basic interface to a much more premium, modern, and immersive design.
- **Dynamic Gradient Theme**: The application no longer relies on static colors. VoidPlayer now extracts the dominant color from the currently playing track's album cover and dynamically applies it to the app's background and player controls.
- **Solid Modern Dark Mode**: The old glassmorphism effects have been replaced by a sleek, elevated dark surface design that feels significantly faster and looks more professional.
- **Redesigned Player Island**: The full-screen player has been completely redesigned with a much larger, more prominent album cover display and refined play controls. The compact island also received visual polish.
- **Polished Song List**: Song items in your library now show visual indications of the currently playing track, complete with an animated waveform that matches the track's dominant accent color.

### 🧠 Performance & Memory Management Improvements
Behind the beautiful new interface, we've rewritten how the app handles resources.
- **Smart Image Caching**: We introduced a new, highly efficient LRU (Least Recently Used) cache for album art.
- **Smoother Scrolling**: By caching `ImageBitmaps` instead of raw byte arrays, the app no longer wastes CPU cycles re-converting images when you scroll through large music libraries. 
- **Reduced Memory Bloat**: The new architecture guarantees that the app only keeps what it needs in memory, preventing crashes or slowdowns on lower-end devices.

### 🛠 Architecture Refactoring
- The underlying codebase was heavily refactored. The monolithic layout logic has been modularized into discrete UI components (`Theme`, `Header`, `SongList`, `PlayerIsland`, `SettingsDialog`), making future updates faster and less prone to bugs.

---
*Thank you for using VoidPlayer! Update your app now to experience the difference.*
