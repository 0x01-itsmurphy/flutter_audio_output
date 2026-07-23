package com.itsmurphy.flutter_audio_output;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/** Communication-device routing on API 31 and later. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FlutterAudioOutputPluginModernTest {

  private AudioManager audioManager;
  private Context context;
  private FlutterPlugin.FlutterPluginBinding binding;
  private FlutterAudioOutputPlugin plugin;
  private MethodChannel.Result result;

  private AudioDeviceInfo earpiece;
  private AudioDeviceInfo speaker;
  private AudioDeviceInfo bluetooth;

  @Before
  public void setUp() {
    audioManager = mock(AudioManager.class);
    context = mock(Context.class);
    when(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager);
    binding = mock(FlutterPlugin.FlutterPluginBinding.class);
    when(binding.getApplicationContext()).thenReturn(context);
    when(binding.getBinaryMessenger()).thenReturn(mock(BinaryMessenger.class));
    result = mock(MethodChannel.Result.class);

    earpiece = device(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, 1);
    speaker = device(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, 2);
    bluetooth = device(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, 5);
    when(audioManager.getAvailableCommunicationDevices())
        .thenReturn(Arrays.asList(earpiece, speaker, bluetooth));
    when(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
        .thenReturn(new AudioDeviceInfo[0]);
    when(audioManager.setCommunicationDevice(any())).thenReturn(true);

    plugin = new FlutterAudioOutputPlugin();
    plugin.onAttachedToEngine(binding);
  }

  private static AudioDeviceInfo device(int type, int id) {
    AudioDeviceInfo info = mock(AudioDeviceInfo.class);
    when(info.getType()).thenReturn(type);
    when(info.getId()).thenReturn(id);
    return info;
  }

  private void call(String method, Map<String, Object> args, MethodChannel.Result result) {
    plugin.onMethodCall(new MethodCall(method, args), result);
  }

  private List<AudioManager.OnCommunicationDeviceChangedListener> capturedListeners(int count) {
    ArgumentCaptor<AudioManager.OnCommunicationDeviceChangedListener> captor =
        ArgumentCaptor.forClass(AudioManager.OnCommunicationDeviceChangedListener.class);
    verify(audioManager, times(count))
        .addOnCommunicationDeviceChangedListener(any(), captor.capture());
    return captor.getAllValues();
  }

  @Test
  public void speakerSwitchCompletesOnceOnListener() {
    call("changeToSpeaker", null, result);

    verify(audioManager).setCommunicationDevice(speaker);
    verify(audioManager).setMode(AudioManager.MODE_NORMAL);
    verify(result, never()).success(any());

    AudioManager.OnCommunicationDeviceChangedListener listener = capturedListeners(1).get(0);
    listener.onCommunicationDeviceChanged(speaker);
    listener.onCommunicationDeviceChanged(speaker);

    verify(result, times(1)).success(true);
  }

  @Test
  public void speakerSwitchAlreadyActiveSucceedsWithoutListener() {
    when(audioManager.getCommunicationDevice()).thenReturn(speaker);

    call("changeToSpeaker", null, result);

    verify(result).success(true);
    verify(audioManager, never()).setCommunicationDevice(any());
    verify(audioManager, never()).addOnCommunicationDeviceChangedListener(any(), any());
  }

  @Test
  public void speakerSwitchTimeoutClearsUnconfirmedSelectionAndFails() {
    call("changeToSpeaker", null, result);
    verify(result, never()).success(any());

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));

    verify(audioManager).clearCommunicationDevice();
    verify(result).success(false);
  }

  @Test
  public void newRequestSupersedesPendingOne() {
    MethodChannel.Result first = mock(MethodChannel.Result.class);
    call("changeToBluetooth", null, first);
    verify(audioManager).setCommunicationDevice(bluetooth);

    call("changeToSpeaker", null, result);

    verify(first, times(1)).success(false);
    verify(first, never()).success(true);
    verify(audioManager).clearCommunicationDevice();
    List<AudioManager.OnCommunicationDeviceChangedListener> listeners = capturedListeners(2);
    verify(audioManager).removeOnCommunicationDeviceChangedListener(listeners.get(0));
    verify(audioManager).setCommunicationDevice(speaker);
    verify(result, never()).success(any());
  }

  @Test
  public void staleListenerDoesNotCompleteSupersedingRequest() {
    MethodChannel.Result first = mock(MethodChannel.Result.class);
    call("changeToBluetooth", null, first);
    call("changeToSpeaker", null, result);

    List<AudioManager.OnCommunicationDeviceChangedListener> listeners = capturedListeners(2);
    listeners.get(0).onCommunicationDeviceChanged(bluetooth);
    verify(result, never()).success(any());

    listeners.get(1).onCommunicationDeviceChanged(speaker);
    verify(result).success(true);
  }

  @Test
  public void failedSuccessorRestoresSettledRouteNotSupersededTarget() {
    MethodChannel.Result initial = mock(MethodChannel.Result.class);
    call("changeToSpeaker", null, initial);
    capturedListeners(1).get(0).onCommunicationDeviceChanged(speaker);

    when(audioManager.getCommunicationDevice()).thenReturn(speaker);
    MethodChannel.Result superseded = mock(MethodChannel.Result.class);
    call("changeToBluetooth", null, superseded);

    // The canceled target can remain current while restoring the settled
    // speaker route. The successor must inherit the saved speaker baseline
    // instead of adopting this transient Bluetooth device.
    when(audioManager.getCommunicationDevice()).thenReturn(bluetooth);
    MethodChannel.Result successor = mock(MethodChannel.Result.class);
    call("changeToReceiver", null, successor);
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));

    verify(superseded).success(false);
    verify(successor).success(false);
    verify(audioManager, times(1)).setCommunicationDevice(bluetooth);
    verify(audioManager).setCommunicationDevice(earpiece);
    verify(audioManager, times(3)).setCommunicationDevice(speaker);
  }

  @Test
  public void receiverWithoutEarpieceFailsWithoutChangingRoute() {
    when(audioManager.getAvailableCommunicationDevices())
        .thenReturn(Collections.singletonList(speaker));

    call("changeToReceiver", null, result);

    verify(result).success(false);
    verify(audioManager, never()).clearCommunicationDevice();
    verify(audioManager, never()).setCommunicationDevice(any());
    verify(audioManager, never()).setMode(anyInt());
  }

  @Test
  public void headphonesWithoutWiredDeviceFails() {
    call("changeToHeadphones", null, result);

    verify(result).success(false);
    verify(audioManager, never()).setCommunicationDevice(any());
  }

  @Test
  public void bluetoothUnavailableFailsWithoutTouchingMode() {
    when(audioManager.getAvailableCommunicationDevices())
        .thenReturn(Arrays.asList(earpiece, speaker));

    call("changeToBluetooth", null, result);

    verify(result).success(false);
    verify(audioManager, never()).setMode(anyInt());
    verify(audioManager, never()).setCommunicationDevice(any());
    verify(audioManager, never()).addOnCommunicationDeviceChangedListener(any(), any());
  }

  @Test
  public void routeTimeoutRollsBackAppliedMode() {
    call("changeToSpeaker", null, result);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));

    verify(result).success(false);
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", null, releaseResult);
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);
  }

  @Test
  public void firstFailureReleasesModeInsteadOfAdoptingAnotherAppsMode() {
    when(audioManager.getMode()).thenReturn(AudioManager.MODE_IN_COMMUNICATION);

    call("changeToSpeaker", null, result);
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));

    verify(result).success(false);
    // Reapplying the observed mode would make this instance its owner.
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);
    verify(audioManager, never()).setMode(AudioManager.MODE_IN_COMMUNICATION);
  }

  @Test
  public void rollbackRestoresPreviousPluginOwnedMode() {
    when(audioManager.getCommunicationDevice()).thenReturn(speaker);
    call("changeToSpeaker", null, result);
    verify(result).success(true);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);

    MethodChannel.Result bluetoothResult = mock(MethodChannel.Result.class);
    call("changeToBluetooth", null, bluetoothResult);
    verify(audioManager).setMode(AudioManager.MODE_IN_COMMUNICATION);

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(30000));

    verify(bluetoothResult).success(false);
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", null, releaseResult);
    verify(audioManager, times(3)).setMode(AudioManager.MODE_NORMAL);
  }

  @Test
  public void syncSetCommunicationDeviceFailureRollsBackAppliedMode() {
    when(audioManager.setCommunicationDevice(speaker)).thenReturn(false);

    call("changeToSpeaker", null, result);

    verify(result, times(1)).success(false);
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);
    verify(audioManager).removeOnCommunicationDeviceChangedListener(any());

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));
    verify(result, times(1)).success(false);
  }

  @Test
  public void setCommunicationDeviceExceptionCompletesFalseAndCleansUp() {
    doThrow(new IllegalArgumentException("device disconnected"))
        .when(audioManager).setCommunicationDevice(speaker);

    call("changeToSpeaker", null, result);

    verify(result).success(false);
    verify(audioManager).clearCommunicationDevice();
    verify(audioManager).removeOnCommunicationDeviceChangedListener(any());
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));
    verify(result, times(1)).success(false);
  }

  @Test
  public void deviceLostAfterPreflightRollsBackAppliedMode() {
    when(audioManager.getAvailableCommunicationDevices())
        .thenReturn(Arrays.asList(earpiece, speaker, bluetooth))
        .thenReturn(Arrays.asList(earpiece, speaker));

    call("changeToBluetooth", null, result);

    verify(result).success(false);
    verify(audioManager).setMode(AudioManager.MODE_IN_COMMUNICATION);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);
    verify(audioManager, never()).setCommunicationDevice(any());
  }

  @Test
  public void preflightFailingSuccessorRollsBackInheritedMode() {
    MethodChannel.Result first = mock(MethodChannel.Result.class);
    call("changeToBluetooth", null, first);
    verify(audioManager).setMode(AudioManager.MODE_IN_COMMUNICATION);

    // The unavailable successor must restore the inherited mode baseline.
    call("changeToHeadphones", null, result);

    verify(first, times(1)).success(false);
    verify(result, times(1)).success(false);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", null, releaseResult);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);
  }

  @Test
  public void speakerSuccessorAppliesNormalModeImmediately() {
    MethodChannel.Result first = mock(MethodChannel.Result.class);
    call("changeToBluetooth", null, first);
    verify(audioManager).setMode(AudioManager.MODE_IN_COMMUNICATION);

    call("changeToSpeaker", null, result);

    verify(first, times(1)).success(false);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);
    verify(result, never()).success(any());
  }

  @Test
  public void successorRollbackRestoresSettledModeNotTransient() {
    MethodChannel.Result first = mock(MethodChannel.Result.class);
    call("changeToReceiver", null, first);
    verify(audioManager).setMode(AudioManager.MODE_IN_COMMUNICATION);

    call("changeToSpeaker", null, result);
    verify(first, times(1)).success(false);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));

    // Restore the settled baseline, not the superseded request's transient mode.
    verify(result).success(false);
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_IN_COMMUNICATION);

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", null, releaseResult);
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);
  }

  @Test
  public void supersededRequestDoesNotRollBackSuccessorMode() {
    MethodChannel.Result first = mock(MethodChannel.Result.class);
    call("changeToSpeaker", null, first);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);

    call("changeToReceiver", null, result);

    // Supersession must not reapply the first request's mode.
    verify(first, times(1)).success(false);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_NORMAL);
    verify(audioManager, times(1)).setMode(AudioManager.MODE_IN_COMMUNICATION);
  }

  @Test
  public void speakerPreservesNormalMode() {
    when(audioManager.getCommunicationDevice()).thenReturn(speaker);

    call("changeToSpeaker", null, result);

    verify(audioManager).setMode(AudioManager.MODE_NORMAL);
  }

  @Test
  public void receiverPreservesInCommunicationMode() {
    when(audioManager.getCommunicationDevice()).thenReturn(earpiece);

    call("changeToReceiver", null, result);

    verify(audioManager).setMode(AudioManager.MODE_IN_COMMUNICATION);
  }

  @Test
  public void releaseAfterConfirmedSwitchClearsDeviceAndRestoresMode() {
    call("changeToSpeaker", null, result);
    capturedListeners(1).get(0).onCommunicationDeviceChanged(speaker);
    verify(result).success(true);

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", null, releaseResult);

    verify(audioManager).clearCommunicationDevice();
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);
    verify(releaseResult).success(null);
  }

  @Test
  public void failedSwitchRestoresPreviousCommunicationDevice() {
    MethodChannel.Result first = mock(MethodChannel.Result.class);
    call("changeToSpeaker", null, first);
    capturedListeners(1).get(0).onCommunicationDeviceChanged(speaker);

    when(audioManager.getCommunicationDevice()).thenReturn(speaker);
    MethodChannel.Result second = mock(MethodChannel.Result.class);
    call("changeToBluetooth", null, second);

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(30000));

    verify(second).success(false);
    verify(audioManager).setCommunicationDevice(bluetooth);
    verify(audioManager).clearCommunicationDevice();
    verify(audioManager, times(2)).setCommunicationDevice(speaker);
  }

  @Test
  public void firstFailedSwitchDoesNotAdoptSystemCommunicationDevice() {
    when(audioManager.getCommunicationDevice()).thenReturn(speaker);

    call("changeToBluetooth", null, result);
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(30000));

    verify(result).success(false);
    verify(audioManager).setCommunicationDevice(bluetooth);
    verify(audioManager).clearCommunicationDevice();
    verify(audioManager, never()).setCommunicationDevice(speaker);

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", null, releaseResult);

    verify(audioManager, times(1)).clearCommunicationDevice();
    verify(releaseResult).success(null);
  }

  @Test
  public void releaseWithoutOwnedRouteClearsNothing() {
    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", null, releaseResult);

    verify(audioManager, never()).clearCommunicationDevice();
    verify(audioManager, never()).setMode(anyInt());
    verify(releaseResult).success(null);
  }

  @Test
  public void releaseAfterInCommunicationSwitchRestoresNormalMode() {
    when(audioManager.getCommunicationDevice()).thenReturn(earpiece);
    call("changeToReceiver", null, result);

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", null, releaseResult);

    verify(audioManager).setMode(AudioManager.MODE_NORMAL);
    verify(releaseResult).success(null);
  }

  @Test
  public void releaseAbortsPendingRequest() {
    call("changeToSpeaker", null, result);
    verify(result, never()).success(any());

    MethodChannel.Result releaseResult = mock(MethodChannel.Result.class);
    call("release", null, releaseResult);

    verify(result, times(1)).success(false);
    verify(releaseResult).success(null);
  }

  @Test
  public void detachWithoutModificationsLeavesAudioStateAlone() {
    plugin.onDetachedFromEngine(binding);

    verify(audioManager, never()).clearCommunicationDevice();
    verify(audioManager, never()).setMode(anyInt());
  }

  @Test
  public void detachAfterConfirmedSwitchClearsDeviceAndRestoresMode() {
    call("changeToSpeaker", null, result);
    capturedListeners(1).get(0).onCommunicationDeviceChanged(speaker);
    verify(result).success(true);

    plugin.onDetachedFromEngine(binding);

    verify(audioManager).clearCommunicationDevice();
    verify(audioManager, times(2)).setMode(AudioManager.MODE_NORMAL);
  }

  @Test
  public void detachAfterEarlyExitSwitchDoesNotClaimTheRoute() {
    when(audioManager.getCommunicationDevice()).thenReturn(speaker);
    call("changeToSpeaker", null, result);
    verify(result).success(true);

    plugin.onDetachedFromEngine(binding);

    verify(audioManager, never()).clearCommunicationDevice();
  }

  @Test
  public void detachAfterFailedRequestDoesNotClaimTheRoute() {
    when(audioManager.getAvailableCommunicationDevices())
        .thenReturn(Arrays.asList(earpiece, speaker));
    call("changeToHeadphones", null, result);
    verify(result).success(false);

    plugin.onDetachedFromEngine(binding);

    verify(audioManager, never()).clearCommunicationDevice();
  }

  @Test
  public void pendingBluetoothSupersededByUnavailableRequestFailsBothAndClears() {
    MethodChannel.Result first = mock(MethodChannel.Result.class);
    call("changeToBluetooth", null, first);
    verify(audioManager).setCommunicationDevice(bluetooth);

    when(audioManager.getAvailableCommunicationDevices())
        .thenReturn(Arrays.asList(earpiece, speaker));
    call("changeToHeadphones", null, result);

    verify(first, times(1)).success(false);
    verify(result, times(1)).success(false);
    verify(audioManager).clearCommunicationDevice();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void getAvailableInputsPreservesA2dpDiscovery() {
    AudioDeviceInfo a2dp = device(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, 9);
    when(audioManager.getAvailableCommunicationDevices())
        .thenReturn(Arrays.asList(earpiece, speaker));
    when(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
        .thenReturn(new AudioDeviceInfo[] {a2dp});

    call("getAvailableInputs", null, result);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(result).success(captor.capture());
    List<List<String>> inputs = (List<List<String>>) captor.getValue();
    assertEquals(
        Arrays.asList(
            Arrays.asList("Receiver", "1"),
            Arrays.asList("Speaker", "2"),
            Arrays.asList("Bluetooth", "4")),
        inputs);
  }

  @Test
  public void getCurrentOutputMapsCommunicationDeviceType() {
    when(audioManager.getCommunicationDevice()).thenReturn(speaker);

    call("getCurrentOutput", null, result);

    verify(result).success(Arrays.asList("Speaker", "2"));
  }

  @Test
  public void getCurrentOutputPreservesReceiverFallbackWithoutCommunicationDevice() {
    when(audioManager.getCommunicationDevice()).thenReturn(null);

    call("getCurrentOutput", null, result);

    verify(result).success(Arrays.asList("Receiver", "1"));
  }
}
