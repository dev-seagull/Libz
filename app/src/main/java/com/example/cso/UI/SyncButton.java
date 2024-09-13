package com.example.cso.UI;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.TextView;

import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.SharedPreferencesHandler;
import com.example.cso.Sync;
import com.example.cso.TimerService;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class SyncButton {

    public static LiquidFillButton syncButton = MainActivity.activity.findViewById(R.id.syncButton);
    public static void initializeSyncButton(Activity activity){

        boolean[] syncState = {SharedPreferencesHandler.getSyncSwitchState()};
        boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");
        if(syncState[0] && isServiceRunning){
            startSyncButtonAnimation(activity);
        }else{
            SharedPreferencesHandler.setSwitchState("syncSwitchState",false, MainActivity.preferences);
            syncState[0] = false;
        }
        updateSyncAndWifiButtonBackground(syncButton, syncState[0], activity);

        syncButton.setOnClickListener(view -> {
            handleSyncButtonClick(activity);
        });
    }

    public static void startSyncButtonAnimation(Activity activity){
        activity.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                syncButton.startFillAnimation();
            }
        });

    }

    public static void handleSyncButtonClick(Activity activity){
        boolean currentSyncState = toggleSyncState();
        boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");
        if(currentSyncState){
            startSyncIfNotRunning(isServiceRunning, activity);
        }else{
            Tools.warningText.setText("");
            stopSyncIfRunning(isServiceRunning, activity);
        }
        updateSyncAndWifiButtonBackground(syncButton,currentSyncState, activity);
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
        activity.runOnUiThread(() -> {
            syncButton.endFillAnimation();
        });
    }

    public static RotateAnimation createContinuousRotateAnimation() {
        RotateAnimation continuousRotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        continuousRotate.setDuration(4000);
        continuousRotate.setStartOffset(1000);
        continuousRotate.setRepeatCount(Animation.INFINITE);
        return continuousRotate;
    }

    public static void updateSyncButtonRotationState(RotateAnimation animation,
                                                      boolean isSyncOn, Activity activity) {
        TextView syncButtonText = activity.findViewById(R.id.syncButtonText);
        if (isSyncOn) {
            syncButtonText.startAnimation(animation);
        } else {
            syncButtonText.clearAnimation();
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

    public static void updateSyncAndWifiButtonBackground(View button, Boolean state, Activity activity) {
        try{
            TextView syncButtonText = activity.findViewById(R.id.syncButtonText);
            TextView wifiButtonText = activity.findViewById(R.id.wifiButtonText);
            TextView textView;
            if (button.getId() == R.id.syncButton){
                textView = syncButtonText;
            }else{
                textView = wifiButtonText;
            }
            int backgroundResource;
            int textColor;
            if (state){
                backgroundResource = R.drawable.circular_button_on;
                textColor = Tools.buttonTextColor;
            }else{
                backgroundResource = R.drawable.circular_button_off;
                textColor = Tools.buttonTextColor;
            }
            textView.setTextColor(textColor);
            button.setBackgroundResource(backgroundResource);
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
    }


}
