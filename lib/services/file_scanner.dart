import 'dart:io';
import 'package:permission_handler/permission_handler.dart';

class FileScannerService {

  Future<bool> requestPermissions() async {
    if (Platform.isWindows) return true;
    
    // Android 13+
    if (await Permission.audio.request().isGranted) return true;
    // Android 12-
    if (await Permission.storage.request().isGranted) return true;
    
    return false;
  }

  /// Unified scanner for both platforms using standard Dart IO
  Future<List<String>> scanDirectory(String path) async {
    final dir = Directory(path);
    if (!await dir.exists()) return [];

    List<String> audioFiles = [];
    try {
      // Recursive scan
      await for (final entity in dir.list(recursive: true, followLinks: false)) {
        if (entity is File) {
          final ext = entity.path.split('.').last.toLowerCase();
          if (['mp3', 'flac', 'm4a', 'wav', 'ogg'].contains(ext)) {
            audioFiles.add(entity.path);
          }
        }
      }
    } catch (e) {
      // debugPrint("Scan Error: $e");
    }
    return audioFiles;
  }

  /// Default Music directory for Android
  String get androidMusicPath => "/storage/emulated/0/Music";
}
