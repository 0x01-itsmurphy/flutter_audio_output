# Flutter Audio Output Plugin ProGuard Rules

# Keep all Flutter Audio Output classes
-keep class com.itsmurphy.flutter_audio_output.** { *; }

# Keep AudioManager related classes
-keep class android.media.AudioManager { *; }
-keep class android.media.AudioDeviceInfo { *; }

# Keep method channel related classes
-keep class io.flutter.plugin.common.MethodChannel { *; }
-keep class io.flutter.plugin.common.MethodChannel$MethodCallHandler { *; }
-keep class io.flutter.plugin.common.MethodChannel$Result { *; }
-keep class io.flutter.plugin.common.MethodCall { *; }

# Keep FlutterPlugin interface
-keep class io.flutter.embedding.engine.plugins.FlutterPlugin { *; }
