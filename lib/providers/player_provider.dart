import 'dart:io';
import 'package:flutter/foundation.dart'; // For compute
import 'package:audio_service/audio_service.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';
import 'package:audio_metadata_reader/audio_metadata_reader.dart';
import '../services/audio_handler.dart';
import '../services/db_service.dart';
import '../services/file_scanner.dart';

class PlayerProvider extends ChangeNotifier {
  // Use the global singleton initialized in main.dart
  final AudioHandler _audioHandler = audioHandler; 
  final DbService _dbService = DbService();
  final FileScannerService _scanner = FileScannerService();

  List<TrackModel> _library = [];
  bool _isLoading = false;
  bool _isQueueSynced = false; // Prevents unnecessary queue reloads
  bool? _lastPlayingState; // Throttle overlay updates

  // Getters
  List<TrackModel> get library => _library;
  bool get isLoading => _isLoading;
  bool get isPlaying => _audioHandler.playbackState.value.playing;
  MediaItem? get currentTrack => _audioHandler.mediaItem.value;

  PlayerProvider() {
    _init();
  }

  Future<void> _init() async {
    await _dbService.init();
    _loadLibrary();
    
    // Listen to audio handler changes
    _audioHandler.playbackState.listen((state) {
      notifyListeners();
      // Throttle overlay updates: Only send if playing state CHANGED
      if (_lastPlayingState != state.playing) {
        _lastPlayingState = state.playing;
        _updateOverlayState(isPlaying: state.playing);
      }
    });
    
    _audioHandler.mediaItem.listen((item) {
      notifyListeners();
      if (item != null) {
        _updateOverlayTitle(item.title);
      }
    });
  }

  Future<void> _updateOverlayTitle(String title) async {
    if (Platform.isAndroid) {
       try {
         await FlutterOverlayWindow.shareData({"title": title});
       } catch (e) {
         // debugPrint("Overlay Check: $e");
       }
    }
  }

  Future<void> _updateOverlayState({required bool isPlaying}) async {
    if (Platform.isAndroid) {
       try {
         await FlutterOverlayWindow.shareData({"isPlaying": isPlaying});
       } catch (e) {
         // debugPrint("Overlay Check: $e");
       }
    }
  }

  Future<void> _loadLibrary() async {
    _library = await _dbService.getAllTracks();
    notifyListeners();
  }

  Future<void> scanFiles(String rootPath) async {
    _isLoading = true;
    notifyListeners();

    try {
      List<String> paths = [];
      String scanPath = rootPath;
      
      if (Platform.isAndroid && scanPath.isEmpty) {
         // Default to Music folder if no path provided
         if (await _scanner.requestPermissions()) {
            scanPath = _scanner.androidMusicPath;
         }
      }

      if (scanPath.isNotEmpty) {
         paths = await _scanner.scanDirectory(scanPath);
      }

      // Metadata Extraction (Background Isolate)
      final newTracks = await compute(_scanAndExtractMetadata, paths);

      // Save to DB
      await _dbService.clearLibrary();
      await _dbService.saveTracks(newTracks);
      _library = newTracks;
      _isQueueSynced = false; // Reset sync flag on new scan
      
    } catch (e) {
      // debugPrint("Scan failed: $e");
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> playTrack(TrackModel track) async {
    final index = _library.indexOf(track);
    if (index == -1) return;

    // Only update queue if library changed or not synced
    // This prevents background service kill/restart on track change
    if (!_isQueueSynced) {
      final queue = _library.map((t) => MediaItem(
        id: t.path,
        album: t.album,
        title: t.title,
        artist: t.artist,
        duration: t.duration != null ? Duration(milliseconds: t.duration!.toInt()) : null,
        artUri: Uri.file(t.path),
      )).toList();

      await _audioHandler.updateQueue(queue);
      _isQueueSynced = true;
    }

    await _audioHandler.skipToQueueItem(index);
    await _audioHandler.play();
    
    // Update Overlay
    if (Platform.isAndroid) {
       try {
         await FlutterOverlayWindow.shareData({
            "title": track.title,
            "isPlaying": true
         });
         _lastPlayingState = true;
       } catch (_) { }
    }
  }

  Future<void> togglePlay() async {
    if (isPlaying) {
      await _audioHandler.pause();
    } else {
      await _audioHandler.play();
    }
  }
}

// Top-level function for Isolate
List<TrackModel> _scanAndExtractMetadata(List<String> paths) {
  List<TrackModel> tracks = [];
  
  for (var p in paths) {
    try {
      final name = p.split(RegExp(r'[/\\]')).last;
      String title = name;
      String artist = "Unknown Artist";
      String album = "Unknown Album";
      double duration = 0.0;

      // 1. Try ID3 Tags
      try {
         final metadata = readMetadata(File(p), getImage: false);
         title = metadata.title ?? name;
         if (title.trim().isEmpty) title = name;
         artist = metadata.artist ?? "Unknown Artist";
         album = metadata.album ?? "Unknown Album";
         duration = metadata.duration?.inMilliseconds.toDouble() ?? 0.0;
      } catch (_) { }

      // 2. Fallback: Parse filename (Artist - Title.mp3)
      if ((artist == "Unknown Artist" || artist.isEmpty) && name.contains("-")) {
         final parts = name.split(RegExp(r'\s*-\s*'));
         if (parts.length >= 2) {
           artist = parts[0].trim();
           // Only update title if it's still the filename
           if (title == name) {
             String tempTitle = parts.sublist(1).join(" - ").trim();
             // Remove extension
             final lastDot = tempTitle.lastIndexOf('.');
             if (lastDot != -1) {
               tempTitle = tempTitle.substring(0, lastDot);
             }
             title = tempTitle;
           }
         }
      }
      
      // Clean extension from title if still present
      if (title.toLowerCase().endsWith(".mp3") || title.toLowerCase().endsWith(".flac")) {
         final lastDot = title.lastIndexOf('.');
         if (lastDot != -1) title = title.substring(0, lastDot);
      }

      tracks.add(TrackModel()
        ..path = p
        ..title = title
        ..artist = artist
        ..album = album
        ..duration = duration
      );
    } catch (_) { }
  }
  return tracks;
}
