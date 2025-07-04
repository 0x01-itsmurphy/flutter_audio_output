import 'dart:async';
import 'package:flutter/services.dart';
import 'flutter_audio_output_platform_interface.dart';

/// Enum representing different audio port types
enum AudioPort {
  /// Unknown audio port (0)
  unknown,

  /// Built-in receiver/earpiece (1)
  receiver,

  /// Built-in speaker (2)
  speaker,

  /// Wired headphones/headset (3)
  headphones,

  /// Bluetooth audio device (4)
  bluetooth,
}

/// Represents an audio input/output device
class AudioInput {
  /// The display name of the audio device
  final String name;

  /// The internal port type identifier
  final int _port;

  /// Gets the audio port type
  AudioPort get port {
    if (_port >= 0 && _port < AudioPort.values.length) {
      return AudioPort.values[_port];
    }
    return AudioPort.unknown;
  }

  /// Creates an AudioInput with the given name and port type
  const AudioInput(this.name, this._port);

  @override
  String toString() {
    return 'AudioInput(name: $name, port: $port)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AudioInput && other.name == name && other._port == _port;
  }

  @override
  int get hashCode => name.hashCode ^ _port.hashCode;
}

/// Main class for Flutter Audio Output plugin
class FlutterAudioOutput {
  static const MethodChannel _channel = MethodChannel('flutter_audio_output');
  static void Function()? _onInputChanged;

  /// Gets the platform version
  static Future<String?> getPlatformVersion() {
    return FlutterAudioOutputPlatform.instance.getPlatformVersion();
  }

  /// Gets the current audio output device
  static Future<AudioInput> getCurrentOutput() async {
    try {
      final dynamic result = await _channel.invokeMethod('getCurrentOutput');
      if (result is List && result.length >= 2) {
        return AudioInput(
            result[0].toString(), int.parse(result[1].toString()));
      }
      return const AudioInput('Unknown', 0);
    } on PlatformException catch (e) {
      throw Exception('Failed to get current output: ${e.message}');
    }
  }

  /// Gets the list of available audio input devices
  static Future<List<AudioInput>> getAvailableInputs() async {
    try {
      final dynamic result = await _channel.invokeMethod('getAvailableInputs');
      if (result is List) {
        return result.map((dynamic data) {
          if (data is List && data.length >= 2) {
            return AudioInput(
                data[0].toString(), int.parse(data[1].toString()));
          }
          return const AudioInput('Unknown', 0);
        }).toList();
      }
      return [];
    } on PlatformException catch (e) {
      throw Exception('Failed to get available inputs: ${e.message}');
    }
  }

  /// Changes audio output to speaker
  static Future<bool> changeToSpeaker() async {
    try {
      final dynamic result = await _channel.invokeMethod('changeToSpeaker');
      return result == true;
    } on PlatformException catch (e) {
      throw Exception('Failed to change to speaker: ${e.message}');
    }
  }

  /// Changes audio output to receiver/earpiece
  static Future<bool> changeToReceiver() async {
    try {
      final dynamic result = await _channel.invokeMethod('changeToReceiver');
      return result == true;
    } on PlatformException catch (e) {
      throw Exception('Failed to change to receiver: ${e.message}');
    }
  }

  /// Changes audio output to headphones
  static Future<bool> changeToHeadphones() async {
    try {
      final dynamic result = await _channel.invokeMethod('changeToHeadphones');
      return result == true;
    } on PlatformException catch (e) {
      throw Exception('Failed to change to headphones: ${e.message}');
    }
  }

  /// Changes audio output to Bluetooth device
  static Future<bool> changeToBluetooth() async {
    try {
      final dynamic result = await _channel.invokeMethod('changeToBluetooth');
      return result == true;
    } on PlatformException catch (e) {
      throw Exception('Failed to change to Bluetooth: ${e.message}');
    }
  }

  /// Sets a listener for audio input changes
  static void setListener(void Function() onInputChanged) {
    _onInputChanged = onInputChanged;
    _channel.setMethodCallHandler(_methodHandle);
  }

  /// Removes the audio input change listener
  static void removeListener() {
    _onInputChanged = null;
    _channel.setMethodCallHandler(null);
  }

  static Future<void> _methodHandle(MethodCall call) async {
    if (_onInputChanged == null) return;
    switch (call.method) {
      case 'inputChanged':
        _onInputChanged!();
        break;
      default:
        break;
    }
  }
}
