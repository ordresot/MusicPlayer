import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../providers/player_provider.dart';
import '../theme/cyber_theme.dart';

class DynamicIslandPlayer extends StatelessWidget {
  const DynamicIslandPlayer({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<PlayerProvider>(
      builder: (context, provider, child) {
        // Only show if media is loaded or playing
        // For demo, we show it always if "isPlaying" is mocked or true
        // In real app, check provider.currentTrack != null
        
        return Align(
          alignment: Alignment.bottomCenter, // Spotify style is bottom, "Dynamic Island" usually top. User said "like spotify" AND "dynamic island". 
          // Compromise: Floating Pill at bottom center (Spotify-like usage, island-like shape).
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Container(
              height: 70,
              width: MediaQuery.of(context).size.width > 600 ? 500 : double.infinity,
              decoration: BoxDecoration(
                color: CyberTheme.surface.withOpacity(0.9),
                borderRadius: BorderRadius.circular(35), // Pill shape
                border: Border.all(color: CyberTheme.primary.withOpacity(0.3)),
                boxShadow: [
                  BoxShadow(
                    color: CyberTheme.primary.withOpacity(0.1),
                    blurRadius: 20,
                    spreadRadius: 2,
                  )
                ],
              ),
              child: Row(
                children: [
                  // Album Art / Visualizer
                  Padding(
                    padding: const EdgeInsets.only(left: 8.0),
                    child: CircleAvatar(
                      radius: 26,
                      backgroundColor: Colors.black,
                      child: Icon(Icons.music_note, color: CyberTheme.primary),
                    ).animate(target: provider.isPlaying ? 1 : 0)
                    .shimmer(duration: 2.seconds, color: CyberTheme.primary.withOpacity(0.5)),
                  ),
                  
                  const SizedBox(width: 12),
                  
                  // Track Info
                  Expanded(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          "Cyberpunk City", // Placeholder
                          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          "Artist Name", // Placeholder
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            fontSize: 12,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
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
                         BoxShadow(color: CyberTheme.primary.withOpacity(0.4), blurRadius: 10)
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
        );
      },
    );
  }
}
