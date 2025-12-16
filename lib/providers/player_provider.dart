import 'dart:io';
import 'package:flutter/material.dart';
import 'package:audio_service/audio_service.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart'; // Added for overlay
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
    });
    _audioHandler.mediaItem.listen((item) {
      notifyListeners();
    });
  }

  Future<void> _loadLibrary() async {
    _library = await _dbService.getAllTracks();
    notifyListeners();
  }

  Future<void> scanFiles(String rootPath) async {
    _isLoading = true;
    notifyListeners();

    try {
      // 1. Scan
      // For Windows: rootPath is passed. For Android: we trigger permission scan.
      // We'll normalize this.
      // 1. Scan
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

      // 2. Clear old DB (optional, or just append)
      await _dbService.clearLibrary();

      // 3. Convert to Models
      List<TrackModel> newTracks = paths.map((p) {
        final name = p.split(RegExp(r'[/\\]')).last;
        return TrackModel()
          ..path = p
          ..title = name
          ..artist = "Unknown"
          ..album = "Unknown"
          ..duration = 0; // Metadata extraction would go here (e.g. using `audiotags` input equivalent in Dart)
      }).toList();

      // 4. Save
      await _dbService.saveTracks(newTracks);
      _library = newTracks;
      
    } catch (e) {
      print("Scan failed: $e");
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> playTrack(TrackModel track) async {
    final index = _library.indexOf(track);
    if (index == -1) return;

    // Convert library to MediaItems
    final queue = _library.map((t) => MediaItem(
      id: t.path,
      album: t.album,
      title: t.title,
      artist: t.artist,
      artUri: null, // TODO: Load artwork
    )).toList();

    await _audioHandler.updateQueue(queue);
    await _audioHandler.skipToQueueItem(index);
    await _audioHandler.play();
    
    // Update Overlay
    if (Platform.isAndroid) {
       try {
         await FlutterOverlayWindow.shareData({
            "title": track.title,
            "isPlaying": true
         });
       } catch (e) {
         print("Overlay error: $e");
       }
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
