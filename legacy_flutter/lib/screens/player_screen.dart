import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import 'package:just_audio/just_audio.dart';
import 'package:just_audio_background/just_audio_background.dart';
import 'package:rxdart/rxdart.dart';
import '../providers/player_provider.dart';
import '../theme/cyber_theme.dart';

class PlayerScreen extends StatefulWidget {
  const PlayerScreen({super.key});

  @override
  State<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends State<PlayerScreen> {
  double? _dragValue;

  String _formatDuration(Duration? d) {
    if (d == null) return "--:--";
    int minutes = d.inMinutes;
    int seconds = d.inSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final player = context.read<PlayerProvider>().player;

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
            // Get Metadata from current Source Tag
            final sequenceState = player.sequenceState;
            final currentItem = sequenceState?.currentSource?.tag as MediaItem?;
            
            if (currentItem == null) return const Center(child: Text("No Track Playing"));

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
                                   BoxShadow(color: CyberTheme.primary.withValues(alpha: 0.2), blurRadius: 15, spreadRadius: -2),
                                   BoxShadow(color: CyberTheme.secondary.withValues(alpha: 0.1), blurRadius: 10, offset: const Offset(2, 2)),
                                 ],
                                 border: Border.all(color: Colors.white.withValues(alpha: 0.1), width: 1),
                               ),
                               child: ClipRRect(
                                 borderRadius: BorderRadius.circular(20),
                                 child: Container(
                                   color: Colors.grey[900], 
                                   child: Icon(Icons.music_note, size: 100, color: Colors.white24)
                                 ),
                               ),
                             ).animate(target: provider.isPlaying ? 1 : 0)
                              .scale(begin: const Offset(0.95, 0.95), end: const Offset(1, 1), duration: 2.seconds, curve: Curves.easeInOut),
                           ),
                           
                           const Spacer(flex: 1),
                           
                           // Text Info
                           Padding(
                             padding: const EdgeInsets.symmetric(horizontal: 24.0),
                             child: Column(
                               children: [
                                 Text(currentItem.title, textAlign: TextAlign.center, style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                                   fontWeight: FontWeight.bold,
                                   color: Colors.white,
                                   letterSpacing: 1.5
                                 )),
                                 const SizedBox(height: 8),
                                 Text(currentItem.artist ?? "Unknown Artist", style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                   color: CyberTheme.primary,
                                 )),
                               ],
                             ),
                           ),
                           
                           const SizedBox(height: 32),
                           
                           // Seek Bar & Duration
                           StreamBuilder<PositionData>(
                             stream: Rx.combineLatest3<Duration, Duration, Duration?, PositionData>(
                                 player.positionStream,
                                 player.bufferedPositionStream,
                                 player.durationStream,
                                 (position, bufferedPosition, duration) => PositionData(
                                     position, bufferedPosition, duration ?? Duration.zero)),
                             builder: (context, snapshot) {
                               final positionData = snapshot.data ?? PositionData(Duration.zero, Duration.zero, Duration.zero);
                               final duration = positionData.duration;
                               final position = positionData.position;
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
                                         max: max > 0 ? max : 1,
                                         value: _dragValue ?? val,
                                         onChangeStart: (value) {
                                            setState(() {
                                              _dragValue = value;
                                            });
                                         },
                                         onChanged: (value) {
                                            setState(() {
                                              _dragValue = value;
                                            });
                                         },
                                         onChangeEnd: (value) {
                                           player.seek(Duration(milliseconds: value.toInt()));
                                           setState(() {
                                             _dragValue = null;
                                           });
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
                           StreamBuilder<PlayerState>(
                             stream: player.playerStateStream,
                             builder: (context, snapshot) {
                               final playerState = snapshot.data;
                               final processingState = playerState?.processingState;
                               final playing = playerState?.playing;
                               if (processingState == ProcessingState.loading ||
                                   processingState == ProcessingState.buffering) {
                                 return const CircularProgressIndicator(color: CyberTheme.primary);
                               }

                               return Row(
                                 mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                                 children: [
                                   // Shuffle
                                   StreamBuilder<bool>(
                                     stream: player.shuffleModeEnabledStream,
                                     builder: (context, snapshot) {
                                       final shuffleEnabled = snapshot.data ?? false;
                                       return IconButton(
                                         icon: Icon(Icons.shuffle, 
                                           color: shuffleEnabled ? CyberTheme.primary : Colors.white54
                                         ), 
                                         onPressed: () {
                                           player.setShuffleModeEnabled(!shuffleEnabled);
                                         }
                                       );
                                     }
                                   ),
                                   
                                   // Previous
                                   IconButton(
                                     icon: const Icon(Icons.skip_previous, size: 40), 
                                     onPressed: () => player.seekToPrevious()
                                   ),
                                   
                                   // Play/Pause
                                   Container(
                                     width: 80,
                                     height: 80,
                                     decoration: BoxDecoration(
                                       shape: BoxShape.circle,
                                       boxShadow: [BoxShadow(color: CyberTheme.primary.withValues(alpha: 0.5), blurRadius: 20)],
                                       gradient: const LinearGradient(colors: [CyberTheme.primary, CyberTheme.secondary])
                                     ),
                                     child: IconButton(
                                       icon: Icon(
                                         (playing ?? false) ? Icons.pause : Icons.play_arrow, 
                                         color: Colors.black, size: 40
                                       ),
                                       onPressed: () => (playing ?? false) ? player.pause() : player.play(),
                                     ),
                                   ),
                                   
                                   // Next
                                   IconButton(
                                     icon: const Icon(Icons.skip_next, size: 40), 
                                     onPressed: () => player.seekToNext()
                                   ),
                                   
                                   // Repeat
                                   StreamBuilder<LoopMode>(
                                     stream: player.loopModeStream,
                                     builder: (context, snapshot) {
                                       final loopMode = snapshot.data ?? LoopMode.off;
                                       return IconButton(
                                         icon: Icon(
                                            loopMode == LoopMode.one ? Icons.repeat_one : Icons.repeat, 
                                            color: loopMode == LoopMode.off ? Colors.white54 : CyberTheme.primary
                                         ), 
                                         onPressed: () {
                                            final newMode = loopMode == LoopMode.off
                                               ? LoopMode.all
                                               : loopMode == LoopMode.all
                                                   ? LoopMode.one
                                                   : LoopMode.off;
                                            player.setLoopMode(newMode);
                                         }
                                       );
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
