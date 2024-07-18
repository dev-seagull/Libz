package com.example.cso;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.JsonObject;
import com.jaredrummler.android.device.DeviceName;

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
        if (!TimerService.isMyServiceRunning(MainActivity.activity.getApplicationContext(),TimerService.class).equals("on")){
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
        HashMap<String,String> dirHashMap = StorageHandler.directoryUIDisplay();
        directoryUsages.setText("");
        for (Map.Entry<String, String> entry : dirHashMap.entrySet()) {
            directoryUsages.append(entry.getKey() + ": " + entry.getValue() + " GB\n");
        }
    }

    public static void addAbackUpAccountToUI(Activity activity, boolean isBackedUp,
                                             ActivityResultLauncher<Intent> signInToBackUpLauncher, View[] child,
                                             GoogleCloud.signInResult signInResult){
        Thread uiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    if (isBackedUp) {
                        activity.runOnUiThread(() -> {
                            LinearLayout backupButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                            Button newBackupLoginButton = MainActivity.googleCloud.createBackUpLoginButton(backupButtonsLinearLayout);
                            newBackupLoginButton.setBackgroundTintList(UIHelper.backupAccountButtonColor);
                            child[0] = backupButtonsLinearLayout.getChildAt(
                                    backupButtonsLinearLayout.getChildCount() - 2);
                            LogHandler.saveLog(signInResult.getUserEmail()
                                    + " has logged in to the backup account", false);

                            if (child[0] instanceof Button) {
                                Button bt = (Button) child[0];
                                bt.setText(signInResult.getUserEmail());
                                bt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D47A1")));
                            }
                            updateButtonsListeners(signInToBackUpLauncher);
                        });
                    }
                }catch (Exception e){
                    LogHandler.saveLog("Failed to add a backup account to ui: " + e.getLocalizedMessage(), true);
                }
            }
        });
        uiThread.start();
        try{
            uiThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join add a backup account to ui thread: " + e.getLocalizedMessage(), true);
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
        MenuItem menuItem2 = navigationView.getMenu().findItem(R.id.navMenuItem2);
        SpannableString centeredText2 = new SpannableString("Device id: " + MainActivity.androidUniqueDeviceIdentifier);
        centeredText2.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, appVersion.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        menuItem2.setTitle(centeredText2);
        AppCompatButton infoButton = activity.findViewById(R.id.infoButton);
        infoButton.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.END));
    }

    public static void initializeButtons(Activity activity,GoogleCloud googleCloud){
        String[] columnsList = {"userEmail", "type", "refreshToken"};
        List<String[]> account_rows = DBHelper.getAccounts(columnsList);
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
        androidDeviceButton.setText(DeviceName.getDeviceName());
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(2000);
        androidDeviceButton.startAnimation(fadeIn);
        androidDeviceButton.setPadding(40,0,150,0);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int windowWidth = displayMetrics.widthPixels;
        int buttonWidth = (int) (windowWidth * 0.6);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                buttonWidth,
                200
        );
        layoutParams.setMargins(0,20,0,16);
        androidDeviceButton.setLayoutParams(layoutParams);

//          LinearLayout primaryAccountsButtonsLayout= findViewById(R.id.primaryAccountsButtons);
        LinearLayout backupAccountsButtonsLayout= activity.findViewById(R.id.backUpAccountsButtons);
