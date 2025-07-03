package com.mvp.sara;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import fi.iki.elonen.NanoHTTPD;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FileShareService extends Service {
    public static final String ACTION_STOP = "com.mvp.sara.action.STOP_FILE_SHARE";
    public static final String ACTION_SHARE_MORE = "com.mvp.sara.action.SHARE_MORE_FILES";
    public static final String ACTION_START = "com.mvp.sara.action.START_FILE_SHARE";
    public static final String EXTRA_URIS = "uris";
    public static final String EXTRA_NAMES = "names";
    public static final String CHANNEL_ID = "file_share_channel";
    public static final int NOTIF_ID = 2024;
    private NanoHTTPD server;
    private final ArrayList<Uri> fileUris = new ArrayList<>();
    private final ArrayList<String> fileNames = new ArrayList<>();
    private final Map<String, Uri> nameToUri = new HashMap<>();
    private String serverUrl = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_SHARE_MORE.equals(action)) {
                // Launch MainActivity to pick more files
                Intent pickIntent = new Intent(this, MainActivity.class);
                pickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                pickIntent.putExtra("share_file", true);
                startActivity(pickIntent);
                return START_STICKY;
            } else if (ACTION_START.equals(action)) {
                // Get files from intent
                fileUris.clear();
                fileNames.clear();
                nameToUri.clear();
                ArrayList<String> uriStrings = intent.getStringArrayListExtra(EXTRA_URIS);
                ArrayList<String> names = intent.getStringArrayListExtra(EXTRA_NAMES);
                if (uriStrings != null && names != null && uriStrings.size() == names.size()) {
                    for (int i = 0; i < uriStrings.size(); i++) {
                        Uri uri = Uri.parse(uriStrings.get(i));
                        String name = names.get(i);
                        fileUris.add(uri);
                        fileNames.add(name);
                        nameToUri.put(name, uri);
                    }
                    startServer();
                } else {
                    Toast.makeText(this, "No files to share.", Toast.LENGTH_LONG).show();
                    stopSelf();
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopServer();
        showDisconnectedNotification();
        super.onDestroy();
    }

    private void startServer() {
        stopServer();
        server = new NanoHTTPD(8080) {
            @Override
            public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
                String uri = session.getUri();
                Map<String, String> params = session.getParms();
                if ("/download".equals(uri) && params.containsKey("name")) {
                    String reqName = params.get("name");
                    Uri fileUri = nameToUri.get(reqName);
                    if (fileUri == null) {
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "File not found");
                    }
                    try {
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(fileUri, "r");
                        if (pfd == null) return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "File not found");
                        FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
                        NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream", fis, pfd.getStatSize());
                        res.addHeader("Content-Disposition", "attachment; filename=\"" + reqName + "\"");
                        showDownloadSuccessNotification(reqName);
                        return res;
                    } catch (Exception e) {
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
                    }
                } else if ("/qr.png".equals(uri)) {
                    // Serve QR code image
                    try {
                        Bitmap qr = generateQrCode(serverUrl);
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        qr.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] bytes = baos.toByteArray();
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "image/png", new java.io.ByteArrayInputStream(bytes), bytes.length);
                    } catch (Exception e) {
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "QR error");
                    }
                } else if ("/".equals(uri)) {
                    // Serve index page
                    StringBuilder html = new StringBuilder();
                    html.append("<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'><title>File Share</title>");
                    html.append("<style>body{font-family:sans-serif;background:#f7f7f7;margin:0;padding:0;}h1{color:#ff6600;text-align:center;}ul{list-style:none;padding:0;}li{margin:16px 0;display:flex;align-items:center;opacity:0;animation:fadeIn 0.8s forwards;}li:nth-child(n){animation-delay:calc(0.1s * var(--i));}a{display:inline-block;padding:12px 24px;background:#ff6600;color:#fff;text-decoration:none;border-radius:8px;transition:background 0.2s;margin-left:12px;}a:hover{background:#ff8800;}div.container{max-width:400px;margin:40px auto;background:#fff;padding:24px 16px 32px 16px;border-radius:16px;box-shadow:0 2px 16px rgba(0,0,0,0.08);}footer{text-align:center;color:#aaa;margin-top:32px;font-size:14px;}.file-icon{font-size:28px;vertical-align:middle;animation:bounce 1.2s cubic-bezier(.68,-0.55,.27,1.55) 1;}@keyframes bounce{0%{transform:translateY(-30px);}50%{transform:translateY(10px);}70%{transform:translateY(-5px);}100%{transform:translateY(0);}}@keyframes fadeIn{to{opacity:1;}}</style>");
                    html.append("</head><body><div class='container'><h1>Shared Files</h1><ul>");
                    int i = 1;
                    for (String name : fileNames) {
                        html.append("<li style='--i:").append(i).append("'><span class='file-icon'>📄</span><a href='download?name=").append(name).append("'>").append(name).append("</a></li>");
                        i++;
                    }
                    html.append("</ul></div><footer>Powered by Sara File Share</footer></body></html>");
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", html.toString());
                } else {
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "Not found");
                }
            }
        };
        try {
            server.start();
            String ip = getDeviceIpAddress();
            serverUrl = "http://" + ip + ":8080/";
            showNotification();
            Toast.makeText(this, "File server started at: " + serverUrl, Toast.LENGTH_LONG).show();
            // Launch FileShareActivity to show the QR code
            Intent qrIntent = new Intent(this, FileShareActivity.class);
            qrIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            qrIntent.putExtra(FileShareActivity.EXTRA_SERVER_URL, serverUrl);
            startActivity(qrIntent);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to start server: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
        stopForeground(true);
    }

    private void showNotification() {
        createNotificationChannel();
        Intent stopIntent = new Intent(this, FileShareService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent shareMoreIntent = new Intent(this, FileShareService.class);
        shareMoreIntent.setAction(ACTION_SHARE_MORE);
        PendingIntent shareMorePending = PendingIntent.getService(this, 2, shareMoreIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent openIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(serverUrl));
        PendingIntent openPending = PendingIntent.getActivity(this, 3, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sara File Share")
                .setContentText("Tap to open sharing page. Server running at " + serverUrl)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentIntent(openPending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
                .addAction(android.R.drawable.ic_menu_share, "Share More", shareMorePending)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_ID, notif);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "File Share", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Sara file sharing server");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private String getDeviceIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                java.net.NetworkInterface intf = en.nextElement();
                java.util.Enumeration<java.net.InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    java.net.InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "0.0.0.0";
    }

    private Bitmap generateQrCode(String text) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BarcodeEncoder encoder = new BarcodeEncoder();
        return encoder.createBitmap(writer.encode(text, BarcodeFormat.QR_CODE, 600, 600));
    }

    private void showDownloadSuccessNotification(String fileName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("File Downloaded")
                .setContentText(fileName + " was downloaded successfully.")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private void showDisconnectedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("File Share Stopped")
                .setContentText("The file sharing server has been stopped or disconnected.")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }
} 