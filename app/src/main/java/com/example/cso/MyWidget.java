package com.example.cso;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class MyWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            Intent syncIntent = new Intent(context, MyWidget.class);
            syncIntent.setAction("SYNC_ACTION");
            PendingIntent syncPendingIntent = PendingIntent.getBroadcast(context, 0, syncIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.syncToBackUpAccountButton, syncPendingIntent);

            Intent restoreIntent = new Intent(context, MyWidget.class);
            restoreIntent.setAction("RESTORE_ACTION");
            PendingIntent restorePendingIntent = PendingIntent.getBroadcast(context, 0, restoreIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.restoreButton, restorePendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if ("SYNC_ACTION".equals(intent.getAction())) {
            // Handle sync button click
            // Your sync logic here
        } else if ("RESTORE_ACTION".equals(intent.getAction())) {
            // Handle restore button click
            // Your restore logic here
        }
    }
}
