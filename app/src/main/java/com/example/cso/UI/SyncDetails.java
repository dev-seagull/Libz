package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
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
//            setListenerForSyncDetailsButton(activity);
        });
    }

    public static void addGradientOnToSyncDetailsButton(ImageButton actionButton, double percentage){
        int colorStart = MainActivity.currentTheme.deviceStorageChartColors[3];
        int colorEnd = MainActivity.currentTheme.deviceStorageChartColors[2];

        GradientDrawable firstLayer = new GradientDrawable();
        if(percentage == 100){
            firstLayer.setColors(new int[]{colorStart, colorStart});
        }else if(percentage == 0){
            firstLayer.setColors(new int[]{colorEnd, colorEnd});
        }else{
            final float inverseRatio = 1 - (float) percentage / 100;
            float r = Color.red(colorStart) * (float) percentage / 100 + Color.red(colorEnd) * inverseRatio;
            float g = Color.green(colorStart) * (float) percentage / 100 + Color.green(colorEnd) * inverseRatio;
            float b = Color.blue(colorStart) * (float) percentage / 100 + Color.blue(colorEnd) * inverseRatio;
            int blendedColor = Color.rgb((int) r, (int) g, (int) b);
            firstLayer.setColors(new int[]{colorStart, blendedColor, colorEnd});
        }

        firstLayer.setShape(GradientDrawable.OVAL);
        firstLayer.setSize(UI.dpToPx(104), UI.dpToPx(104));
        firstLayer.setCornerRadius(UI.dpToPx(52));

        firstLayer.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        firstLayer.setGradientCenter(0.5f, 0.5f);
        firstLayer.setGradientRadius(1f);
        firstLayer.setOrientation(GradientDrawable.Orientation.TR_BL);

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
        syncDetailsText.setTextSize(32);
        syncDetailsText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        syncDetailsText.setScaleX(0.9f);
        syncDetailsText.setPadding(UI.dpToPx(4), UI.dpToPx(4), UI.dpToPx(4), UI.dpToPx(4));
        syncDetailsText.setTypeface(null, Typeface.BOLD);
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

}
