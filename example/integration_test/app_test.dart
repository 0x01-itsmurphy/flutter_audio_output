import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:flutter_audio_output_example/main.dart' as app;

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('smoke test', (WidgetTester tester) async {
    app.main();
    await tester.pumpAndSettle();
    expect(find.text('Audio Controls'), findsOneWidget);
  });
}
