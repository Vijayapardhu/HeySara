package com.mvp.sara;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class FileShareActivity extends AppCompatActivity {
    public static final String EXTRA_SERVER_URL = "server_url";
    private ImageView qrImageView;
    private Button selectFileButton;
    private Button shareMoreButton;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    // Store selected files
    private final List<Uri> selectedUris = new ArrayList<>();
    private final List<String> selectedNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_share);

        qrImageView = findViewById(R.id.qrImageView);
        selectFileButton = findViewById(R.id.selectFileButton);
        shareMoreButton = findViewById(R.id.shareMoreButton);

        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleFilePickerResult(result.getData());
                }
            }
        );

        String serverUrl = getIntent().getStringExtra(EXTRA_SERVER_URL);
        if (serverUrl != null && !serverUrl.isEmpty()) {
            showQrCode(serverUrl);
        } else {
            // No server URL, prompt for file selection
            selectFiles();
        }

        selectFileButton.setOnClickListener(v -> selectFiles());
        shareMoreButton.setOnClickListener(v -> selectFiles());
    }

    private void selectFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    private void handleFilePickerResult(@Nullable Intent data) {
        if (data == null) return;
        List<Uri> newUris = new ArrayList<>();
        List<String> newNames = new ArrayList<>();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                newUris.add(uri);
                newNames.add(getFileName(uri));
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            newUris.add(uri);
            newNames.add(getFileName(uri));
        }
        if (newUris.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show();
            return;
        }
        // Merge new files with existing, avoiding duplicates
        Set<String> uriStringsSet = new HashSet<>();
        for (Uri uri : selectedUris) uriStringsSet.add(uri.toString());
        for (int i = 0; i < newUris.size(); i++) {
            Uri uri = newUris.get(i);
            String uriStr = uri.toString();
            if (!uriStringsSet.contains(uriStr)) {
                selectedUris.add(uri);
                selectedNames.add(newNames.get(i));
                uriStringsSet.add(uriStr);
            }
        }
        // Start the service with the full list
        Intent serviceIntent = new Intent(this, FileShareService.class);
        serviceIntent.setAction(FileShareService.ACTION_START);
        ArrayList<String> uriStrings = new ArrayList<>();
        for (Uri uri : selectedUris) uriStrings.add(uri.toString());
        serviceIntent.putStringArrayListExtra(FileShareService.EXTRA_URIS, uriStrings);
        serviceIntent.putStringArrayListExtra(FileShareService.EXTRA_NAMES, new ArrayList<>(selectedNames));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        // Wait a moment for the server to start, then finish this activity (the service will launch a new FileShareActivity with the QR code)
        finish();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            String path = uri.getPath();
            int cut = path != null ? path.lastIndexOf('/') : -1;
            if (cut != -1) result = path.substring(cut + 1);
        }
        return result != null ? result : "file";
    }

    private void showQrCode(String serverUrl) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(serverUrl, BarcodeFormat.QR_CODE, 600, 600);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFFFFFFFF : 0x00000000); // white or transparent
                }
            }
            qrImageView.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
} 