package com.mvp.sara;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.media.AudioManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mvp.sara.handlers.OpenAppHandler;
import com.mvp.sara.handlers.CallContactHandler;
import com.mvp.sara.handlers.PlayMusicHandler;
import com.mvp.sara.handlers.VolumeHandler;
import com.mvp.sara.handlers.BrightnessHandler;
import com.mvp.sara.handlers.AirplaneModeHandler;
import com.mvp.sara.handlers.FlashlightHandler;
import com.mvp.sara.handlers.DoNotDisturbHandler;
import com.mvp.sara.handlers.OpenSettingsHandler;
import com.mvp.sara.handlers.SaraSettingsHandler;
import com.mvp.sara.handlers.OpenCameraHandler;
import com.mvp.sara.handlers.TakePhotoHandler;
import com.mvp.sara.handlers.SendSmsHandler;
import com.mvp.sara.handlers.ReminderHandler;
import com.mvp.sara.handlers.AlarmHandler;
import com.mvp.sara.handlers.BatteryHandler;
import com.mvp.sara.handlers.NoteHandler;
import com.mvp.sara.handlers.SmsPrompt;
import com.mvp.sara.handlers.WifiHandler;
import com.mvp.sara.handlers.CalendarHandler;
import com.mvp.sara.handlers.WeatherHandler;
import com.mvp.sara.handlers.DeviceInfoHandler;
import com.mvp.sara.handlers.ClickLabelHandler;
import com.mvp.sara.handlers.CallAnswerHandler;
import com.mvp.sara.handlers.TypeTextHandler;
import com.mvp.sara.handlers.WhatsAppHandler;
import com.mvp.sara.handlers.PaymentHandler;
import com.mvp.sara.handlers.FindPhoneHandler;
import com.mvp.sara.handlers.SearchHandler;
import android.text.TextUtils;
import android.view.View;
import android.util.Log;
import com.mvp.sara.handlers.RingerModeHandler;
import com.mvp.sara.handlers.NavigationHandler;
import com.mvp.sara.handlers.TimerHandler;
import com.mvp.sara.handlers.CalculatorHandler;
import com.mvp.sara.handlers.JokeHandler;
import com.mvp.sara.handlers.SmsReaderHandler;
import com.mvp.sara.handlers.LocationHandler;
import com.mvp.sara.handlers.ScreenshotHandler;
import com.mvp.sara.handlers.OpenWebsiteHandler;
import com.mvp.sara.handlers.NewsHandler;
import com.mvp.sara.handlers.DictionaryHandler;
import com.mvp.sara.handlers.MusicControlHandler;
import com.mvp.sara.handlers.HelpHandler;
import com.mvp.sara.handlers.EmailHandler;
import com.mvp.sara.handlers.StopwatchHandler;
import com.mvp.sara.handlers.RandomUtilityHandler;
import com.mvp.sara.handlers.ReadScreenHandler;
import com.mvp.sara.handlers.TranslateHandler;
import com.mvp.sara.handlers.ImageAnalysisHandler;
import com.mvp.sara.handlers.BluetoothHandler;
import com.mvp.sara.handlers.CameraTranslateHandler;
import com.mvp.sara.handlers.ChitChatHandler;

public class MainActivity extends AppCompatActivity {

    private Button btnEnableAccessibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android.util.Log.d("MainActivity", "MainActivity onCreate called");

