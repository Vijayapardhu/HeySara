package com.mvp.sara;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
        executor.execute(() -> {
            try {
                GeminiResult result = GeminiImageHelper.analyzeImage(bitmap, "Describe this image? Respond with one or two sentences only.", GEMINI_API_KEY);
                runOnUiThread(() -> {
                    if (result.text != null) {
                        FeedbackProvider.speakAndToast(GeminiAnalysisService.this, result.text);
                    }
                    // Optionally handle result.image (generated image)
                    stopSelf();
                });
            } catch (Exception e) {
                Log.e("GeminiService", "Error generating content", e);
                FeedbackProvider.speakAndToast(GeminiAnalysisService.this, "Sorry, I couldn't analyze the image.");
                stopSelf();
            }
        });
    }

    private void runOnUiThread(Runnable runnable) {
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        handler.post(runnable);
    }

    public static class GeminiResult {
        public String text;
        public Bitmap image;
    }

    public static class GeminiImageHelper {
        public static GeminiResult analyzeImage(Bitmap bitmap, String prompt, String apiKey) throws Exception {
            // Convert Bitmap to base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            // Build JSON request
            String jsonInputString = "{" +
                    "\"contents\":[{" +
                    "  \"parts\":[{" +
                    "    \"inline_data\": {" +
                    "      \"mime_type\": \"image/jpeg\"," +
                    "      \"data\": \"" + base64Image + "\"" +
                    "    }" +
                    "  },{" +
                    "    \"text\": \"" + prompt + "\"" +
                    "  }]" +
                    "}]," +
                    "}";

            // Send HTTP POST
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonInputString.getBytes("utf-8"));
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Parse response
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray parts = jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content")
                    .getJSONArray("parts");

            GeminiResult result = new GeminiResult();
            for (int i = 0; i < parts.length(); i++) {
                JSONObject part = parts.getJSONObject(i);
                if (part.has("text")) {
                    result.text = part.getString("text");
                } else if (part.has("inline_data")) {
                    JSONObject inlineData = part.getJSONObject("inline_data");
                    if (inlineData.has("data")) {
                        byte[] imgBytes = Base64.decode(inlineData.getString("data"), Base64.DEFAULT);
                        result.image = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                    }
                }
            }
            return result;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 