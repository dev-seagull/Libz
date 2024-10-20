package com.example.cso;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyBroadcastReceiver extends BroadcastReceiver {

    public static long syncStatusCheckTimeInterval = (1 * 60 * 1000);

    @Override
    public void onReceive(Context context, Intent intent){
        int requestCode = intent.getIntExtra("requestCode", 0);
        if (requestCode == MyAlarmManager.deviceStatusSyncRequestId){
            Log.d(MyAlarmManager.TAG,"alarm for deviceStatusSync received at : " + new Date().getTime() + " code : " + requestCode);
            DeviceStatusSync.uploadDeviceStatusJsonFileToAccounts(context);
            long timeInMillis = new Date().getTime() + DeviceStatusSync.timeInterval;
            setAlarm(context,timeInMillis, requestCode);
        }else if (requestCode == MyAlarmManager.syncStatusCheckRequestId ||requestCode == MyAlarmManager.syncStatusCheckRequestId2) {
            Log.d(MyAlarmManager.TAG,"alarm for syncStatusCheck received at : " + new Date().getTime() + " code : " + requestCode);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Log.d(MyAlarmManager.TAG,"SharedPreferences prefs : " +prefs);
            boolean syncState = SharedPreferencesHandler.getSyncSwitchState(prefs);
            Log.d(MyAlarmManager.TAG,"sync state based on shared preferences before set : " + syncState);
//            SharedPreferencesHandler.setSwitchState("syncSwitchState",true,prefs);
            Log.d(MyAlarmManager.TAG,"set sync state true on shared preferences : " + syncState);
            syncState = SharedPreferencesHandler.getSyncSwitchState(prefs);
            Log.d(MyAlarmManager.TAG,"sync state based on shared preferences after set : " + syncState);
            boolean isServiceRunning = TimerService.isMyServiceRunning(context, TimerService.class).equals("on");
            Log.d(MyAlarmManager.TAG,"sync state based on is my service running : " + syncState);
            if (syncState){
                if (!isServiceRunning){
                    Log.d(MyAlarmManager.TAG,"try to start sync by alarm");
                    Sync.startSync(context, new Intent(context, TimerService.class));
                    Log.d(MyAlarmManager.TAG,"after try to start sync by alarm");
                }
            }
            Log.d(MyAlarmManager.TAG,"try to set alarm for next sync status check");
            long timeInMillis = new Date().getTime() + syncStatusCheckTimeInterval;
            setAlarm(context,timeInMillis,requestCode);
            Log.d(MyAlarmManager.TAG,"after try to set alarm for next sync status check");
        }
    }

    public static void setAlarm(Context context, long timeInMillis, int requestCode) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, MyBroadcastReceiver.class);
            intent.putExtra("requestCode", requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            Log.d(MyAlarmManager.TAG,"new alarm set at " + formatter.format(timeInMillis) + " with requestCode " + requestCode);
        } catch (Exception e) {
            LogHandler.saveLog("Failed to set alarm: " + e.getLocalizedMessage(), true);
        }
    }
}
