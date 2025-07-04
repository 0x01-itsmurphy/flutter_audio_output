import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_audio_output_platform_interface.dart';

/// An implementation of [FlutterAudioOutputPlatform] that uses method channels.
class MethodChannelFlutterAudioOutput extends FlutterAudioOutputPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_audio_output');

  @override
  Future<String?> getPlatformVersion() async {
    try {
      final version =
          await methodChannel.invokeMethod<String>('getPlatformVersion');
      return version;
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print('Error getting platform version: ${e.message}');
      }
      return null;
    }
  }
}
