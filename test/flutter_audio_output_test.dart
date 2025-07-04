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
  final FlutterAudioOutputPlatform initialPlatform =
      FlutterAudioOutputPlatform.instance;

  test('$MethodChannelFlutterAudioOutput is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterAudioOutput>());
  });

  test('getPlatformVersion', () async {
    MockFlutterAudioOutputPlatform fakePlatform =
        MockFlutterAudioOutputPlatform();
    FlutterAudioOutputPlatform.instance = fakePlatform;

    expect(await FlutterAudioOutput.getPlatformVersion(), '42');
  });

  test('AudioInput equality', () {
    const input1 = AudioInput('Speaker', 2);
    const input2 = AudioInput('Speaker', 2);
    const input3 = AudioInput('Receiver', 1);

    expect(input1, equals(input2));
    expect(input1, isNot(equals(input3)));
  });

  test('AudioInput port property', () {
    const input = AudioInput('Speaker', 2);
    expect(input.port, AudioPort.speaker);
  });

  test('AudioInput toString', () {
    const input = AudioInput('Speaker', 2);
    expect(
        input.toString(), 'AudioInput(name: Speaker, port: AudioPort.speaker)');
  });
}
