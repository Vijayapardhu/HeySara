package com.mvp.sarah.handlers;

import android.content.Context;
import android.os.AsyncTask;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import android.util.Log;
import com.mvp.sarah.FeedbackProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class NewsHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String TAG = "NewsHandler";
    private static final List<String> COMMANDS = Arrays.asList(
            "news",
            "live news",
            "show me the news",
            "what's the news",
            "read the news"
    );

    private String apiKey = null;
    private boolean isFetchingKey = false;
    private final Object keyLock = new Object();

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("news") || lower.contains("headlines");
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
        db.collection("app_config").document("news").get()
            .addOnSuccessListener(document -> {
                if (document.exists() && document.contains("api_key")) {
                    apiKey = document.getString("api_key");
                    Log.d(TAG, "News API key fetched successfully.");
                } else {
                    Log.e(TAG, "News API key not found in Firebase.");
                }
                synchronized (keyLock) { isFetchingKey = false; }
                onReady.run();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to fetch News API key from Firebase.", e);
                synchronized (keyLock) { isFetchingKey = false; }
                onReady.run();
            });
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Fetching the latest news headlines...");
        fetchApiKey(() -> {
            if (apiKey == null || apiKey.isEmpty()) {
                FeedbackProvider.speakAndToast(context, "Sorry, the news service is not configured.");
                return;
            }
            new FetchNewsTask(context).execute();
        });
    }

    private class FetchNewsTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        FetchNewsTask(Context context) { this.context = context; }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlStr = "https://newsdata.io/api/1/news?apikey=" + apiKey + "&country=us&language=en&category=top";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JSONObject json = new JSONObject(response.toString());
                JSONArray articles = json.optJSONArray("results");
                if (articles == null || articles.length() == 0) {
                    Log.d(TAG, "No news articles found in API response.");
                    return null;
                }
                StringBuilder headlines = new StringBuilder();
                for (int i = 0; i < Math.min(5, articles.length()); i++) {
                    JSONObject article = articles.getJSONObject(i);
                    String title = article.optString("title");
                    if (title != null && !title.isEmpty()) {
                        headlines.append(i + 1).append(": ").append(title).append(". ");
                    }
                }
                return headlines.toString().trim();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching news", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.isEmpty()) {
                FeedbackProvider.speakAndToast(context, "Here are the top news headlines: " + result);
            } else {
                FeedbackProvider.speakAndToast(context, "Sorry, I couldn't fetch the news right now.");
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 