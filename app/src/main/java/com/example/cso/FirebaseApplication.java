package com.example.cso;

import android.app.Application;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.jaredrummler.android.device.DeviceName;

public class FirebaseApplication extends Application {

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            // Initialize Firebase
            FirebaseApp.initializeApp(this);

            // Enable Firebase Analytics
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);

            // Enable Firebase Crashlytics collection
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

            // Get device and user information
            String userId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            String userName = DeviceName.getDeviceName();

            // Set user properties for analytics
            FirebaseAnalytics.getInstance(this).setUserProperty("user_id", userId);
            FirebaseAnalytics.getInstance(this).setUserProperty("user_name", userName);
            FirebaseAnalytics.getInstance(this).setUserId(userId);

            // Set custom keys for Crashlytics (for debugging/logging purposes)
            FirebaseCrashlytics.getInstance().setCustomKey("user_id", userId);
            FirebaseCrashlytics.getInstance().setCustomKey("user_name", userName);
            FirebaseCrashlytics.getInstance().setUserId(userId);

            // Log a custom event for data analytics
            logCustomEvent("app_launch", "app_version", "1.0.0");

        } catch (Exception e) {
            // Catch any error during Firebase initialization and log it to Crashlytics
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.d("firebase", "firebase initialization failed", e);
        }
    }

    public void logCustomEvent(String eventName, String paramKey, String paramValue) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(paramKey, paramValue);  // Add the parameters to the bundle
            FirebaseAnalytics.getInstance(this).logEvent(eventName, bundle);  // Log the event with parameters
            Log.d("firebase", "Custom event logged: " + eventName);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.d("firebase", "Error logging event: " + eventName, e);
        }
    }


    // Method to record non-fatal exceptions (can be used anywhere in the app)
    public static void logNonFatalException(Exception e) {
        FirebaseCrashlytics.getInstance().recordException(e);
        Log.d("firebase", "Non-fatal exception recorded", e);
    }
}
