package com.example.cso;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyBroadcastReceiver extends BroadcastReceiver {
    public static long timeInterval = 2 * 60 * 1000;
    @Override
    public void onReceive(Context context, Intent intent){
        Log.d("DeviceStatusSync","upload alarm recieved at : " + new Date().getTime());
        DeviceStatusSync.uploadDeviceStatusJsonFileToAccounts(context);
        int requestCode = intent.getIntExtra("requestCode", 0);
        long timeInMillis = new Date().getTime() + timeInterval;
        setAlarm(context,timeInMillis, requestCode);
    }

    public static void setAlarm(Context context, long timeInMillis, int requestCode) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, MyBroadcastReceiver.class);
            intent.putExtra("requestCode", requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            Log.d("DeviceStatusSync","new DeviceStatusSync alarm set at " + formatter.format(timeInMillis));
        } catch (Exception e) {
            LogHandler.saveLog("Failed to set alarm: " + e.getLocalizedMessage(), true);
        }
    }
}
