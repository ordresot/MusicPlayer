import 'dart:io';
import 'package:flutter/foundation.dart'; 
import 'package:audio_service/audio_service.dart';
import 'package:audio_metadata_reader/audio_metadata_reader.dart';
import '../services/audio_handler.dart';
import '../services/db_service.dart';
import '../services/file_scanner.dart';
import '../services/crash_logger.dart';

class PlayerProvider extends ChangeNotifier {
  // Access the singleton Phoenix Engine
  final AudioHandler _audioHandler = audioHandler; 
  final DbService _dbService = DbService();
  final FileScannerService _scanner = FileScannerService();

  List<TrackModel> _library = [];
  bool _isLoading = false;
  
  // Getters relying solely on AudioService state (Single Source of Truth)
  List<TrackModel> get library => _library;
  bool get isLoading => _isLoading;
  bool get isPlaying => _audioHandler.playbackState.value.playing;
  MediaItem? get currentTrack => _audioHandler.mediaItem.value;

  PlayerProvider() {
    _init();
  }

  Future<void> _init() async {
    try {
      await _dbService.init();
      await _loadLibrary();
      
      // Listen to Engine State
      // We don't maintain local state; we mirror the Engine.
      _audioHandler.playbackState.listen((state) {
        notifyListeners();
      });
      
      _audioHandler.mediaItem.listen((item) {
        notifyListeners();
      });
    } catch (e, stack) {
      CrashLogger.log("Provider Init Error", stack);
    }
  }

  Future<void> _loadLibrary() async {
    try {
      _library = await _dbService.getAllTracks();
      notifyListeners();
    } catch (e) {
      debugPrint("Library Load Error: $e");
    }
  }

  Future<void> scanFiles(String rootPath) async {
    _isLoading = true;
    notifyListeners();

    try {
      List<String> paths = [];
      String scanPath = rootPath;
      
      // Auto-detect Music folder on Android
      if (Platform.isAndroid && scanPath.isEmpty) {
         if (await _scanner.requestPermissions()) {
            scanPath = _scanner.androidMusicPath;
         }
      }

      if (scanPath.isNotEmpty) {
         paths = await _scanner.scanDirectory(scanPath);
      }

      // Heavy metadata parsing in background Isolate
      final newTracks = await compute(_scanAndExtractMetadata, paths);

      await _dbService.clearLibrary();
      await _dbService.saveTracks(newTracks);
      _library = newTracks;
      
    } catch (e, stack) {
      CrashLogger.log("Scan Error", stack);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> playTrack(TrackModel track) async {
    try {
      final index = _library.indexOf(track);
      if (index == -1) return;

      // Create Queue from Library
      // Optimization: Only update queue if it's different? 
      // For stability, we'll just update it. The Engine handles diffing.
      final queue = _library.map((t) => MediaItem(
        id: t.path,
        album: t.album,
        title: t.title,
        artist: t.artist,
        title: t.title,
        artist: t.artist,
        duration: t.duration != null ? Duration(milliseconds: t.duration!.toInt()) : null,
        // artUri: Uri.file(t.path), // Disabled to prevent Notification Bitmap crash
      )).toList();

      await _audioHandler.updateQueue(queue);
      await _audioHandler.skipToQueueItem(index);
      await _audioHandler.play();
    } catch (e, stack) {
      CrashLogger.log("PlayTrack Error", stack);
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

// Background Isolate Function (Kept pure and outside class)
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

      // 2. Fallback: Parse filename
      if ((artist == "Unknown Artist" || artist.isEmpty) && name.contains("-")) {
         final parts = name.split(RegExp(r'\s*-\s*'));
         if (parts.length >= 2) {
           artist = parts[0].trim();
           if (title == name) {
             String tempTitle = parts.sublist(1).join(" - ").trim();
             final lastDot = tempTitle.lastIndexOf('.');
             if (lastDot != -1) {
               tempTitle = tempTitle.substring(0, lastDot);
             }
             title = tempTitle;
           }
         }
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
