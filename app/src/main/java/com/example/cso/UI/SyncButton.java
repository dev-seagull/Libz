package com.example.cso.UI;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.cso.LogHandler;
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
    public static int syncTextId;
    public static int rotateSyncButtonId;
    public static void initializeSyncButton(Activity activity){
        LiquidFillButton syncButton = activity.findViewById(syncButtonId);
        ImageView rotateSyncButton = activity.findViewById(rotateSyncButtonId);

        syncButton.setOnClickListener(view -> {
            handleSyncButtonClick(activity);
        });

        rotateSyncButton.setOnClickListener(view -> {
            handleRotateSyncButtonClick(activity);
        });

        boolean syncState = SharedPreferencesHandler.getSyncSwitchState();
        boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");

        if(!(syncState && isServiceRunning)){
            SharedPreferencesHandler.setSwitchState("syncSwitchState",false, MainActivity.preferences);
            syncState = false;
        }
        updateSyncAndWifiButtonBackground(activity,syncState);
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
        try{
            Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            boolean currentSyncState = toggleSyncState();
            Log.d("ui","Sync state after button click: " + currentSyncState);
            updateSyncAndWifiButtonBackground(activity,true);
            Sync.startSync(activity);
        }catch (Exception e){
            LogHandler.crashLog(e,"ui");
        }
    }

    public static void handleRotateSyncButtonClick(Activity activity){
        try{
            Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            boolean currentSyncState = toggleSyncState();
            Log.d("ui","Sync state after button click: " + currentSyncState);
            updateSyncAndWifiButtonBackground(activity,false);
            Sync.stopSync(activity);
        }catch (Exception e){
            LogHandler.crashLog(e,"ui");
        }
    }

    public static boolean toggleSyncState() {
        try{
            boolean state = SharedPreferencesHandler.getSyncSwitchState();
            SharedPreferencesHandler.setSwitchState("syncSwitchState",!state,MainActivity.preferences);
            return !state;
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
        return false;
    }

    public static void updateSyncAndWifiButtonBackground(Activity activity, Boolean syncState) {
        LiquidFillButton syncButton = activity.findViewById(syncButtonId);
        TextView syncText = activity.findViewById(syncTextId);
        ImageView rotateSyncButton = activity.findViewById(rotateSyncButtonId);
        try{
            if (syncState){
                syncButton.setVisibility(View.GONE);
                syncText.setVisibility(View.GONE);
                rotateSyncButton.setVisibility(View.VISIBLE);
            }else{
                rotateSyncButton.setVisibility(View.GONE);
                syncText.setVisibility(View.VISIBLE);
                syncButton.setVisibility(View.VISIBLE);
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
        addGradientOffToSyncButton(syncButton);


        TextView syncText = new TextView(context);
        syncTextId = View.generateViewId();
        syncText.setId(syncTextId);
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

        ImageView rotateSyncButton = new ImageView(context);
        rotateSyncButtonId = View.generateViewId();
        rotateSyncButton.setId(rotateSyncButtonId);
        rotateSyncButton.setImageResource(R.drawable.back_sync);
        ObjectAnimator rotateAnimation = ObjectAnimator.ofFloat(rotateSyncButton, "rotation", 0f, 360f);
        rotateAnimation.setDuration(2000);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimation.start();

        frameLayout.addView(syncButton);
        frameLayout.addView(syncText);
        frameLayout.addView(rotateSyncButton);

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
