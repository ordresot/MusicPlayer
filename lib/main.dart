import 'dart:io';
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:media_kit/media_kit.dart';
import 'package:window_manager/window_manager.dart';

// Internal Service Imports
import 'theme/cyber_theme.dart';
import 'services/audio_handler.dart'; 
import 'services/crash_logger.dart';
import 'providers/player_provider.dart';

// Screens
import 'screens/home_screen.dart';
import 'widgets/system_dynamic_island.dart';

void main() async {
  // Global Error Trap (The Black Box)
  runZonedGuarded(() async {
    WidgetsFlutterBinding.ensureInitialized();
    
    // Initialize Native Media Engine
    MediaKit.ensureInitialized();
    
    // Initialize Audio Service (Background & Notifications)
    await initAudioService();

    // Desktop: Window Management
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

    // Launch Application
    runApp(
      MultiProvider(
        providers: [
          ChangeNotifierProvider(create: (_) => PlayerProvider()),
        ],
        child: const CyberMusicApp(),
      ),
    );

  }, (error, stack) {
    // Catch-All for Fatal Errors
    debugPrint("🔥 FATAL ERROR: $error");
    CrashLogger.log(error, stack);
  });
}

// Overlay Entry Point (Background/PiP)
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
      title: 'Void Player',
      debugShowCheckedModeBanner: false,
      theme: CyberTheme.theme,
      home: const HomeScreen(),
    );
  }
}
