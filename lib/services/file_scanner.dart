import 'dart:io';
import 'package:permission_handler/permission_handler.dart';
import 'package:on_audio_query/on_audio_query.dart';
import 'package:audio_metadata_reader/audio_metadata_reader.dart';
import 'db_service.dart';

class FileScannerService {
  final OnAudioQuery _audioQuery = OnAudioQuery();

  Future<bool> requestPermissions() async {
    if (Platform.isWindows) return true;
    return await _audioQuery.permissionsRequest();
  }

  Future<List<TrackModel>> scanTracks() async {
    if (Platform.isAndroid) {
      return await _scanAndroid();
    } else {
      // Default Windows Music Path
      String path = Platform.environment['USERPROFILE']! + "\\Music";
      return await _scanDesktop(path);
    }
  }

  Future<List<TrackModel>> _scanAndroid() async {
    try {
      List<SongModel> songs = await _audioQuery.querySongs(
        sortType: null,
        orderType: OrderType.ASC_OR_SMALLER,
        uriType: UriType.EXTERNAL,
        ignoreCase: true,
      );

      return songs.map((song) {
         // Construct Content URI for Android 11+ compatibility
         final contentUri = "content://media/external/audio/media/${song.id}";
         
         return TrackModel()
           ..title = song.title
           ..artist = song.artist ?? "<Unknown>"
           ..album = song.album ?? "<Unknown>"
           ..path = contentUri 
           ..duration = song.duration?.toDouble() ?? 0.0;
      }).toList();
      
    } catch (e) {
      return [];
    }
  }

  Future<List<TrackModel>> _scanDesktop(String path) async {
    final dir = Directory(path);
    if (!await dir.exists()) return [];

    List<TrackModel> tracks = [];
    try {
      await for (final entity in dir.list(recursive: true, followLinks: false)) {
        if (entity is File) {
          final ext = entity.path.split('.').last.toLowerCase();
          if (['mp3', 'flac', 'm4a', 'wav', 'ogg'].contains(ext)) {
             tracks.add(await _extractMetadata(entity));
          }
        }
      }
    } catch (_) {}
    return tracks;
  }

  Future<TrackModel> _extractMetadata(File file) async {
      String title = file.path.split(Platform.pathSeparator).last;
      String artist = "Unknown Artist";
      String album = "Unknown Album";
      double duration = 0.0;

      try {
         final metadata = readMetadata(file, getImage: false);
         title = metadata.title ?? title;
         if (title.isEmpty) title = file.path.split(Platform.pathSeparator).last;
         artist = metadata.artist ?? artist;
         album = metadata.album ?? album;
         duration = metadata.duration?.inMilliseconds.toDouble() ?? 0.0;
      } catch (_) {}
      
      return TrackModel()
        ..path = file.path
        ..title = title
        ..artist = artist
        ..album = album
        ..duration = duration;
  }
  
  String get androidMusicPath => "/storage/emulated/0/Music"; // Kept for reference but unused
}
