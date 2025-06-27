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

        // Android 12+ requires BLUETOOTH_CONNECT permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                FeedbackProvider.speakAndToast(context, "I need Bluetooth permission to control Bluetooth. Please grant it in settings.");
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }
        }

        try {
            boolean actionSuccess = false;
            if (enable) {
                if (!bluetoothAdapter.isEnabled()) {
                    actionSuccess = bluetoothAdapter.enable();
                    if (actionSuccess) {
                        FeedbackProvider.speakAndToast(context, "Bluetooth enabling...");
                    } else {
                        FeedbackProvider.speakAndToast(context, "Couldn't enable Bluetooth directly. Opening settings.");
                        openBluetoothSettings(context);
                    }
                } else {
                    FeedbackProvider.speakAndToast(context, "Bluetooth is already enabled.");
                }
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    actionSuccess = bluetoothAdapter.disable();
                    if (actionSuccess) {
                        FeedbackProvider.speakAndToast(context, "Bluetooth disabling...");
                    } else {
                        FeedbackProvider.speakAndToast(context, "Couldn't disable Bluetooth directly. Opening settings.");
                        openBluetoothSettings(context);
                    }
                } else {
                    FeedbackProvider.speakAndToast(context, "Bluetooth is already disabled.");
                }
            }
        } catch (SecurityException e) {
            FeedbackProvider.speakAndToast(context, "Please toggle Bluetooth from settings.");
            openBluetoothSettings(context);
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Error controlling Bluetooth. Opening settings.");
            openBluetoothSettings(context);
        }
    }

    private void openBluetoothSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 