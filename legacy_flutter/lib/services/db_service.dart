import 'package:hive_flutter/hive_flutter.dart';

part 'db_service.g.dart';

@HiveType(typeId: 0)
class TrackModel extends HiveObject {
  @HiveField(0)
  late String path;

  @HiveField(1)
  late String title;

  @HiveField(2)
  late String artist;

  @HiveField(3)
  late String album;
  
  @HiveField(4)
  double? duration;
}

class DbService {
  late Box<TrackModel> _libraryBox;

  Future<void> init() async {
    await Hive.initFlutter();
    if (!Hive.isAdapterRegistered(0)) {
       Hive.registerAdapter(TrackModelAdapter());
    }
    _libraryBox = await Hive.openBox<TrackModel>('library_v1');
  }

  Future<void> saveTracks(List<TrackModel> tracks) async {
    // Unused map removed
    await _libraryBox.addAll(tracks);
  }

  Future<List<TrackModel>> getAllTracks() async {
    return _libraryBox.values.toList();
  }
  
  Future<void> clearLibrary() async {
    await _libraryBox.clear();
  }
}
