package com.mvp.sara;

import ai.picovoice.porcupine.PorcupineActivationException;
import ai.picovoice.porcupine.PorcupineActivationLimitException;
import ai.picovoice.porcupine.PorcupineActivationRefusedException;
import ai.picovoice.porcupine.PorcupineActivationThrottledException;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineInvalidArgumentException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioFocusRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import com.mvp.sara.handlers.SearchHandler;

public class SaraVoiceService extends Service implements AudioManager.OnAudioFocusChangeListener {
    public static final String ACTION_COMMAND_FINISHED = "com.mvp.sara.ACTION_COMMAND_FINISHED";
    public static final String ACTION_CLOSE_ASSISTANT_UI = "com.mvp.sara.ACTION_CLOSE_ASSISTANT_UI";
    private static final String CHANNEL_ID = "sara_voice_channel";
    private static final int NOTIF_ID = 1001;

    // Replace with your actual AccessKey
    private static final String PICOVOICE_ACCESS_KEY = "qjO6h/Siao9qoOZ0e/KTFaJKPcBTo2/RfYi1bjyf8P8LkS2JwYL7cw==";

    private PorcupineManager porcupineManager;
    private Handler handler;
    private boolean isShuttingDown = false;
    private boolean isPausedForCommand = false;
    private boolean isInCallListeningMode = false;
    private BroadcastReceiver callListeningReceiver;
    private BroadcastReceiver searchStopButtonReceiver;
    private WindowManager stopWindowManager;
    private View stopOverlayView;
    private BroadcastReceiver screenOnReceiver;
    private View bubbleOverlayView;
    private WindowManager bubbleWindowManager;
    private SpeechRecognizer bubbleRecognizer;
    private boolean isBubbleListening = false;
    private ObjectAnimator glowPulseAnimator;
    private ObjectAnimator bubblePulseAnimator;
    private BroadcastReceiver closeUIReciever;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private BroadcastReceiver interruptReceiver;

    private final PorcupineManagerCallback porcupineManagerCallback = new PorcupineManagerCallback() {
        @Override
        public void invoke(int keywordIndex) {
            Log.d("Porcupine", "Wake word detected!");
            if (isPausedForCommand) return;

            isPausedForCommand = true;
            try {
                porcupineManager.stop();
            } catch (PorcupineException e) {
                Log.e("Porcupine", "Failed to stop porcupine: " + e.getMessage());
            }

            // Wake word detected, now show overlay to listen for command
            sendBroadcast(new Intent("com.mvp.sara.ACTION_PLAY_BEEP"));
            wakeScreenAndNotify();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        startPorcupineListening();
        setupCallListeningReceiver();
        setupSearchStopButtonReceiver();
        setupCloseUIReciever();
        setupInterruptReceiver();
    }

    private void startPorcupineListening() {
        // Request audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setOnAudioFocusChangeListener(this)
                .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
        
        try {
            if (porcupineManager == null) {
                 porcupineManager = new PorcupineManager.Builder()
                        .setAccessKey(PICOVOICE_ACCESS_KEY)
                        .setKeywordPath("keywords/sara_android.ppn")
                        .build(getApplicationContext(), porcupineManagerCallback);
            }
            porcupineManager.start();
            Log.d("Porcupine", "Porcupine started listening...");
        } catch (PorcupineException e) {
            Log.e("Porcupine", "Failed to initialize or start Porcupine: " + e.getMessage());
        }
    }

    private void stopPorcupineListening() {
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                Log.d("Porcupine", "Porcupine stopped listening due to audio focus loss.");
            } catch (PorcupineException e) {
                Log.e("Porcupine", "Failed to stop Porcupine: " + e.getMessage());
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sara Assistant")
                .setContentText("Listening for 'Hey Sara'...")
                .setSmallIcon(R.drawable.ic_mic)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Sara Voice Service Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void wakeScreenAndNotify() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "sara:WakeLock");
        wakeLock.acquire(3000);
        showBubbleOverlayAndListen();
    }

    private void showBubbleOverlayAndListen() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        bubbleWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        bubbleOverlayView = inflater.inflate(R.layout.assistant_bubble, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 100;

        bubbleWindowManager.addView(bubbleOverlayView, params);

        // UI references
        View bubbleContainer = bubbleOverlayView.findViewById(R.id.bubble_container);
        View glowEffect = bubbleOverlayView.findViewById(R.id.glow_effect);
        com.mvp.sara.VoiceBarsView voiceLines = bubbleOverlayView.findViewById(R.id.voice_lines);

        // Start listening and animate
        startBubbleListening(bubbleContainer, glowEffect, voiceLines);
    }

