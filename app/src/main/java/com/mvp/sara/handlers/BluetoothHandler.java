package com.mvp.sara.handlers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.List;

public class BluetoothHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "turn on bluetooth",
            "turn off bluetooth",
            "enable bluetooth",
            "disable bluetooth"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("bluetooth");
    }

    @Override
    public void handle(Context context, String command) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            FeedbackProvider.speakAndToast(context, "Bluetooth is not supported on this device.");
            return;
        }

        boolean enable = command.contains("on") || command.contains("enable");

        try {
            if (enable) {
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();
                    FeedbackProvider.speakAndToast(context, "Bluetooth enabled.");
                } else {
                    FeedbackProvider.speakAndToast(context, "Bluetooth is already enabled.");
                }
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                    FeedbackProvider.speakAndToast(context, "Bluetooth disabled.");
                } else {
                    FeedbackProvider.speakAndToast(context, "Bluetooth is already disabled.");
                }
            }
        } catch (SecurityException e) {
            // Fallback to settings if direct control fails (should not happen for system app)
            FeedbackProvider.speakAndToast(context, "Please toggle Bluetooth from settings.");
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 