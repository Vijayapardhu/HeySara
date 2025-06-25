package com.mvp.sara.handlers;

import android.content.Context;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class ChitChatHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "who are you",
            "what's your name",
            "who made you"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        return lowerCmd.contains("who are you") ||
               lowerCmd.contains("what's your name") ||
               lowerCmd.contains("who made you") ||
               lowerCmd.contains("who created you");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase();
        String response;

        if (lowerCmd.contains("who are you") || lowerCmd.contains("what's your name")) {
            response = "I am Sara, your personal voice assistant.";
        } else if (lowerCmd.contains("who made you") || lowerCmd.contains("who created you")) {
            response = "I was created by a talented developer, with a little help from my friends at Google and Picovoice.";
        } else {
            response = "That's an interesting question.";
        }
        
        FeedbackProvider.speakAndToast(context, response);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 