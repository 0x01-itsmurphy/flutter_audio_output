package com.itsmurphy.flutter_audio_output;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

interface AudioEventListener {
    void onChanged();
}

/** Receives wired-headset changes below API 23, where AudioDeviceCallback is unavailable. */
public class AudioChangeReceiver extends BroadcastReceiver {

    private final AudioEventListener audioEventListener;

    public AudioChangeReceiver(final AudioEventListener listener) {
        this.audioEventListener = listener;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (AudioManager.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
            audioEventListener.onChanged();
        }
    }
}
