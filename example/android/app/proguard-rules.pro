# Flutter Audio Output Example ProGuard Rules

# Keep all Flutter classes
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }

# Keep Flutter Audio Output Plugin classes
-keep class com.itsmurphy.flutter_audio_output.** { *; }

# Keep AudioManager related classes
-keep class android.media.AudioManager { *; }
-keep class android.media.AudioDeviceInfo { *; }

# Keep just_audio plugin classes (if any native parts)
-keep class com.ryanheise.just_audio.** { *; }

# General Android rules
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes that are referenced by Flutter
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

# Keep custom application class if any
-keep public class * extends android.app.Application

# Obfuscation optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
