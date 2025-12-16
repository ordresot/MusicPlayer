import 'dart:io';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:window_manager/window_manager.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';
import '../widgets/dynamic_island.dart';
import '../providers/player_provider.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _requestOverlayPermission();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Show overlay when app goes to background (paused/inactive)
    if (Platform.isAndroid) {
      if (state == AppLifecycleState.hidden || state == AppLifecycleState.paused) {
         // Show overlay if playing
         final provider = Provider.of<PlayerProvider>(context, listen: false);
         if (provider.isPlaying) {
            await FlutterOverlayWindow.showOverlay(
              enableDrag: true,
              overlayTitle: "Cyber Player",
              overlayContent: "Dynamic Island",
              flag: OverlayFlag.defaultFlag,
              visibility: NotificationVisibility.visibilitySecret,
              positionGravity: PositionGravity.auto,
              height: 100,
              width: WindowSize.matchParent
            );
            
            // Sync current state immediately
            if (provider.currentTrack != null) {
              await FlutterOverlayWindow.shareData({
                "title": provider.currentTrack!.title,
                "isPlaying": true
              });
            }
         }
      } else if (state == AppLifecycleState.resumed) {
        FlutterOverlayWindow.closeOverlay();
      }
    }
  }

  Future<void> _requestOverlayPermission() async {
    if (Platform.isAndroid) {
      final bool status = await FlutterOverlayWindow.isPermissionGranted();
      if (!status) {
        await FlutterOverlayWindow.requestPermission();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          Column(
            children: [
              // Windows Title Bar Drag Area
              if (Platform.isWindows)
                GestureDetector(
                  onPanStart: (details) => windowManager.startDragging(),
                  child: Container(
                    height: 40,
                    color: Colors.black.withValues(alpha: 0.5),
                    alignment: Alignment.centerRight,
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                    child: IconButton(
                      icon: const Icon(Icons.close, size: 20),
                      onPressed: () => windowManager.close(),
                    ),
                  ),
                ),
              Expanded(
                child: Consumer<PlayerProvider>(
                  builder: (context, provider, _) {
                    if (provider.isLoading) {
                       return const Center(child: CircularProgressIndicator(color: Colors.cyan));
                    }
                    return ListView.builder(
                      padding: const EdgeInsets.only(bottom: 100),
                      itemCount: provider.library.length,
                      itemBuilder: (context, index) {
                        final track = provider.library[index];
                        return ListTile(
                          leading: Icon(Icons.music_note, color: Colors.white54),
                          title: Text(track.title, style: TextStyle(color: Colors.white)),
                          subtitle: Text(track.artist, style: TextStyle(color: Colors.white38)),
                          onTap: () => provider.playTrack(track),
                        );
                      },
                    );
                  }
                ),
              ),
            ],
          ),
          
          // Scan Button (Temporary placement)
          Positioned(
            right: 20,
            bottom: 120,
            child: FloatingActionButton(
              backgroundColor: Colors.cyan,
              child: const Icon(Icons.folder_open),
              onPressed: () {
                 Provider.of<PlayerProvider>(context, listen: false).scanFiles("");
              },
            ),
          ),
          
          // Floating Player (Dynamic Island)
          const DynamicIslandPlayer(),
        ],
      ),
    );
  }
}
