import 'package:isar/isar.dart';
import 'package:path_provider/path_provider.dart';

part 'db_service.g.dart'; // Isar generator will create this

@collection
class TrackModel {
  Id id = Isar.autoIncrement;

  @Index(type: IndexType.value)
  late String path;

  late String title;
  late String artist;
  late String album;
  
  double? duration; // Seconds
}

class DbService {
  late Isar _isar;

  Future<void> init() async {
    final dir = await getApplicationDocumentsDirectory();
    _isar = await Isar.open(
      [TrackModelSchema],
      directory: dir.path,
    );
  }

  Future<void> saveTracks(List<TrackModel> tracks) async {
    await _isar.writeTxn(() async {
      await _isar.trackModels.putAll(tracks);
    });
  }

  Future<List<TrackModel>> getAllTracks() async {
    return await _isar.trackModels.where().findAll();
  }
  
  Future<void> clearLibrary() async {
    await _isar.writeTxn(() async {
      await _isar.clear();
    });
  }
}
