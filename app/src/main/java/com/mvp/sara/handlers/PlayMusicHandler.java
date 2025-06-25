package com.mvp.sara.handlers;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PlayMusicHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.startsWith("play ") || lower.contains("play song") || lower.contains("play music");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        String song = null;
        String app = null;

        // Try to extract app name
        if (lower.contains("on spotify")) {
            app = "spotify";
            song = extractSong(command, "on spotify");
        } else if (lower.contains("in spotify")) {
            app = "spotify";
            song = extractSong(command, "in spotify");
        } else if (lower.contains("on youtube music")) {
            app = "ytmusic";
            song = extractSong(command, "on youtube music");
        } else if (lower.contains("in youtube music")) {
            app = "ytmusic";
            song = extractSong(command, "in youtube music");
        } else if (lower.contains("on youtube")) {
            app = "youtube";
            song = extractSong(command, "on youtube");
        } else if (lower.contains("in youtube")) {
            app = "youtube";
            song = extractSong(command, "in youtube");
        } else if (lower.contains("on music")) {
            app = "default";
            song = extractSong(command, "on music");
        } else if (lower.contains("in music")) {
            app = "default";
            song = extractSong(command, "in music");
        } else if (lower.contains("in amazon music")) {
            app = "amazon music";
            song = extractSong(command, "in amazon music");
        } else if (lower.contains("on amazon music")) {
            app = "amazon music";
            song = extractSong(command, "on amazon music");
        } else if (lower.contains("on amazon")) {
            app = "amazon music";
            song = extractSong(command, "on amazon");
        } else if (lower.contains("in amazon")) {
            app = "amazon music";
            song = extractSong(command, "in amazon");
        } else if (lower.contains("on apple music")) {
            app = "apple music";
            song = extractSong(command, "on apple music");
        } else if (lower.contains("in apple music")) {
            app = "apple music";
            song = extractSong(command, "in apple music");
        } else if (lower.contains("on apple")) {
            app = "apple music";
            song = extractSong(command, "on apple");
        } else if (lower.contains("in apple")) {
            app = "apple music";
            song = extractSong(command, "in apple");
        } else {
            // Default: try to extract after "play"
            int idx = lower.indexOf("play ");
            if (idx != -1) {
                song = command.substring(idx + 5).trim();
            }
        }

        if (TextUtils.isEmpty(song)) {
            FeedbackProvider.speakAndToast(context, "What song or artist do you want to play?");
            return;
        }

        if (app == null || app.equals("default")) {
            // Try default music player
            Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
            intent.putExtra(SearchManager.QUERY, song);
            try {
                context.startActivity(intent);
                FeedbackProvider.speakAndToast(context, "Playing " + song + ".");
                return;
            } catch (Exception e) {
                // Fallback to YouTube Music if installed
                if (isAppInstalled(context, "com.google.android.apps.youtube.music")) {
                    launchYouTubeMusic(context, song);
                    return;
                }
                FeedbackProvider.speakAndToast(context, "Couldn't find a music app to play your song.");
                return;
            }
        } else if (app.equals("spotify")) {
            if (isAppInstalled(context, "com.spotify.music")) {
                launchSpotify(context, song);
            } else {
                FeedbackProvider.speakAndToast(context, "Spotify is not installed.");
            }
        } else if (app.equals("amazon music")) {
            if (isAppInstalled(context, "com.amazon.mp3")) {
                launchAmazonMusic(context, song);
            } else {
                FeedbackProvider.speakAndToast(context, "Amazon Music is not installed.");
            }
        } else if (app.equals("apple music")) {
            if (isAppInstalled(context, "com.apple.android.music")) {
                launchAppleMusic(context, song);
            } else {
                FeedbackProvider.speakAndToast(context, "Apple Music is not installed.");
            }
        } else if (app.equals("ytmusic") || app.equals("youtube")) {
            if (isAppInstalled(context, "com.google.android.apps.youtube.music")) {
                launchYouTubeMusic(context, song);
            } else if (isAppInstalled(context, "com.google.android.youtube")) {
                launchYouTube(context, song);
            } else {
                FeedbackProvider.speakAndToast(context, "YouTube Music is not installed.");
            }
        } else {
            FeedbackProvider.speakAndToast(context, "Sorry, I don't know how to play music in that app yet.");
        }
    }

    private String extractSong(String command, String keyword) {
        int idx = command.toLowerCase(Locale.ROOT).indexOf("play ");
        if (idx != -1) {
            String afterPlay = command.substring(idx + 5);
            int appIdx = afterPlay.toLowerCase(Locale.ROOT).indexOf(keyword);
            if (appIdx != -1) {
                return afterPlay.substring(0, appIdx).trim();
            } else {
                return afterPlay.trim();
            }
        }
        return null;
    }

    private boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void launchSpotify(Context context, String song) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("spotify:search:" + Uri.encode(song)));
            intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.getPackageName()));
            intent.setPackage("com.spotify.music");
            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Searching for " + song + " in Spotify.");
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Couldn't open Spotify.");
        }
    }

    private void launchYouTubeMusic(Context context, String song) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("app.revanced.android.apps.youtube.music");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                FeedbackProvider.speakAndToast(context, "Opened YouTube Music. Please search for " + song + ".");
                // Optionally, use accessibility to type the song name
            } else {
                FeedbackProvider.speakAndToast(context, "Couldn't open YouTube Music.");
            }
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Couldn't open YouTube Music.");
        }
    }

    private void launchYouTube(Context context, String song) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage("com.google.android.youtube");
            intent.putExtra("query", song);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Searching for " + song + " on YouTube.");
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Couldn't open YouTube.");
        }
    }

    private void launchAmazonMusic(Context context, String song) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.amazon.mp3");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                FeedbackProvider.speakAndToast(context, "Opened Amazon Music. Please search for " + song + ".");
            } else {
                FeedbackProvider.speakAndToast(context, "Couldn't open Amazon Music.");
            }
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Couldn't open Amazon Music.");
        }
    }

    private void launchAppleMusic(Context context, String song) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.apple.android.music");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                FeedbackProvider.speakAndToast(context, "Opened Apple Music. Please search for " + song + ".");
            } else {
                FeedbackProvider.speakAndToast(context, "Couldn't open Apple Music.");
            }
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Couldn't open Apple Music.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
                "Play Shape of You on Spotify",
                "Play Imagine Dragons",
                "Play album Thriller",
                "Play Bohemian Rhapsody in YouTube Music"
        );
    }
} 