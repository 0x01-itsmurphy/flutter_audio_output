# Flutter Audio Output Example

A comprehensive example demonstrating how to use the `flutter_audio_output` plugin to manage audio output routing in Flutter applications.

## Features Demonstrated

- 🎵 **Audio Playback Control** - Play, pause, and stop audio
- 📱 **Current Output Detection** - Display the currently active audio output device
- 🔄 **Device Switching** - Switch between different audio output devices
- 👂 **Real-time Updates** - Listen to audio device changes and update UI accordingly
- 🎨 **Modern UI Design** - Clean, intuitive interface with Material Design 3
- 🛡️ **Error Handling** - Comprehensive error handling and user feedback
- 📊 **Device Information** - Display detailed information about available audio devices

## Supported Audio Devices

- **📞 Receiver/Earpiece** - Built-in earpiece for private listening
- **🔊 Speaker** - Built-in speaker for hands-free audio
- **🎧 Headphones** - Wired headphones or headset
- **📶 Bluetooth** - Wireless Bluetooth audio devices

## How to Run

1. **Prerequisites**:
   - Flutter 3.0+ 
   - Dart 2.19+
   - Android device with API 21+ (Android 5.0+)
   - iOS device with iOS 12.0+

2. **Installation**:
   ```bash
   cd example
   flutter pub get
   ```

3. **Run the app**:
   ```bash
   flutter run
   ```

## What the Example Shows

### 1. Audio Playback Controls
- Play/Pause/Stop buttons for controlling audio playback
- Real-time playback state updates
- Uses `just_audio` plugin for audio playback

### 2. Current Output Display
- Shows the currently active audio output device
- Displays device name and type
- Visual indicators with device-specific icons and colors

### 3. Available Devices List
- Lists all available audio output devices
- Interactive tap-to-switch functionality
- Visual indicators for the currently selected device
- Color-coded device types for easy identification

### 4. Quick Action Buttons
- Quick buttons for common device switches
- Direct access to Receiver and Speaker switching
- Visual feedback for loading states

### 5. Real-time Updates
- Automatic UI updates when audio devices change
- Notification system for device changes
- Error handling and user feedback

## Code Structure

```
example/
├── lib/
│   └── main.dart          # Main application with comprehensive UI
├── assets/
│   └── Sunflower.mp3      # Sample audio file for testing
├── pubspec.yaml           # Dependencies and configuration
├── analysis_options.yaml  # Linting rules
└── README.md             # This file
```

## Key Implementation Details

### Audio Device Management
```dart
// Get current output device
final current = await FlutterAudioOutput.getCurrentOutput();

// Get available devices
final devices = await FlutterAudioOutput.getAvailableInputs();

// Switch to specific device
await FlutterAudioOutput.changeToSpeaker();
await FlutterAudioOutput.changeToReceiver();
```

### Real-time Updates
```dart
// Listen to device changes
FlutterAudioOutput.setListener(() async {
  await _refreshAudioDevices();
  _showSnackBar('Audio device changed');
});

// Clean up listener
FlutterAudioOutput.removeListener();
```

### Error Handling
```dart
try {
  final success = await FlutterAudioOutput.changeToSpeaker();
  if (success) {
    _showSnackBar('Successfully switched to Speaker');
  }
} catch (e) {
  _showError('Failed to switch device: $e');
}
```

## UI Components

### Device Icons and Colors
- **📞 Receiver**: Blue phone icon
- **🔊 Speaker**: Orange volume icon  
- **🎧 Headphones**: Purple headphones icon
- **📶 Bluetooth**: Indigo bluetooth icon

### Interactive Elements
- **Device Cards**: Tap to switch to that device
- **Quick Actions**: One-tap switching for common devices
- **Audio Controls**: Play/pause/stop audio playback
- **Refresh Button**: Manually refresh device list

## Testing the Example

1. **Basic Functionality**:
   - Play audio and verify it comes from the expected device
   - Switch between different audio devices
   - Test device change detection by plugging/unplugging headphones

2. **Error Scenarios**:
   - Try switching to unavailable devices
   - Test behavior when no devices are available
   - Verify error messages are displayed correctly

3. **State Management**:
   - Verify UI updates correctly when devices change
   - Test app behavior during device transitions
   - Ensure proper cleanup when app is closed

## Dependencies

- `flutter_audio_output`: The main plugin being demonstrated
- `just_audio`: For audio playback functionality
- `cupertino_icons`: For iOS-style icons

## Platform-Specific Notes

### Android
- Requires permissions for audio recording (handled automatically)
- Some devices may have additional audio routing restrictions
- Bluetooth devices may require pairing before appearing in the list

### iOS
- Audio session configuration is handled automatically
- Receiver switching works correctly with the fixed implementation
- Background audio playback follows iOS guidelines

## Troubleshooting

### Common Issues

1. **No audio playback**: 
   - Check device volume settings
   - Verify audio file is properly loaded
   - Ensure device permissions are granted

2. **Device switching not working**:
   - Verify device is actually available
   - Check for any error messages in the console
   - Try refreshing the device list

3. **UI not updating**:
   - Ensure listener is properly set up
   - Check for any state management issues
   - Verify widget is properly disposed

### Debug Information

The example app displays:
- Platform version information
- Current device details
- Available device list
- Error messages when they occur
- Loading states during operations

This comprehensive example demonstrates all the capabilities of the `flutter_audio_output` plugin in a real-world scenario with proper error handling, modern UI design, and best practices for Flutter development.
