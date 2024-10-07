package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.cso.DBHelper;
import com.example.cso.DeviceHandler;
import com.example.cso.GoogleDriveFolders;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SyncDetails {

    public static int syncDetailsButtonTextId ;
    public static int syncDetailsButtonId;
    public static int syncDetailsStatisticsLayoutId;

    public static void handleSyncDetailsButton(Activity activity){
        double percentageOfSyncedAssets = DBHelper.getPercentageOfSyncedAssets();
        TextView syncDetailsButtonTextView = activity.findViewById(syncDetailsButtonTextId);
        activity.runOnUiThread( () -> {
            syncDetailsButtonTextView.setText(String.format("%d%%", Math.round(percentageOfSyncedAssets)));
            Typeface dseg7classic_regular = ResourcesCompat.getFont(activity,R.font.ptsansnarrowwebregular);
            syncDetailsButtonTextView.setTypeface(dseg7classic_regular);
            syncDetailsButtonTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            syncDetailsButtonTextView.setGravity(Gravity.CENTER);
            setListenerForSyncDetailsButton(activity);
        });
    }

    public static LinearLayout createSyncDetailsStatisticsLayout(Activity activity){
        LinearLayout syncDetailsStatisticsLayout = Details.createDetailsLayout(activity);
        syncDetailsStatisticsLayout.setPadding(0,15,0,15);

        syncDetailsStatisticsLayout.setVisibility(View.GONE);
        syncDetailsStatisticsLayoutId = View.generateViewId();
        syncDetailsStatisticsLayout.setId(syncDetailsStatisticsLayoutId);
        new Thread(() -> {
            ImageView[] loadingImage = new ImageView[]{new ImageView(activity)};
            MainActivity.activity.runOnUiThread(() -> {
                loadingImage[0].setBackgroundResource(R.drawable.yellow_loading);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(128,128);
                params.setMargins(0,64,0,64);
                loadingImage[0].setLayoutParams(params);
                syncDetailsStatisticsLayout.addView(loadingImage[0]);
            });

            View pieChartView = SyncDetailsPieChart.createPieChartView(activity);
            activity.runOnUiThread(() -> {
                syncDetailsStatisticsLayout.addView(pieChartView);
                syncDetailsStatisticsLayout.removeView(loadingImage[0]);
            });
        }).start();

        return syncDetailsStatisticsLayout;
    }

    public static void addGradientOnToSyncDetailsButton(ImageButton actionButton){
        GradientDrawable firstLayer = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {MainActivity.currentTheme.OnSyncButtonGradientStart,
                        MainActivity.currentTheme.OnSyncButtonGradientEnd}
        );
        firstLayer.setShape(GradientDrawable.OVAL);
        firstLayer.setSize(UI.dpToPx(104), UI.dpToPx(104));
        firstLayer.setCornerRadius(UI.dpToPx(52));

//        ShapeDrawable secondLayer = new ShapeDrawable(new OvalShape());
//        secondLayer.getPaint().setColor(android.graphics.Color.parseColor("#B0BEC5"));
//        secondLayer.setPadding((int) UI.dpToPx(4), (int) UI.dpToPx(4), (int) UI.dpToPx(4), (int) UI.dpToPx(4));
//
//        GradientDrawable secondLayerStroke = new GradientDrawable();
//        secondLayerStroke.setShape(GradientDrawable.OVAL);
//        secondLayerStroke.setStroke((int) UI.dpToPx(2), android.graphics.Color.parseColor("#90A4AE"));
//        secondLayerStroke.setCornerRadius(UI.dpToPx(50));
//
//        GradientDrawable thirdLayer = new GradientDrawable(
//                GradientDrawable.Orientation.BOTTOM_TOP,
//                new int[]{android.graphics.Color.parseColor("#B0BEC5"), android.graphics.Color.parseColor("#90A4AE")}
//        );
//        thirdLayer.setShape(GradientDrawable.OVAL);
//        thirdLayer.setCornerRadius(UI.dpToPx(50));

//        Drawable[] layers = {firstLayer, secondLayerStroke, thirdLayer};
//        LayerDrawable layerDrawable = new LayerDrawable(layers);

        actionButton.setBackground(firstLayer);
    }

    public static FrameLayout createCircularSyncDetailsButtonContainer(Activity activity) {
        FrameLayout frameLayout = new FrameLayout(activity);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                UI.dpToPx(100),
                UI.dpToPx(100)
        );
        frameParams.setMargins(UI.dpToPx(8), UI.dpToPx(8), UI.dpToPx(8), UI.dpToPx(8));
        frameLayout.setLayoutParams(frameParams);

        ImageButton syncDetailsButton = new ImageButton(activity);
        syncDetailsButtonId = View.generateViewId();
        syncDetailsButton.setId(syncDetailsButtonId);
        syncDetailsButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        syncDetailsButton.setPadding(UI.dpToPx(16), UI.dpToPx(16), UI.dpToPx(16), UI.dpToPx(16));
        syncDetailsButton.setLayoutParams(imageParams);

        TextView syncDetailsText = new TextView(activity);
        syncDetailsButtonTextId = View.generateViewId();
        syncDetailsText.setId(syncDetailsButtonTextId);
        syncDetailsText.setText("Sync Details");
        syncDetailsText.setTextSize(28);
        syncDetailsText.setTextColor(ContextCompat.getColor(activity, R.color.textColor));
        syncDetailsText.setScaleX(0.9f);
        syncDetailsText.setPadding(UI.dpToPx(4), UI.dpToPx(4), UI.dpToPx(4), UI.dpToPx(4));
        syncDetailsText.setTypeface(ResourcesCompat.getFont(activity, R.font.ptsansnarrowwebregular));
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        syncDetailsText.setLayoutParams(textParams);

        addGradientOnToSyncDetailsButton(syncDetailsButton);
        frameLayout.addView(syncDetailsButton);
        frameLayout.addView(syncDetailsText);

        return frameLayout;
    }

    public static void setListenerForSyncDetailsButton(Activity activity){
        LinearLayout syncDetailsStatisticsLayout = activity.findViewById(syncDetailsStatisticsLayoutId);
        ImageButton syncDetailsButton = activity.findViewById(syncDetailsButtonId);
        syncDetailsButton.setOnClickListener(view -> {
            if(syncDetailsStatisticsLayout.getVisibility() == View.GONE){
                syncDetailsStatisticsLayout.setVisibility(View.VISIBLE);
            }else{
                syncDetailsStatisticsLayout.setVisibility(View.GONE);
            }
        });
    }

    public static void setSyncStatusDetailsTextView(Activity activity, boolean isWarning){
        TextView syncStatusTextView = activity.findViewById(SyncButton.warningTextViewId);
        MainActivity.activity.runOnUiThread(() -> {
            syncStatusTextView.setText(MainActivity.syncDetailsStatus);
            if(isWarning){
                syncStatusTextView.setTextColor(MainActivity.currentTheme.warningTextColor);
            }else{
                syncStatusTextView.setTextColor(MainActivity.currentTheme.syncProgressTextColor);
            }
        });
    }

    public static double getTotalLibzFolderSizes(){
        double[] libzFolderSize = {0};

        Thread getTotalLibzFolderSizesThread = new Thread(() -> {
            List<String[]> accounts = DBHelper.getAccounts(new String[]{"type","userEmail"});

            for (String[] account: accounts){
                if (account[0].equals("backup")){
                    double folderSize = GoogleDriveFolders.getSizeOfAssetsFolder(account[1]);
                    libzFolderSize[0] += folderSize;
                    Log.d("SyncDetails","size of " + account[1] + " is " + folderSize);
                }
            }
        });
        getTotalLibzFolderSizesThread.start();
        try {
            getTotalLibzFolderSizesThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"SyncDetails");
        }
        return libzFolderSize[0];
    }

    public static double getTotalUnsyncedAssetsOfDevices(){
        double unsyncedAssetsOfDevicesSize = 0;
        ArrayList<DeviceHandler> devices = DBHelper.getDevicesFromDB();
        for (DeviceHandler device : devices){
            JsonObject storageData = Devices.getStorageStatus(device.getDeviceId());
            double media = storageData.get("mediaStorage").getAsDouble();
            double synced = 0;
            if (storageData.has("syncedAssetsStorage")){
                synced =storageData.get("syncedAssetsStorage").getAsDouble();
            }
            Log.d("SyncDetails", "media - sync : " + media + " - " + synced + " = " + (media -synced));
            unsyncedAssetsOfDevicesSize += (media - synced);
        }
        return unsyncedAssetsOfDevicesSize;
    }


}
