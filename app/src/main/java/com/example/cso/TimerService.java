package com.example.cso;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

public class TimerService extends Service {


    private static final String TAG = "TimerForegroundService";
    private Timer timer;
    private TimerTask timerTask;

    private static final int NOTIFICATION_ID = 12345;
    private static final String CHANNEL_ID = "TimerServiceChannel";
    private static final String CHANNEL_NAME = "Timer Service Channel";
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        startTimer();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        startTimer();

        // Create the notification for the foreground service
        Notification notification = createNotification();

        // Start the service in the foreground
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = MainActivity.activity.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Intent intent = new Intent(MainActivity.activity, MainActivity.class);
        PendingIntent pendingIntent ;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            pendingIntent = PendingIntent.getActivity(MainActivity.activity, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }else{
            pendingIntent = PendingIntent.getActivity(MainActivity.activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.android_device_icon)
                .setContentTitle("Timer Service")
                .setContentText("Timer is running in the background")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }
    private void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 1000, 5000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initializeTimerTask() {

        final Thread[] storageUpdaterThreadForService = {new Thread(() -> {MainActivity.storageHandler.storageUpdater();})};

        final Thread[] deleteRedundantAndroidThreadForService = {new Thread(() -> {MainActivity.dbHelper.deleteRedundantAndroid();})};

        final Thread[] updateAndroidFilesThreadForService = {new Thread(() -> {Android.getGalleryMediaItems(MainActivity.activity);})};

        System.out.println("initialize Timer Task Done");

        timerTask = new TimerTask() {
            public void run() {
                try{
                    System.out.println("running new timer");
                    Thread storageUpdaterThreadTemp = new Thread(storageUpdaterThreadForService[0]);
                    storageUpdaterThreadTemp.start();

                    Thread deleteRedundantAndroidThreadTemp = new Thread(deleteRedundantAndroidThreadForService[0]);
                    deleteRedundantAndroidThreadTemp.start();

                    Thread updateAndroidFilesThreadTemp = new Thread(updateAndroidFilesThreadForService[0]);
                    updateAndroidFilesThreadTemp.start();
                    System.out.println("MainActivity.dbHelper.countAndroidAssets() : " + MainActivity.dbHelper.countAndroidAssets());
                    System.out.println("finishing new timer");

                }catch (Exception e){
                    LogHandler.saveLog("Failed to run timer in service" + e.getLocalizedMessage() , true);
                }
            }
        };
    }

}
