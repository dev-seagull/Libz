package com.example.cso;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SharedPreferencesHandler {

    public static boolean getWifiOnlySwitchState(){
        return MainActivity.preferences.getBoolean("wifiOnlySwitchState", false);
    }

    public static boolean getSyncSwitchState(){
        return MainActivity.preferences.getBoolean("syncSwitchState", false);
    }

    public static void setFirstTime(SharedPreferences sharedPreferences){
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("firstTime", false);
        editor.apply();
    }

    public static boolean getFirstTime(SharedPreferences sharedPreferences){
        return sharedPreferences.getBoolean("firstTime", true);
    }

    public static void setSwitchState(String switchStateKey, boolean state, SharedPreferences sharedPreferences) {
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(switchStateKey, state);
        editor.apply();
    }

    public static void setJsonModifiedTime(SharedPreferences sharedPreferences) {
        Thread setJsonModifiedTimeThread = new Thread(()-> {
            Date now = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String currentTimestamp = dateFormat.format(now);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("jsonModifiedTime", currentTimestamp);
            editor.apply();
        });
        setJsonModifiedTimeThread.start();
        try{
            setJsonModifiedTimeThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join setJsonModifiedTimeThread: " + e.getLocalizedMessage(), true);
        }
    }

    public static Date getJsonModifiedTime(SharedPreferences sharedPreferences) {
        String timeStamp = sharedPreferences.getString("jsonModifiedTime", "0");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        try {
            return dateFormat.parse(timeStamp);
        } catch (ParseException e) {
            LogHandler.saveLog("failed to parse stored date to timestamp");
            return null;
        }
    }
}
