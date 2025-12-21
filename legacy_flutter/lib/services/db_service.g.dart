// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'db_service.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class TrackModelAdapter extends TypeAdapter<TrackModel> {
  @override
  final int typeId = 0;

  @override
  TrackModel read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return TrackModel()
      ..path = fields[0] as String
      ..title = fields[1] as String
      ..artist = fields[2] as String
      ..album = fields[3] as String
      ..duration = fields[4] as double?;
  }

  @override
  void write(BinaryWriter writer, TrackModel obj) {
    writer
      ..writeByte(5)
      ..writeByte(0)
      ..write(obj.path)
      ..writeByte(1)
      ..write(obj.title)
      ..writeByte(2)
      ..write(obj.artist)
      ..writeByte(3)
      ..write(obj.album)
      ..writeByte(4)
      ..write(obj.duration);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is TrackModelAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
