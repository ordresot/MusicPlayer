import 'package:flutter/material.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../theme/cyber_theme.dart';

class SystemDynamicIsland extends StatefulWidget {
  const SystemDynamicIsland({super.key});

  @override
  State<SystemDynamicIsland> createState() => _SystemDynamicIslandState();
}

class _SystemDynamicIslandState extends State<SystemDynamicIsland> {
  String _songTitle = "Void Player";
  bool _isPlaying = false;

  @override
  void initState() {
    super.initState();
    FlutterOverlayWindow.overlayListener.listen((event) {
       if (event is Map) {
         if (event.containsKey('title')) {
           setState(() => _songTitle = event['title']);
         }
         if (event.containsKey('isPlaying')) {
           setState(() => _isPlaying = event['isPlaying']);
         }
       }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: Align(
        alignment: Alignment.topCenter,
        child: Container(
          margin: const EdgeInsets.only(top: 5), // Close to camera
          width: 200,
          height: 35,
          decoration: BoxDecoration(
            color: Colors.black, // Opaque black to match camera hole
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: CyberTheme.primary.withValues(alpha: 0.3), width: 1),
            boxShadow: [
              BoxShadow(
                color: CyberTheme.primary.withValues(alpha: 0.4),
                blurRadius: 10,
                spreadRadius: 1,
              )
            ],
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Visualizer Animation
              if (_isPlaying) ...[
                 Icon(Icons.graphic_eq, color: CyberTheme.primary, size: 14)
                   .animate(onPlay: (c) => c.repeat())
                   .shimmer(duration: 2.seconds, color: Colors.white),
                 const SizedBox(width: 8),
              ],
              
              Flexible(
                child: Text(
                  _songTitle,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 12,
                    fontFamily: 'Orbitron',
                    decoration: TextDecoration.none,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),

               if (_isPlaying) ...[
                 const SizedBox(width: 8),
                 Icon(Icons.circle, color: CyberTheme.secondary, size: 8)
                   .animate(onPlay: (c) => c.repeat(reverse: true))
                   .scale(begin: Offset(0.5, 0.5), end: Offset(1.2, 1.2)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
