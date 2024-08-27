package com.example.cso;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;

public class MyBroadcastReceiver extends BroadcastReceiver {
    public static long timeInterval = 8 * 60 * 60 * 1000;
    @Override
    public void onReceive(Context context, Intent intent){
        StorageSync.uploadStorageJsonFileToAccounts(context);
        int requestCode = intent.getIntExtra("requestCode", 0);
        setAlarm(context,timeInterval, requestCode);
    }

    public static void setAlarm(Context context, long timeInMillis, int requestCode) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, MyBroadcastReceiver.class);
            intent.putExtra("requestCode", requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            System.out.println("alarm set for time : " + formatter.format(timeInMillis));
        } catch (Exception e) {
            LogHandler.saveLog("Failed to set alarm: " + e.getLocalizedMessage(), true);
        }
    }
}
