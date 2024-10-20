package com.example.cso;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.StaticLayout;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MyAlarmManager {

    public static int deviceStatusSyncRequestId = 172839;
    public static int syncStatusCheckRequestId = 172842;
    public static int syncStatusCheckRequestId2 = 172843;
    public static String TAG = "MyAlarmManager";

    public static void setAlarmForDeviceStatusSync(Context context, int requestCode , long timeInMillis) {
        Log.d(TAG,"starting to set alarm for storage upload");
        try {
            if (hasAlarmSet(context,requestCode)){
                return;
            }
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, MyBroadcastReceiver.class);
            intent.putExtra("requestCode", requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            Log.d(TAG,"upload alarm set at " + formatter.format(timeInMillis));
        } catch (Exception e) {
            LogHandler.crashLog(e,TAG);
        }
    }

    public static void setAlarmForSyncStatusCheck(Context context, int requestCode , long timeInMillis) {
        Log.d(TAG,"starting to set alarm for sync status check");
        try {
            if (hasAlarmSet(context,requestCode)){
                cancelAlarmForDeviceStatusSync(context,requestCode);
            }
            if (hasAlarmSet(context,requestCode)){
                cancelAlarmForDeviceStatusSync(context,requestCode);
            }
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, MyBroadcastReceiver.class);
            intent.putExtra("requestCode", requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            Log.d(TAG,"sync status check alarm set at " + formatter.format(timeInMillis));
        } catch (Exception e) {
            LogHandler.crashLog(e,TAG);
        }
    }

    public static boolean hasAlarmSet(Context context, int requestCode) {
        Intent intent = new Intent(context, MyBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_MUTABLE);
        boolean alarmExists = (pendingIntent != null);
        Log.d(TAG, "alarm with request code " + requestCode + " exists : " + alarmExists);
        return alarmExists;
    }

    public static void cancelAlarmForDeviceStatusSync(Context context, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MyBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);
        if (pendingIntent!= null) {
            Log.d(TAG,"cancelling alarm with request code " + requestCode);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel(); // to avoid memory leaks and potential crashes in some cases.
            pendingIntent = null; // to ensure the reference is removed from memory.
        } else {
            Log.d(TAG,"no alarm found with request code " + requestCode);
        }
    }
}
