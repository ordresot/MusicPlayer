import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:flutter/foundation.dart';

class CrashLogger {
  static Future<void> log(Object error, StackTrace? stack) async {
    try {
      final dir = await getApplicationDocumentsDirectory();
      final file = File('${dir.path}/crash_logs.txt');
      
      final timestamp = DateTime.now().toIso8601String();
      final logEntry = '''
--------------------------------------------------
TIMESTAMP: $timestamp
ERROR: $error
STACK TRACE:
$stack
--------------------------------------------------
''';
      
      await file.writeAsString(logEntry, mode: FileMode.append);
      debugPrint("📝 CRASH LOG SAVED TO: ${file.path}");
    } catch (e) {
      debugPrint("FAILED TO WRITE CRASH LOG: $e");
    }
  }

  static Future<String> readLogs() async {
    try {
      final dir = await getApplicationDocumentsDirectory();
      final file = File('${dir.path}/crash_logs.txt');
      if (await file.exists()) {
        return await file.readAsString();
      }
    } catch (_) {}
    return "No logs found.";
  }
}
