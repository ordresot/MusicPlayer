import 'package:audio_service/audio_service.dart';
import 'package:just_audio/just_audio.dart';

import 'package:audio_session/audio_session.dart';

late AudioHandler audioHandler;

Future<void> initAudioService() async {
  audioHandler = await AudioService.init(
    builder: () => CyberAudioHandler(),
    config: AudioServiceConfig(
      androidNotificationChannelId: 'com.example.cyber_music_player.channel.audio',
      androidNotificationChannelName: 'Cyber Music Playback',
      androidNotificationOngoing: true,
      androidNotificationIcon: 'ic_launcher',
      androidStopForegroundOnPause: false,
      androidResumeOnClick: true,
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
      // warning: The operand can't be 'null' -> properly accessed
      final currentItem = sequenceState.currentSource;
      // Map tag to MediaItem (we will store MediaItem in tag)
      if (currentItem?.tag is MediaItem) { 
        final tagItem = currentItem!.tag as MediaItem;
        final realDuration = _player.duration;
        if (realDuration != null && realDuration != Duration.zero) {
          mediaItem.add(tagItem.copyWith(duration: realDuration));
        } else {
          mediaItem.add(tagItem);
        }
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
  Future<void> play() async {
    try { await _player.play(); } catch (_) { }
  }

  @override
  Future<void> pause() async {
    try { await _player.pause(); } catch (_) { }
  }

  @override
  Future<void> seek(Duration position) async {
    try { await _player.seek(position); } catch (_) { }
  }

  @override
  Future<void> stop() async {
    try { await _player.stop(); } catch (_) { }
  }

  @override
  Future<void> skipToNext() async {
    try { await _player.seekToNext(); } catch (_) { }
  }

  @override
  Future<void> skipToPrevious() async {
    try { await _player.seekToPrevious(); } catch (_) { }
  }

  @override
  Future<void> skipToQueueItem(int index) async {
    try { await _player.seek(null, index: index); } catch (_) { }
  }

  // Load a playlist
  @override
  Future<void> updateQueue(List<MediaItem> queue) async {
    try {
      // Convert MediaItems to AudioSources
      final audioSources = queue.map((item) {
        return AudioSource.uri(
          Uri.parse(item.id),
          tag: item, // Store MediaItem in tag for easy retrieval
        );
      }).toList();

      // Define shuffle order (simple sequential for now)
      await _player.setAudioSource(
        // ignore: deprecated_member_use
        ConcatenatingAudioSource(children: audioSources),
        initialIndex: 0,
      );
      
      // Update queue in audio_service
      this.queue.add(queue);
      mediaItem.add(queue[0]);
    } catch (_) { }
  }
}
