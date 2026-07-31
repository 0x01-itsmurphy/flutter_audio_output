package com.itsmurphy.flutter_audio_output;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** FlutterAudioOutputPlugin */
public class FlutterAudioOutputPlugin implements FlutterPlugin, MethodCallHandler {
  private static final String TAG = "FlutterAudioOutput";

  // Keep these values aligned with the Dart AudioPort enum ordinals.
  private static final int PORT_UNKNOWN = 0;
  private static final int PORT_RECEIVER = 1;
  private static final int PORT_SPEAKER = 2;
  private static final int PORT_HEADPHONES = 3;
  private static final int PORT_BLUETOOTH = 4;

  // Bluetooth route establishment can take longer than local routing.
  private static final long ROUTE_CHANGE_TIMEOUT_MS = 5000;
  private static final long BLUETOOTH_ROUTE_TIMEOUT_MS = 30000;
  private static final long SCO_CONNECT_TIMEOUT_MS = 10000;

  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private MethodChannel channel;
  private AudioManager audioManager;
  private Context context;

  // AudioManager routing is process-wide. These fields record only changes
  // made through this instance; other audio clients must coordinate access.
  private boolean modifiedAudioSettings;
  private Integer pluginSetMode;

  private AudioChangeReceiver audioChangeReceiver; // API < 23 fallback
  private AudioDeviceCallback audioDeviceCallback; // API 23+

