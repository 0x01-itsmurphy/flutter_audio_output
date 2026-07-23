package com.itsmurphy.flutter_audio_output;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.AudioManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/** Exercises the API 21 fallback, where output enumeration is unavailable. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21)
public class FlutterAudioOutputPluginApi21Test {

  @Test
  public void bluetoothSwitchDoesNotFailFastWithoutAvailabilityCheck() {
    AudioManager audioManager = mock(AudioManager.class);
    Context context = mock(Context.class);
    when(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager);
    FlutterPlugin.FlutterPluginBinding binding = mock(FlutterPlugin.FlutterPluginBinding.class);
    when(binding.getApplicationContext()).thenReturn(context);
    when(binding.getBinaryMessenger()).thenReturn(mock(BinaryMessenger.class));
    MethodChannel.Result result = mock(MethodChannel.Result.class);

    FlutterAudioOutputPlugin plugin = new FlutterAudioOutputPlugin();
    plugin.onAttachedToEngine(binding);
    plugin.onMethodCall(new MethodCall("changeToBluetooth", null), result);

    verify(audioManager).startBluetoothSco();
    verify(result, never()).success(any());
  }
}
