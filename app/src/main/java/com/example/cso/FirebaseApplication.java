package com.example.cso;

import android.app.Application;
import android.provider.Settings;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.jaredrummler.android.device.DeviceName;

public class FirebaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try{
            FirebaseApp.initializeApp(this);
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
            String userId = Settings.Secure.getString(getApplicationContext().getContentResolver(),Settings.Secure.ANDROID_ID);
            String userName = DeviceName.getDeviceName();
            FirebaseAnalytics.getInstance(this).setUserProperty("user_id", userId);
            FirebaseAnalytics.getInstance(this).setUserProperty("user_name", userName);
            FirebaseAnalytics.getInstance(this).setUserId(userId);
            FirebaseCrashlytics.getInstance().setCustomKey("user_id", userId);
            FirebaseCrashlytics.getInstance().setCustomKey("user_name", userName);
            FirebaseCrashlytics.getInstance().setUserId(userId);
        }catch (Exception e){

        }

    }
}
