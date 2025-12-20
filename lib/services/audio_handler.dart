import 'dart:async';
import 'package:audio_service/audio_service.dart';
import 'package:audio_session/audio_session.dart';
import 'package:media_kit/media_kit.dart';
import 'package:flutter/material.dart';
import 'crash_logger.dart'; // The Black Box

late AudioHandler audioHandler;

/// Initialize the Audio Service with the Phoenix Engine
Future<void> initAudioService() async {
  try {
    audioHandler = await AudioService.init(
      builder: () => CyberAudioHandler(),
      config: AudioServiceConfig(
        androidNotificationChannelId: 'com.void.player.channel.audio',
        androidNotificationChannelName: 'Void Player Playback',
        androidNotificationOngoing: true,
        androidNotificationIcon: 'ic_launcher',
        androidStopForegroundOnPause: false, // Keep service alive when paused (standard behavior)
        androidResumeOnClick: true,
        notificationColor: const Color(0xFF00FFFF), // Cyan brand color
      ),
    );
  } catch (e, stack) {
    CrashLogger.log("AudioService Init Failed", stack);
  }
}

/// The Phoenix Engine: A robust, simplified AudioHandler
class CyberAudioHandler extends BaseAudioHandler with QueueHandler, SeekHandler {
  // CRITICAL FIX: 'vo: null' prevents generic surface crash in background.
  final Player _player = Player(configuration: const PlayerConfiguration(vo: null));
  
  // Internal mapping of MediaItems to MediaKit Media
  final List<Media> _mediaQueue = [];

  CyberAudioHandler() {
    // SAFETY: Ensure FFI loaded if running in background isolate
    MediaKit.ensureInitialized();
    _init();
  }

  Future<void> _init() async {
    try {
      // 1. Configure Audio Session (Critical for Focus handling)
      final session = await AudioSession.instance;
      await session.configure(const AudioSessionConfiguration.music());
      
      // Handle Audio Focus interruptions (calls/other apps)
      session.interruptionEventStream.listen((event) {
        if (event.begin) {
          switch (event.type) {
            case AudioInterruptionType.duck:
              _player.setVolume(50);
              break;
            case AudioInterruptionType.pause:
            case AudioInterruptionType.unknown:
              pause();
              break;
          }
        } else {
          switch (event.type) {
            case AudioInterruptionType.duck:
              _player.setVolume(100);
              break;
            case AudioInterruptionType.pause:
              play();
              break;
            default:
              break;
          }
        }
      });

      // 2. Playback State Stream
      _player.stream.playing.listen((playing) {
        _broadcastState();
      }, onError: (Object e, StackTrace s) => CrashLogger.log("Stream Error: Playing", s));

      // 3. Duration Stream
      _player.stream.duration.listen((duration) {
        final current = mediaItem.value;
        if (current != null && duration != Duration.zero) {
          mediaItem.add(current.copyWith(duration: duration));
        }
      }, onError: (Object e, StackTrace s) => CrashLogger.log("Stream Error: Duration", s));

      // 4. Completion Stream
      _player.stream.completed.listen((completed) async {
         if (completed) {
           await skipToNext();
         }
      }, onError: (Object e, StackTrace s) => CrashLogger.log("Stream Error: Completed", s));

      // 5. Cleanup on isolate death? (Not needed for singleton usually)
      
    } catch (e, stack) {
      CrashLogger.log("AudioHandler Init Error", stack);
    }
  }



  /// Broadcasts the current state to the UI
  void _broadcastState({Duration? newPosition}) {
    final playing = _player.state.playing;
    final position = newPosition ?? _player.state.position;
    final buffered = _player.state.buffer;

    playbackState.add(playbackState.value.copyWith(
      controls: [
        MediaControl.skipToPrevious,
        if (playing) MediaControl.pause else MediaControl.play,
        MediaControl.skipToNext,
        MediaControl.stop,
      ],
      systemActions: const {
        MediaAction.seek,
        MediaAction.seekForward,
        MediaAction.seekBackward,
      },
      androidCompactActionIndices: const [0, 1, 2],
      processingState: _player.state.buffering 
          ? AudioProcessingState.buffering
          : AudioProcessingState.ready,
      playing: playing,
      updatePosition: position,
      bufferedPosition: buffered,
      queueIndex: null, 
    ));
  }

  @override
  Future<void> play() async {
    try {
      final session = await AudioSession.instance;
      if (await session.setActive(true)) {
        await _player.play();
      } else {
        debugPrint("Failed to activate audio session");
      }
    } catch (e, s) {
      CrashLogger.log("Play Error", s);
    }
  }

  @override
  Future<void> pause() async {
    try {
      await _player.pause();
    } catch (e, s) {
      CrashLogger.log("Pause Error", s);
    }
  }

  @override
  Future<void> stop() async {
    try {
      await _player.stop();
      await (await AudioSession.instance).setActive(false);
    } catch (e, s) {
      CrashLogger.log("Stop Error", s);
    }
  }

  @override
  Future<void> seek(Duration position) async {
    try {
      await _player.seek(position);
      _broadcastState(newPosition: position);
    } catch (e, s) {
      CrashLogger.log("Seek Error", s);
    }
  }

  @override
  Future<void> skipToNext() async {
     try {
       await _player.next(); 
     } catch (e) {
       // Often generic error if no next track, ignore or log lightly
     }
  }

  @override
  Future<void> skipToPrevious() async {
    try {
      await _player.previous();
    } catch (e) {}
  }

  @override
  Future<void> updateQueue(List<MediaItem> queue) async {
    try {
      // 1. Update AudioService Queue
      this.queue.add(queue);
      
      // 2. Convert to MediaKit Media
      _mediaQueue.clear();
      for (var item in queue) {
        _mediaQueue.add(Media(item.id, extras: {'item': item}));
      }

      // 3. Load into Player
      // Note: We use 'autoStart: false' to prevent auto-play on queue update
      final playlist = Playlist(_mediaQueue);
      await _player.open(playlist, play: false);
      
      // 4. Set first item as current
      if (queue.isNotEmpty) {
        mediaItem.add(queue.first);
      }
    } catch (e, s) {
      CrashLogger.log("Queue Update Error", s);
    }
  }

  @override
  Future<void> skipToQueueItem(int index) async {
    try {
      if (index < 0 || index >= _mediaQueue.length) return;
      
      await _player.jump(index);
      
      // Update MediaItem manually to ensure rapid UI feedback
      final item = queue.value[index];
      mediaItem.add(item);
      
      play();
    } catch (e, s) {
      CrashLogger.log("Skip To Item Error", s);
    }
  }
}
