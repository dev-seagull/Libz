package com.example.cso.UI;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.SharedPreferencesHandler;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class WifiOnlyButton {

    public static void initializeWifiOnlyButton(Activity activity){
        ImageButton wifiButton = activity.findViewById(R.id.wifiButton);
        TextView wifiButtonText = activity.findViewById(R.id.wifiButtonText);
        boolean[] wifiOnlyState = {SharedPreferencesHandler.getWifiOnlySwitchState()};
        if(wifiOnlyState[0]){
            wifiButtonText.setTextColor(Tools.buttonTextColor);
        }else{
            wifiButtonText.setTextColor(Tools.buttonTextColor);
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
            wifiButtonText.setTextColor(Tools.buttonTextColor);
        }else{
            wifiButtonText.setTextColor(Tools.buttonTextColor);
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
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);}
    }

}
