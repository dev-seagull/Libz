package com.example.cso;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.gson.JsonObject;

public class SharedPreferencesHandler {

    public static boolean getWifiOnlySwitchState(SharedPreferences sharedPreferences){
        return sharedPreferences.getBoolean("wifiOnlySwitchState", false);
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


}
