package com.example.cso;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
            try{
                Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                String currentTimestamp = dateFormat.format(now);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("jsonModifiedTime", currentTimestamp);
                editor.apply();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        setJsonModifiedTimeThread.start();
        try{
            setJsonModifiedTimeThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void setJsonModifiedTime(SharedPreferences sharedPreferences, Date jsonModifiedTime) {
        Thread setJsonModifiedTimeThread = new Thread(()-> {
            try{
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                String newTimestamp = dateFormat.format(jsonModifiedTime);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("jsonModifiedTime", newTimestamp);
                editor.apply();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        setJsonModifiedTimeThread.start();
        try{
            setJsonModifiedTimeThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static Date getJsonModifiedTime(SharedPreferences sharedPreferences) {
        String timeStamp = sharedPreferences.getString("jsonModifiedTime", "0");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        try {
            Date date =dateFormat.parse(timeStamp);
            Log.d("jsonChange","getJsonModifiedTime, timeStamp : " + timeStamp + "Date : " + date);
            return date;
        } catch (ParseException e) {
            LogHandler.saveLog("failed to parse stored date to timestamp");
            return null;
        }
    }

    public static JsonObject getDeviceStatus(String filename){
        String jsonString = MainActivity.preferences.getString(filename, null);
        Log.d("DeviceStatusSync","get device status for : " + filename + " with content : " + jsonString);
        if(jsonString == null) return null;

        return JsonParser.parseString(jsonString).getAsJsonObject();
    }

    public static void setDeviceStatus(String filename, JsonObject jsonObject){
        Thread setDeviceStatusThread = new Thread(()-> {
            try{
                android.content.SharedPreferences.Editor editor = MainActivity.preferences.edit();
                editor.putString(filename, jsonObject.toString());
                editor.apply();
                Log.d("DeviceStatusSync", "Device status saved for " + filename + " with content : " + jsonObject);
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        setDeviceStatusThread.start();
        try{
            setDeviceStatusThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static String getCurrentTheme(){
        String name = MainActivity.preferences.getString("theme", "purple");
        if (name.equals("purple") || name.equals("gray")){
            return name;
        }
        return "purple";
    }

    public static void setCurrentTheme(String theme){
        Thread setCurrentThemeThread = new Thread(()-> {
            try{
                android.content.SharedPreferences.Editor editor = MainActivity.preferences.edit();
                editor.putString("theme", theme);
                editor.apply();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        setCurrentThemeThread.start();
        try{
            setCurrentThemeThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void setAccountStorageStatus(String userEmail, String dataJsonContent){
        Thread setAccountStorageThread = new Thread(() -> {
            try{
                android.content.SharedPreferences.Editor editor = MainActivity.preferences.edit();
                editor.putString(userEmail, dataJsonContent);
                editor.apply();
            }catch (Exception e){
                LogHandler.crashLog(e,"SharedPreferences");
            }
        });
        setAccountStorageThread.start();
        try{
            setAccountStorageThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"SharedPreferences");
        }
    }


}
