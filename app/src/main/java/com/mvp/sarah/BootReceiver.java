package com.mvp.sarah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, starting services...");

            // Start SaraVoiceService for hotword detection
            Intent saraVoiceServiceIntent = new Intent(context, SaraVoiceService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(saraVoiceServiceIntent);
            } else {
                context.startService(saraVoiceServiceIntent);
            }

            // Start TriggerDetectionService for shake and emergency triggers
            Intent triggerDetectionServiceIntent = new Intent(context, TriggerDetectionService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(triggerDetectionServiceIntent);
            } else {
                context.startService(triggerDetectionServiceIntent);
            }

            Log.d(TAG, "Services started on boot.");
        }
    }
} 