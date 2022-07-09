import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_audio_output_method_channel.dart';

abstract class FlutterAudioOutputPlatform extends PlatformInterface {
  /// Constructs a FlutterAudioOutputPlatform.
  FlutterAudioOutputPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterAudioOutputPlatform _instance = MethodChannelFlutterAudioOutput();

  /// The default instance of [FlutterAudioOutputPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterAudioOutput].
  static FlutterAudioOutputPlatform get instance => _instance;
  
  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterAudioOutputPlatform] when
  /// they register themselves.
  static set instance(FlutterAudioOutputPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
