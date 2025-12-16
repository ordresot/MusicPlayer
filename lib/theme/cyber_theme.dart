import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class CyberTheme {
  static const Color background = Color(0xFF050505);
  static const Color surface = Color(0xFF101010);
  static const Color primary = Color(0xFF00F3FF); // Neon Blue
  static const Color secondary = Color(0xFFFF00FF); // Neon Purple
  static const Color tertiary = Color(0xFF00FF9F); // Neon Green
  static const Color error = Color(0xFFFF0055); // Neon Red

  static ThemeData get theme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: background,
      colorScheme: const ColorScheme.dark(
        primary: primary,
        secondary: secondary,
        surface: surface,
        background: background,
        error: error,
        onPrimary: Colors.black,
        onSecondary: Colors.black,
        onSurface: Colors.white,
      ),
      textTheme: TextTheme(
        displayLarge: GoogleFonts.orbitron(
          color: primary,
          fontWeight: FontWeight.bold,
          shadows: [
             Shadow(color: primary.withValues(alpha: 0.5), blurRadius: 10),
          ],
        ),
        displayMedium: GoogleFonts.orbitron(color: Colors.white),
        bodyLarge: GoogleFonts.robotoMono(color: Colors.white70),
        bodyMedium: GoogleFonts.robotoMono(color: Colors.white60),
      ),
      iconTheme: const IconThemeData(color: primary),
      sliderTheme: SliderThemeData(
        activeTrackColor: primary,
        inactiveTrackColor: surface,
        thumbColor: Colors.white,
        overlayColor: primary.withValues(alpha: 0.2),
        trackHeight: 2,
        thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
      ),
      cardTheme: CardThemeData(
        color: surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(4), // Sharp edges for cyberpunk
          side: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
        ),
      ),
    );
  }
}
