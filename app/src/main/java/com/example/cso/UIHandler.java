package com.example.cso;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class UIHandler {

    public static void handleSwitchMaterials(){
        handleSyncSwitchMaterials();
        handleWifiOnlySwitchMaterial();
    }

    public static void handleSyncSwitchMaterials(){
        SwitchMaterial syncSwitchMaterialButton = MainActivity.activity.findViewById(R.id.syncSwitchMaterial);
        if (!MainActivity.isMyServiceRunning(MainActivity.activity.getApplicationContext(),TimerService.class).equals("on")){
            MainActivity.activity.runOnUiThread(() -> {
                syncSwitchMaterialButton.setChecked(false);
                syncSwitchMaterialButton.setThumbTintList(UIHelper.offSwitchMaterialThumb);
                syncSwitchMaterialButton.setTrackTintList(UIHelper.offSwitchMaterialTrack);
            });
        }else{
            MainActivity.activity.runOnUiThread(() -> {
                syncSwitchMaterialButton.setChecked(true);
                syncSwitchMaterialButton.setThumbTintList(UIHelper.onSwitchMaterialThumb);
                syncSwitchMaterialButton.setTrackTintList(UIHelper.onSwitchMaterialTrack);
            });

        }
    }

    public static void handleWifiOnlySwitchMaterial(){
        SwitchMaterial syncSwitchMaterialButton = MainActivity.activity.findViewById(R.id.syncSwitchMaterial);
        SwitchMaterial wifiOnlySwitchMaterialButton = MainActivity.activity.findViewById(R.id.wifiOnlySwitchMaterial);
        MainActivity.activity.runOnUiThread(() -> {
            if (SharedPreferencesHandler.getWifiOnlySwitchState(MainActivity.preferences)){
                    wifiOnlySwitchMaterialButton.setChecked(true);
                    wifiOnlySwitchMaterialButton.setThumbTintList(UIHelper.onSwitchMaterialThumb);
                    wifiOnlySwitchMaterialButton.setTrackTintList(UIHelper.onSwitchMaterialTrack);
            }else{
                    wifiOnlySwitchMaterialButton.setChecked(false);
                    wifiOnlySwitchMaterialButton.setThumbTintList(UIHelper.offSwitchMaterialThumb);
                    wifiOnlySwitchMaterialButton.setTrackTintList(UIHelper.offSwitchMaterialTrack);
            }
            if (syncSwitchMaterialButton.isChecked()){
                    wifiOnlySwitchMaterialButton.setAlpha(1.0f);
                    wifiOnlySwitchMaterialButton.setEnabled(true);

            }else{
                    wifiOnlySwitchMaterialButton.setAlpha(0.5f);
                    wifiOnlySwitchMaterialButton.setEnabled(false);
            }
        });
    }

    public static void setLastBackupAccountButtonClickableFalse(Activity activity) {
        try {
            LinearLayout backupButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
            View[] child = {backupButtonsLinearLayout.getChildAt(
                    backupButtonsLinearLayout.getChildCount() - 1)};
            activity.runOnUiThread(() ->
                    child[0].setClickable(false));
        } catch (Exception e) {
            LogHandler.saveLog("Failed to set last back up button clickable false:" + e.getLocalizedMessage(), true);
        }
    }

}
