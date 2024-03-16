package com.example.cso;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TimerService extends Service {


    private static final String TAG = "TimerForegroundService";
    public Timer timer;
    public static boolean shouldCancel = false;
    public TimerTask timerTask;
    private static final int NOTIFICATION_ID = 12345;
    private static final String CHANNEL_ID = "TimerServiceChannel";
    private static final String CHANNEL_NAME = "Syncing Channel";
    Thread storageUpdaterThreadTemp;
    Thread deleteRedundantAndroidThreadTemp;
    Thread updateAndroidFilesThreadTemp;
    Thread syncAndroidToDriveThreadTemp;
    Thread deleteRedundantDriveThreadTemp;

    Thread updateDriveFilesThreadTemp;
    Thread deleteDuplicatedInDriveTemp;
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

        return Service.START_REDELIVER_INTENT;
    }

    private void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 5000 , 1000);
    }

    public void stopTimerService(){
        stopSelf();
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
                .setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setContentIntent(pendingIntent);
        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void initializeTimerTask() {

        final Thread[] deleteRedundantAndroidThreadForService = {new Thread(() -> {MainActivity.dbHelper.deleteRedundantAndroid();})};

        final Thread[] updateAndroidFilesThreadForService = {new Thread(() -> {Android.getGalleryMediaItems(MainActivity.activity);})};

        final Thread[] storageUpdaterThreadForService = {new Thread(() -> {MainActivity.storageHandler.storageUpdater();})};

        final Thread[] deleteRedundantDriveThreadForService = {new Thread(() -> {

            String[] columns = {"accessToken","userEmail", "type"};
            List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columns);

            for(String[] account_row : account_rows) {
                String type = account_row[2];
                if(type.equals("backup")){
                    String userEmail = account_row[1];
                    String accessToken = account_row[0];
                    ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                    ArrayList<String> driveFileIds = new ArrayList<>();

                    for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                        String fileId = driveMediaItem.getId();
                        driveFileIds.add(fileId);
                    }
                    MainActivity.dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
                }
            }
        })};

        final Thread[] updateDriveFilesThreadForService = {new Thread(() -> {

            String[] columns = {"accessToken", "userEmail","type"};
            List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columns);

            for(String[] account_row : account_rows){
                String type = account_row[2];
                if (type.equals("backup")){
                    String accessToken = account_row[0];
                    String userEmail = account_row[1];
                    ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                    for(DriveAccountInfo.MediaItem driveMediaItem: driveMediaItems){
                        Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
                        if (last_insertId != -1) {
                            MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
                                    driveMediaItem.getHash(), userEmail);
                        } else {
                            LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
                        }
                    }
                }
            }
        })};

        final Thread[] deleteDuplicatedInDriveForService = {new Thread(() -> {

            String[] columns = {"accessToken","userEmail", "type"};
            List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columns);

            for(String[] account_row : account_rows) {
                String type = account_row[2];
                if(type.equals("backup")){
                    String userEmail = account_row[1];
                    String accessToken = account_row[0];
                    GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                }
            }
        })};

        final Thread[] syncAndroidToDriveThreadForService = {new Thread( () -> {
                MainActivity.storageHandler.syncAndroidToDrive();
        })};


        timerTask = new TimerTask() {
            public void run() {
                try{
                    Log.d(TAG, "my service is run like");
                    if (shouldCancel == true){
                        timer.cancel();
                        timer.purge();
                        return;
                    }
                    if(Looper.myLooper() == Looper.getMainLooper()){
                        System.out.println("Running on ui thread on run timer task");
                    }

                    if (
                            (deleteRedundantAndroidThreadTemp != null && deleteRedundantAndroidThreadTemp.isAlive()) ||
                                    (updateAndroidFilesThreadTemp != null && updateAndroidFilesThreadTemp.isAlive()) ||
                                    (storageUpdaterThreadTemp != null && storageUpdaterThreadTemp.isAlive()) ||
                                    (deleteRedundantDriveThreadTemp != null && deleteRedundantDriveThreadTemp.isAlive()) ||
                                    (updateDriveFilesThreadTemp != null && updateDriveFilesThreadTemp.isAlive()) ||
                                    (deleteDuplicatedInDriveTemp != null && deleteDuplicatedInDriveTemp.isAlive())
                    ){
                        return;
                    }
                    deleteRedundantAndroidThreadTemp = new Thread(deleteRedundantAndroidThreadForService[0]);
                    deleteRedundantAndroidThreadTemp.start();
                    try {
                        deleteRedundantAndroidThreadTemp.join();
                    } catch (InterruptedException e) {
                        LogHandler.saveLog("Failed to join delete redundant temp : "  +
                                e.getLocalizedMessage(), true);
                    }

                    updateAndroidFilesThreadTemp = new Thread(updateAndroidFilesThreadForService[0]);
                    updateAndroidFilesThreadTemp.start();
                    try{
                        updateAndroidFilesThreadTemp.join();
                    }catch (InterruptedException e){
                        LogHandler.saveLog("Failed to join update android temp : "  +
                                e.getLocalizedMessage(), true);
                    }

                    storageUpdaterThreadTemp = new Thread(storageUpdaterThreadForService[0]);
                    storageUpdaterThreadTemp.start();
                    try{
                        storageUpdaterThreadTemp.join();
                    }catch (InterruptedException e) {
                        LogHandler.saveLog("Failed to join storage update temp : " +
                                e.getLocalizedMessage(), true);
                    }
                    System.out.println("Android Status is up-to-date and storageUpdaterThreadTemp is done");


                    deleteRedundantDriveThreadTemp = new Thread(deleteRedundantDriveThreadForService[0]);
                    deleteRedundantDriveThreadTemp.start();
                    try{
                        deleteRedundantDriveThreadTemp.join();
                    }catch (InterruptedException e){
                        LogHandler.saveLog("Failed to join delete redundant drive temp : "  +
                                e.getLocalizedMessage(), true);
                    }

                    updateDriveFilesThreadTemp = new Thread(updateDriveFilesThreadForService[0]);
                    updateDriveFilesThreadTemp.start();
                    try{
                        updateDriveFilesThreadTemp.join();
                    }catch (InterruptedException e){
                        LogHandler.saveLog("Failed to join update drive temp : "  +
                                e.getLocalizedMessage(), true);
                    }

                    deleteDuplicatedInDriveTemp = new Thread(deleteDuplicatedInDriveForService[0]);
                    deleteDuplicatedInDriveTemp.start();
                    try{
                        deleteDuplicatedInDriveTemp.join();
                    }catch (InterruptedException e){
                        LogHandler.saveLog("Failed to join delete duplicated drive temp : "  +
                                e.getLocalizedMessage(), true);
                    }

                    syncAndroidToDriveThreadTemp = new Thread(syncAndroidToDriveThreadForService[0]);
                    syncAndroidToDriveThreadTemp.start();
                    try{
                        syncAndroidToDriveThreadTemp.join();
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to join syncAndroidToDrive thread in timer :" +
                                e.getLocalizedMessage(),true);
                    }

                    // need to free up ?
                        //if duplicate in android -> delete the duplicate
                        //if duplicate between android and drive -> delete the duplicate in android
                        //else first upload then delete the duplicate in android

                    System.out.println("MainActivity.dbHelper.countAndroidAssets() : " + MainActivity.dbHelper.countAndroidAssets());
                }catch (Exception e){
                    LogHandler.saveLog("Failed to run timer in service" + e.getLocalizedMessage() , true);
                }
            }
        };
    }

}
