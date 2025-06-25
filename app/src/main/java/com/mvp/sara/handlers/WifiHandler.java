package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
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
            "disable wi-fi"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("wifi") || command.contains("wi-fi");
    }

    @Override
    public void handle(Context context, String command) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FeedbackProvider.speakAndToast(context, "Please toggle Wi-Fi from the settings panel.");
            Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(panelIntent);
        } else {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String lowerCmd = command.toLowerCase();
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

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 