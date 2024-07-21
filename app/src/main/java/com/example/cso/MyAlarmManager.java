package com.example.cso;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class MyAlarmManager {
    public void scheduleDailyDataAnalysisAlarm(Context context, int hour, int minute){
        int unique_requestCode = hour * 1000 + minute;
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) 
                context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MyBroadcastReceiver.class);

        int flagForAlarm ;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            flagForAlarm = PendingIntent.FLAG_IMMUTABLE;
        }else{
            flagForAlarm = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,unique_requestCode, intent, flagForAlarm);

        alarmManager.cancel(pendingIntent);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY,hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND,0);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        alarmManager.setRepeating(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                android.app.AlarmManager.INTERVAL_DAY, pendingIntent);
    }
}
