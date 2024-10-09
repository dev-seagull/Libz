package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.SharedPreferencesHandler;
import com.example.cso.Sync;
import com.example.cso.TimerService;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class SyncButton {

    public static int syncButtonId;
    public static int warningTextViewId;
    public static int syncButtonsParentLayoutId;
    public static void initializeSyncButton(Activity activity){
        LiquidFillButton syncButton = activity.findViewById(syncButtonId);

        boolean[] syncState = {SharedPreferencesHandler.getSyncSwitchState()};
        boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");
        if(! (syncState[0] && isServiceRunning)){
            SharedPreferencesHandler.setSwitchState("syncSwitchState",false, MainActivity.preferences);
            syncState[0] = false;
        }
        updateSyncAndWifiButtonBackground(syncButton, syncState[0]);

        syncButton.setOnClickListener(view -> {
            handleSyncButtonClick(activity);
        });
    }

    public static void startSyncButtonAnimation(Activity activity){
        LiquidFillButton syncButton = activity.findViewById(syncButtonId);
        activity.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                syncButton.startFillAnimation();
            }
            SyncDetails.setSyncStatusDetailsTextView(activity, false);
        });

    }

    public static void handleSyncButtonClick(Activity activity){
        LiquidFillButton syncButton = activity.findViewById(syncButtonId);
        boolean currentSyncState = toggleSyncState();
        boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");
        Log.d("ui","Sync state after button click: " + currentSyncState);
        if(currentSyncState){
            startSyncIfNotRunning(isServiceRunning, activity);
        }else{
            TextView warningText = MainActivity.activity.findViewById(warningTextViewId);
            warningText.setText("");
            stopSyncIfRunning(isServiceRunning, activity);
        }
        updateSyncAndWifiButtonBackground(syncButton,currentSyncState);
    }

    public static void startSyncIfNotRunning(boolean isServiceRunning, Activity activity){
        try{
            if(!isServiceRunning){
                Sync.startSync(activity);
            }
//            startSyncButtonAnimation(activity);
        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e);}
    }

    public static void stopSyncIfRunning(boolean isServiceRunning, Activity activity){
        try{
            if(isServiceRunning){
                Sync.stopSync();
            }
            stopSyncButtonAnimation(activity);
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
    }

    public static void stopSyncButtonAnimation(Activity activity){
        LiquidFillButton syncButton = activity.findViewById(syncButtonId);
        activity.runOnUiThread(syncButton::endFillAnimation);
        SyncDetails.setSyncStatusDetailsTextView(activity, false);
    }

    public static RotateAnimation createContinuousRotateAnimation() {
        RotateAnimation continuousRotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        continuousRotate.setDuration(4000);
        continuousRotate.setStartOffset(1000);
        continuousRotate.setRepeatCount(Animation.INFINITE);
        return continuousRotate;
    }

    public static boolean toggleSyncState() {
        try{
            boolean state = SharedPreferencesHandler.getSyncSwitchState();
            SharedPreferencesHandler.setSwitchState("syncSwitchState",!state,MainActivity.preferences);
            return !state;
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
        return false;
    }

    public static void updateSyncAndWifiButtonBackground(View button, Boolean state) {
        try{
            if (state){
                addGradientOnToSyncButton((LiquidFillButton) button);
            }else{
                addGradientOffToSyncButton((LiquidFillButton) button);
            }
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
    }

    public static void addGradientOffToSyncButton(LiquidFillButton actionButton){
        GradientDrawable firstLayer = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {MainActivity.currentTheme.OffSyncButtonGradientStart,
                MainActivity.currentTheme.OffSyncButtonGradientEnd}
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

    public static void addGradientOnToSyncButton(LiquidFillButton actionButton){
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

    public static TextView createSyncProgressTextView(Activity activity){
        TextView warningText = new TextView(activity);
        warningTextViewId = View.generateViewId();
        warningText.setId(warningTextViewId);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, UI.dpToPx(20), 0, 0);
        warningText.setGravity(Gravity.CENTER);
        warningText.setVisibility(View.GONE);
        warningText.setTextColor(MainActivity.currentTheme.warningTextColor);
        warningText.setHorizontallyScrolling(true);
        warningText.setSingleLine(true);
        warningText.setEllipsize(TextUtils.TruncateAt.END);
        return warningText;
    }

    public static FrameLayout createCircularButtonContainer(Context context) {
        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                UI.dpToPx(100),
                UI.dpToPx(100)
        );
        frameParams.setMargins(UI.dpToPx(8), UI.dpToPx(8), UI.dpToPx(8), UI.dpToPx(8));
        frameLayout.setLayoutParams(frameParams);

        LiquidFillButton syncButton = new LiquidFillButton(context,null);
        syncButtonId = View.generateViewId();
        syncButton.setId(syncButtonId);
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                UI.dpToPx(100),
                UI.dpToPx(100),
                Gravity.CENTER
        );
        syncButton.setPadding(UI.dpToPx(16), UI.dpToPx(16), UI.dpToPx(16), UI.dpToPx(16));
        syncButton.setLayoutParams(buttonParams);

        TextView syncText = new TextView(context);
        syncText.setId(View.generateViewId());
        syncText.setText("Sync");
        syncText.setTextSize(22);
        syncText.setTextColor(ContextCompat.getColor(context, R.color.textColor));
        syncText.setScaleX(0.9f);
        syncText.setLetterSpacing(0.1f);
        syncText.setPadding(UI.dpToPx(4), UI.dpToPx(4), UI.dpToPx(4), UI.dpToPx(4));
        syncText.setTypeface(ResourcesCompat.getFont(context, R.font.ptsansnarrowwebregular), Typeface.BOLD);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        syncText.setLayoutParams(textParams);

        frameLayout.addView(syncButton);
        frameLayout.addView(syncText);

        return frameLayout;
    }

    public static LinearLayout createSyncButtonsParentLayout(Activity activity){
        LinearLayout syncButtonsParentLayout = new LinearLayout(activity);
        syncButtonsParentLayout.setOrientation(LinearLayout.HORIZONTAL);
        syncButtonsParentLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        syncButtonsParentLayout.setLayoutParams(params);
        syncButtonsParentLayout.addView(SyncDetails.createCircularSyncDetailsButtonContainer(activity));
        syncButtonsParentLayout.addView(createCircularButtonContainer(activity));
        syncButtonsParentLayout.addView(WifiOnlyButton.createCircularWifiButtonContainer(activity));

        return syncButtonsParentLayout;
    }

    public static LinearLayout createVerticalLayoutForSyncButtonsAndStatistics(Activity activity){
        LinearLayout syncDetailsVerticalLayout = new LinearLayout(activity);
        syncDetailsVerticalLayout.setOrientation(LinearLayout.VERTICAL);
        syncDetailsVerticalLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 0);
        syncButtonsParentLayoutId = View.generateViewId();
        syncDetailsVerticalLayout.setId(syncButtonsParentLayoutId);
        syncDetailsVerticalLayout.setLayoutParams(params);
        LinearLayout syncDetailsStatisticsLayout = SyncDetails.createSyncDetailsStatisticsLayout(activity);
        syncDetailsVerticalLayout.addView(createSyncButtonsParentLayout(activity));
        syncDetailsVerticalLayout.addView(SyncButton.createSyncProgressTextView(activity));
        syncDetailsVerticalLayout.addView(syncDetailsStatisticsLayout);

        return syncDetailsVerticalLayout;
    }


}
