package com.mvp.sara.handlers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocationHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "where am i",
            "what's my current location",
            "get my address"
    );
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public boolean canHandle(String command) {
        return command.contains("where am i") || command.contains("my location") || command.contains("my address");
    }

    @Override
    public void handle(Context context, String command) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            FeedbackProvider.speakAndToast(context, "I need location permission to find you.");
            return;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        getAddressFromLocation(context, location);
                    } else {
                        FeedbackProvider.speakAndToast(context, "I couldn't get your location. Please make sure GPS is enabled.");
                    }
                })
                .addOnFailureListener(e -> FeedbackProvider.speakAndToast(context, "Failed to get location."));
    }

    private void getAddressFromLocation(Context context, Location location) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                FeedbackProvider.speakAndToast(context, "You are near " + addressText);
            } else {
                FeedbackProvider.speakAndToast(context, "I found your coordinates, but couldn't get a street address.");
            }
        } catch (IOException e) {
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't get your address right now.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 