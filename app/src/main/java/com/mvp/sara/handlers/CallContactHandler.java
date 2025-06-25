package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.widget.Toast;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;

public class CallContactHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "call [contact name]",
            "call [number]",
            "make a call to [contact name]",
            "make a call to [number]",
            "dial [contact name]",
            "dial [number]"
    );

    // Conversational state for confirmation
    private static String pendingCallNumber = null;
    private static String pendingCallName = null;
    private static boolean awaitingCallConfirmation = false;

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        return lowerCmd.startsWith("call ") ||
               lowerCmd.startsWith("make a call to ") ||
               lowerCmd.startsWith("dial ");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase();
        // If awaiting confirmation
        if (awaitingCallConfirmation && pendingCallNumber != null) {
            if (lowerCmd.contains("yes") || lowerCmd.contains("call now") || lowerCmd.contains("confirm")) {
                makeDirectCall(context, pendingCallNumber);
                awaitingCallConfirmation = false;
                pendingCallNumber = null;
                pendingCallName = null;
            } else {
                FeedbackProvider.speakAndToast(context, "Cancelled the call.");
                awaitingCallConfirmation = false;
                pendingCallNumber = null;
                pendingCallName = null;
            }
            return;
        }
        String contactOrNumber = "";
        
        if (lowerCmd.startsWith("call ")) {
            contactOrNumber = command.substring(5).trim();
        } else if (lowerCmd.startsWith("make a call to ")) {
            contactOrNumber = command.substring(15).trim();
        } else if (lowerCmd.startsWith("dial ")) {
            contactOrNumber = command.substring(5).trim();
        }
        
        if (contactOrNumber.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify who to call");
            return;
        }
        
        if (contactOrNumber.matches("[\\d\\s\\+\\-\\(\\)]+")) {
            String cleanNumber = contactOrNumber.replaceAll("[\\s\\-\\(\\)]", "");
            // Ask for confirmation before calling
            pendingCallNumber = cleanNumber;
            pendingCallName = cleanNumber;
            awaitingCallConfirmation = true;
            String confirmation = "Do you want to call " + cleanNumber + "?";
            FeedbackProvider.speakAndToast(context, confirmation);
        } else {
            String number = getNumberForContact(context, contactOrNumber);
            if (number != null) {
                // Ask for confirmation before calling
                pendingCallNumber = number;
                pendingCallName = contactOrNumber;
                awaitingCallConfirmation = true;
                String confirmation = "Do you want to call " + contactOrNumber + " at " + number + "?";
                FeedbackProvider.speakAndToast(context, confirmation);
            } else {
                FeedbackProvider.speakAndToast(context, "Contact not found: " + contactOrNumber);
            }
        }
    }
    
    private void makeDirectCall(Context context, String number) {
        try {
            // Check CALL_PHONE permission at runtime
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + number));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                FeedbackProvider.speakAndToast(context, "Calling " + number);
            } else {
                // Permission not granted, open dialer instead
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + number));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                FeedbackProvider.speakAndToast(context, "Opening dialer for " + number + ". Please grant call permission for direct calling.");
            }
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't place the call.");
        }
    }

    private String getNumberForContact(Context context, String name) {
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + name + "%"},
                null
        );
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 