//           Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryAccountsButtonsLayout);
//           newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        Button newBackupLoginButton = googleCloud.createBackUpLoginButton(backupAccountsButtonsLayout);
        newBackupLoginButton.setBackgroundTintList(UIHelper.backupAccountButtonColor);
    }

    private static void handeSyncSwitchMaterialButton(UIHelper uiHelper, Activity activity){
        if (!TimerService.isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
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
        int total_Assets_count = MainActivity.dbHelper.countAssets();
        int total_android_assets = MainActivity.dbHelper.countAndroidAssetsOnThisDevice();
        int android_synced_assets_count = MainActivity.dbHelper.countAndroidSyncedAssetsOnThisDevice();
        int unsynced_android_assets =  total_android_assets - android_synced_assets_count;
        int synced_assets = total_Assets_count -(MainActivity.dbHelper.countAndroidAssets() - MainActivity.dbHelper.countAndroidUnsyncedAssets());
        uiHelper.androidStatisticsTextView.setText("Sync Status : " + synced_assets +
                " Out Of " + total_Assets_count + "\n" + "Unsynced assets of this device : " + unsynced_android_assets);
    }

    private static void handleDisplayDirectoriesUsagesButton(UIHelper uiHelper, Activity activity){
        UIHelper.waitingGif.setVisibility(View.GONE);
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

    private static void handleSyncTextViewStatus(){
        UIHelper uiHelper = new UIHelper();
        if(!uiHelper.syncSwitchMaterialButton.isChecked()){
            uiHelper.syncMessageTextView.setVisibility(View.GONE);
            UIHelper.waitingSyncGif.setVisibility(View.GONE);
        }
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
                    handleSyncTextViewStatus();
                });
            }catch (Exception e){
                LogHandler.saveLog("Failed to run on ui thread : " + e.getLocalizedMessage() , true);
            }
        });
        updateUIThread.start();
        LogHandler.saveLog("Finished startUpdateUIThread", false);
    }

    public static void handleFailedSignInToBackUp(Activity activity, ActivityResultLauncher<Intent> signInToBackUpLauncher,
                                                  ActivityResult result){
        activity.runOnUiThread(() -> {
            LogHandler.saveLog("login with back up launcher failed with response code :" + result.getResultCode());
            LinearLayout backupAccountsButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
            View childview = backupAccountsButtonsLinearLayout.getChildAt(
                    backupAccountsButtonsLinearLayout.getChildCount() - 1);
            if(childview instanceof Button){
                Button bt = (Button) childview;
                bt.setText("ADD A BACK UP ACCOUNT");
            }
            updateButtonsListeners(signInToBackUpLauncher);
        });
    }

    public static void updateButtonsListeners(ActivityResultLauncher<Intent> signInToBackUpLauncher) {
//            updatePrimaryButtonsListener();
        updateBackupButtonsListener(MainActivity.activity, signInToBackUpLauncher);
    }

    public static void updateBackupButtonsListener(Activity activity, ActivityResultLauncher<Intent> signInToBackUpLauncher){
        LinearLayout backUpAccountsButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
        for (int i = 0; i < backUpAccountsButtonsLinearLayout.getChildCount(); i++) {
            View childView = backUpAccountsButtonsLinearLayout.getChildAt(i);
            if (childView instanceof Button) {
                Button button = (Button) childView;
                button.setOnClickListener(
                        view -> {
                            String buttonText = button.getText().toString().toLowerCase();
                            if (buttonText.equals("add a back up account")) {
                                button.setText("Wait");
                                button.setClickable(false);
                                MainActivity.googleCloud.signInToGoogleCloud(signInToBackUpLauncher);
                                button.setClickable(true);
                            } else if (buttonText.equals("wait")){
                                button.setText("add a back up account");
                            }
                            else {
                                PopupMenu popupMenu = setPopUpMenuOnButton(activity, button);
                                popupMenu.setOnMenuItemClickListener(item -> {
                                    if (item.getItemId() == R.id.sign_out) {
                                        try {
                                            button.setText("Wait...");
                                        }catch (Exception e){}

                                        GoogleCloud.startSignOutThreads(buttonText, item, button);
                                    }
                                    return true;
                                });
                                popupMenu.show();
                            }
                        }
                );
            }
        }
    }

    public static void startUiThreadForSignOut(MenuItem item, Button button, String buttonText, boolean isBackedUp){
        LogHandler.saveLog("Starting Ui Thread For Sign Out Thread", false);
        Thread uiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                MainActivity.activity.runOnUiThread(() -> {
                    if(isBackedUp){
                        try {
                            item.setEnabled(false);
                            ViewGroup parentView = (ViewGroup) button.getParent();
                            parentView.removeView(button);
                        } catch (Exception e) {
                            LogHandler.saveLog(
                                    "Failed to handle ui after signout : "
                                            + e.getLocalizedMessage(), true
                            );
                        }
                    } else {
                        try {
                            button.setText(buttonText);
                        } catch (Exception e) {
                            LogHandler.saveLog(
                                    "Failed to handle ui when signout : "
                                            + e.getLocalizedMessage(), true
                            );
                        }
                    }
                });
            }
        });
        uiThread.start();
        LogHandler.saveLog("Finished Ui Thread For Sign Out Thread", false);
    }

    private static PopupMenu setPopUpMenuOnButton(Activity activity, Button button){
        PopupMenu popupMenu = new PopupMenu(activity.getApplicationContext(), button, Gravity.CENTER);
        popupMenu.getMenuInflater().inflate(R.menu.account_button_menu, popupMenu.getMenu());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupMenu.setGravity(Gravity.CENTER);
        }
        return popupMenu;
    }

    public static void displayLinkProfileDialog(String userEmail){
        JsonObject profileMapContent = Profile.readProfileMapContent(userEmail);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
        builder.setTitle("Link Profile");
        builder.setMessage("We found linked accounts to " + userEmail + ". " +
                        "Do you want to add linked accounts to current profile ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // should be thread
                        DBHelper.insertBackupFromProfileMap(profileMapContent.get("backupAccounts").getAsJsonArray());
                        DBHelper.insertPrimaryFromProfileMap(profileMapContent.get("primaryAccounts").getAsJsonArray());
                        reInitializeButtons(MainActivity.activity, MainActivity.googleCloud);
                        dialog.dismiss();
                }}).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Thread detachThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    Profile.detachAccount(profileMapContent,userEmail);
                                    dialog.dismiss();
                                }catch (Exception e){
                                    LogHandler.saveLog("Failed to join and run sign in to backUp thread : " + e.getLocalizedMessage(), true);
                                }
                            }
                        });
                        detachThread.start();
                        try {
                            detachThread.join();
                        }catch (Exception e){
                            LogHandler.saveLog("Failed to join sign in to backUp thread : " + e.getLocalizedMessage(), true);
                        }

                    }});
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static void reInitializeButtons(Activity activity,GoogleCloud googleCloud){
//            LinearLayout primaryLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
//            for (int i = 0; i < primaryLinearLayout.getChildCount(); i++) {
//                View child = primaryLinearLayout.getChildAt(i);
//                if (child instanceof Button) {
//                    primaryLinearLayout.removeView(child);
//                    i--;
//                }
//            }

        LinearLayout backupLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
        for (int i = 0; i < backupLinearLayout.getChildCount(); i++) {
            View child = backupLinearLayout.getChildAt(i);
            if (child instanceof Button) {
                backupLinearLayout.removeView(child);
                i--;
            }
        }

        UIHandler.initializeButtons(activity,googleCloud);

//            Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryLinearLayout);
//            newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        Button newBackupLoginButton = googleCloud.createBackUpLoginButton(backupLinearLayout);
        newBackupLoginButton.setBackgroundTintList(UIHelper.backupAccountButtonColor);
    }

}
