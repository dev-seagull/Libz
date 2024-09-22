package com.example.cso.UI;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.example.cso.DBHelper;
import com.example.cso.R;

public class SyncDetails {

    public static void handleSyncDetailsButton(Activity activity){
        double percentageOfSyncedAssets = DBHelper.getPercentageOfSyncedAssets();
        TextView syncDetailsButtonTextView = activity.findViewById(R.id.syncDetailsButtonText);
        activity.runOnUiThread( () -> {
            syncDetailsButtonTextView.setText(String.format("%d%%", Math.round(percentageOfSyncedAssets)));
            Typeface dseg7classic_regular = ResourcesCompat.getFont(activity,R.font.dseg7classic_regular);
            syncDetailsButtonTextView.setTypeface(dseg7classic_regular);
            syncDetailsButtonTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            syncDetailsButtonTextView.setGravity(Gravity.CENTER);
            syncDetailsButtonTextView.setLetterSpacing(0.1f);
        });
    }

}
