package com.example.cso.UI;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.SharedPreferencesHandler;
import com.example.cso.Sync;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class WifiOnlyButton {

    public static void initializeWifiOnlyButton(Activity activity){
        ImageButton wifiButton = activity.findViewById(R.id.wifiButton);
        TextView wifiButtonText = activity.findViewById(R.id.wifiButtonText);
        boolean[] wifiOnlyState = {SharedPreferencesHandler.getWifiOnlySwitchState()};
        if(wifiOnlyState[0]){
            wifiButtonText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        }else{
            wifiButtonText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        }
        updateSyncAndWifiButtonBackground(wifiButton, wifiOnlyState[0], activity);

        wifiButton.setOnClickListener(view -> {
            handleWifiOnlyButtonClick(activity);
        });
    }

    public static void handleWifiOnlyButtonClick(Activity activity){
        ImageButton wifiButton = activity.findViewById(R.id.wifiButton);
        TextView wifiButtonText = activity.findViewById(R.id.wifiButtonText);
        boolean currentWifiOnlyState = toggleWifiOnlyOnState();
        if(currentWifiOnlyState){
            wifiButtonText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        }else{
            wifiButtonText.setTextColor(MainActivity.currentTheme.primaryTextColor);
        }
        updateSyncAndWifiButtonBackground(wifiButton,currentWifiOnlyState, activity);
    }

    public static boolean toggleWifiOnlyOnState() {
        boolean previousState = SharedPreferencesHandler.getWifiOnlySwitchState();
        SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",!previousState, MainActivity.preferences);
        return !previousState;
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
            if (state){
                backgroundResource = R.drawable.circular_button_on;
                button.setBackgroundResource(backgroundResource);
            }else{
                SyncButton.addGradientOffToButton((ImageButton) button);
            }
            textView.setTextColor(MainActivity.currentTheme.primaryTextColor);
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);}
    }

}
