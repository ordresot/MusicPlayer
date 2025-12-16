import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../theme/cyber_theme.dart';

class PlayerScreen extends StatelessWidget {
  const PlayerScreen({super.key});

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
              Color(0xFF0F0F0F),
            ],
          ),
        ),
        child: Column(
          children: [
            const Spacer(flex: 1),
            
            // Album Art (Glitch Effect Placeholder)
            Center(
              child: Container(
                width: 300,
                height: 300,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(color: CyberTheme.primary.withOpacity(0.3), blurRadius: 40, spreadRadius: -5),
                    BoxShadow(color: CyberTheme.secondary.withOpacity(0.2), blurRadius: 20, offset: Offset(5, 5)),
                  ],
                  border: Border.all(color: Colors.white.withOpacity(0.1), width: 1),
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(20),
                  child: Container(color: Colors.grey[900], child: Icon(Icons.music_note, size: 100, color: Colors.white24)),
                ),
              ).animate().shimmer(duration: 3.seconds, delay: 1.seconds, color: CyberTheme.primary.withOpacity(0.1)),
            ),
            
            const Spacer(flex: 1),
            
            // Text Info
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0),
              child: Column(
                children: [
                  Text("Neon Nights", style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                    letterSpacing: 1.5
                  )),
                  const SizedBox(height: 8),
                  Text("Cyber Artist - Album 2077", style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                    color: CyberTheme.primary,
                  )),
                ],
              ),
            ),
            
            const SizedBox(height: 32),
            
            // Seek Bar
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0),
              child: Row(
                children: [
                  Text("0:42", style: TextStyle(fontFamily: 'RobotoMono', color: Colors.white54)),
                  Expanded(
                    child: Slider(
                      value: 0.3,
                      onChanged: (v) {},
                    ),
                  ),
                  Text("3:15", style: TextStyle(fontFamily: 'RobotoMono', color: Colors.white54)),
                ],
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Controls
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                IconButton(icon: Icon(Icons.shuffle, color: Colors.white54), onPressed: () {}),
                IconButton(icon: Icon(Icons.skip_previous, size: 40), onPressed: () {}),
                Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    boxShadow: [BoxShadow(color: CyberTheme.primary.withOpacity(0.5), blurRadius: 20)],
                    gradient: LinearGradient(colors: [CyberTheme.primary, CyberTheme.secondary])
                  ),
                  child: Icon(Icons.pause, color: Colors.black, size: 40),
                ).animate(onPlay: (c) => c.repeat(reverse: true))
                 .scale(end: const Offset(1.1, 1.1), duration: 1.seconds)
                 .boxShadow(
                    end: BoxShadow(color: CyberTheme.primary.withValues(alpha: 0.8), blurRadius: 30),
                    duration: 1.seconds,
                 ),
                IconButton(icon: Icon(Icons.skip_next, size: 40), onPressed: () {}),
                IconButton(icon: Icon(Icons.repeat, color: Colors.white54), onPressed: () {}),
              ],
            ),
            
            const Spacer(flex: 2),
            
            // Close Button / Lyrics Indicator
            Icon(Icons.keyboard_arrow_up, color: Colors.white24).animate(onPlay: (c)=>c.repeat()).moveY(begin: 0, end: -5, duration: 1.seconds),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }
}
