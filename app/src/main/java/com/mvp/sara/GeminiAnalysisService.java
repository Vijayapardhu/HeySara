package com.mvp.sara;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiAnalysisService extends Service {
    private final Executor executor = Executors.newSingleThreadExecutor();
    private static final String GEMINI_API_KEY = "AIzaSyA5Czfapq60ZbIWO-5UjZVtIlHAWefHUl4";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String imagePath = intent.getStringExtra("image_path");
        if (imagePath == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            File imgFile = new File(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(imgFile));
            analyzeBitmap(bitmap);
        } catch (Exception e) {
            Log.e("GeminiService", "Error processing image", e);
            FeedbackProvider.speakAndToast(this, "Sorry, I couldn't process the image.");
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void analyzeBitmap(Bitmap bitmap) {
        GenerativeModel gm = new GenerativeModel("gemini-pro-vision", GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder()
                .addImage(bitmap)
                .addText("Describe this image in detail.")
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String description = result.getText();
                FeedbackProvider.speakAndToast(GeminiAnalysisService.this, description);
                stopSelf();
            }
            @Override
            public void onFailure(Throwable t) {
                Log.e("GeminiService", "Error generating content", t);
                FeedbackProvider.speakAndToast(GeminiAnalysisService.this, "Sorry, I couldn't analyze the image.");
                stopSelf();
            }
        }, executor);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 