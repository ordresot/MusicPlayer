import 'package:audio_service/audio_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import 'package:rxdart/rxdart.dart';
import '../providers/player_provider.dart';
import '../services/audio_handler.dart'; // For global audioHandler
import '../theme/cyber_theme.dart';

class PlayerScreen extends StatefulWidget {
  const PlayerScreen({super.key});

  @override
  State<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends State<PlayerScreen> {
  late Stream<PositionData> _positionDataStream;

  @override
  void initState() {
    super.initState();
    // Initialize stream ONCE to prevent memory leaks and unnecessary overhead
    _positionDataStream = Rx.combineLatest3<Duration, Duration, Duration?, PositionData>(
        AudioService.position,
        audioHandler.playbackState.map((state) => state.bufferedPosition),
        audioHandler.mediaItem.map((item) => item?.duration),
        (position, bufferedPosition, duration) => PositionData(
            position, bufferedPosition, duration ?? Duration.zero));
  }

  String _formatDuration(Duration? d) {
    if (d == null) return "--:--";
    int minutes = d.inMinutes;
    int seconds = d.inSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        title: Text("NOW_PLAYING", style: Theme.of(context).textTheme.displayLarge?.copyWith(fontSize: 18)),
        centerTitle: true,
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: Icon(Icons.keyboard_arrow_down, color: CyberTheme.primary),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Colors.black,
              CyberTheme.surface,
              const Color(0xFF0F0F0F),
            ],
          ),
        ),
        child: Consumer<PlayerProvider>(
          builder: (context, provider, child) {
            final track = provider.currentTrack;
            if (track == null) return const Center(child: Text("No Track Playing"));

            return LayoutBuilder(
              builder: (context, constraints) {
                return SingleChildScrollView(
                  child: ConstrainedBox(
                    constraints: BoxConstraints(minHeight: constraints.maxHeight),
                    child: IntrinsicHeight(
                      child: Column(
                        children: [
                           const Spacer(flex: 1),
                           
                           // Album Art
                           Center(
                             child: Container(
                               width: 300,
                               height: 300,
                               decoration: BoxDecoration(
                                 borderRadius: BorderRadius.circular(20),
                                 boxShadow: [
                                   BoxShadow(color: CyberTheme.primary.withOpacity(0.3), blurRadius: 40, spreadRadius: -5),
                                   BoxShadow(color: CyberTheme.secondary.withOpacity(0.2), blurRadius: 20, offset: const Offset(5, 5)),
                                 ],
                                 border: Border.all(color: Colors.white.withOpacity(0.1), width: 1),
                               ),
                               child: ClipRRect(
                                 borderRadius: BorderRadius.circular(20),
                                 child: Container(
                                   color: Colors.grey[900], 
                                   child: Icon(Icons.music_note, size: 100, color: Colors.white24)
                                 ),
                               ),
                             ).animate(target: provider.isPlaying ? 1 : 0)
                              .shimmer(duration: 3.seconds, delay: 1.seconds, color: CyberTheme.primary.withOpacity(0.1))
                              .scale(begin: const Offset(0.95, 0.95), end: const Offset(1, 1), duration: 2.seconds, curve: Curves.easeInOut),
                           ),
                           
                           const Spacer(flex: 1),
                           
                           // Text Info
                           Padding(
                             padding: const EdgeInsets.symmetric(horizontal: 24.0),
                             child: Column(
                               children: [
                                 Text(track.title, textAlign: TextAlign.center, style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                                   fontWeight: FontWeight.bold,
                                   color: Colors.white,
                                   letterSpacing: 1.5
                                 )),
                                 const SizedBox(height: 8),
                                 Text(track.artist ?? "Unknown Artist", style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                   color: CyberTheme.primary,
                                 )),
                               ],
                             ),
                           ),
                           
                           const SizedBox(height: 32),
                           
                           // Seek Bar & Duration
                           StreamBuilder<PositionData>(
                             stream: _positionDataStream,
                             builder: (context, snapshot) {
                               final positionData = snapshot.data ?? PositionData(Duration.zero, Duration.zero, Duration.zero);
                               final duration = positionData.duration;
                               final position = positionData.position;
                               // Prevent slider error
                               final max = duration.inMilliseconds.toDouble();
                               final val = position.inMilliseconds.toDouble().clamp(0.0, max);
                               
                               return Padding(
                                 padding: const EdgeInsets.symmetric(horizontal: 24.0),
                                 child: Column(
                                   children: [
                                     Row(
                                       mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                       children: [
                                         Text(_formatDuration(position), style: const TextStyle(fontFamily: 'RobotoMono', color: Colors.white54)),
                                         Text(_formatDuration(duration), style: const TextStyle(fontFamily: 'RobotoMono', color: Colors.white54)),
                                       ],
                                     ),
                                     SliderTheme(
                                       data: Theme.of(context).sliderTheme.copyWith(
                                         thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 8),
                                       ),
                                       child: Slider(
                                         min: 0,
                                         max: max > 0 ? max : 1, // Avoid division by zero
                                         value: val,
                                         onChanged: (value) {
                                           audioHandler.seek(Duration(milliseconds: value.toInt()));
                                         },
                                       ),
                                     ),
                                   ],
                                 ),
                               );
                             }
                           ),
                           
                           const SizedBox(height: 16),
                           
                           // Controls
                           StreamBuilder<PlaybackState>(
                             stream: audioHandler.playbackState,
                             builder: (context, snapshot) {
                               final playing = snapshot.data?.playing ?? false;
                               final shuffleMode = snapshot.data?.shuffleMode ?? AudioServiceShuffleMode.none;
                               final repeatMode = snapshot.data?.repeatMode ?? AudioServiceRepeatMode.none;

                               return Row(
                                 mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                                 children: [
                                   // Shuffle
                                   IconButton(
                                     icon: Icon(Icons.shuffle, 
                                       color: shuffleMode == AudioServiceShuffleMode.all ? CyberTheme.primary : Colors.white54
                                     ), 
                                     onPressed: () {
                                       final newMode = shuffleMode == AudioServiceShuffleMode.none
                                           ? AudioServiceShuffleMode.all
                                           : AudioServiceShuffleMode.none;
                                       audioHandler.setShuffleMode(newMode);
                                     }
                                   ),
                                   
                                   // Previous
                                   IconButton(
                                     icon: const Icon(Icons.skip_previous, size: 40), 
                                     onPressed: audioHandler.skipToPrevious
                                   ),
                                   
                                   // Play/Pause
                                   Container(
                                     width: 80,
                                     height: 80,
                                     decoration: BoxDecoration(
                                       shape: BoxShape.circle,
                                       boxShadow: [BoxShadow(color: CyberTheme.primary.withOpacity(0.5), blurRadius: 20)],
                                       gradient: const LinearGradient(colors: [CyberTheme.primary, CyberTheme.secondary])
                                     ),
                                     child: IconButton(
                                       icon: Icon(
                                         playing ? Icons.pause : Icons.play_arrow, 
                                         color: Colors.black, size: 40
                                       ),
                                       onPressed: playing ? audioHandler.pause : audioHandler.play,
                                     ),
                                   ),
                                   
                                   // Next
                                   IconButton(
                                     icon: const Icon(Icons.skip_next, size: 40), 
                                     onPressed: audioHandler.skipToNext
                                   ),
                                   
                                   // Repeat
                                   IconButton(
                                     icon: Icon(
                                        repeatMode == AudioServiceRepeatMode.one ? Icons.repeat_one : Icons.repeat, 
                                        color: repeatMode == AudioServiceRepeatMode.none ? Colors.white54 : CyberTheme.primary
                                     ), 
                                     onPressed: () {
                                        final newMode = repeatMode == AudioServiceRepeatMode.none
                                           ? AudioServiceRepeatMode.all
                                           : repeatMode == AudioServiceRepeatMode.all
                                               ? AudioServiceRepeatMode.one
                                               : AudioServiceRepeatMode.none;
                                        audioHandler.setRepeatMode(newMode);
                                     }
                                   ),
                                 ],
                               );
                             }
                           ),
                           
                           const Spacer(flex: 2),
                           
                           const SizedBox(height: 20),
                        ],
                      ),
                    ),
                  ),
                );
              }
            );
          }
        ),
      ),
    );
  }
}

class PositionData {
  final Duration position;
  final Duration bufferedPosition;
  final Duration duration;

  PositionData(this.position, this.bufferedPosition, this.duration);
}