  // Only one request may be pending. The generation invalidates callbacks
  // queued by completed or superseded requests.
  private int requestGeneration;
  private Result pendingResult;
  private Runnable pendingTimeout;
  // True while an API 31+ selection is awaiting confirmation.
  private boolean pendingSelectionActive;
  // A null previous mode means this instance did not own the mode.
  private boolean pendingModeApplied;
  private Integer pendingPreviousMode;
  // Route state captured for rollback of the pending request.
  private boolean pendingPreviousAudioSettingsModified;
  private AudioDeviceInfo pendingPreviousCommunicationDevice;
  private boolean pendingLegacySpeakerphoneApplied;
  private boolean pendingPreviousSpeakerphoneOn;
  // Stored as Object so class verification succeeds below API 31.
  private Object pendingDeviceListener;
  private BroadcastReceiver scoStateReceiver;
  // True while this instance owns an unmatched startBluetoothSco() request.
  private boolean scoStarted;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_audio_output");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
          notifyInputChanged();
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
          notifyInputChanged();
        }
      };
      audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler);
    } else {
      // AudioDeviceCallback is unavailable below API 23. Preserve the legacy
      // wired-headset broadcast on those releases.
      audioChangeReceiver = new AudioChangeReceiver(this::notifyInputChanged);
      IntentFilter filter = new IntentFilter(AudioManager.ACTION_HEADSET_PLUG);
      context.registerReceiver(audioChangeReceiver, filter);
    }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "getPlatformVersion":
        result.success("Android " + Build.VERSION.RELEASE);
        break;
      case "getCurrentOutput":
        result.success(getCurrentOutput());
        break;
      case "getAvailableInputs":
        result.success(getAvailableInputs());
        break;
      case "changeToReceiver":
        changeOutput(PORT_RECEIVER, result);
        break;
      case "changeToSpeaker":
        changeOutput(PORT_SPEAKER, result);
        break;
      case "changeToHeadphones":
        changeOutput(PORT_HEADPHONES, result);
        break;
      case "changeToBluetooth":
        changeOutput(PORT_BLUETOOTH, result);
        break;
      case "release":
        releaseRouting(result);
        break;
      default:
        result.notImplemented();
    }
  }

  // Route changes

  private void changeOutput(int port, Result result) {
    // Carry the previous mode baseline across superseded requests. The
    // successor either establishes a new baseline or restores this one.
    boolean inheritedRollback = pendingModeApplied;
    Integer inheritedPreviousMode = pendingPreviousMode;
    boolean inheritedRouteRollback = pendingSelectionActive;
    AudioDeviceInfo inheritedPreviousCommunicationDevice =
        pendingPreviousCommunicationDevice;
    boolean inheritedPreviousAudioSettingsModified =
        pendingPreviousAudioSettingsModified;
    completePendingRequest(false);

    // Validate before changing mode so an unavailable route has no side effects.
    if (!isRouteAvailable(port)) {
      if (inheritedRollback) {
        restoreSettledMode(inheritedPreviousMode);
      }
      result.success(false);
      return;
    }

    Integer previousMode = inheritedRollback ? inheritedPreviousMode : pluginSetMode;
    applyAudioMode(port);
    pendingModeApplied = true;
    pendingPreviousMode = previousMode;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      changeOutputModern(
          port,
          result,
          inheritedRouteRollback,
          inheritedPreviousCommunicationDevice,
          inheritedPreviousAudioSettingsModified);
    } else {
      changeOutputLegacy(port, result);
    }
  }

  private boolean isRouteAvailable(int port) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return findCommunicationDevice(port) != null;
    }
    switch (port) {
      case PORT_RECEIVER:
      case PORT_SPEAKER:
        return true;
      case PORT_HEADPHONES:
        return isWiredHeadsetOn();
      case PORT_BLUETOOTH:
        // API 21 and 22 cannot enumerate outputs. SCO completion determines
        // whether Bluetooth is available.
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            || legacyIsBluetoothScoOn()
            || isBluetoothCommunicationAvailable();
      default:
        return false;
    }
  }

  // Clear only state changed through this instance.
  private void releaseRouting(Result result) {
    completePendingRequest(false);
    if (modifiedAudioSettings) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        audioManager.clearCommunicationDevice();
      } else {
        resetLegacyRouting();
      }
    }
    if (pluginSetMode != null) {
      audioManager.setMode(AudioManager.MODE_NORMAL);
      pluginSetMode = null;
    }
    modifiedAudioSettings = false;
    result.success(null);
    notifyInputChanged();
  }

  @SuppressWarnings("deprecation")
  private void resetLegacyRouting() {
    stopScoIfStarted();
    audioManager.setBluetoothScoOn(false);
    audioManager.setSpeakerphoneOn(false);
  }

  // Preserve the 1.x audio-mode policy for compatibility.
  private void applyAudioMode(int port) {
    int mode = port == PORT_SPEAKER
        ? AudioManager.MODE_NORMAL
        : AudioManager.MODE_IN_COMMUNICATION;
    audioManager.setMode(mode);
    pluginSetMode = mode;
  }

  @RequiresApi(Build.VERSION_CODES.S)
  private void changeOutputModern(
      int port,
      Result result,
      boolean inheritedRouteRollback,
      @Nullable AudioDeviceInfo inheritedPreviousCommunicationDevice,
      boolean inheritedPreviousAudioSettingsModified) {
    AudioDeviceInfo target = findCommunicationDevice(port);
    if (target == null) {
      // The device disappeared between availability checks. Do not substitute
      // the platform default, which may be a different route.
      rollbackPendingMode();
      result.success(false);
      return;
    }

    AudioDeviceInfo current = audioManager.getCommunicationDevice();
    final int targetId = target.getId();
    if (!inheritedRouteRollback && current != null && current.getId() == targetId) {
      settlePendingMode();
      result.success(true);
      notifyInputChanged();
      return;
    }

    pendingResult = result;
    final int generation = requestGeneration;
    AudioManager.OnCommunicationDeviceChangedListener listener =
        new AudioManager.OnCommunicationDeviceChangedListener() {
          @Override
          public void onCommunicationDeviceChanged(@Nullable AudioDeviceInfo device) {
            // Listener removal does not cancel callbacks already queued on
            // the executor; the generation check rejects stale deliveries.
            if (device != null && device.getId() == targetId) {
              completePendingRequest(generation, true);
            }
          }
        };
    pendingDeviceListener = listener;
    audioManager.addOnCommunicationDeviceChangedListener(context.getMainExecutor(), listener);

    pendingPreviousAudioSettingsModified = inheritedRouteRollback
        ? inheritedPreviousAudioSettingsModified
        : modifiedAudioSettings;
    pendingPreviousCommunicationDevice = inheritedRouteRollback
        ? inheritedPreviousCommunicationDevice
        : current;
    // Record rollback state before invoking AudioManager so rejection and
    // exceptions use the same cleanup path.
    pendingSelectionActive = true;
    try {
      if (!audioManager.setCommunicationDevice(target)) {
        completePendingRequest(generation, false);
        return;
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Unable to select communication device: " + e.getMessage());
      completePendingRequest(generation, false);
      return;
    }

    pendingTimeout = () -> {
      AudioDeviceInfo now = audioManager.getCommunicationDevice();
      completePendingRequest(generation, now != null && now.getId() == targetId);
    };
    mainHandler.postDelayed(
        pendingTimeout,
        port == PORT_BLUETOOTH ? BLUETOOTH_ROUTE_TIMEOUT_MS : ROUTE_CHANGE_TIMEOUT_MS);
  }

  @RequiresApi(Build.VERSION_CODES.S)
  @Nullable
  private AudioDeviceInfo findCommunicationDevice(int port) {
    List<AudioDeviceInfo> devices = audioManager.getAvailableCommunicationDevices();
    for (int type : communicationDeviceTypesFor(port)) {
      for (AudioDeviceInfo device : devices) {
        if (device.getType() == type) {
          return device;
        }
      }
    }
    return null;
  }

  // Device types are ordered by selection preference.
  private static int[] communicationDeviceTypesFor(int port) {
    switch (port) {
      case PORT_RECEIVER:
        return new int[] {AudioDeviceInfo.TYPE_BUILTIN_EARPIECE};
      case PORT_SPEAKER:
        return new int[] {AudioDeviceInfo.TYPE_BUILTIN_SPEAKER};
      case PORT_HEADPHONES:
        return new int[] {
          AudioDeviceInfo.TYPE_WIRED_HEADSET,
          AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
          AudioDeviceInfo.TYPE_USB_HEADSET,
          AudioDeviceInfo.TYPE_USB_DEVICE
        };
      case PORT_BLUETOOTH:
        return new int[] {
          AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
          AudioDeviceInfo.TYPE_BLE_HEADSET,
          AudioDeviceInfo.TYPE_BLE_SPEAKER,
          AudioDeviceInfo.TYPE_HEARING_AID
        };
      default:
        return new int[0];
    }
  }

  @SuppressWarnings("deprecation")
  private void changeOutputLegacy(int port, Result result) {
    switch (port) {
      case PORT_RECEIVER:
      case PORT_HEADPHONES:
        // Legacy Android selects the wired device or earpiece when SCO and
        // speakerphone routing are disabled.
        stopScoIfStarted();
        audioManager.setBluetoothScoOn(false);
        audioManager.setSpeakerphoneOn(false);
        modifiedAudioSettings = true;
        settlePendingMode();
        result.success(true);
        notifyInputChanged();
        break;
      case PORT_SPEAKER:
        stopScoIfStarted();
        audioManager.setBluetoothScoOn(false);
        audioManager.setSpeakerphoneOn(true);
        modifiedAudioSettings = true;
        settlePendingMode();
        result.success(true);
        notifyInputChanged();
        break;
      case PORT_BLUETOOTH:
        changeToBluetoothLegacy(result);
        break;
      default:
        result.success(false);
    }
  }

  @SuppressWarnings("deprecation")
  private void changeToBluetoothLegacy(Result result) {
    boolean alreadyConnected = audioManager.isBluetoothScoOn();
    if (alreadyConnected && scoStarted) {
      // Reuse the SCO reference already held by this instance.
      settlePendingMode();
      result.success(true);
      notifyInputChanged();
      return;
    }

    pendingPreviousAudioSettingsModified = modifiedAudioSettings;
    pendingPreviousSpeakerphoneOn = audioManager.isSpeakerphoneOn();
    pendingLegacySpeakerphoneApplied = true;
    audioManager.setSpeakerphoneOn(false);

    pendingResult = result;
    final int generation = requestGeneration;
    scoStateReceiver = new BroadcastReceiver() {
      // Ignore sticky state and trailing disconnects from earlier requests.
      // A disconnect fails this request only after its own CONNECTING state.
      private boolean seenConnecting;

      @Override
      public void onReceive(Context ctx, Intent intent) {
        if (generation != requestGeneration) {
          return;
        }
        if (isInitialStickyBroadcast()) {
          return;
        }
        int state = intent.getIntExtra(
            AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED);
        if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
          audioManager.setBluetoothScoOn(true);
          completePendingRequest(generation, true);
        } else if (state == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
          seenConnecting = true;
        } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED && seenConnecting) {
          completePendingRequest(generation, false);
        }
      }
    };
    Intent stickyState = context.registerReceiver(
        scoStateReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));

    pendingTimeout = () -> completePendingRequest(generation, false);
    mainHandler.postDelayed(pendingTimeout, SCO_CONNECT_TIMEOUT_MS);

    if (!scoStarted) {
      try {
        // SCO is reference-counted per client. Acquire this instance's own
        // reference even when another client established the link.
        audioManager.startBluetoothSco();
        scoStarted = true;
      } catch (RuntimeException e) {
        Log.w(TAG, "Unable to start Bluetooth SCO: " + e.getMessage());
        completePendingRequest(generation, false);
        return;
      }
    }

    int stickyScoState = stickyState != null
        ? stickyState.getIntExtra(
            AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED)
        : AudioManager.SCO_AUDIO_STATE_DISCONNECTED;
    if (alreadyConnected || stickyScoState == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
      audioManager.setBluetoothScoOn(true);
      completePendingRequest(generation, true);
    }
  }

  // MODE_NORMAL clears the mode set by this instance. Restoring a mode that
  // was only observed could override another client's choice.
  private void restoreSettledMode(Integer settledPluginMode) {
    audioManager.setMode(
        settledPluginMode != null ? settledPluginMode : AudioManager.MODE_NORMAL);
    pluginSetMode = settledPluginMode;
  }

  private void rollbackPendingMode() {
    if (!pendingModeApplied) {
      return;
    }
    pendingModeApplied = false;
    restoreSettledMode(pendingPreviousMode);
    pendingPreviousMode = null;
  }

  private void settlePendingMode() {
    pendingModeApplied = false;
    pendingPreviousMode = null;
  }

  // Async callbacks are generation-guarded. Supersession and lifecycle
  // cleanup use the unguarded overload and manage rollback directly.
  private void completePendingRequest(int generation, boolean success) {
    if (generation != requestGeneration) {
      return;
    }
    if (!success) {
      rollbackPendingMode();
    }
    completePendingRequest(success);
  }

  private void completePendingRequest(boolean success) {
    requestGeneration++;
    pendingModeApplied = false;
    pendingPreviousMode = null;

    boolean modernSelectionApplied = pendingSelectionActive;
    AudioDeviceInfo previousCommunicationDevice = pendingPreviousCommunicationDevice;
    boolean legacySpeakerphoneApplied = pendingLegacySpeakerphoneApplied;
    boolean previousSpeakerphoneOn = pendingPreviousSpeakerphoneOn;
    boolean previousAudioSettingsModified = pendingPreviousAudioSettingsModified;

    Result result = pendingResult;
    pendingResult = null;

    if (pendingTimeout != null) {
      mainHandler.removeCallbacks(pendingTimeout);
      pendingTimeout = null;
    }
    if (pendingDeviceListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      audioManager.removeOnCommunicationDeviceChangedListener(
          (AudioManager.OnCommunicationDeviceChangedListener) pendingDeviceListener);
      pendingDeviceListener = null;
    }
    if (scoStateReceiver != null) {
      if (!success) {
        // Balance the SCO reference acquired for an aborted request.
        stopScoIfStarted();
      }
      try {
        context.unregisterReceiver(scoStateReceiver);
      } catch (IllegalArgumentException e) {
        Log.w(TAG, "SCO receiver already unregistered: " + e.getMessage());
      }
      scoStateReceiver = null;
    }

    if (modernSelectionApplied || legacySpeakerphoneApplied) {
      if (success) {
        modifiedAudioSettings = true;
      } else {
        if (modernSelectionApplied && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          restoreCommunicationDevice(
              previousCommunicationDevice, previousAudioSettingsModified);
        }
        if (legacySpeakerphoneApplied) {
          try {
            audioManager.setSpeakerphoneOn(previousSpeakerphoneOn);
          } catch (RuntimeException e) {
            Log.w(TAG, "Unable to restore speakerphone route: " + e.getMessage());
          }
        }
        modifiedAudioSettings = previousAudioSettingsModified;
      }
    }
    pendingSelectionActive = false;
    pendingPreviousCommunicationDevice = null;
    pendingLegacySpeakerphoneApplied = false;
    pendingPreviousSpeakerphoneOn = false;
    pendingPreviousAudioSettingsModified = false;

    if (result != null) {
      result.success(success);
      notifyInputChanged();
    }
  }

  @RequiresApi(Build.VERSION_CODES.S)
  private void restoreCommunicationDevice(
      @Nullable AudioDeviceInfo previousDevice, boolean previousRouteOwnedByPlugin) {
    try {
      audioManager.clearCommunicationDevice();
    } catch (RuntimeException e) {
      Log.w(TAG, "Unable to clear failed communication route: " + e.getMessage());
    }
    if (!previousRouteOwnedByPlugin || previousDevice == null) {
      // Clearing reveals the platform-selected route. Reselecting an unowned
      // device would create a persistent request from this plugin.
      return;
    }
    try {
      if (!audioManager.setCommunicationDevice(previousDevice)) {
        Log.w(TAG, "Unable to restore previous communication device");
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Unable to restore communication device: " + e.getMessage());
    }
  }

  // Queries

  private List<String> getCurrentOutput() {
    int port;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      AudioDeviceInfo device = audioManager.getCommunicationDevice();
      port = device != null ? portForDeviceType(device.getType()) : PORT_UNKNOWN;
      if (port == PORT_UNKNOWN) {
        port = PORT_RECEIVER;
      }
    } else {
      port = getCurrentOutputLegacy();
    }
    return Arrays.asList(nameForPort(port), String.valueOf(port));
  }

  @SuppressWarnings("deprecation")
  private int getCurrentOutputLegacy() {
    if (audioManager.isSpeakerphoneOn()) {
      return PORT_SPEAKER;
    } else if (audioManager.isBluetoothScoOn()) {
      return PORT_BLUETOOTH;
    } else if (isWiredHeadsetOn()) {
      return PORT_HEADPHONES;
    } else {
      // Preserve the existing receiver fallback for API compatibility.
      return PORT_RECEIVER;
    }
  }

  private List<List<String>> getAvailableInputs() {
    List<List<String>> list = new ArrayList<>();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Set<Integer> ports = new HashSet<>();
      for (AudioDeviceInfo device : audioManager.getAvailableCommunicationDevices()) {
        int port = portForDeviceType(device.getType());
        if (port != PORT_UNKNOWN) {
          ports.add(port);
        }
      }
      // Preserve the legacy discovery contract, which includes physical
      // outputs that may not be selectable communication routes.
      for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
        switch (device.getType()) {
          case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
            ports.add(PORT_RECEIVER);
            break;
          case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
            ports.add(PORT_BLUETOOTH);
            break;
          case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
            ports.add(PORT_HEADPHONES);
            break;
          default:
            break;
        }
      }
      for (int port : new int[] {PORT_RECEIVER, PORT_SPEAKER, PORT_HEADPHONES, PORT_BLUETOOTH}) {
        if (ports.contains(port)) {
          list.add(Arrays.asList(nameForPort(port), String.valueOf(port)));
        }
      }
    } else {
      list.add(Arrays.asList(nameForPort(PORT_RECEIVER), String.valueOf(PORT_RECEIVER)));
      list.add(Arrays.asList(nameForPort(PORT_SPEAKER), String.valueOf(PORT_SPEAKER)));
      if (isWiredHeadsetOn()) {
        list.add(Arrays.asList(nameForPort(PORT_HEADPHONES), String.valueOf(PORT_HEADPHONES)));
      }
      if (isBluetoothAvailable()) {
        list.add(Arrays.asList(nameForPort(PORT_BLUETOOTH), String.valueOf(PORT_BLUETOOTH)));
      }
    }
    return list;
  }

  private static String nameForPort(int port) {
    switch (port) {
      case PORT_RECEIVER:
        return "Receiver";
      case PORT_SPEAKER:
        return "Speaker";
      case PORT_HEADPHONES:
        return "Headset";
      case PORT_BLUETOOTH:
        return "Bluetooth";
      default:
        return "Unknown";
    }
  }

  private static int portForDeviceType(int type) {
    switch (type) {
      case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
        return PORT_RECEIVER;
      case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
        return PORT_SPEAKER;
      case AudioDeviceInfo.TYPE_WIRED_HEADSET:
      case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
      case AudioDeviceInfo.TYPE_USB_HEADSET:
      case AudioDeviceInfo.TYPE_USB_DEVICE:
      case AudioDeviceInfo.TYPE_USB_ACCESSORY:
        return PORT_HEADPHONES;
      case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
      case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
      case AudioDeviceInfo.TYPE_BLE_HEADSET:
      case AudioDeviceInfo.TYPE_BLE_SPEAKER:
      case AudioDeviceInfo.TYPE_BLE_BROADCAST:
      case AudioDeviceInfo.TYPE_HEARING_AID:
        return PORT_BLUETOOTH;
      default:
        return PORT_UNKNOWN;
    }
  }

  private boolean isWiredHeadsetOn() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return hasOutputDeviceType(
          AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET);
    } else {
      return legacyIsWiredHeadsetOn();
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  private boolean hasOutputDeviceType(int... types) {
    for (AudioDeviceInfo deviceInfo : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
      for (int type : types) {
        if (deviceInfo.getType() == type) {
          return true;
        }
      }
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  private boolean legacyIsWiredHeadsetOn() {
    return audioManager.isWiredHeadsetOn();
  }

  @SuppressWarnings("deprecation")
  private void stopScoIfStarted() {
    if (scoStarted) {
      audioManager.stopBluetoothSco();
      scoStarted = false;
    }
  }

  private boolean isBluetoothAvailable() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return hasOutputDeviceType(
          AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
    } else {
      return legacyIsBluetoothScoOn();
    }
  }

  private boolean isBluetoothCommunicationAvailable() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // Legacy switching uses SCO. An A2DP-only device is discoverable but is
      // not a valid communication route.
      return hasOutputDeviceType(AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
    } else {
      return legacyIsBluetoothScoOn();
    }
  }

  @SuppressWarnings("deprecation")
  private boolean legacyIsBluetoothScoOn() {
    return audioManager.isBluetoothScoOn();
  }

  // Lifecycle and events

  private void notifyInputChanged() {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      if (channel != null) {
        channel.invokeMethod("inputChanged", 1);
      }
    } else {
      mainHandler.post(() -> {
        if (channel != null) {
          channel.invokeMethod("inputChanged", 1);
        }
      });
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    completePendingRequest(false);

    if (audioDeviceCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
      audioDeviceCallback = null;
    }
    if (audioChangeReceiver != null && context != null) {
      try {
        context.unregisterReceiver(audioChangeReceiver);
      } catch (IllegalArgumentException e) {
        Log.w(TAG, "Receiver not registered: " + e.getMessage());
      }
      audioChangeReceiver = null;
    }

    if (channel != null) {
      channel.setMethodCallHandler(null);
      channel = null;
    }

    // Avoid process-wide resets when this instance recorded no change.
    if (audioManager != null) {
      if (modifiedAudioSettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          audioManager.clearCommunicationDevice();
        } else {
          resetLegacyRouting();
        }
      }
      if (pluginSetMode != null) {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        pluginSetMode = null;
      }
    }
    modifiedAudioSettings = false;

    context = null;
    audioManager = null;
  }
}
