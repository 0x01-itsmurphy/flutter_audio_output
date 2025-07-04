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
