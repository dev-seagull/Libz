package com.example.cso;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHandler {

    public static void sendNotification(String channelId, String channelName, Activity activity,
                                        String contentTitle, String contentText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName,
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, channelId)
                .setSmallIcon(R.drawable.googlephotosimage)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Intent intent = new Intent(activity, MainActivity.class);
        int androidVersion = android.os.Build.VERSION.SDK_INT;
        String androidVersionName = android.os.Build.VERSION.RELEASE;

        Log.d("Android Version", "SDK Version: " + androidVersion);
        Log.d("Android Version", "Release Version: " + androidVersionName);
        PendingIntent pendingIntent ;
        System.out.println("Build.VERSION.SDK_INT < Build.VERSION_CODES.M " + Build.VERSION.SDK_INT  + "   " +   Build.VERSION_CODES.M);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }else{
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        builder.setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(activity);
        if (ActivityCompat.checkSelfPermission(activity,
                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            int REQUEST_CODE = 1;
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE);
        }
        notificationManager.notify(Integer.valueOf(channelId), builder.build());
    }
}
