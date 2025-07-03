package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import com.mvp.sara.ImageGenerationService;
import java.util.Arrays;
import java.util.List;

public class ImageGenerationHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        return command.toLowerCase().startsWith("generate image of ");
    }

    @Override
    public void handle(Context context, String command) {
        String prompt = command.substring("generate image of ".length()).trim();
        if (prompt.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify what image to generate.");
            return;
        }
        FeedbackProvider.speakAndToast(context, "Generating image of " + prompt);
        Intent intent = new Intent(context, ImageGenerationService.class);
        intent.putExtra("prompt", prompt);
        context.startService(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "generate image of a cat",
            "generate image of a robot holding a skateboard"
        );
    }
} 