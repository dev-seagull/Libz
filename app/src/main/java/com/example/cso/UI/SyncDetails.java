package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
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

import com.anychart.charts.Pie;
import com.example.cso.DBHelper;
import com.example.cso.DeviceHandler;
import com.example.cso.GoogleDriveFolders;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.Sync;
import com.github.mikephil.charting.charts.PieChart;
import com.google.gson.JsonObject;

import org.checkerframework.checker.index.qual.LengthOf;

import java.util.ArrayList;
import java.util.List;

public class SyncDetails {

    public static int syncDetailsButtonTextId ;
    public static int syncDetailsButtonId;
    public static int syncDetailsStatisticsLayoutId;

    public static void handleSyncDetailsButton(Activity activity){
        double percentageOfSyncedAssets = DBHelper.getPercentageOfSyncedAssets();
        TextView syncDetailsButtonTextView = activity.findViewById(syncDetailsButtonTextId);
        ImageButton syncDetailsButton = activity.findViewById(syncDetailsButtonId);
        activity.runOnUiThread( () -> {
            syncDetailsButtonTextView.setText(String.format("%d%%", Math.round(percentageOfSyncedAssets)));
            SyncDetails.addGradientOnToSyncDetailsButton(syncDetailsButton, percentageOfSyncedAssets);
            Typeface dseg7classic_regular = ResourcesCompat.getFont(activity,R.font.ptsansnarrowwebregular);
            syncDetailsButtonTextView.setTypeface(dseg7classic_regular);
            syncDetailsButtonTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            syncDetailsButtonTextView.setGravity(Gravity.CENTER);
            setListenerForSyncDetailsButton(activity);
        });
    }

    public static LinearLayout createSyncDetailsStatisticsLayout(Activity activity){
        LinearLayout syncDetailsStatisticsLayout = Details.createInnerDetailsLayout(activity);
        syncDetailsStatisticsLayout.setPadding(0,15,0,15);

        syncDetailsStatisticsLayout.setVisibility(View.GONE);
        syncDetailsStatisticsLayoutId = View.generateViewId();
        syncDetailsStatisticsLayout.setId(syncDetailsStatisticsLayoutId);

        Details.createTitleTextView(activity, syncDetailsStatisticsLayout,"Sync Details");

        return syncDetailsStatisticsLayout;
    }

    public static void addGradientOnToSyncDetailsButton(ImageButton actionButton, double percentage){
        int colorStart = MainActivity.currentTheme.deviceStorageChartColors[3];
        int colorEnd = MainActivity.currentTheme.deviceStorageChartColors[2];

        if(percentage == 100){
            colorEnd = colorStart;
            GradientDrawable firstLayer = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {colorStart, colorEnd}
            );
            firstLayer.setShape(GradientDrawable.OVAL);
            firstLayer.setSize(UI.dpToPx(104), UI.dpToPx(104));
            firstLayer.setCornerRadius(UI.dpToPx(52));

            actionButton.setBackground(firstLayer);
        }else if(percentage == 0){
            colorStart = colorEnd;
            GradientDrawable firstLayer = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {colorStart, colorEnd}
            );
            firstLayer.setShape(GradientDrawable.OVAL);
            firstLayer.setSize(UI.dpToPx(104), UI.dpToPx(104));
            firstLayer.setCornerRadius(UI.dpToPx(52));

            actionButton.setBackground(firstLayer);
        }

        final float inverseRatio = 1 - (float) percentage / 100;
        float r = Color.red(colorStart) * (float) percentage / 100 + Color.red(colorEnd) * inverseRatio;
        float g = Color.green(colorStart) * (float) percentage / 100 + Color.green(colorEnd) * inverseRatio;
        float b = Color.blue(colorStart) * (float) percentage / 100 + Color.blue(colorEnd) * inverseRatio;
        int blendedColor =  Color.rgb((int) r, (int) g, (int) b);

        GradientDrawable firstLayer = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {colorStart,
                        blendedColor, colorEnd}
        );
        firstLayer.setShape(GradientDrawable.OVAL);
        firstLayer.setSize(UI.dpToPx(104), UI.dpToPx(104));
        firstLayer.setCornerRadius(UI.dpToPx(52));

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
        double percentageOfSyncedAssets = DBHelper.getPercentageOfSyncedAssets();
        syncDetailsText.setText(String.format("%d%%", Math.round(percentageOfSyncedAssets)));
        syncDetailsText.setTextSize(30);
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

        addGradientOnToSyncDetailsButton(syncDetailsButton, percentageOfSyncedAssets);
        frameLayout.addView(syncDetailsButton);
        frameLayout.addView(syncDetailsText);

        return frameLayout;
    }

    public static void setListenerForSyncDetailsButton(Activity activity){
        LinearLayout syncDetailsStatisticsLayout = activity.findViewById(syncDetailsStatisticsLayoutId);
        ImageButton syncDetailsButton = activity.findViewById(syncDetailsButtonId);
        syncDetailsButton.setOnClickListener(view -> {
            if(syncDetailsStatisticsLayout.getVisibility() == View.GONE){
                SyncDetailsPieChart.addPieChartToSyncDetailsStatisticsLayout(activity,syncDetailsStatisticsLayout);
                syncDetailsStatisticsLayout.setVisibility(View.VISIBLE);
            }else{
                syncDetailsStatisticsLayout.setVisibility(View.GONE);
                syncDetailsStatisticsLayout.removeAllViews();
            }
        });
    }

}
