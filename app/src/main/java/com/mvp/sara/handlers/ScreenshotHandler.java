package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import com.mvp.sara.ScreenshotActivity;

import java.util.Arrays;
import java.util.List;

public class ScreenshotHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "take a screenshot",
            "capture the screen"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("screenshot") || command.contains("capture screen");
    }

    @Override
    public void handle(Context context, String command) {
        Intent intent = new Intent(context, ScreenshotActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 