        // Request microphone permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        // Request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 3);
        }

        // Request phone state permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE}, 4);
        }

        // Request answer phone calls permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ANSWER_PHONE_CALLS}, 5);
        }

        // Request foreground service microphone permission for Android 14+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.FOREGROUND_SERVICE_MICROPHONE")
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.FOREGROUND_SERVICE_MICROPHONE"}, 2);
            }
        }

        // Request contacts permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, 6);
        }

        // Request call phone permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, 7);
        }

        // Request send SMS permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, 8);
        }

        // Request read SMS permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS}, 9);
        }

        // Request location permissions if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
        }

        // Check and request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            android.widget.Toast.makeText(this, "Please grant overlay permission for Sara to work over other apps", android.widget.Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // Check and request write settings permission for brightness control
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                android.widget.Toast.makeText(this, "Please grant write settings permission for Sara to control brightness", android.widget.Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        // Initialize button
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility);
        findViewById(R.id.btn_call_sara).setVisibility(View.GONE); // Hide the old button

        // Register command handlers
        CommandRegistry.register(new FindPhoneHandler());
        CommandRegistry.register(new OpenAppHandler());
        CommandRegistry.register(new CallContactHandler());
        CommandRegistry.register(new PlayMusicHandler());
        CommandRegistry.register(new VolumeHandler());
        CommandRegistry.register(new BrightnessHandler());
        CommandRegistry.register(new AirplaneModeHandler());
        CommandRegistry.register(new FlashlightHandler());
        CommandRegistry.register(new DoNotDisturbHandler());
        CommandRegistry.register(new OpenSettingsHandler());
        CommandRegistry.register(new SaraSettingsHandler());
        CommandRegistry.register(new OpenCameraHandler());
        CommandRegistry.register(new TakePhotoHandler());
        CommandRegistry.register(new SendSmsHandler());
        CommandRegistry.register(new ReminderHandler());
        CommandRegistry.register(new RingerModeHandler());
        CommandRegistry.register(new WeatherHandler());
        CommandRegistry.register(new NavigationHandler());
        CommandRegistry.register(new TimerHandler());
        CommandRegistry.register(new CalculatorHandler());
        CommandRegistry.register(new DeviceInfoHandler());
        CommandRegistry.register(new JokeHandler());
        CommandRegistry.register(new SmsReaderHandler());
        CommandRegistry.register(new LocationHandler());
        CommandRegistry.register(new ScreenshotHandler());
        CommandRegistry.register(new OpenWebsiteHandler());
        CommandRegistry.register(new NewsHandler());
        CommandRegistry.register(new DictionaryHandler());
        CommandRegistry.register(new MusicControlHandler());
        CommandRegistry.register(new HelpHandler());
        CommandRegistry.register(new EmailHandler());
        CommandRegistry.register(new StopwatchHandler());
        CommandRegistry.register(new RandomUtilityHandler());
        CommandRegistry.register(new ReadScreenHandler());
        CommandRegistry.register(new TranslateHandler());
        CommandRegistry.register(new ImageAnalysisHandler());
        CommandRegistry.register(new CameraTranslateHandler());
        CommandRegistry.register(new ChitChatHandler());
        CommandRegistry.register(new AlarmHandler());
        CommandRegistry.register(new BatteryHandler());
        CommandRegistry.register(new NoteHandler());
        CommandRegistry.register(new ClickLabelHandler());
        CommandRegistry.register(new CallAnswerHandler());
        CommandRegistry.register(new TypeTextHandler());
        CommandRegistry.register(new WhatsAppHandler());
        CommandRegistry.register(new PaymentHandler());
        CommandRegistry.register(new SearchHandler());
        CommandRegistry.register(new WifiHandler());
        CommandRegistry.register(new BluetoothHandler());

        
        // Stubs
        CommandRegistry.register(new CalendarHandler());
        CommandRegistry.register(new DeviceInfoHandler());
        
        btnEnableAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        // If all permissions are in place, start the service directly
        if (isAccessibilityServiceEnabled()) {
            startAssistantService();
        }
    }

    private void startAssistantService() {
        Log.d("MainActivity", "Starting assistant service and finishing activity.");
        Intent serviceIntent = new Intent(this, SaraVoiceService.class);
        serviceIntent.setAction("com.mvp.sara.ACTION_START_COMMAND_LISTENING");
        startForegroundService(serviceIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for accessibility service
        if (isAccessibilityServiceEnabled()) {
            btnEnableAccessibility.setVisibility(View.GONE);
            // If the service got enabled while the app was in background,
            // start the assistant now.
            startAssistantService();
        } else {
            btnEnableAccessibility.setVisibility(View.VISIBLE);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String serviceId = getPackageName() + "/" + ClickAccessibilityService.class.getCanonicalName();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (settingValue != null) {
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                if (splitter.next().equalsIgnoreCase(serviceId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // FeedbackProvider.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // RECORD_AUDIO permission granted
                android.util.Log.d("MainActivity", "RECORD_AUDIO permission granted");
            } else {
                // Permission denied, show a message or disable voice features
                android.widget.Toast.makeText(this, "Microphone permission is required for Sara to work", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // FOREGROUND_SERVICE_MICROPHONE permission granted
                android.util.Log.d("MainActivity", "FOREGROUND_SERVICE_MICROPHONE permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Foreground service permission is required for Sara to work in background", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 3) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // CAMERA permission granted
                android.util.Log.d("MainActivity", "CAMERA permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Camera permission is required for Sara to work", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 4) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_PHONE_STATE permission granted
                android.util.Log.d("MainActivity", "READ_PHONE_STATE permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Phone state permission is required for Sara to work", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 5) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ANSWER_PHONE_CALLS permission granted
                android.util.Log.d("MainActivity", "ANSWER_PHONE_CALLS permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Answer phone calls permission is required for Sara to work", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 6) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_CONTACTS permission granted
                android.util.Log.d("MainActivity", "READ_CONTACTS permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Contacts permission is required for Sara to announce caller names", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 7) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // CALL_PHONE permission granted
                android.util.Log.d("MainActivity", "CALL_PHONE permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Call phone permission is required for Sara to make calls", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 8) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SEND_SMS permission granted
                android.util.Log.d("MainActivity", "SEND_SMS permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Send SMS permission is required for Sara to send messages", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 9) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_SMS permission granted
                android.util.Log.d("MainActivity", "READ_SMS permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Read SMS permission is required for Sara to read messages", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted
                android.util.Log.d("MainActivity", "Location permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Location permission is required to find out where you are", android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_ASSIST.equals(intent.getAction())) {
            // Start the voice service and overlay
            Intent serviceIntent = new Intent(this, SaraVoiceService.class);
            serviceIntent.setAction("com.mvp.sara.ACTION_START_COMMAND_LISTENING");
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
            finish(); // Close MainActivity so only overlay is visible
        }
    }
}
