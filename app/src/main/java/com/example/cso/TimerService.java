package com.example.cso;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.cso.UI.SyncButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TimerService extends Service {
    private static int NOTIFICATION_ID = 12345;
    private static String CHANNEL_ID = "TimerServiceChannel";
    private static String CHANNEL_NAME = "Syncing Channel";

    private Timer timer;
    public static boolean isTimerRunning = false; // init
    private TimerTask timerTask;
    private  Notification notification;
    public static boolean isAppinForeGround;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("service", "Service onCreate Started");
        requestNotificationPermission();
        createNotificationChannel();
        notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        startTimer();
        Log.d("service", "Service onCreate Finished");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("service", "Service onStartCommand Started");

        if (intent != null && intent.getAction() != null && intent.getAction().equals("STOP_SERVICE")) {
            Log.d("service", "Service stop request received");
            stopTimer();
            if(isAppInForeground()){
                SyncButton.handleRotateSyncButtonClick(MainActivity.activity);
            }else{
                SyncButton.toggleSyncState();
            }

            stopForeground(true);
            stopSelf();

            Log.d("service","Service stopped");
        }
        Log.d("service", "Service onStartCommand Finished");

        return Service.START_STICKY;
    }

    private void startTimer() {
        Log.d("service","Service Timer Started : " +  (timer == null) );
        if(timer == null){
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    try{
                        Log.d("service", "Android timer is running: " + MainActivity.isAndroidTimerRunning);
                        Log.d("service", "Is any process on: " + MainActivity.isAnyProccessOn);
                        Log.d("service","isTimer Running : " + isTimerRunning);
                        isAppinForeGround = isAppInForeground();
                        if (isTimerRunning || MainActivity.isAnyProccessOn) {
                            return;
                        }
                        isTimerRunning = true;

                        new Thread( () -> {
                            try {

                                //if database needed to upload do it here
                                
                                if(!MainActivity.isAndroidTimerRunning){
                                    Android.startThreads(MainActivity.activity);
                                }

                                GoogleDrive.startThreads();

                                Sync.syncAndroidFiles(getApplicationContext(), MainActivity.activity);

                            }catch (Exception e) {
                                FirebaseCrashlytics.getInstance().recordException(e);
                            } finally{
                                isTimerRunning = false; // end of timer service
                            }

                        }).start();

                    }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
                }
            };

            timer.schedule(timerTask, 1000 , 1000);
        }
        Log.d("service","Service Timer Finished");
    }

    private void requestNotificationPermission(){
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("service","Notification Permission required");
                int REQUEST_CODE = 1;
                ActivityCompat.requestPermissions(MainActivity.activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    public Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Syncing in progress...")
                .setContentText("Syncing process is running in the background...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.libzlogo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Syncing process is running in the background..."));

        Intent actionIntent = new Intent(getApplicationContext(), TimerService.class);
        actionIntent.setAction("STOP_SERVICE");

       PendingIntent actionPendingIntent;
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           actionPendingIntent = PendingIntent.getService(this, NOTIFICATION_ID, actionIntent, PendingIntent.FLAG_IMMUTABLE);
       } else {
           actionPendingIntent = PendingIntent.getService(this, NOTIFICATION_ID, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
       }

        builder.addAction(R.drawable.googledriveimage, "Stop Service", actionPendingIntent);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        isTimerRunning = false;
        Log.d("TimerForegroundService", "Timer stop status : " + (timer == null) + " and timerTask : " + timerTask);
    }

    @Override
    public void onDestroy() {
        Log.d("service", "Service onDestroy Started");
        isTimerRunning = false;
        stopTimer();
        if (!isAppInForeground()) {
            if (MainActivity.dbHelper != null) {
                MainActivity.dbHelper.close();
                Log.d("service", "Database closed as app is not in foreground.");
                System.exit(0);
            }
        } else {
            Log.d("service", "App is in foreground, not closing the database.");
        }
        super.onDestroy();
        Log.d("service", "Service onDestroy Finished");
    }

    public boolean isAppInForeground() {
        try{
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                Log.d("service", "ActivityManager is null, cannot determine app foreground status");
                return false;
            }

            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                Log.d("service", "No running app processes found");
                return false;
            }

            final String packageName = getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return false;
    }

    public static String isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (manager == null) {
            Log.d("service", "ActivityManager is null, cannot determine if service is running.");
        }

        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null) {
            Log.d("service", "No running services found.");
        }

        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d("service", "Service " + serviceClass.getName() + " is running.");
                return "on";
            }
        }

        Log.d("service", "Service " + serviceClass.getName() + " is not running.");
        return "off";
    }
}
