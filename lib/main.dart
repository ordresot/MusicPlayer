import 'dart:io';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:window_manager/window_manager.dart';
import 'theme/cyber_theme.dart';
import 'providers/player_provider.dart';
import 'screens/home_screen.dart';
import 'widgets/system_dynamic_island.dart';
import 'services/audio_handler.dart'; // Import the handler

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initAudioService(); // Initialize Singleton Audio Handler

  
  // Desktop Window Management
  if (Platform.isWindows || Platform.isLinux || Platform.isMacOS) {
    await windowManager.ensureInitialized();
    WindowOptions windowOptions = const WindowOptions(
      size: Size(1200, 800),
      center: true,
      backgroundColor: Colors.transparent,
      skipTaskbar: false,
      titleBarStyle: TitleBarStyle.hidden,
    );
    
    windowManager.waitUntilReadyToShow(windowOptions, () async {
      await windowManager.show();
      await windowManager.focus();
    });
  }

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => PlayerProvider()),
      ],
      child: const CyberMusicApp(),
    ),
  );
}

// Overlay Entry Point
@pragma("vm:entry-point")
void overlayMain() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    MaterialApp(
      debugShowCheckedModeBanner: false,
      home: const SystemDynamicIsland(),
    ),
  );
}

class CyberMusicApp extends StatelessWidget {
  const CyberMusicApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Cyber Music',
      debugShowCheckedModeBanner: false,
      theme: CyberTheme.theme,
      home: const HomeScreen(),
    );
  }
}
