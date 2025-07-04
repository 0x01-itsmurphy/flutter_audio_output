import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_audio_output_method_channel.dart';

/// The interface that implementations of flutter_audio_output must implement.
///
/// Platform implementations should extend this class rather than implement it
/// as `FlutterAudioOutput` does not consider newly added methods to be breaking
/// changes. Extending this class (using `extends`) ensures that the subclass
/// will get the default implementation, while platform implementations that
/// `implements` this interface will be broken by newly added
/// [FlutterAudioOutputPlatform] methods.
abstract class FlutterAudioOutputPlatform extends PlatformInterface {
  /// Constructs a FlutterAudioOutputPlatform.
  FlutterAudioOutputPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterAudioOutputPlatform _instance =
      MethodChannelFlutterAudioOutput();

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

  /// Gets the platform version.
  Future<String?> getPlatformVersion() {
    throw UnimplementedError('getPlatformVersion() has not been implemented.');
  }
}
