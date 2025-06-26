package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class WifiHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "turn on wifi",
            "turn off wifi",
            "enable wi-fi",
            "disable wi-fi",
            "turn on mobile data",
            "turn off mobile data",
            "enable mobile data",
            "disable mobile data"
    );

    @Override
public boolean canHandle(String command) {
    String lowerCmd = command.toLowerCase();
    return lowerCmd.contains("wifi") || lowerCmd.contains("wi-fi") || lowerCmd.contains("mobile data");
}

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase();
        boolean isWifi = lowerCmd.contains("wifi") || lowerCmd.contains("wi-fi");
        boolean isMobileData = lowerCmd.contains("mobile data");
        boolean isEnableWifi = (lowerCmd.contains("turn on wifi") || lowerCmd.contains("enable wi-fi"));

        if (isMobileData) {
            FeedbackProvider.speakAndToast(context, "Please toggle Mobile Data from the settings panel.");
            Intent panelIntent = new Intent(Settings.ACTION_DATA_USAGE_SETTINGS);
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(panelIntent);
            return;
        }

        if (isWifi) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FeedbackProvider.speakAndToast(context, "Opening Wi-Fi settings. Please enable Wi-Fi. I will help you click and go back.");
                Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(panelIntent);
                if (isEnableWifi) {
                    // Send broadcast to click 'Wi-Fi' after a short delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent clickIntent = new Intent("com.mvp.sara.ACTION_CLICK_LABEL");
                        clickIntent.putExtra("label", "Wi-Fi");
                        context.sendBroadcast(clickIntent);
                    }, 1200); // Wait for settings to open
                    // Send broadcast to perform back action after another delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent backIntent = new Intent("com.mvp.sara.ACTION_PERFORM_BACK");
                        context.sendBroadcast(backIntent);
                    }, 2500); // Wait for Wi-Fi to be toggled
                }
            } else {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                boolean enable = lowerCmd.contains("on") || lowerCmd.contains("enable") || lowerCmd.contains("connect");
                boolean disable = lowerCmd.contains("off") || lowerCmd.contains("disable") || lowerCmd.contains("disconnect");
                if (enable && !disable) {
                    wifiManager.setWifiEnabled(true);
                    FeedbackProvider.speakAndToast(context, "Wi-Fi enabled");
                } else if (disable && !enable) {
                    wifiManager.setWifiEnabled(false);
                    FeedbackProvider.speakAndToast(context, "Wi-Fi disabled");
                } else {
                    FeedbackProvider.speakAndToast(context, "Please specify if you want to enable or disable Wi-Fi.");
                }
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
}
