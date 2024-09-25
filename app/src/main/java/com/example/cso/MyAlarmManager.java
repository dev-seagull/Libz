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

    public static void setAlarmForDeviceStatusSync(Context context, int requestCode , long timeInMillis) {
        Log.d("DeviceStatusSync","starting to set alarm for storage upload");
        try {
//            if (hasAlarmSet(context,requestCode)){
//                cancelAlarmForDeviceStatusSync(context,requestCode);
//            }
            if (hasAlarmSet(context,requestCode)){
                return;
            }
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, MyBroadcastReceiver.class);
            intent.putExtra("requestCode", requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            Log.d("DeviceStatusSync","upload alarm set at " + formatter.format(timeInMillis));
        } catch (Exception e) {
            LogHandler.saveLog("Failed to set alarm: " + e.getLocalizedMessage(), true);
        }
    }

    public static boolean hasAlarmSet(Context context, int requestCode) {
        Intent intent = new Intent(context, MyBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_MUTABLE);
        boolean alarmExists = (pendingIntent != null);
        Log.d("DeviceStatusSync", "alarm with request code " + requestCode + " exists : " + alarmExists);
        return alarmExists;
    }

    public static void cancelAlarmForDeviceStatusSync(Context context, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MyBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);
        if (pendingIntent!= null) {
            Log.d("DeviceStatusSync","cancelling alarm with request code " + requestCode);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel(); // to avoid memory leaks and potential crashes in some cases.
            pendingIntent = null; // to ensure the reference is removed from memory.
        } else {
            Log.d("DeviceStatusSync","no alarm found with request code " + requestCode);
        }
    }
}

//public void scheduleDailyDataAnalysisAlarm(Context context, int hour, int minute){
//    int unique_requestCode = hour * 1000 + minute;
//    android.app.AlarmManager alarmManager = (android.app.AlarmManager)
//            context.getSystemService(Context.ALARM_SERVICE);
//    Intent intent = new Intent(context, MyBroadcastReceiver.class);
//
//    int flagForAlarm ;
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
//        flagForAlarm = PendingIntent.FLAG_IMMUTABLE;
//    }else{
//        flagForAlarm = PendingIntent.FLAG_UPDATE_CURRENT;
//    }
//    PendingIntent pendingIntent = PendingIntent.getBroadcast(context,unique_requestCode, intent, flagForAlarm);
//
//    alarmManager.cancel(pendingIntent);
//    Calendar calendar = Calendar.getInstance();
//    calendar.setTimeInMillis(System.currentTimeMillis());
//    calendar.set(Calendar.HOUR_OF_DAY,hour);
//    calendar.set(Calendar.MINUTE, minute);
//    calendar.set(Calendar.SECOND,0);
//    if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
//        calendar.add(Calendar.DAY_OF_YEAR, 1);
//    }
//    alarmManager.setRepeating(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
//            android.app.AlarmManager.INTERVAL_DAY, pendingIntent);
//}

