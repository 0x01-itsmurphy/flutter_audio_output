package com.itsmurphy.flutter_audio_output;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Duration;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/** Legacy speakerphone and Bluetooth SCO routing. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class FlutterAudioOutputPluginLegacyTest {

  private AudioManager audioManager;
  private Context context;
  private FlutterPlugin.FlutterPluginBinding binding;
  private FlutterAudioOutputPlugin plugin;
  private MethodChannel.Result result;

  @Before
  public void setUp() {
    audioManager = mock(AudioManager.class);
    context = mock(Context.class);
    when(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager);
    binding = mock(FlutterPlugin.FlutterPluginBinding.class);
    when(binding.getApplicationContext()).thenReturn(context);
    when(binding.getBinaryMessenger()).thenReturn(mock(BinaryMessenger.class));
    result = mock(MethodChannel.Result.class);

    when(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
        .thenReturn(new AudioDeviceInfo[0]);

    plugin = new FlutterAudioOutputPlugin();
    plugin.onAttachedToEngine(binding);
  }

  private void call(String method, MethodChannel.Result result) {
    plugin.onMethodCall(new MethodCall(method, null), result);
  }

  private void stubScoDeviceAvailable() {
    AudioDeviceInfo sco = mock(AudioDeviceInfo.class);
    when(sco.getType()).thenReturn(AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
    when(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
        .thenReturn(new AudioDeviceInfo[] {sco});
  }

  private void stubA2dpDeviceAvailable() {
    AudioDeviceInfo a2dp = mock(AudioDeviceInfo.class);
    when(a2dp.getType()).thenReturn(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
    when(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
        .thenReturn(new AudioDeviceInfo[] {a2dp});
  }

  private BroadcastReceiver capturedScoReceiver() {
    ArgumentCaptor<BroadcastReceiver> captor = ArgumentCaptor.forClass(BroadcastReceiver.class);
    verify(context).registerReceiver(captor.capture(), any(IntentFilter.class));
    return captor.getValue();
  }

  private static Intent scoIntent(int state) {
    Intent intent = new Intent(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
    intent.putExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, state);
    return intent;
  }

  @Test
  public void bluetoothSwitchCompletesOnScoConnected() {
    stubScoDeviceAvailable();

    call("changeToBluetooth", result);

    verify(audioManager).startBluetoothSco();
    verify(result, never()).success(any());

    BroadcastReceiver receiver = capturedScoReceiver();
    receiver.onReceive(context, scoIntent(AudioManager.SCO_AUDIO_STATE_CONNECTED));

    verify(audioManager).setBluetoothScoOn(true);
    verify(result).success(true);
  }

  @Test
  public void scoDisconnectAfterConnectingFailsAndBalancesRefcount() {
    stubScoDeviceAvailable();
    call("changeToBluetooth", result);
    BroadcastReceiver receiver = capturedScoReceiver();

    receiver.onReceive(context, scoIntent(AudioManager.SCO_AUDIO_STATE_CONNECTING));
    verify(result, never()).success(any());

    receiver.onReceive(context, scoIntent(AudioManager.SCO_AUDIO_STATE_DISCONNECTED));

    verify(result).success(false);
    verify(audioManager).stopBluetoothSco();
  }

  @Test
  public void scoConnectTimeoutFails() {
    stubScoDeviceAvailable();
    call("changeToBluetooth", result);

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10000));

    verify(result).success(false);
    verify(audioManager).stopBluetoothSco();
    verify(context).unregisterReceiver(any(BroadcastReceiver.class));
  }

  @Test
  public void scoTimeoutRestoresPreviousSpeakerphoneRoute() {
    stubScoDeviceAvailable();
    when(audioManager.isSpeakerphoneOn()).thenReturn(true);

    call("changeToBluetooth", result);
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10000));

    verify(result).success(false);
    verify(audioManager).setSpeakerphoneOn(false);
    verify(audioManager).setSpeakerphoneOn(true);
  }

  @Test
  public void startBluetoothScoExceptionRestoresAudioState() {
    stubScoDeviceAvailable();
    when(audioManager.isSpeakerphoneOn()).thenReturn(true);
    doThrow(new IllegalStateException("SCO unavailable"))
        .when(audioManager).startBluetoothSco();

    call("changeToBluetooth", result);

    verify(result).success(false);
    verify(audioManager).setSpeakerphoneOn(false);
    verify(audioManager).setSpeakerphoneOn(true);
    verify(audioManager).setMode(AudioManager.MODE_IN_COMMUNICATION);
    verify(audioManager).setMode(AudioManager.MODE_NORMAL);
    verify(context).unregisterReceiver(any(BroadcastReceiver.class));
    verify(audioManager, never()).stopBluetoothSco();
  }

  @Test
  public void speakerSwitchSucceedsImmediately() {
    call("changeToSpeaker", result);

    verify(audioManager).setSpeakerphoneOn(true);
    verify(result).success(true);
  }

  @Test
  public void headphonesWithoutWiredDeviceFailsWithoutTouchingAudioState() {
    call("changeToHeadphones", result);

    verify(result).success(false);
    verify(audioManager, never()).setSpeakerphoneOn(anyBoolean());
    verify(audioManager, never()).setMode(anyInt());
  }

  @Test
  public void bluetoothUnavailableFailsBeforeTouchingAudioState() {
    call("changeToBluetooth", result);

    verify(result).success(false);
    verify(audioManager, never()).setMode(anyInt());
    verify(audioManager, never()).setSpeakerphoneOn(anyBoolean());
    verify(audioManager, never()).startBluetoothSco();
  }

  @Test
  public void a2dpOnlyDeviceCannotSatisfyScoRoute() {
    stubA2dpDeviceAvailable();

    call("changeToBluetooth", result);

    verify(result).success(false);
    verify(audioManager, never()).setMode(anyInt());
    verify(audioManager, never()).startBluetoothSco();
  }

  @Test
  public void externallyConnectedScoStillAcquiresPluginRequest() {
    stubScoDeviceAvailable();
    when(audioManager.isBluetoothScoOn()).thenReturn(true);
    when(context.registerReceiver(
            any(BroadcastReceiver.class), any(IntentFilter.class)))
        .thenReturn(scoIntent(AudioManager.SCO_AUDIO_STATE_CONNECTED));

    call("changeToBluetooth", result);

    verify(audioManager).startBluetoothSco();
    verify(audioManager).setBluetoothScoOn(true);
    verify(result).success(true);

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", releaseResult);
    verify(audioManager, times(1)).stopBluetoothSco();
  }

  @Test
  public void connectedStickyStateCompletesAfterAcquiringScoRequest() {
    stubScoDeviceAvailable();
    when(context.registerReceiver(
            any(BroadcastReceiver.class), any(IntentFilter.class)))
        .thenReturn(scoIntent(AudioManager.SCO_AUDIO_STATE_CONNECTED));

    call("changeToBluetooth", result);

    verify(audioManager).startBluetoothSco();
    verify(audioManager).setBluetoothScoOn(true);
    verify(result).success(true);
  }

  @Test
  public void scoTimeoutRollsBackAppliedMode() {
    stubScoDeviceAvailable();
    call("changeToBluetooth", result);
    verify(audioManager).setMode(AudioManager.MODE_IN_COMMUNICATION);

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10000));

    verify(result).success(false);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", releaseResult);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);
  }

  @Test
  public void staleScoBroadcastDoesNotAffectSuccessor() {
    stubScoDeviceAvailable();
    call("changeToBluetooth", result);
    BroadcastReceiver receiver = capturedScoReceiver();

    MethodChannel.Result speakerResult = mock(MethodChannel.Result.class);
    call("changeToSpeaker", speakerResult);
    verify(result, times(1)).success(false);
    verify(speakerResult, times(1)).success(true);

    // Simulate a broadcast queued before receiver removal.
    receiver.onReceive(context, scoIntent(AudioManager.SCO_AUDIO_STATE_CONNECTED));

    verify(audioManager, never()).setBluetoothScoOn(true);
    verify(result, times(1)).success(false);
    verify(speakerResult, times(1)).success(true);
  }

  @Test
  public void detachAfterSwitchResetsLegacyRoutingWithoutUnbalancedScoStop() {
    call("changeToSpeaker", result);
    clearInvocations(audioManager);

    plugin.onDetachedFromEngine(binding);

    verify(audioManager, never()).stopBluetoothSco();
    verify(audioManager).setBluetoothScoOn(false);
    verify(audioManager).setSpeakerphoneOn(false);
    verify(audioManager).setMode(AudioManager.MODE_NORMAL);
  }

  @Test
  public void releaseDuringPendingScoStopsExactlyOnce() {
    stubScoDeviceAvailable();
    call("changeToBluetooth", result);
    verify(audioManager, times(1)).startBluetoothSco();

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", releaseResult);

    verify(result, times(1)).success(false);
    verify(releaseResult).success(null);
    verify(audioManager, times(1)).stopBluetoothSco();
  }

  @Test
  public void detachDuringPendingScoStopsExactlyOnce() {
    stubScoDeviceAvailable();
    call("changeToBluetooth", result);
    verify(audioManager, times(1)).startBluetoothSco();

    plugin.onDetachedFromEngine(binding);

    verify(result, times(1)).success(false);
    verify(audioManager, times(1)).stopBluetoothSco();
  }
}
