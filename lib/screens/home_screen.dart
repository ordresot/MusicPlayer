import 'package:flutter/material.dart';
import 'package:window_manager/window_manager.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
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
          const Expanded(
            child: Center(
              child: Text(
                'CYBER PLAYER SYSTEM ONLINE',
                style: TextStyle(fontSize: 24, letterSpacing: 2),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
