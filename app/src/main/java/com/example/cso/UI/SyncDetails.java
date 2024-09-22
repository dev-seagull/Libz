package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.example.cso.DBHelper;
import com.example.cso.MainActivity;
import com.example.cso.R;

public class SyncDetails {

    public static void handleSyncDetailsButton(Activity activity){
        double percentageOfSyncedAssets = DBHelper.getPercentageOfSyncedAssets();
        TextView syncDetailsButtonTextView = activity.findViewById(R.id.syncDetailsButtonText);
        activity.runOnUiThread( () -> {
            syncDetailsButtonTextView.setText(String.format("%d%%", Math.round(percentageOfSyncedAssets)));
            Typeface dseg7classic_regular = ResourcesCompat.getFont(activity,R.font.ptsansnarrowwebregular);
            syncDetailsButtonTextView.setTypeface(dseg7classic_regular);
            syncDetailsButtonTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            syncDetailsButtonTextView.setGravity(Gravity.CENTER);
        });

        createSyncDetailsLayout(activity);
    }

    public static void createSyncDetailsLayout(Activity activity){
        LinearLayout syncButtonsLayout = activity.findViewById(R.id.syncButtonsLayout);
        LinearLayout syncDetailsLayout = Details.createDetailsLayout(activity);
        syncDetailsLayout.setPadding(0,15,0,15);
        ImageButton syncDetailsButton = activity.findViewById(R.id.syncDetailsButton);

        syncDetailsLayout.setVisibility(View.GONE);
        syncButtonsLayout.addView(syncDetailsLayout);

        TextView textView = new TextView(activity);
        textView.setText("Sync assets count: "+ DBHelper.countAndroidSyncedAssetsOnThisDevice(MainActivity.androidUniqueDeviceIdentifier));
        syncDetailsLayout.addView(textView);

        syncDetailsButton.setOnClickListener(view -> {
            if(syncDetailsLayout.getVisibility() == View.GONE){
                syncDetailsLayout.setVisibility(View.VISIBLE);
            }else{
                syncButtonsLayout.setVisibility(View.GONE);
            }
        });
    }

}
