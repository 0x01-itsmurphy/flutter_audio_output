package com.itsmurphy.flutter_audio_output;

import androidx.annotation.NonNull;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioDeviceInfo;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** FlutterAudioOutputPlugin */
public class FlutterAudioOutputPlugin implements FlutterPlugin, MethodCallHandler {
  private static final String TAG = "FlutterAudioOutput";
  
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private AudioManager audioManager;
  private Context context;
  private AudioChangeReceiver audioChangeReceiver;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flutter_audio_output");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    
    // Register audio change receiver
    audioChangeReceiver = new AudioChangeReceiver(listener);
    IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
    context.registerReceiver(audioChangeReceiver, filter);
  }

  private final AudioEventListener listener = new AudioEventListener() {
    @Override
    public void onChanged() {
      if (channel != null) {
        channel.invokeMethod("inputChanged", 1);
      }
    }
  };

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("getCurrentOutput")) {
      result.success(getCurrentOutput());
    } else if (call.method.equals("getAvailableInputs")) {
      result.success(getAvailableInputs());
    } else if (call.method.equals("changeToReceiver")) {
      result.success(changeToReceiver());
    } else if (call.method.equals("changeToSpeaker")) {
      result.success(changeToSpeaker());
    } else if (call.method.equals("changeToHeadphones")) {
      result.success(changeToHeadphones());
    } else if (call.method.equals("changeToBluetooth")) {
      result.success(changeToBluetooth());
    } else {
      result.notImplemented();
    }
  }

  private Boolean changeToReceiver() {
    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    audioManager.stopBluetoothSco();
    audioManager.setBluetoothScoOn(false);
    audioManager.setSpeakerphoneOn(false);
    listener.onChanged();
    return true;
  }

  private Boolean changeToSpeaker() {
    audioManager.setMode(AudioManager.MODE_NORMAL);
    audioManager.stopBluetoothSco();
    audioManager.setBluetoothScoOn(false);
    audioManager.setSpeakerphoneOn(true);
    listener.onChanged();
    return true;
  }

  private Boolean changeToHeadphones() {
    return changeToReceiver();
  }

  private Boolean changeToBluetooth() {
    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    audioManager.startBluetoothSco();
    audioManager.setBluetoothScoOn(true);
    listener.onChanged();
    return true;
  }

  private List<String> getCurrentOutput() {
    List<String> info = new ArrayList<>();
    
    if (audioManager.isSpeakerphoneOn()) {
      info.add("Speaker");
      info.add("2");
    } else if (audioManager.isBluetoothScoOn()) {
      info.add("Bluetooth");
      info.add("4");
    } else if (isWiredHeadsetOn()) {
      info.add("Headset");
      info.add("3");
    } else {
      info.add("Receiver");
      info.add("1");
    }
    return info;
  }

  private boolean isWiredHeadsetOn() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
      for (AudioDeviceInfo deviceInfo : audioDevices) {
        if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
          return true;
        }
      }
      return false;
    } else {
      // Fallback for older versions
      return audioManager.isWiredHeadsetOn();
    }
  }

  private List<List<String>> getAvailableInputs() {
    List<List<String>> list = new ArrayList<>();
    list.add(Arrays.asList("Receiver", "1"));
    list.add(Arrays.asList("Speaker", "2"));
    
    if (isWiredHeadsetOn()) {
      list.add(Arrays.asList("Headset", "3"));
    }
    
    if (isBluetoothAvailable()) {
      list.add(Arrays.asList("Bluetooth", "4"));
    }
    
    return list;
  }

  private boolean isBluetoothAvailable() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
      for (AudioDeviceInfo deviceInfo : audioDevices) {
        if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
          return true;
        }
      }
      return false;
    } else {
      // For older versions, we'll assume bluetooth is available if SCO is on
      return audioManager.isBluetoothScoOn();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    // Unregister receiver
    if (audioChangeReceiver != null && context != null) {
      try {
        context.unregisterReceiver(audioChangeReceiver);
      } catch (IllegalArgumentException e) {
        Log.w(TAG, "Receiver not registered: " + e.getMessage());
      }
    }
    
    // Clean up channel
    if (channel != null) {
      channel.setMethodCallHandler(null);
      channel = null;
    }
    
    // Reset audio manager to normal mode
    if (audioManager != null) {
      audioManager.setMode(AudioManager.MODE_NORMAL);
    }
    
    context = null;
    audioManager = null;
    audioChangeReceiver = null;
  }
}
