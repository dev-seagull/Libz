package com.example.cso;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.gson.JsonObject;

public class SharedPreferencesHandler {

    public static boolean getWifiSwitchState(SharedPreferences sharedPreferences){
        return sharedPreferences.getBoolean("wifiSwitchState", false);
    }

    public static boolean getDataSwitchState(SharedPreferences sharedPreferences){
        return sharedPreferences.getBoolean("dataSwitchState", false);
    }

    public void setSwitchState(String switchStateKey, boolean state, SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean(switchStateKey, state);
        sharedPreferences.edit().apply();
    }

    public static void displayDialogForRestoreAccountsDecision(SharedPreferences preferences) {
        try{
            boolean shouldDisplay = preferences.getBoolean("shouldDisplayDialogForRestoreAccounts", false);
            if (shouldDisplay) {
                Button button = null ;
                LinearLayout linearLayout = MainActivity.activity.findViewById(R.id.backUpAccountsButtons);
                int buttonCounter = 0 ;
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    View child = linearLayout.getChildAt(i);
                    if (child instanceof Button) {
                        buttonCounter++;
                    }
                }
                button = (Button) linearLayout.getChildAt(buttonCounter - 1);
                String userEmail = button.getText().toString();
                JsonObject profileMapContent = Profile.readProfileMapContent(userEmail);
                MainActivity.showAccountsAddPopup(profileMapContent);
            }else{
                android.content.SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("shouldDisplayDialogForRestoreAccounts", false);
                editor.apply();
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to displayDialogForRestoreAccountsDecision : " + e.getLocalizedMessage());
        }
    }

    public static void setDisplayDialogForRestoreAccountsDecision(SharedPreferences preferences, boolean value) {
        try {
            android.content.SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("shouldDisplayDialogForRestoreAccounts", value);
            editor.apply();
        } catch (Exception e) {
     LogHandler.saveLog("Failed to set boolean in setDisplayDialogForRestoreAccountsDecision : " + e.getLocalizedMessage());
        }
    }
}
