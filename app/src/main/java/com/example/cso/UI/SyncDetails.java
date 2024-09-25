package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
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
import com.example.cso.MainActivity;
import com.example.cso.R;

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
        TextView textView = new TextView(activity);
        textView.setText("Sync assets count: "+ DBHelper.countAndroidSyncedAssetsOnThisDevice(MainActivity.androidUniqueDeviceIdentifier));
        syncDetailsStatisticsLayout.addView(textView);
        return syncDetailsStatisticsLayout;
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
        syncDetailsButton.setBackgroundResource(R.drawable.circular_button_on);
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

}
