package com.mvp.sarah;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.mvp.sarah.data.ApiClient;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import android.util.Base64;
import com.google.firebase.firestore.FirebaseFirestore;

public class GeminiAnalysisService extends Service {

    private static final String TAG = "GeminiAnalysisService";
    private String apiKey = null;
    private boolean isFetchingKey = false;
    private final Object keyLock = new Object();
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public GeminiAnalysisService getService() {
            return GeminiAnalysisService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void fetchApiKey(Runnable onReady) {
        synchronized (keyLock) {
            if (apiKey != null) {
                onReady.run();
                return;
            }
            if (isFetchingKey) return;
            isFetchingKey = true;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("app_config").document("gemini").get()
            .addOnSuccessListener(document -> {
                if (document.exists() && document.contains("api_key")) {
                    apiKey = document.getString("api_key");
                }
                synchronized (keyLock) { isFetchingKey = false; }
                onReady.run();
            })
            .addOnFailureListener(e -> {
                synchronized (keyLock) { isFetchingKey = false; }
                onReady.run();
            });
    }

    public void analyzeImage(Bitmap image, AnalysisCallback callback) {
        fetchApiKey(() -> {
            new Thread(() -> {
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

                    String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro-vision:generateContent";
                    String innerPrompt = "Analyze the screen for a question and its options. Also, look for a submit button. " +
                                       "Return a JSON object with the following structure: " +
                                       "{\"action\": \"click\" or \"type\", \"x\": X_COORDINATE, \"y\": Y_COORDINATE, \"text\": \"ANSWER_TEXT\", \"submit_button\": {\"found\": true/false, \"x\": SUBMIT_X, \"y\": SUBMIT_Y}}. " +
                                       "If no question is found, return null for the answer fields. If no submit button is found, set found to false.";
                    String outerPrompt = "Please perform the following analysis and wrap the resulting JSON object in another JSON object with two fields: 'type' and 'response'. " +
                                       "The 'type' should be 'image_analysis'. The 'response' should be the JSON object you generate from the analysis. " +
                                       "The analysis task is: " + innerPrompt;

                    String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + JSONObject.quote(outerPrompt) + "\"},{\"inline_data\":{\"mime_type\":\"image/jpeg\",\"data\":\"" + base64Image + "\"}}]}]}";

                    String response = ApiClient.makeApiCall(apiUrl, jsonPayload, apiKey, this);

                    if (response != null) {
                        JSONObject outerResponse = new JSONObject(response);
                        String innerJsonString = outerResponse.getJSONArray("candidates")
                                                          .getJSONObject(0)
                                                          .getJSONObject("content")
                                                          .getJSONArray("parts")
                                                          .getJSONObject(0)
                                                          .getString("text");

                        JSONObject innerResponse = new JSONObject(innerJsonString);
                        String type = innerResponse.optString("type", "unknown");

                        if ("image_analysis".equals(type)) {
                            String analysisResult = innerResponse.getJSONObject("response").toString();
                            callback.onAnalysisComplete(analysisResult);
                        } else {
                            Log.w(TAG, "Received unexpected response type: " + type);
                            callback.onAnalysisComplete(null);
                        }
                    } else {
                        Log.e(TAG, "Failed to get response from API.");
                        callback.onAnalysisComplete(null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error analyzing image", e);
                    callback.onAnalysisComplete(null);
                }
            }).start();
        });
    }

    public interface AnalysisCallback {
        void onAnalysisComplete(String result);
    }
}