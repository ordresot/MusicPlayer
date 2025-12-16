import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:window_manager/window_manager.dart';
import '../widgets/dynamic_island.dart';
import '../providers/player_provider.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          Column(
            children: [
              // Windows Title Bar Drag Area
              GestureDetector(
                onPanStart: (details) => windowManager.startDragging(),
                child: Container(
                  height: 40,
                  color: Colors.black.withOpacity(0.5),
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
                 // Trigger scan
                 // On Windows we usually show dialog.
                 // For now, hardcode a test path or use generic scan
                 // We will ask provider to scan.
                 // Ideally we use file_picker package here to get path.
                 // Let's assume generic android scan for now, or add file_picker later.
                 // Passing empty string triggers android scan in our logic.
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
