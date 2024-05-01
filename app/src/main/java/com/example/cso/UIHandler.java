package com.example.cso;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class UIHandler {

    public static void handleSwitchMaterials(){
        handleSyncSwitchMaterials();
        handleWifiSwitchMaterial();
        handleMobileDataSwitchMaterial();
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

    public static void handleWifiSwitchMaterial(){
        SwitchMaterial syncSwitchMaterialButton = MainActivity.activity.findViewById(R.id.syncSwitchMaterial);
        SwitchMaterial wifiSwitchMaterialButton = MainActivity.activity.findViewById(R.id.wifiSwitchMaterial);
        if (!syncSwitchMaterialButton.isChecked()){
            SharedPreferencesHandler.setSwitchState("wifiSwitchState", false, MainActivity.preferences);
            MainActivity.activity.runOnUiThread(() -> {
                wifiSwitchMaterialButton.setChecked(false);
                wifiSwitchMaterialButton.setThumbTintList(UIHelper.disabledSwitchMaterialThumb);
                wifiSwitchMaterialButton.setTrackTintList(UIHelper.disabledSwitchMaterialTrack);
            });
            wifiSwitchMaterialButton.setEnabled(false);
        }else{
            wifiSwitchMaterialButton.setEnabled(true);
            if (SharedPreferencesHandler.getWifiSwitchState(MainActivity.preferences)){
                MainActivity.activity.runOnUiThread(() -> {
                    wifiSwitchMaterialButton.setChecked(true);
                    wifiSwitchMaterialButton.setThumbTintList(UIHelper.onSwitchMaterialThumb);
                    wifiSwitchMaterialButton.setTrackTintList(UIHelper.onSwitchMaterialTrack);
                });
            }else{
                MainActivity.activity.runOnUiThread(() -> {
                    wifiSwitchMaterialButton.setChecked(false);
                    wifiSwitchMaterialButton.setThumbTintList(UIHelper.offSwitchMaterialThumb);
                    wifiSwitchMaterialButton.setTrackTintList(UIHelper.offSwitchMaterialTrack);
                });
            }
        }
    }


    public static void handleMobileDataSwitchMaterial(){
        SwitchMaterial syncSwitchMaterialButton = MainActivity.activity.findViewById(R.id.syncSwitchMaterial);
        SwitchMaterial mobileDataSwitchMaterial = MainActivity.activity.findViewById(R.id.mobileDataSwitchMaterial);
        if (!syncSwitchMaterialButton.isChecked()){
            SharedPreferencesHandler.setSwitchState("dataSwitchState", false, MainActivity.preferences);
            MainActivity.activity.runOnUiThread(() -> {
                mobileDataSwitchMaterial.setChecked(false);
                mobileDataSwitchMaterial.setThumbTintList(UIHelper.disabledSwitchMaterialThumb);
                mobileDataSwitchMaterial.setTrackTintList(UIHelper.disabledSwitchMaterialTrack);
            });
            mobileDataSwitchMaterial.setEnabled(false);
        }else{
            mobileDataSwitchMaterial.setEnabled(true);
            if (SharedPreferencesHandler.getDataSwitchState(MainActivity.preferences)){
                MainActivity.activity.runOnUiThread(() -> {
                    mobileDataSwitchMaterial.setChecked(true);
                    mobileDataSwitchMaterial.setThumbTintList(UIHelper.onSwitchMaterialThumb);
                    mobileDataSwitchMaterial.setTrackTintList(UIHelper.onSwitchMaterialTrack);
                });
            }else{
                MainActivity.activity.runOnUiThread(() -> {
                    mobileDataSwitchMaterial.setChecked(false);
                    mobileDataSwitchMaterial.setThumbTintList(UIHelper.offSwitchMaterialThumb);
                    mobileDataSwitchMaterial.setTrackTintList(UIHelper.offSwitchMaterialTrack);
                });
            }
        }
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
