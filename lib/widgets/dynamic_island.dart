import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../providers/player_provider.dart';
import '../theme/cyber_theme.dart';
import '../screens/player_screen.dart';

class DynamicIslandPlayer extends StatelessWidget {
  const DynamicIslandPlayer({super.key});

  @override
  @override
  Widget build(BuildContext context) {
    return Consumer<PlayerProvider>(
      builder: (context, provider, child) {
        
        return LayoutBuilder(
          builder: (context, constraints) {
            // Safe width calculation
            final width = constraints.maxWidth > 600 ? 500.0 : constraints.maxWidth * 0.95;
            
            return Align(
              alignment: Alignment.bottomCenter, 
              child: Padding(
                padding: const EdgeInsets.only(bottom: 16.0),
                child: GestureDetector(
                  onTap: () {
                    Navigator.of(context).push(
                      PageRouteBuilder(
                        pageBuilder: (context, animation, secondaryAnimation) => const PlayerScreen(),
                        transitionsBuilder: (context, animation, secondaryAnimation, child) {
                          const begin = Offset(0.0, 1.0);
                          const end = Offset.zero;
                          const curve = Curves.easeOutExpo;
                          var tween = Tween(begin: begin, end: end).chain(CurveTween(curve: curve));
                          return SlideTransition(position: animation.drive(tween), child: child);
                        },
                      ),
                    );
                  },
                  child: Container(
                    height: 70,
                    width: width,
                    decoration: BoxDecoration(
                      color: CyberTheme.surface.withValues(alpha: 0.9),
                      borderRadius: BorderRadius.circular(35),
                      border: Border.all(color: CyberTheme.primary.withValues(alpha: 0.3)),
                      boxShadow: [
                        BoxShadow(
                          color: CyberTheme.primary.withValues(alpha: 0.1),
                          blurRadius: 20,
                          spreadRadius: 2,
                        )
                      ],
                    ),
                    child: Row(
                      children: [
                        // Album Art
                        Padding(
                          padding: const EdgeInsets.only(left: 8.0),
                          child: CircleAvatar(
                            radius: 26,
                            backgroundColor: Colors.black,
                            child: Icon(Icons.music_note, color: CyberTheme.primary),
                          ).animate(target: provider.isPlaying ? 1 : 0)
                          .shimmer(duration: 2.seconds, color: CyberTheme.primary.withValues(alpha: 0.5)),
                        ),
                        
                        const SizedBox(width: 12),
                        
                        // Track Info
                        Expanded(
                          child: Builder(
                            builder: (context) {
                              final track = provider.currentTrack;
                              final title = track?.title ?? "No Song Playing";
                              final artist = track?.artist ?? "Select a track";

                              return Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    title, 
                                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                      color: Colors.white,
                                      fontWeight: FontWeight.bold,
                                    ),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                  Text(
                                    artist, 
                                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                      fontSize: 12,
                                    ),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ],
                              );
                            }
                          ),
                        ),
                        
                        // Controls
                        IconButton(
                          icon: Icon(Icons.skip_previous_rounded, color: Colors.white),
                          onPressed: () {},
                        ),
                        Container(
                          width: 48,
                          height: 48,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: CyberTheme.primary,
                            boxShadow: [
                               BoxShadow(color: CyberTheme.primary.withValues(alpha: 0.4), blurRadius: 10)
                            ]
                          ),
                          child: IconButton(
                            icon: Icon(
                              provider.isPlaying ? Icons.pause_rounded : Icons.play_arrow_rounded,
                              color: Colors.black,
                              size: 30,
                            ),
                            onPressed: provider.togglePlay,
                          ),
                        ),
                        IconButton(
                          icon: Icon(Icons.skip_next_rounded, color: Colors.white),
                          onPressed: () {},
                        ),
                        const SizedBox(width: 16),
                      ],
                    ),
                  ).animate()
                   .slideY(begin: 1.0, end: 0.0, curve: Curves.easeOutBack, duration: 600.ms)
                   .fadeIn(),
                ), 
              ), 
            ); 
          }
        );
      }, 
    );
  }
}
