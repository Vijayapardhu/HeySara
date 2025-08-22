package com.mvp.sarah.handlers;

import android.content.Context;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.FeedbackProvider;
import com.mvp.sarah.data.ApiClient;
import org.json.JSONObject;
import com.google.firebase.firestore.FirebaseFirestore;

public class GeminiHandler implements CommandHandler {

    private static final String TAG = "GeminiHandler";
    private String apiKey = null;
    private boolean isFetchingKey = false;
    private final Object keyLock = new Object();

    private void fetchApiKey(Context context, Runnable onReady) {
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

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase().trim();
        // Match general questions, but not calculations
        boolean isGeneral = lower.startsWith("ask") || lower.startsWith("what is") || lower.startsWith("who is") || lower.startsWith("explain");
        boolean isCalculation = lower.matches(".*\\d.*[+\\-*/].*\\d.*");
        return isGeneral && !isCalculation;
    }

    @Override
    public void handle(Context context, String command) {
        fetchApiKey(context, () -> {
            if (apiKey == null) {
                FeedbackProvider.speakAndToast(context, "Gemini API key not available.");
                return;
            }
            // Extract the actual query from the command
            String query = command;
            String lowercasedCommand = command.toLowerCase();
            if (lowercasedCommand.startsWith("ask")) {
                query = command.substring(3).trim();
            }
            final String finalQuery = "Please answer the following question and also provide a type for your response. " +
                                      "The response should be a JSON object with two fields: 'type' and 'response'. " +
                                      "The 'type' should be 'answer'. The 'response' should be your answer to the question. " +
                                      "The question is: " + query;

            new Thread(() -> {
                try {
                    String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
                    String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + JSONObject.quote(finalQuery) + "\"}]}]}";

                    String response = ApiClient.makeApiCall(apiUrl, jsonPayload, apiKey, context);

                    if (response != null) {
                        // The response from the API is a JSON string that contains another JSON string.
                        // We need to parse it twice.
                        JSONObject outerResponse = new JSONObject(response);
                        String innerJsonString = outerResponse.getJSONArray("candidates")
                                                          .getJSONObject(0)
                                                          .getJSONObject("content")
                                                          .getJSONArray("parts")
                                                          .getJSONObject(0)
                                                          .getString("text");

                        JSONObject innerResponse = new JSONObject(innerJsonString);
                        String type = innerResponse.optString("type", "unknown");
                        String responseText = innerResponse.optString("response", "I'm sorry, I don't have an answer for that.");

                        // For now, we just speak the response. In the future, we could use the 'type' for more complex logic.
                        FeedbackProvider.speakAndToast(context, responseText);
                    } else {
                        FeedbackProvider.speakAndToast(context, "Sorry, I couldn't get a response from the AI at the moment.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing Gemini response", e);
                    FeedbackProvider.speakAndToast(context, "Sorry, there was an error communicating with the AI.");
                }
            }).start();
        });
    }
}