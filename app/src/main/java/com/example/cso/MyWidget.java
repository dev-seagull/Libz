//package com.example.cso;
//
//import static com.example.cso.MainActivity.timerService;
//
//import android.app.PendingIntent;
//import android.appwidget.AppWidgetManager;
//import android.appwidget.AppWidgetProvider;
//import android.content.Context;
//import android.content.Intent;
//import android.content.res.ColorStateList;
//import android.graphics.Color;
//import android.os.Build;
//import android.widget.RemoteViews;
//import android.widget.Switch;
//
//public class MyWidget extends AppWidgetProvider {
//
//    @Override
//    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
//        for (int appWidgetId : appWidgetIds) {
//            System.out.println("appWidgetId: " + appWidgetId);
//            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
//            Switch syncWidgetSwitch = (Switch) MainActivity.activity.findViewById(R.id.syncWidgetSwitch);
//            syncWidgetSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//                System.out.println("isChecked: " + isChecked);
//            });
//            Intent startIntent = new Intent(context, MyWidget.class);
//            startIntent.setAction("START_SYNC");
//            PendingIntent syncPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, startIntent, PendingIntent.FLAG_IMMUTABLE);
//            Intent stopIntent = new Intent(context, MyWidget.class);
//            stopIntent.setAction("STOP_SYNC");
//            PendingIntent restorePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, stopIntent, PendingIntent.FLAG_IMMUTABLE);
//            views.setOnClickPendingIntent(R.id.syncWidgetSwitch, syncPendingIntent);
//            appWidgetManager.updateAppWidget(appWidgetId, views);
//        }
//    }
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        super.onReceive(context, intent);
//        Switch syncWidgetSwitch = (Switch) MainActivity.activity.findViewById(R.id.syncWidgetSwitch);
//        Intent serviceIntent = new Intent(MainActivity.activity.getApplicationContext(), timerService.getClass());
//
//        if ("START_SYNC".equals(intent.getAction())) {
//            System.out.println("START_SYNC");
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                MainActivity.activity.runOnUiThread(() -> {
//                    syncWidgetSwitch.setTrackTintList(ColorStateList.valueOf(Color.GREEN));
//                    syncWidgetSwitch.setThumbTintList(ColorStateList.valueOf(Color.GREEN));
//                });
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                if (!MainActivity.isMyServiceRunning(MainActivity.activity.getApplicationContext(), timerService.getClass()).equals("on")){
//                    TimerService.shouldCancel = false;
//                    MainActivity.activity.startForegroundService(serviceIntent);
//                }
//            }
//        } else if ("STOP_SYNC".equals(intent.getAction())) {
//            System.out.println("STOP_SYNC");
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                MainActivity.activity.runOnUiThread(() -> {
//                    syncWidgetSwitch.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#BF2C2C")));
//                    syncWidgetSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#850808")));
//                });
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                if (MainActivity.isMyServiceRunning(MainActivity.activity.getApplicationContext(), timerService.getClass()).equals("on")){
//                    TimerService.shouldCancel = true;
//                    MainActivity.activity.stopService(serviceIntent);
//                }
//            }
//        }
//    }
//}
