# flutter_audio_output

[![pub package](https://img.shields.io/pub/v/flutter_audio_output.svg)](https://pub.dev/packages/flutter_audio_output)
[![Flutter](https://img.shields.io/badge/Flutter-3.0%2B-blue.svg)](https://flutter.dev/)
[![Android](https://img.shields.io/badge/Android-21%2B-green.svg)](https://developer.android.com/)
[![iOS](https://img.shields.io/badge/iOS-12.0%2B-blue.svg)](https://developer.apple.com/ios/)

A Flutter plugin for managing audio output routing, providing seamless control over audio devices.

## Features

- 🎵 **Get current audio output device**
- 🔄 **Switch between audio devices** (Speaker, Receiver, Headphones, Bluetooth)
- 👂 **Listen to audio device changes**
- 🛡️ **Robust error handling** with descriptive messages
- 🚀 **Modern Android and iOS support**
- 📱 **Null safety** and type-safe APIs

## Supported Platforms

- **Android**: API 21+ (Android 5.0+)
- **iOS**: 12.0+
- **Flutter**: 3.0+
- **Dart**: 2.19+

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  flutter_audio_output: ^0.0.3
```

## Usage

### Basic Usage

```dart
import 'package:flutter_audio_output/flutter_audio_output.dart';

// Get current audio output
final currentOutput = await FlutterAudioOutput.getCurrentOutput();
print('Current output: ${currentOutput.name} (${currentOutput.port})');

// Get available audio devices
final availableDevices = await FlutterAudioOutput.getAvailableInputs();
for (final device in availableDevices) {
  print('Available: ${device.name} (${device.port})');
}

// Switch to speaker
final success = await FlutterAudioOutput.changeToSpeaker();
if (success) {
  print('Successfully switched to speaker');
}
```

### Listen to Audio Device Changes

```dart
class AudioOutputExample extends StatefulWidget {
  @override
  _AudioOutputExampleState createState() => _AudioOutputExampleState();
}

class _AudioOutputExampleState extends State<AudioOutputExample> {
  AudioInput? _currentOutput;
  List<AudioInput> _availableDevices = [];

  @override
  void initState() {
    super.initState();
    _initAudioOutput();
  }

  Future<void> _initAudioOutput() async {
    // Set up listener for audio device changes
    FlutterAudioOutput.setListener(() async {
      await _refreshAudioDevices();
    });

    // Initial load
    await _refreshAudioDevices();
  }

  Future<void> _refreshAudioDevices() async {
    try {
      final current = await FlutterAudioOutput.getCurrentOutput();
      final available = await FlutterAudioOutput.getAvailableInputs();
      
      setState(() {
        _currentOutput = current;
        _availableDevices = available;
      });
    } catch (e) {
      print('Error refreshing audio devices: $e');
    }
  }

  @override
  void dispose() {
    // Clean up the listener
    FlutterAudioOutput.removeListener();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Audio Output Control')),
      body: Column(
        children: [
          // Current output display
          if (_currentOutput != null)
            ListTile(
              title: Text('Current Output'),
              subtitle: Text('${_currentOutput!.name} (${_currentOutput!.port})'),
              leading: Icon(_getIconForPort(_currentOutput!.port)),
            ),
          
          Divider(),
          
          // Available devices
          Expanded(
            child: ListView.builder(
              itemCount: _availableDevices.length,
              itemBuilder: (context, index) {
                final device = _availableDevices[index];
                return ListTile(
                  title: Text(device.name),
                  subtitle: Text(device.port.toString()),
                  leading: Icon(_getIconForPort(device.port)),
                  onTap: () => _switchToDevice(device.port),
                );
              },
            ),
          ),
          
          // Quick action buttons
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ElevatedButton.icon(
                  onPressed: () => _switchToDevice(AudioPort.receiver),
                  icon: Icon(Icons.phone),
                  label: Text('Receiver'),
                ),
                ElevatedButton.icon(
                  onPressed: () => _switchToDevice(AudioPort.speaker),
                  icon: Icon(Icons.volume_up),
                  label: Text('Speaker'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  IconData _getIconForPort(AudioPort port) {
    switch (port) {
      case AudioPort.receiver:
        return Icons.phone;
      case AudioPort.speaker:
        return Icons.volume_up;
      case AudioPort.headphones:
        return Icons.headphones;
      case AudioPort.bluetooth:
        return Icons.bluetooth;
      default:
        return Icons.device_unknown;
    }
  }

  Future<void> _switchToDevice(AudioPort port) async {
    try {
      bool success = false;
      
      switch (port) {
        case AudioPort.receiver:
          success = await FlutterAudioOutput.changeToReceiver();
          break;
        case AudioPort.speaker:
          success = await FlutterAudioOutput.changeToSpeaker();
          break;
        case AudioPort.headphones:
          success = await FlutterAudioOutput.changeToHeadphones();
          break;
        case AudioPort.bluetooth:
          success = await FlutterAudioOutput.changeToBluetooth();
          break;
        default:
          return;
      }
      
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Switched to ${port.toString().split('.').last}')),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to switch to ${port.toString().split('.').last}')),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: $e')),
      );
    }
  }
}
```

## API Reference

### Methods

#### `getCurrentOutput()`
Returns the current audio output device.

```dart
Future<AudioInput> getCurrentOutput()
```

#### `getAvailableInputs()`
Returns a list of available audio input/output devices.

```dart
Future<List<AudioInput>> getAvailableInputs()
```

#### `changeToSpeaker()`
Switches audio output to the built-in speaker.

```dart
Future<bool> changeToSpeaker()
```

#### `changeToReceiver()`
Switches audio output to the built-in receiver (earpiece).

```dart
Future<bool> changeToReceiver()
```

#### `changeToHeadphones()`
Switches audio output to wired headphones (if connected).

```dart
Future<bool> changeToHeadphones()
```

#### `changeToBluetooth()`
Switches audio output to Bluetooth audio device (if connected).

```dart
Future<bool> changeToBluetooth()
```

#### `setListener()`
Sets a listener for audio device changes.

```dart
void setListener(void Function() onInputChanged)
```

#### `removeListener()`
Removes the audio device change listener.

```dart
void removeListener()
```

### Data Types

#### `AudioInput`
Represents an audio input/output device.

```dart
class AudioInput {
  final String name;        // Display name of the device
  final AudioPort port;     // Type of audio port
}
```

#### `AudioPort`
Enum representing different audio port types.

```dart
enum AudioPort {
  unknown,      // Unknown device
  receiver,     // Built-in receiver (earpiece)
  speaker,      // Built-in speaker
  headphones,   // Wired headphones
  bluetooth,    // Bluetooth audio device
}
```

## Error Handling

The plugin provides comprehensive error handling with descriptive error messages:

```dart
try {
  final result = await FlutterAudioOutput.changeToSpeaker();
  if (result) {
    print('Successfully switched to speaker');
  } else {
    print('Failed to switch to speaker');
  }
} catch (e) {
  print('Error switching to speaker: $e');
  // Handle the error appropriately
}
```

## Platform-Specific Notes

### Android
- Requires API level 21 (Android 5.0) or higher
- Uses modern `AudioDeviceInfo` API for device detection on API 23+
- Automatically falls back to legacy APIs on older devices
- Properly handles audio focus and routing

### iOS
- Requires iOS 12.0 or higher
- Uses `AVAudioSession` for audio routing
- Automatically configures audio session for optimal performance
- Handles audio route changes and notifications

## Migration from 0.0.2

### Breaking Changes
- Minimum Flutter version increased to 3.0.0
- Minimum Dart version increased to 2.19.0
- Android minimum SDK increased to 21
- iOS minimum version increased to 12.0

### New Features
- Added `removeListener()` method
- Enhanced error handling with exceptions
- Improved type safety and null safety
- Better documentation and examples

### Migration Steps
1. Update your Flutter and Dart versions
2. Update minimum SDK versions in your Android and iOS configurations
3. Add proper error handling for the new exception-based error reporting
4. Use `removeListener()` to clean up listeners in `dispose()` methods

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a detailed list of changes.

## Complete Example

```dart
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late AudioPlayer _audioPlayer;
  final logger = Logger();

  late AudioInput _currentInput = const AudioInput("unknown", 0);
  late List<AudioInput> _availableInputs = [];
  bool isSpeaker = true;

  @override
  void initState() {
    _audioPlayer = AudioPlayer();
    _audioPlayer
        .setAudioSource(AudioSource.uri(
      Uri.parse('asset:///assets/Sunflower.mp3'),
    ))
        .catchError((error) {
      print("An error occured $error");
    });
    _audioPlayer.play();

    init();
    super.initState();
  }

  Future<void> init() async {
    FlutterAudioOutput.setListener(() async {
      logger.d("-----changed-------");
      await _getInput();
      setState(() {});
    });

    await _getInput();
    if (!mounted) return;
    setState(() {});
  }

  _getInput() async {
    _currentInput = await FlutterAudioOutput.getCurrentOutput();
    logger.w("current:$_currentInput");
    _availableInputs = await FlutterAudioOutput.getAvailableInputs();
    logger.w("available $_availableInputs");
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Column(
            children: <Widget>[
              Text(
                "current output:${_currentInput.name} ${_currentInput.port}",
              ),
              Divider(),
              Expanded(
                child: ListView.builder(
                  itemBuilder: (_, index) {
                    AudioInput input = _availableInputs[index];
                    return Row(
                      children: <Widget>[
                        Expanded(child: Text(input.name)),
                        Expanded(child: Text("${input.port}")),
                      ],
                    );
                  },
                  itemCount: _availableInputs.length,
                ),
              ),
              Container(
                height: 400,
                child: Row(
                  children: [
                    IconButton(
                      onPressed: () {
                        _audioPlayer.pause();
                      },
                      icon: Icon(Icons.pause_circle),
                    ),
                    IconButton(
                      onPressed: () {
                        _audioPlayer.play();
                      },
                      icon: Icon(Icons.play_circle),
                    ),
                    IconButton(
                      onPressed: () async {
                        await _getInput();
                        if (_currentInput.port == AudioPort.speaker) {
                          isSpeaker =
                              await FlutterAudioOutput.changeToReceiver();
                          setState(() {
                            isSpeaker = false;
                          });
                          print("change to Receiver :$isSpeaker");
                        } else {
                          isSpeaker =
                              await FlutterAudioOutput.changeToSpeaker();
                          setState(() {
                            isSpeaker = true;
                          });
                          print("change to Speaker :$isSpeaker");
                        }
                      },
                      icon: isSpeaker == false
                          ? const Icon(
                              Icons.volume_off,
                              color: Colors.black,
                            )
                          : const Icon(
                              Icons.volume_up,
                              color: Colors.black,
                            ),
                    ),
                  ],
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}
```
