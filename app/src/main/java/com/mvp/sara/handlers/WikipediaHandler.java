package com.mvp.sara.handlers;

import android.content.Context;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WikipediaHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.startsWith("wikipedia ") || lower.startsWith("tell me about ");
    }

    @Override
    public void handle(Context context, String command) {
        String query;
        String lower = command.toLowerCase(Locale.ROOT);
        if (lower.startsWith("wikipedia ")) {
            query = command.substring(10).trim();
        } else if (lower.startsWith("tell me about ")) {
            query = command.substring(13).trim();
        } else {
            query = command.trim();
        }
        FeedbackProvider.speakAndToast(context, "Searching Wikipedia for: " + query);
        new Thread(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String apiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encodedQuery;
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "HeySara/1.0 (Android Wikipedia Client)");
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    JSONObject json = new JSONObject(response.toString());
                    String extract = json.optString("extract", "No summary available.");
                    FeedbackProvider.speakAndToast(context, extract);
                } else {
                    FeedbackProvider.speakAndToast(context, "No Wikipedia article found for " + query);
                }
            } catch (Exception e) {
                FeedbackProvider.speakAndToast(context, "Error fetching Wikipedia summary.");
            }
        }).start();
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "wikipedia Albert Einstein",
            "wikipedia Android (operating system)",
            "wikipedia Artificial Intelligence"
        );
    }
} 