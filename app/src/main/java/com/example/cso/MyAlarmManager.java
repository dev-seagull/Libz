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
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US);
            Log.d(TAG,"upload alarm set at " + formatter.format(timeInMillis));
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
}
