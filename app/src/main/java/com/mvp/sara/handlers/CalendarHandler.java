package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CalendarHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "add event to my calendar",
            "create a calendar event"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("calendar") && (command.contains("add") || command.contains("create"));
    }

    @Override
    public void handle(Context context, String command) {
        // Remove trigger words
        String details = command.replace("add event", "").replace("create event", "").replace("add event to my calendar", "").replace("create a calendar event", "").trim();
        String title = details;
        long startTimeMillis = -1;
        String dateTimeStr = null;

        // Try to extract date and time (robust)
        // Examples: "meeting with John tomorrow at 3pm", "doctor appointment on June 5th at 2pm", "call mom at 15:30"
        Pattern dateTimePattern = Pattern.compile("(on [a-zA-Z0-9 ,]+)? ?(tomorrow)? ?at (\\d{1,2})(:(\\d{2}))? ?([ap]m)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = dateTimePattern.matcher(details);
        Calendar calendar = Calendar.getInstance();
        boolean foundTime = false;
        if (matcher.find()) {
            foundTime = true;
            // Title is before the time
            title = details.substring(0, matcher.start()).trim();
            String onDate = matcher.group(1);
            String tomorrow = matcher.group(2);
            String hourStr = matcher.group(3);
            String minStr = matcher.group(5);
            String ampm = matcher.group(6);
            int hour = Integer.parseInt(hourStr);
            int minute = (minStr != null) ? Integer.parseInt(minStr) : 0;
            if (ampm != null && ampm.equalsIgnoreCase("pm") && hour < 12) hour += 12;
            if (ampm != null && ampm.equalsIgnoreCase("am") && hour == 12) hour = 0;
            if (tomorrow != null) calendar.add(Calendar.DAY_OF_YEAR, 1);
            if (onDate != null && !onDate.trim().isEmpty()) {
                // Try to parse date (e.g., "on June 5th")
                String dateStr = onDate.replace("on", "").trim() + " " + calendar.get(Calendar.YEAR);
                try {
                    Date parsedDate = new SimpleDateFormat("MMMM d yyyy").parse(dateStr.replaceAll("(st|nd|rd|th)", ""));
                    calendar.setTime(parsedDate);
                } catch (ParseException e) {
                    // Ignore, fallback to today/tomorrow
                }
            }
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            startTimeMillis = calendar.getTimeInMillis();
            dateTimeStr = new SimpleDateFormat("EEE, MMM d 'at' h:mm a").format(calendar.getTime());
        }

        if (title.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please provide a title for the event.");
            return;
        }

        // Confirm event details with user
        String confirmMsg = "Create event: '" + title + "'";
        if (dateTimeStr != null) confirmMsg += " on " + dateTimeStr;
        FeedbackProvider.speakAndToast(context, confirmMsg);

        // (Optional: Wait for user confirmation. For now, proceed directly.)
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title);
        if (startTimeMillis != -1) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Opening calendar to create your event.");
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't open the calendar app.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 