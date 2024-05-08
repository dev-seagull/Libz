package com.example.cso;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIHandler {
    public static TextView directoryUsages = MainActivity.activity.findViewById(R.id.directoryUsages);
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

    public static void updateDirectoriesUsages(){
        HashMap<String,String> dirHashMap = MainActivity.storageHandler.directoryUIDisplay();
        directoryUsages.setText("");
        for (Map.Entry<String, String> entry : dirHashMap.entrySet()) {
            directoryUsages.append(entry.getKey() + ": " + entry.getValue() + " GB\n");
        }
    }

    public static void initializeDrawerLayout(Activity activity){
        DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        NavigationView navigationView = activity.findViewById(R.id.navigationView);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                activity, drawerLayout, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        MenuItem menuItem1 = navigationView.getMenu().findItem(R.id.navMenuItem1);
        String appVersion = BuildConfig.VERSION_NAME;
        SpannableString centeredText = new SpannableString("Version: " + appVersion);
        centeredText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, appVersion.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        menuItem1.setTitle(centeredText);
        AppCompatButton infoButton = activity.findViewById(R.id.infoButton);
        infoButton.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.END));
    }

    public static void initializeButtons(Activity activity,GoogleCloud googleCloud){
        String[] columnsList = {"userEmail", "type", "refreshToken"};
        List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columnsList);
        for (String[] account_row : account_rows) {
            String userEmail = account_row[0];
            String type = account_row[1];
            if (type.equals("primary")){
//                    LinearLayout primaryLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
//                    Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryLinearLayout);
//                    newGoogleLoginButton.setText(userEmail);
            }else if (type.equals("backup")){
                LinearLayout backupLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                Button newGoogleLoginButton = googleCloud.createBackUpLoginButton(backupLinearLayout);
                newGoogleLoginButton.setText(userEmail);
            }
        }
        Button androidDeviceButton = activity.findViewById(R.id.androidDeviceButton);
        androidDeviceButton.setText(MainActivity.androidDeviceName);

//          LinearLayout primaryAccountsButtonsLayout= findViewById(R.id.primaryAccountsButtons);
        LinearLayout backupAccountsButtonsLayout= activity.findViewById(R.id.backUpAccountsButtons);
//           Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryAccountsButtonsLayout);
//           newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        Button newBackupLoginButton = googleCloud.createBackUpLoginButton(backupAccountsButtonsLayout);
        newBackupLoginButton.setBackgroundTintList(UIHelper.addBackupAccountButtonColor);

    }

    private static void handeSyncSwitchMaterialButton(UIHelper uiHelper, Activity activity){
        if (!MainActivity.isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
            uiHelper.syncSwitchMaterialButton.setChecked(false);
            uiHelper.syncSwitchMaterialButton.setThumbTintList(UIHelper.offSwitchMaterialThumb);
            uiHelper.syncSwitchMaterialButton.setTrackTintList(UIHelper.offSwitchMaterialTrack);
        }else{
            uiHelper.syncSwitchMaterialButton.setChecked(true);
            uiHelper.syncSwitchMaterialButton.setThumbTintList(UIHelper.onSwitchMaterialThumb);
            uiHelper.syncSwitchMaterialButton.setTrackTintList(UIHelper.onSwitchMaterialTrack);
        }
    }

    private static void handleStatistics(UIHelper uiHelper){
        StorageHandler storageHandler = new StorageHandler();
        uiHelper.deviceStorage.setText("Storage : " + storageHandler.getFreeSpace() +
                " Out Of " + storageHandler.getTotalStorage()+ " GB\n"+
                "Media : "  + MainActivity.dbHelper.getPhotosAndVideosStorage() + "\n");

        uiHelper.androidStatisticsTextView.setVisibility(View.VISIBLE);
        int total_androidAssets_count = MainActivity.dbHelper.countAndroidAssets();
        uiHelper.androidStatisticsTextView.setText("Sync Status : " + MainActivity.dbHelper.countAndroidSyncedAssets() +
                " Of " + total_androidAssets_count);
    }

    private static void handleDisplayDirectoriesUsagesButton(UIHelper uiHelper, Activity activity){
        uiHelper.displayDirectoriesUsagesButton.setVisibility(View.VISIBLE);
        uiHelper.displayDirectoriesUsagesButton.setOnClickListener(view -> {
            if (UIHandler.directoryUsages.getVisibility() == View.VISIBLE) {
                Drawable newBackground = activity.getResources().getDrawable(R.drawable.down_vector_icon);
                uiHelper.displayDirectoriesUsagesButton.setBackground(newBackground);
                UIHandler.directoryUsages.setVisibility(View.GONE);
            } else {
                updateDirectoriesUsages();
                Drawable newBackground = activity.getResources().getDrawable(R.drawable.up_vector_icon);
                uiHelper.displayDirectoriesUsagesButton.setBackground(newBackground);
                UIHandler.directoryUsages.setVisibility(View.VISIBLE);
            }
        });
    }

    public static void startUpdateUIThread(Activity activity){
        LogHandler.saveLog("Starting startUpdateUIThread", false);
        Thread updateUIThread =  new Thread(() -> {
            try{
                UIHelper uiHelper = new UIHelper();
                activity.runOnUiThread(() -> {
                    handeSyncSwitchMaterialButton(uiHelper, activity);
                    handleStatistics(uiHelper);
                    handleDisplayDirectoriesUsagesButton(uiHelper, activity);
                });
            }catch (Exception e){
                LogHandler.saveLog("Failed to run on ui thread : " + e.getLocalizedMessage() , true);
            }
        });
        updateUIThread.start();
        LogHandler.saveLog("Finished startUpdateUIThread", false);
    }

}
