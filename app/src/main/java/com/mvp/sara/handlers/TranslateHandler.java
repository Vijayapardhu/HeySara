package com.mvp.sara.handlers;

import android.content.Context;
import android.os.AsyncTask;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "translate 'hello' to spanish",
            "say 'good morning' in french"
    );
    private static final Pattern TRANSLATE_PATTERN = Pattern.compile(
        "(?:translate|say) '([^']+)' (?:to|in) (\\w+)", 
        Pattern.CASE_INSENSITIVE
    );
    private static final Map<String, String> langMap = new HashMap<>();
    static {
        langMap.put("french", "fr");
        langMap.put("spanish", "es");
        langMap.put("german", "de");
        langMap.put("italian", "it");
        langMap.put("japanese", "ja");
        langMap.put("korean", "ko");
        // Add more languages as needed
    }

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("translate") || command.startsWith("say");
    }

    @Override
    public void handle(Context context, String command) {
        Matcher matcher = TRANSLATE_PATTERN.matcher(command);
        if (matcher.find()) {
            String textToTranslate = matcher.group(1);
            String targetLangName = matcher.group(2).toLowerCase();
            String targetLangCode = langMap.get(targetLangName);

            if (targetLangCode == null) {
                FeedbackProvider.speakAndToast(context, "I don't know how to translate to " + targetLangName + " yet.");
                return;
            }
            new FetchTranslationTask(context, textToTranslate, targetLangCode).execute();
        } else {
            FeedbackProvider.speakAndToast(context, "Please say: translate 'phrase' to [language].");
        }
    }

    private static class FetchTranslationTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final String text, langCode;

        FetchTranslationTask(Context context, String text, String langCode) {
            this.context = context;
            this.text = text;
            this.langCode = langCode;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlStr = "https://api.mymemory.translated.net/get?q=" + 
                                URLEncoder.encode(text, "UTF-8") + "&langpair=en|" + langCode;
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
                return json.getJSONObject("responseData").getString("translatedText");
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                FeedbackProvider.speakAndToast(context, result);
            } else {
                FeedbackProvider.speakAndToast(context, "Sorry, I couldn't translate that right now.");
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 