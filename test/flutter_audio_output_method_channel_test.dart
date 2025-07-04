import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_audio_output/flutter_audio_output_method_channel.dart';

void main() {
  MethodChannelFlutterAudioOutput platform = MethodChannelFlutterAudioOutput();
  const MethodChannel channel = MethodChannel('flutter_audio_output');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      switch (methodCall.method) {
        case 'getPlatformVersion':
          return '42';
        default:
          return null;
      }
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });

  test('getPlatformVersion with error', () async {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      throw PlatformException(
        code: 'ERROR',
        message: 'Test error',
      );
    });

    expect(await platform.getPlatformVersion(), isNull);
  });
}
