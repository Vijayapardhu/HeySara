package com.mvp.sarah.data;

import android.content.Context;
import android.util.Log;
import com.mvp.sarah.NetworkUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1 second

    public static String makeApiCall(String apiUrl, String jsonPayload, String apiKey, Context context) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.e(TAG, "No network connection available.");
            return null;
        }

        for (int retryCount = 0; retryCount < MAX_RETRIES; retryCount++) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(apiUrl + "?key=" + apiKey);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000); // 15 seconds
                conn.setReadTimeout(15000); // 15 seconds

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        return response.toString();
                    }
                } else if (responseCode >= 500) { // Server error, worth retrying
                    Log.w(TAG, "API call failed with server error: " + responseCode + ". Retrying...");
                    // Fall through to the retry logic
                } else { // Client error (4xx), not worth retrying
                    Log.e(TAG, "API call failed with client error: " + responseCode);
                    // You might want to read the error stream here to log details
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during API call", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            // If we reach here, it means the request failed and we should retry
            if (retryCount < MAX_RETRIES - 1) {
                try {
                    long backoffTime = INITIAL_BACKOFF_MS * (long) Math.pow(2, retryCount);
                    Log.d(TAG, "Waiting for " + backoffTime + "ms before next retry.");
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        Log.e(TAG, "API call failed after " + MAX_RETRIES + " retries.");
        return null;
    }
}
