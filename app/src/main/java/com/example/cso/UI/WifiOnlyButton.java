package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.SharedPreferencesHandler;
import com.example.cso.Sync;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Arrays;

public class WifiOnlyButton {

    public static int wifiOnlyButtonId ;

    public static void initializeWifiOnlyButton(Activity activity){
        SwitchMaterial wifiButton = activity.findViewById(wifiOnlyButtonId);
        boolean wifiOnlyState = SharedPreferencesHandler.getWifiOnlySwitchState();
        Log.d("syncstat", "wifi : "  + String.valueOf(wifiOnlyState));
        updateSyncAndWifiButtonBackground(wifiButton, wifiOnlyState);

        wifiButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            handleWifiOnlyButtonClick(activity);
        });
    }

    public static void handleWifiOnlyButtonClick(Activity activity){
        boolean currentWifiOnlyState = toggleWifiOnlyOnState();
        SwitchMaterial wifiButton = activity.findViewById(wifiOnlyButtonId);
        Log.d("syncstat","current is: " + currentWifiOnlyState) ;
        updateSyncAndWifiButtonBackground(wifiButton,currentWifiOnlyState);
    }

    public static boolean toggleWifiOnlyOnState() {
        boolean previousState = SharedPreferencesHandler.getWifiOnlySwitchState();
        SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",!previousState, MainActivity.preferences);
        return !previousState;
    }

    public static void updateSyncAndWifiButtonBackground(View button, Boolean state) {
        try{
            MainActivity.activity.runOnUiThread(() -> {
                SwitchMaterial switchButton = (SwitchMaterial) button;
                if (state){
                    Log.d("syncstat","here setting it to true");
                    switchButton.setChecked(true);
                    int color =  MainActivity.currentTheme.OnSyncButtonGradientStart;
                    switchButton.setTrackTintList(ColorStateList.valueOf(color));
                    switchButton.setThumbTintList(ColorStateList.valueOf(color));
                }else{
                    Log.d("syncstat","here setting it to false");
                    switchButton.setChecked(false);
                    int color =  MainActivity.currentTheme.OffSyncButtonGradientStart;
                    switchButton.setTrackTintList(ColorStateList.valueOf(color));
                    switchButton.setThumbTintList(ColorStateList.valueOf(color));
                }
            });
        }catch (Exception e){
            LogHandler.crashLog(e,"ui");}
    }

    public static void addGradientOnToWifiButton(SwitchMaterial actionSwitch){
        GradientDrawable firstLayer = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {MainActivity.currentTheme.OnSyncButtonGradientStart,
                        MainActivity.currentTheme.OnSyncButtonGradientEnd}
        );
        firstLayer.setShape(GradientDrawable.RECTANGLE);
        firstLayer.setCornerRadius(UI.dpToPx(20));
        actionSwitch.setBackground(firstLayer);

        if (actionSwitch.isChecked()) {
            actionSwitch.setThumbTintList(ColorStateList.valueOf(Color.GREEN));
        } else {
            actionSwitch.setThumbTintList(ColorStateList.valueOf(Color.RED));
        }
    }


    public static FrameLayout createCircularWifiButtonContainer(Context context) {
        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                UI.dpToPx(100),
                UI.dpToPx(50)
        );
        frameParams.setMargins(UI.dpToPx(10), 0, UI.dpToPx(2), 0);
        frameLayout.setLayoutParams(frameParams);

        SwitchMaterial wifiSwitch = new SwitchMaterial(context);
        wifiOnlyButtonId = View.generateViewId();
        wifiSwitch.setId(wifiOnlyButtonId);
        FrameLayout.LayoutParams switchParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.LEFT
        );
        wifiSwitch.setLayoutParams(switchParams);
        wifiSwitch.setText("Wifi only");
        wifiSwitch.setTextColor(MainActivity.currentTheme.primaryTextColor);

        frameLayout.addView(wifiSwitch);

        return frameLayout;
    }


}
