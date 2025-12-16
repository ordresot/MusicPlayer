import 'package:audio_service/audio_service.dart';
import 'package:just_audio/just_audio.dart';

import 'package:audio_session/audio_session.dart';

late AudioHandler audioHandler;

Future<void> initAudioService() async {
  audioHandler = await AudioService.init(
    builder: () => CyberAudioHandler(),
    config: const AudioServiceConfig(
      androidNotificationChannelId: 'com.example.cyber_music_player.channel.audio',
      androidNotificationChannelName: 'Cyber Music Playback',
      androidNotificationOngoing: true,
      androidNotificationIcon: 'mipmap/ic_launcher',
    ),
  );
}

class CyberAudioHandler extends BaseAudioHandler with QueueHandler, SeekHandler {
  final _player = AudioPlayer(); // Define player here


  CyberAudioHandler() {
    _init();
  }

  Future<void> _init() async {
    // Configure session for background playback
    final session = await AudioSession.instance;
    await session.configure(const AudioSessionConfiguration.music());

    // Broadcast playback state changes
    _player.playbackEventStream.listen(_broadcastState);
    
    // Broadcast duration changes
    _player.durationStream.listen((duration) {
       final old = mediaItem.value;
       if (old != null && duration != null) {
         mediaItem.add(old.copyWith(duration: duration));
       }
    });

    // Handle completion
    _player.processingStateStream.listen((state) {
      if (state == ProcessingState.completed) {
        skipToNext();
      }
    });

    // Broadcast current media item
    _player.sequenceStateStream.listen((sequenceState) {
      if (sequenceState == null) return;
      final currentItem = sequenceState.currentSource;
      if (currentItem == null) return;
      // Map tag to MediaItem (we will store MediaItem in tag)
      if (currentItem.tag is MediaItem) {
        mediaItem.add(currentItem.tag as MediaItem);
      }
    });
  }

  void _broadcastState(PlaybackEvent event) {
    final playing = _player.playing;
    playbackState.add(playbackState.value.copyWith(
      controls: [
        MediaControl.skipToPrevious,
        if (playing) MediaControl.pause else MediaControl.play,
        MediaControl.skipToNext,
      ],
      systemActions: const {
        MediaAction.seek,
        MediaAction.seekForward,
        MediaAction.seekBackward,
      },
      androidCompactActionIndices: const [0, 1, 2],
      processingState: const {
        ProcessingState.idle: AudioProcessingState.idle,
        ProcessingState.loading: AudioProcessingState.loading,
        ProcessingState.buffering: AudioProcessingState.buffering,
        ProcessingState.ready: AudioProcessingState.ready,
        ProcessingState.completed: AudioProcessingState.completed,
      }[_player.processingState]!,
      playing: playing,
      updatePosition: _player.position,
      bufferedPosition: _player.bufferedPosition,
      speed: _player.speed,
      queueIndex: event.currentIndex,
    ));
  }

  @override
  Future<void> play() => _player.play();

  @override
  Future<void> pause() => _player.pause();

  @override
  Future<void> seek(Duration position) => _player.seek(position);

  @override
  Future<void> stop() => _player.stop();

  @override
  Future<void> skipToNext() => _player.seekToNext();

  @override
  Future<void> skipToPrevious() => _player.seekToPrevious();

  // Load a playlist
  Future<void> updateQueue(List<MediaItem> newQueue, {int index = 0}) async {
    // Convert MediaItems to AudioSources
    final audioSources = newQueue.map((item) {
      return AudioSource.uri(
        Uri.parse(item.id),
        tag: item, // Store MediaItem in tag for easy retrieval
      );
    }).toList();

    // Define shuffle order (simple sequential for now)
    // Use setAudioSource with a list if available, or ignore deprecation for now as API might be version specific
    // ignore: deprecated_member_use
    await _player.setAudioSource(
      ConcatenatingAudioSource(children: audioSources),
      initialIndex: index,
    );
    
    // Update queue in audio_service
    queue.add(newQueue);
    mediaItem.add(newQueue[index]);
  }
}