    private void startBubbleListening(View bubbleContainer, View glowEffect, com.mvp.sara.VoiceBarsView voiceLines) {
        isBubbleListening = true;

        // Shrinking pulse for the main bubble
        bubblePulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                bubbleContainer,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.9f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.9f, 1f)
        );
        bubblePulseAnimator.setDuration(1500);
        bubblePulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        bubblePulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bubblePulseAnimator.start();

        // Slow pulse for the background glow
        glowPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                glowEffect,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 1f, 0.7f)
        );
        glowPulseAnimator.setDuration(2000);
        glowPulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        glowPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        glowPulseAnimator.start();

        // Show and animate voice lines
        voiceLines.setVisibility(View.VISIBLE);
        voiceLines.startBarsAnimation();

        if (bubbleRecognizer != null) {
            bubbleRecognizer.destroy();
        }
        bubbleRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        bubbleRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}
            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {}
            @Override
            public void onError(int error) {
                Log.e("SaraVoiceService", "Bubble recognition error: " + getErrorText(error));
                if (bubbleOverlayView != null) {
                    bubbleOverlayView.postDelayed(() -> removeBubbleOverlay(), 200);
                }
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase();
                    Log.d("SaraVoiceService", "Recognized command: " + command);
                    if (!CommandRegistry.handleCommand(SaraVoiceService.this, command)) {
                        Log.w("SaraVoiceService", "No handler for command: " + command);
                        // No visual feedback for unrecognized command, as requested.
                    }
                }
                if (bubbleOverlayView != null) {
                    bubbleOverlayView.postDelayed(() -> removeBubbleOverlay(), 200);
                }
            }
            @Override
            public void onPartialResults(Bundle partialResults) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
        bubbleRecognizer.startListening(intent);
    }

    private void removeBubbleOverlay() {
        try {
            if (glowPulseAnimator != null) {
                glowPulseAnimator.cancel();
                glowPulseAnimator = null;
            }
            if (bubblePulseAnimator != null) {
                bubblePulseAnimator.cancel();
                bubblePulseAnimator = null;
            }
            if (bubbleRecognizer != null) {
                bubbleRecognizer.destroy();
                bubbleRecognizer = null;
            }
            if (bubbleOverlayView != null) {
                com.mvp.sara.VoiceBarsView voiceLines = bubbleOverlayView.findViewById(R.id.voice_lines);
                if (voiceLines != null) voiceLines.stopBarsAnimation();
            }
            if (bubbleWindowManager != null && bubbleOverlayView != null) {
                bubbleWindowManager.removeView(bubbleOverlayView);
                bubbleOverlayView = null;
            }
        } catch (Exception e) {
            Log.e("SaraVoiceService", "Error removing bubble overlay: " + e.getMessage());
        }
        isBubbleListening = false;
        // Resume Porcupine listening
        isPausedForCommand = false;
        try {
            porcupineManager.start();
            Log.d("Porcupine", "Porcupine restarted listening.");
        } catch (PorcupineException e) {
            Log.e("Porcupine", "Failed to restart porcupine: " + e.getMessage());
        }
    }

    private String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: message = "Audio recording error"; break;
            case SpeechRecognizer.ERROR_CLIENT: message = "Client side error"; break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "Insufficient permissions"; break;
            case SpeechRecognizer.ERROR_NETWORK: message = "Network error"; break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: message = "Network timeout"; break;
            case SpeechRecognizer.ERROR_NO_MATCH: message = "No match"; break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: message = "Recognizer is busy"; break;
            case SpeechRecognizer.ERROR_SERVER: message = "Error from server"; break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "No speech input"; break;
            default: message = "Didn't understand, please try again."; break;
        }
        return message;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "com.mvp.sara.ACTION_START_COMMAND_LISTENING".equals(intent.getAction())) {
            Log.d("SaraVoiceService", "Received ACTION_START_COMMAND_LISTENING");
            if (isPausedForCommand) {
                 Log.d("SaraVoiceService", "Already in a listening state, ignoring.");
                 return START_STICKY;
            }

            isPausedForCommand = true;
            if (porcupineManager != null) {
                try {
                    porcupineManager.stop();
                    Log.d("Porcupine", "Porcupine stopped for direct command listening.");
                } catch (PorcupineException e) {
                    Log.e("Porcupine", "Failed to stop porcupine for command listening: " + e.getMessage());
                }
            }
            wakeScreenAndNotify();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isShuttingDown = true;
        
        // Abandon audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioManager != null && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            if (audioManager != null) {
                audioManager.abandonAudioFocus(this);
            }
        }
        
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
            } catch (PorcupineException e) {
                Log.e("Porcupine", "Failed to stop/delete porcupine: " + e.getMessage());
            }
        }
        
        if (callListeningReceiver != null) {
            try {
                unregisterReceiver(callListeningReceiver);
            } catch (Exception e) {
                Log.w("SaraVoiceService", "Error unregistering call listening receiver: " + e.getMessage());
            }
        }
        if (searchStopButtonReceiver != null) {
            try {
                unregisterReceiver(searchStopButtonReceiver);
            } catch (Exception e) {
                Log.w("SaraVoiceService", "Error unregistering search stop button receiver: " + e.getMessage());
            }
        }
        if (screenOnReceiver != null) {
            unregisterReceiver(screenOnReceiver);
        }
        if (closeUIReciever != null) {
            unregisterReceiver(closeUIReciever);
        }
        if (interruptReceiver != null) {
            unregisterReceiver(interruptReceiver);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupCallListeningReceiver() {
        callListeningReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.mvp.sara.ACTION_START_CALL_LISTENING".equals(action)) {
                    String number = intent.getStringExtra("number");
                    String name = intent.getStringExtra("name");
                    startCallListeningMode(number, name);
                } else if ("com.mvp.sara.ACTION_STOP_CALL_LISTENING".equals(action)) {
                    stopCallListeningMode();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mvp.sara.ACTION_START_CALL_LISTENING");
        filter.addAction("com.mvp.sara.ACTION_STOP_CALL_LISTENING");
        registerReceiver(callListeningReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }
    
    private void startCallListeningMode(String phoneNumber, String callerName) {
        Log.d("SaraVoiceService", "Starting call listening mode for: " + callerName);
        isInCallListeningMode = true;
        
        // Pause wake word detection
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
            } catch (PorcupineException e) {
                Log.e("SaraVoiceService", "Failed to stop porcupine: " + e.getMessage());
            }
        }
        
        // Start immediate voice recognition for call commands
        startImmediateCommandListening();
    }
    
    private void stopCallListeningMode() {
        Log.d("SaraVoiceService", "Stopping call listening mode");
        isInCallListeningMode = false;
        
        // Resume wake word detection
        if (porcupineManager != null) {
            try {
                porcupineManager.start();
            } catch (PorcupineException e) {
                Log.e("SaraVoiceService", "Failed to start porcupine: " + e.getMessage());
            }
        }
    }
    
    private void startImmediateCommandListening() {
        // Create overlay and start listening immediately without wake word
        wakeScreenAndNotify();
    }

    private void setupSearchStopButtonReceiver() {
        searchStopButtonReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                hideStopButtonOverlay();
            }
        };
        IntentFilter filter = new IntentFilter(SearchHandler.ACTION_STOP_SEARCH);
        registerReceiver(searchStopButtonReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void showStopButtonOverlay() {
        if (stopOverlayView != null) return; // Already showing

        stopWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        stopOverlayView = inflater.inflate(R.layout.floating_stop_button, null);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 100; // 100px from the bottom

        stopOverlayView.findViewById(R.id.btn_stop_search).setOnClickListener(v -> {
            // Send broadcast to stop the search
            sendBroadcast(new Intent(SearchHandler.ACTION_STOP_SEARCH));
            hideStopButtonOverlay(); // Hide immediately on click
        });

        try {
            stopWindowManager.addView(stopOverlayView, params);
        } catch (Exception e) {
            Log.e("SaraVoiceService", "Error adding stop button overlay", e);
        }
    }

    private void hideStopButtonOverlay() {
        if (stopOverlayView != null && stopWindowManager != null) {
            try {
                stopWindowManager.removeView(stopOverlayView);
            } catch (Exception e) {
                Log.e("SaraVoiceService", "Error removing stop button overlay", e);
            }
            stopOverlayView = null;
            stopWindowManager = null;
        }
    }

    private void setupCloseUIReciever() {
        closeUIReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                removeBubbleOverlay();
                hideStopButtonOverlay();
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_CLOSE_ASSISTANT_UI);
        registerReceiver(closeUIReciever, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void setupInterruptReceiver() {
        interruptReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.mvp.sara.ACTION_INTERRUPT".equals(action)) {
                    stopAllAssistantActions();
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.mvp.sara.ACTION_INTERRUPT");
        registerReceiver(interruptReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void stopAllAssistantActions() {
        // Stop speech recognition
        if (bubbleRecognizer != null) {
            bubbleRecognizer.cancel();
            bubbleRecognizer.destroy();
            bubbleRecognizer = null;
        }
        // Remove overlays
        removeBubbleOverlay();
        hideStopButtonOverlay();
        // Reset state
        isPausedForCommand = false;
        // Provide feedback
        handler.post(() -> Toast.makeText(this, "Okay, I've stopped.", Toast.LENGTH_SHORT).show());
        // Optionally, speak feedback
        FeedbackProvider.speakAndToast(this, "Okay, I've stopped.");
        // Resume Porcupine listening
        try {
            if (porcupineManager != null) porcupineManager.start();
        } catch (PorcupineException e) {
            Log.e("Porcupine", "Failed to restart porcupine: " + e.getMessage());
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d("AudioFocus", "Gained audio focus, starting Porcupine.");
                startPorcupineListening();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d("AudioFocus", "Lost audio focus, stopping Porcupine.");
                stopPorcupineListening();
                break;
        }
    }
}