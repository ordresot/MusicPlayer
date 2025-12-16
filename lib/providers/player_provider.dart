import 'package:flutter/material.dart';

class PlayerProvider extends ChangeNotifier {
  bool _isPlaying = false;
  double _volume = 1.0;
  
  bool get isPlaying => _isPlaying;
  double get volume => _volume;

  void togglePlay() {
    _isPlaying = !_isPlaying;
    notifyListeners();
  }

  void setVolume(double v) {
    _volume = v;
    notifyListeners();
  }
}
