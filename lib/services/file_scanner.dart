import 'dart:io';
import 'package:permission_handler/permission_handler.dart';
import 'package:on_audio_query/on_audio_query.dart';

class FileScannerService {
  final OnAudioQuery _audioQuery = OnAudioQuery();

  Future<bool> requestPermissions() async {
    if (Platform.isWindows) return true;
    
    // Android 13+ check
    if (await Permission.audio.request().isGranted) return true;
    // Older Android
    if (await Permission.storage.request().isGranted) return true;
    
    return false;
  }

  // Unified fetch (for now, mainly Android via OnAudioQuery, Windows manual coming later if needed)
  // Since OnAudioQuery supports Windows in recent versions, we try it first.
  // Warning: on_audio_query_windows might be needed or just dart:io manual scan.
  // For this prototype, let's write a manual recursive scan for Windows to be safe.

  Future<List<String>> scanWindows(String directoryPath) async {
    List<String> audioFiles = [];
    final dir = Directory(directoryPath);
    if (!await dir.exists()) return [];

    try {
      await for (final entity in dir.list(recursive: true, followLinks: false)) {
        if (entity is File) {
          final ext = entity.path.split('.').last.toLowerCase();
          if (['mp3', 'flac', 'm4a', 'wav'].contains(ext)) {
            audioFiles.add(entity.path);
          }
        }
      }
    } catch (e) {
      print("Error scanning: $e");
    }
    return audioFiles;
  }

  // Android: use OnAudioQuery for system MediaStore
  Future<List<SongModel>> scanAndroid() async {
    if (!await requestPermissions()) return [];
    return await _audioQuery.querySongs(
      sortType: null,
      orderType: OrderType.ASC_OR_SMALLER,
      uriType: UriType.EXTERNAL,
      ignoreCase: true,
    );
  }
}
