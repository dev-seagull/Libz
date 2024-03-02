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
import android.os.Looper;
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
    private static final String CHANNEL_NAME = "Syncing Channel";
    Thread storageUpdaterThreadTemp;
    Thread deleteRedundantAndroidThreadTemp;
    Thread updateAndroidFilesThreadTemp;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        startTimer();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        Notification notification = createNotification();

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
                .setContentTitle("Syncing Service")
                .setContentText("Syncing process is running in the background")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }
    private void startTimer() {

        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 5000 , 10000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initializeTimerTask() {

        final Thread[] storageUpdaterThreadForService = {new Thread(() -> {
            if(Looper.myLooper() == Looper.getMainLooper()){
                System.out.println("Running on ui thread on init timer task");
            }
            MainActivity.storageHandler.storageUpdater();
        })};

        final Thread[] deleteRedundantAndroidThreadForService = {new Thread(() -> {MainActivity.dbHelper.deleteRedundantAndroid();})};

        final Thread[] updateAndroidFilesThreadForService = {new Thread(() -> {Android.getGalleryMediaItems(MainActivity.activity);})};

        timerTask = new TimerTask() {
            public void run() {
                try{
                    if(Looper.myLooper() == Looper.getMainLooper()){
                        System.out.println("Running on ui thread on run timer task");
                    }

                    System.out.println("running new timer");
                    storageUpdaterThreadTemp = new Thread(storageUpdaterThreadForService[0]);
                    if(!storageUpdaterThreadTemp.isAlive() && !storageUpdaterThreadForService[0].isAlive()){
                        storageUpdaterThreadTemp.start();
                    }

                    deleteRedundantAndroidThreadTemp = new Thread(deleteRedundantAndroidThreadForService[0]);
                    if(!deleteRedundantAndroidThreadTemp.isAlive() && !deleteRedundantAndroidThreadForService[0].isAlive()){

                        deleteRedundantAndroidThreadTemp.start();
                    }

                    updateAndroidFilesThreadTemp = new Thread(updateAndroidFilesThreadForService[0]);
                    if(!updateAndroidFilesThreadTemp.isAlive() && !updateAndroidFilesThreadForService[0].isAlive()){
                        updateAndroidFilesThreadTemp.start();
                    }
                    System.out.println("MainActivity.dbHelper.countAndroidAssets() : " + MainActivity.dbHelper.countAndroidAssets());
                    System.out.println("finishing new timer");

                }catch (Exception e){
                    LogHandler.saveLog("Failed to run timer in service" + e.getLocalizedMessage() , true);
                }
            }
        };
    }

}
