package com.mvp.sara.handlers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class SmsReaderHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "read my last message",
            "read my last text"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("read my last") && (command.contains("message") || command.contains("text"));
    }

    @Override
    public void handle(Context context, String command) {
        try {
            Cursor cursor = context.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI, null, null, null, "date DESC");
            if (cursor != null && cursor.moveToFirst()) {
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                FeedbackProvider.speakAndToast(context, "Your last message is from " + address + ". It says: " + body);
                cursor.close();
            } else {
                FeedbackProvider.speakAndToast(context, "You have no messages.");
                if (cursor != null) cursor.close();
            }
        } catch (SecurityException e) {
            FeedbackProvider.speakAndToast(context, "I need permission to read your text messages.");
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't read your messages.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 