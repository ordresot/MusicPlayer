import 'package:window_manager/window_manager.dart';
import '../widgets/dynamic_island.dart';

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
                child: ListView.builder(
                  padding: const EdgeInsets.only(bottom: 100), // Space for island
                  itemCount: 20,
                  itemBuilder: (context, index) {
                    return ListTile(
                      leading: Icon(Icons.music_note, color: Colors.white54),
                      title: Text("Track #$index", style: TextStyle(color: Colors.white)),
                      subtitle: Text("Artist Name", style: TextStyle(color: Colors.white38)),
                      onTap: () {
                         // TODO: Play track
                      },
                    );
                  },
                ),
              ),
            ],
          ),
          
          // Floating Player (Dynamic Island)
          const DynamicIslandPlayer(),
        ],
      ),
    );
  }
}
