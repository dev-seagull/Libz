package com.example.cso;

import android.app.Application;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;


public class LogHandler extends Application {

    public static void recordException(Throwable message, String tag){
        try{
            if (message != null && tag != null){
                Log.e(tag, message.getLocalizedMessage() + message.getMessage());
                FirebaseCrashlytics.getInstance().recordException(message);
            }
        }catch (Exception e1){
            System.out.println("Failed to crash log: " + e1.getLocalizedMessage());
        }
    }

    public static void log(String message, String tag){
        try{
            if (message != null && tag != null){
                Log.d(tag, message);
                FirebaseCrashlytics.getInstance().log(tag + " "  + message);
            }
        }catch (Exception e1){
            System.out.println("Failed to crash log: " + e1.getLocalizedMessage());
        }
    }
}