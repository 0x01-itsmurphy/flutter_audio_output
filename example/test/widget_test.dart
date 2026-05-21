import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_audio_output_example/main.dart';

void main() {
  const MethodChannel channel = MethodChannel('flutter_audio_output');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      switch (methodCall.method) {
        case 'getPlatformVersion':
          return 'Android 34';
        case 'getCurrentOutput':
          return ['Speaker', '2'];
        case 'getAvailableInputs':
          return [
            ['Receiver', '1'],
            ['Speaker', '2'],
          ];
        default:
          return null;
      }
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  testWidgets('Audio Output example app smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();

    // Verify that the app title is displayed.
    expect(find.text('Flutter Audio Output'), findsOneWidget);

    // Verify that the Audio Controls header is displayed.
    expect(find.text('Audio Controls'), findsOneWidget);

    // Verify that the current output device (Speaker) is displayed.
    expect(find.text('Speaker'), findsAtLeastNWidgets(1));
  });
}
