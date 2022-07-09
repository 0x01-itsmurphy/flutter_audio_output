import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_audio_output/flutter_audio_output.dart';
import 'package:flutter_audio_output/flutter_audio_output_platform_interface.dart';
import 'package:flutter_audio_output/flutter_audio_output_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterAudioOutputPlatform 
    with MockPlatformInterfaceMixin
    implements FlutterAudioOutputPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FlutterAudioOutputPlatform initialPlatform = FlutterAudioOutputPlatform.instance;

  test('$MethodChannelFlutterAudioOutput is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterAudioOutput>());
  });

  test('getPlatformVersion', () async {
    FlutterAudioOutput flutterAudioOutputPlugin = FlutterAudioOutput();
    MockFlutterAudioOutputPlatform fakePlatform = MockFlutterAudioOutputPlatform();
    FlutterAudioOutputPlatform.instance = fakePlatform;
  
    expect(await FlutterAudioOutput.getPlatformVersion(), '42');
  });
}
