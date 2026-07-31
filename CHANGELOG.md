## 1.1.0

### Dart
- Add `FlutterAudioOutput.release()` to cancel pending Android route changes and clear routing state owned by the plugin. The method is a no-op on iOS.

### Android
- Use `setCommunicationDevice()` on Android 12 and newer, while retaining speakerphone and Bluetooth SCO fallbacks on Android 5 through 11.
- Complete Android 12+ route-change futures after the platform confirms or rejects the requested device; legacy Bluetooth changes complete after SCO connects, fails, or times out.
- Make routing lifecycle-safe: superseded and failed requests roll back partial changes, while `release()` and engine detachment clean up only the communication device, audio mode, speakerphone, and SCO state owned by this plugin.
- Derive current and available routes from modern device APIs, including wired, USB, Bluetooth LE, and hearing-aid devices where Android exposes them.
- Declare the `MODIFY_AUDIO_SETTINGS` permission required by Android routing APIs.

### iOS
- Defer `AVAudioSession` configuration and activation until an output change is explicitly requested, preventing plugin registration and read-only queries from interrupting existing audio.

### Testing and tooling
- Add native Android routing tests for API 21, legacy, and Android 12+ behavior and run them in CI.
- Align CI and publishing workflows with Flutter 3.44.5.

## 1.0.7

### 🔧 CI/CD & Infrastructure
- **Reusable Workflows**: Added `workflow_call:` trigger to `ci.yml` allowing reusable workflow execution within `publish.yml`.
- **Global Executables**: Standardized execution of `pana` in GitHub Actions via `dart pub global run pana` to resolve runner tool resolution issues.

### 🧪 Testing & Reliability
- **Integration Tests**: Added the missing integration test suite (`example/integration_test/app_test.dart` and `example/test_driver/integration_test.dart`) required by the CI workflows.
- **Example Tests**: Modernized example application unit tests using mocked method channels (`getPlatformVersion`, `getCurrentOutput`, and `getAvailableInputs`) to isolate platform calls during widget testing.

### 🧹 Code Quality & Compatibility
- **Lints & Deprecations**: Fixed modern Flutter SDK lints by replacing deprecated `withOpacity(0.1)` color modifiers with `withAlpha(26)`.

## 1.0.6

### 🚀 New Features & Enhancements
- **Environment**: Upgraded platform constraints fully supporting Dart 3 (`sdk: ">=3.0.0 <4.0.0"`) and Flutter `>=3.3.0`.
- **iOS**: Explicitly re-asserts `AVAudioSession` `playAndRecord` coupled with `voiceChat` mode when changing output devices, permanently solving iOS receiver failures when media players force `playback` mode.
- **iOS**: Elevated iOS integration target permanently to 13.0 mimicking standard native app defaults.

### 🐛 Bug Fixes & Stability
- **Android**: Closed modern Gradle compatibility crashes by officially embedding required namespaces natively bridging Android Studio Flamingo+ features and enforcing SDK 36 validation.
- **Android**: Updated Internal Gradle Plugin safely to `8.6.0` paired with Kotlin `2.1.0`.
- **Android**: Suppressed implicit legacy API build warnings for backwards-compatible hardware routines ensuring 100% spotless compilation cycles.
- **All**: Reconstructed legacy evaluation app with fully native 2026-standard architecture and dropped outdated platform bindings.

## 0.0.4### 🐛 Bug Fixes
- **Integration Tests**: Fixed `getAvailableOutputs()` method call to use correct `getAvailableInputs()` method
- **Example App**: Updated widget tests to match modernized UI
- **Android**: Fixed Gradle configuration for modern Flutter versions
- **Code Quality**: Improved integration test reliability and error handling

### 🔧 Improvements
- **Tests**: Enhanced integration test coverage and stability
- **Documentation**: Updated API documentation for clarity
- **CI/CD**: Refined automated testing and publishing workflows

## 0.0.3

### 🔥 Breaking Changes
- **Android**: Updated minimum SDK version to 21 (Android 5.0)
- **iOS**: Updated minimum iOS version to 12.0
- **Flutter**: Updated minimum Flutter version to 3.0.0
- **Dart**: Updated minimum Dart version to 2.19.0

### 🚀 New Features
- **Android**: Added support for modern Android audio APIs (API 23+)
- **iOS**: Fixed `changeToReceiver()` method not working properly
- **All**: Added proper error handling with descriptive error messages
- **All**: Added `removeListener()` method to properly clean up listeners
- **All**: Improved audio device detection and availability checking

### 🐛 Bug Fixes
- **iOS**: Fixed `changeToReceiver()` using wrong API calls
- **iOS**: Fixed method routing bug where `changeToHeadphones` was calling `changeToBluetooth`
- **Android**: Fixed deprecated `isWiredHeadsetOn()` method usage
- **Android**: Fixed memory leaks and proper cleanup in `onDetachedFromEngine`
- **Android**: Fixed non-static method calls and thread safety issues

### 🔧 Improvements
- **Android**: Added Gradle 8.8 compatibility
- **Android**: Added namespace declaration for Android Gradle Plugin 8.x
- **Android**: Updated Android Gradle Plugin to 8.1.4
- **Android**: Updated Kotlin version to 1.8.22
- **Android**: Updated compileSdk to 34
- **Android**: Added ProGuard rules for proper code obfuscation
- **iOS**: Added proper audio session configuration
- **iOS**: Updated iOS deployment target to 12.0
- **iOS**: Improved Swift version compatibility (5.0)
- **All**: Added comprehensive documentation and code comments
- **All**: Improved code quality with modern linting rules
- **All**: Added proper null safety and type checking
- **All**: Enhanced error handling and debugging information

### 📦 Dependencies
- Updated `plugin_platform_interface` to ^2.1.7
- Updated `flutter_lints` to ^3.0.0
- Updated various Android and iOS dependencies

### 🔧 Development
- Added strict analysis options for better code quality
- Added comprehensive ProGuard rules for Android
- Improved CI/CD compatibility with modern build tools
- Enhanced documentation and code examples

## 0.0.2

-   audio source switching control
