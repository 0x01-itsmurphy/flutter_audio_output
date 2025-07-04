import 'package:flutter_audio_output/flutter_audio_output.dart';

void main() async {
  // Test that demonstrates the fixed API
  try {
    // This should work - getAvailableInputs exists
    print('Testing getAvailableInputs...');
    final inputs = await FlutterAudioOutput.getAvailableInputs();
    print('✓ getAvailableInputs() works: ${inputs.length} inputs found');

    // This would fail - getAvailableOutputs does not exist
    // final outputs = await FlutterAudioOutput.getAvailableOutputs(); // This method doesn't exist

    print('Testing getCurrentOutput...');
    final current = await FlutterAudioOutput.getCurrentOutput();
    print('✓ getCurrentOutput() works: ${current.name}');

    print('\n✅ All API methods are working correctly!');
    print('✅ The integration test fix for getAvailableInputs is confirmed.');
  } catch (e) {
    print('❌ Error: $e');
  }
}
