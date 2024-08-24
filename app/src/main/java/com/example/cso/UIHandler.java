package com.example.cso;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.api.services.drive.Drive;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class UIHandler {
    public static UIHelper uiHelper = new UIHelper();
    LiquidFillButton syncButton = MainActivity.activity.findViewById(R.id.syncButton);
    private boolean isWifiOnlyOn = false;

    public UIHandler(){
        uiHelper = new UIHelper();
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

//    public static void updateDirectoriesUsages(){
//        HashMap<String,String> dirHashMap = StorageHandler.directoryUIDisplay();
//        directoryUsages.setText("");
//        for (Map.Entry<String, String> entry : dirHashMap.entrySet()) {
//            directoryUsages.append(entry.getKey() + ": " + entry.getValue() + " GB\n");
//        }
//    }


    public void addAbackUpAccountToUI(Activity activity, boolean isBackedUp,
                                             ActivityResultLauncher<Intent> signInToBackUpLauncher, View[] child,
                                             GoogleCloud.SignInResult signInResult){
        Thread uiThread = new Thread(() -> {
                if (isBackedUp) {
                    activity.runOnUiThread(() -> {
                        try{
                            LinearLayout backupButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                            Button newBackupLoginButton = MainActivity.googleCloud.createBackUpLoginButton(backupButtonsLinearLayout);
                            newBackupLoginButton.setBackgroundResource(R.drawable.gradient_purple);

                            child[0] = backupButtonsLinearLayout.getChildAt(
                                    backupButtonsLinearLayout.getChildCount() - 2);

                            if (child[0] instanceof Button) {
                                Button bt = (Button) child[0];
                                bt.setText(signInResult.getUserEmail());
                                bt.setBackgroundResource(R.drawable.gradient_purple);
                            }

                            setupDeviceButtons();

                            updateButtonsListeners(signInToBackUpLauncher);
                        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
                    });
                }
        });

        uiThread.start();
        try{
            uiThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join add a backup account to ui thread: " + e.getLocalizedMessage(), true);
        }
    }

    public void initializeWifiOnlyButton(){
        boolean[] wifiOnlyState = {SharedPreferencesHandler.getWifiOnlySwitchState()};
        if(wifiOnlyState[0]){
            uiHelper.wifiButtonText.setTextColor(uiHelper.buttonTextColor);
        }else{
            uiHelper.wifiButtonText.setTextColor(uiHelper.buttonTransparentTextColor);
        }
        updateButtonBackground(uiHelper.wifiButton, wifiOnlyState[0]);

        uiHelper.wifiButton.setOnClickListener(view -> {
                handleWifiOnlyButtonClick();
        });
    }

    private void handleWifiOnlyButtonClick(){
        boolean currentWifiOnlyState = toggleWifiOnlyOnState();
        if(currentWifiOnlyState){
            uiHelper.wifiButtonText.setTextColor(uiHelper.buttonTextColor);
        }else{
            uiHelper.wifiButtonText.setTextColor(uiHelper.buttonTransparentTextColor);
        }
        updateButtonBackground(uiHelper.wifiButton,currentWifiOnlyState);
    }

    public void initializeSyncButton(){
        boolean[] syncState = {SharedPreferencesHandler.getSyncSwitchState()};
        boolean isServiceRunning = TimerService.isMyServiceRunning(MainActivity.activity.getApplicationContext(), TimerService.class).equals("on");
        if(syncState[0] && isServiceRunning){
            startSyncButtonAnimation();
        }else{
            SharedPreferencesHandler.setSwitchState("syncSwitchState",false,MainActivity.preferences);
        }
        updateButtonBackground(syncButton, syncState[0]);

        syncButton.setOnClickListener(view -> {
            handleSyncButtonClick();
        });
    }

    private void startSyncButtonAnimation(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            syncButton.startFillAnimation();
        }
    }

    private void handleSyncButtonClick(){
        boolean currentSyncState = toggleSyncState();
        boolean isServiceRunning = TimerService.isMyServiceRunning(MainActivity.activity.getApplicationContext(), TimerService.class).equals("on");
        if(currentSyncState){
            startSyncIfNotRunning(isServiceRunning);
        }else{
            stopSyncIfRunning(isServiceRunning);
        }
        updateButtonBackground(syncButton,currentSyncState);
    }

    private void startSyncIfNotRunning(boolean isServiceRunning){
        try{
            if(!isServiceRunning){
                Sync.startSync();

            }
            startSyncButtonAnimation();
        }catch (Exception e){}
    }

    private void stopSyncIfRunning(boolean isServiceRunning){
        try{
            if(isServiceRunning){
                Sync.stopSync();
            }
            syncButton.endFillAnimation();
        }catch (Exception e){}
    }

    private RotateAnimation createContinuousRotateAnimation() {
        RotateAnimation continuousRotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        continuousRotate.setDuration(4000);
        continuousRotate.setStartOffset(1000);
        continuousRotate.setRepeatCount(Animation.INFINITE);
        return continuousRotate;
    }

    private void updateSyncButtonRotationState(RotateAnimation animation, boolean isSyncOn) {
        if (isSyncOn) {
            uiHelper.syncButtonText.startAnimation(animation);
        } else {
            uiHelper.syncButtonText.clearAnimation();
        }
    }

    private boolean toggleSyncState() {
        try{
            boolean state = SharedPreferencesHandler.getSyncSwitchState();
            SharedPreferencesHandler.setSwitchState("syncSwitchState",!state,MainActivity.preferences);
            return !state;
        }catch (Exception e){}
        return false;
    }

    private boolean toggleWifiOnlyOnState() {
        boolean previousState = SharedPreferencesHandler.getWifiOnlySwitchState();
        SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",!previousState,MainActivity.preferences);
        return !previousState;
    }


    private void updateButtonBackground(View button, Boolean state) {
        try{
            TextView textView;
            if (button.getId() == R.id.syncButton){
                textView = uiHelper.syncButtonText;
            }else{
                textView = uiHelper.wifiButtonText;
            }
            int backgroundResource;
            int textColor;
            if (state){
                backgroundResource = R.drawable.circular_button_on;
                textColor = uiHelper.buttonTextColor;
            }else{
                backgroundResource = R.drawable.circular_button_off;
                textColor = uiHelper.buttonTransparentTextColor;
            }
            textView.setTextColor(textColor);
            button.setBackgroundResource(backgroundResource);
        }catch (Exception e){}
    }

    public void initializeDrawerLayout(){
        System.out.println(MainActivity.activity);
        setupDrawerToggle();
        setMenuItems();
        setupInfoButton();
    }

    private static void setupDrawerToggle(){
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                MainActivity.activity, uiHelper.drawerLayout, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        uiHelper.drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    private static void setMenuItems() {
        try{
            PackageInfo pInfo = MainActivity.activity.getApplicationContext()
                    .getPackageManager().getPackageInfo(MainActivity.activity.getApplicationContext()
                            .getPackageName(), 0);
            setMenuItemTitle(R.id.navMenuItem1, "Version: " + pInfo.versionName);
            setMenuItemTitle(R.id.navMenuItem2, "Device id: " + MainActivity.androidUniqueDeviceIdentifier);
        }catch (Exception e) {
        }

    }

    private static void setMenuItemTitle(int menuItemId, String text) {
        MenuItem menuItem = uiHelper.navigationView.getMenu().findItem(menuItemId);
        SpannableString centeredText = new SpannableString(text);
        centeredText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        centeredText.setSpan(new ForegroundColorSpan(Color.parseColor("#202124")),
                0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        centeredText.setSpan(new TypefaceSpan("sans-serif-light"),
                0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        menuItem.setTitle(centeredText);
    }

    private static void setupInfoButton() {
        uiHelper.infoButton.setOnClickListener(view -> {
            if (uiHelper.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                uiHelper.drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                uiHelper.drawerLayout.openDrawer(GravityCompat.END);
            }
        });
    }

    public void initializeButtons(GoogleCloud googleCloud){
        initializeAccountButtons(googleCloud);
        initializeAddAnAccountButton(googleCloud);
    }

    private void initializeAddAnAccountButton(GoogleCloud googleCloud){
        Button newBackupLoginButton = googleCloud.createBackUpLoginButton(uiHelper.backupAccountsButtonsLayout);
        newBackupLoginButton.setBackgroundResource(R.drawable.gradient_purple);
    }

    private void initializeAccountButtons(GoogleCloud googleCloud) {
        String[] columnsList = {"userEmail", "type", "refreshToken"};
        List<String[]> accountRows = DBHelper.getAccounts(columnsList);
        for (String[] accountRow : accountRows) {
            String userEmail = accountRow[0];
            String type = accountRow[1];
            if (type.equals("primary")) {
            } else if (type.equals("backup")) {
                LinearLayout backupLinearLayout = MainActivity.activity.findViewById(R.id.backUpAccountsButtons);
                Button newGoogleLoginButton = googleCloud.createBackUpLoginButton(backupLinearLayout);
                newGoogleLoginButton.setText(userEmail);
            }
        }
    }


    private static void handleStatistics(String deviceId){
        int total_Assets_count = MainActivity.dbHelper.countAssets();
        int total_android_assets = MainActivity.dbHelper.countAndroidAssetsOnThisDevice(deviceId);
        int android_synced_assets_count = MainActivity.dbHelper.countAndroidSyncedAssetsOnThisDevice(deviceId);
        int unsynced_android_assets =  total_android_assets - android_synced_assets_count;
//        int synced_assets = total_Assets_count -(MainActivity.dbHelper.countAndroidAssets() - MainActivity.dbHelper.countAndroidUnsyncedAssets());

        System.out.println("total_Assets_count : " + total_Assets_count +
                "\ntotal_android_assets : " + total_android_assets +
                "\nandroid_synced_assets_count : " +  android_synced_assets_count +
                "\nunsynced_android_assets : " + unsynced_android_assets +
//                "\nsynced_assets : " + synced_assets +
                "\nMainActivity.dbHelper.countAndroidAssets() : " + MainActivity.dbHelper.countAndroidAssets() +
                "\nMainActivity.dbHelper.countAndroidUnsyncedAssets() : " + MainActivity.dbHelper.countAndroidUnsyncedAssets()
                );
        MainActivity.dbHelper.getAndroidSyncedAssetsOnThisDevice();

        ArrayList<BarEntry> syncedEntries = new ArrayList<>();
        syncedEntries.add(new BarEntry(0f, new float[]{android_synced_assets_count}));

        ArrayList<BarEntry> unsyncedEntries = new ArrayList<>();
        unsyncedEntries.add(new BarEntry(1f, new float[]{unsynced_android_assets}));

        // Create BarDataSets for each category
        BarDataSet syncedDataSet = new BarDataSet(syncedEntries, "Synced");
        syncedDataSet.setColor(ColorTemplate.MATERIAL_COLORS[0]); // Customize color

        BarDataSet unsyncedDataSet = new BarDataSet(unsyncedEntries, "Unsynced");
        unsyncedDataSet.setColor(ColorTemplate.MATERIAL_COLORS[1]);

        BarData barData = new BarData(syncedDataSet, unsyncedDataSet);
//        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
//        dataSet.setStackLabels(new String[]{"Synced", "Unsynced"});

        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%d", (int) value);
            }
        });
        Log.d("Threads","startUpdateUIThread finished");
}

    public static void startUpdateUIThread(Activity activity){
        Log.d("Threads","startUpdateUIThread started");
        Thread updateUIThread =  new Thread(() -> {
            try{
                activity.runOnUiThread(() -> {
                    handleStatistics(MainActivity.androidUniqueDeviceIdentifier);
                });
            }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
        });
        updateUIThread.start();
    }

    public static void updateButtonsListeners(ActivityResultLauncher<Intent> signInToBackUpLauncher) {
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
                            if (MainActivity.isAnyProccessOn) {
                                return;
                            }

                            String buttonText = button.getText().toString().toLowerCase();
                            if (buttonText.equals("add a back up account")) {
                                MainActivity.isAnyProccessOn = true;
                                button.setText("signing in ...");
                                button.setClickable(false);
                                MainActivity.googleCloud.signInToGoogleCloud(signInToBackUpLauncher);
                                button.setClickable(true);
                                MainActivity.isAnyProccessOn = false;
                            } else if (buttonText.equals("signing in ...")){
                                button.setText("add a back up account");
                            } else if (buttonText.equals("signing out...")){
                                button.setText(button.getContentDescription());
                            } else {
                                button.setContentDescription(buttonText);
                                try{
                                    PopupMenu popupMenu = setPopUpMenuOnButton(activity, button,"account");
                                    popupMenu.setOnMenuItemClickListener(item -> {
                                        if (item.getItemId() == R.id.unlink) {
                                            MainActivity.isAnyProccessOn = true;
                                            button.setText("signing out...");
                                            button.setClickable(false);
                                            GoogleCloud.startUnlinkThreads(buttonText, item, button);
                                            button.setClickable(true);
                                            MainActivity.isAnyProccessOn = false;
                                        }
                                        return true;
                                    });
                                    popupMenu.show();
                                }catch (Exception e){
                                    button.setText(button.getContentDescription());
                                }

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
                                    "Failed to handle ui after sign out : "
                                            + e.getLocalizedMessage(), true
                            );
                        }
                    } else {
                        try {
                            button.setText(buttonText);
                        } catch (Exception e) {
                            LogHandler.saveLog(
                                    "Failed to handle ui when sign out : "
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

    private static PopupMenu setPopUpMenuOnButton(Activity activity, Button button, String type) {
        PopupMenu popupMenu = new PopupMenu(activity.getApplicationContext(), button, Gravity.CENTER);

        // Inflate the menu first
        popupMenu.getMenuInflater().inflate(R.menu.account_button_menu, popupMenu.getMenu());
        Menu menu = popupMenu.getMenu();

        int unlink = 0;
        int details = 1;
        int reportStolen = 2;

        // Remove items based on the type
        if (type.equals("ownDevice")) {
            menu.removeItem(menu.getItem(unlink).getItemId());
            menu.removeItem(menu.getItem(reportStolen - 1 ).getItemId());
        } else if (type.equals("account")) {
            menu.removeItem(menu.getItem(reportStolen).getItemId());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupMenu.setGravity(Gravity.CENTER);
        }

        return popupMenu;
    }


    public static void displayLinkProfileDialog(ActivityResultLauncher<Intent> signInToBackUpLauncher, View[] child,
                                                JsonObject resultJson, GoogleCloud.SignInResult signInResult){
        MainActivity.activity.runOnUiThread(() -> {
            try{
                String userEmail = signInResult.getUserEmail();
                String message = "This account is already linked with an existing profile. This action " +
                        "will link a profile to this device. If you want to add "
                        + userEmail + " alone, you have to unlink from the existing profile.";

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                builder.setTitle("Existing profile detected")
                       .setMessage(message)
                       .setPositiveButton("Proceed", (dialog, id) -> {
                            Log.d("signInToBackUpLauncher","Proceed pressed");
                            dialog.dismiss();
                            Profile profile = new Profile();
                            profile.startSignInToProfileThread(signInToBackUpLauncher,child,resultJson,signInResult);
                        }).setNegativeButton("Cancel", (dialog, id) -> {
                            Log.d("signInToBackUpLauncher","Cancel pressed");
                            dialog.dismiss();
                            UIHandler.handleSignInFailure(signInToBackUpLauncher);
                        }).setCancelable(false);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
    }

    public static boolean showMoveDriveFilesDialog(String userEmail){
        boolean[] wantToUnlink = {false};
        try {
            List<String[]> accounts = DBHelper.getAccounts(new String[]{"userEmail","totalStorage","usedStorage"});
            int totalFreeSpace = 0;
            int[] otherAccountsSize = {0};

            for (String[] account : accounts) {
                if (!account[0].equals(userEmail)){
                    int totalStorage = Integer.parseInt(account[1]);
                    int usedStorage = Integer.parseInt(account[2]);
                    totalFreeSpace += totalStorage - usedStorage;
                    otherAccountsSize[0] += 1 ;
                }
            }

            int assetsSize = GoogleDrive.getAssetsSizeOfDriveAccount(userEmail);

            System.out.println("other accounts size : " + otherAccountsSize[0]);

            String[] text = {""};
            boolean[] ableToMoveAllAssets = {false};
            if (otherAccountsSize[0] == 0){
                text[0] = "Caution : All of your assets in " + userEmail + " will be out of sync.";
            }else{
                if (totalFreeSpace < assetsSize) {
                    text[0] = "Approximately " + totalFreeSpace / 1024 + " GB out of " + assetsSize / 1024 + " GB of your assets in " + userEmail + " will be moved to other accounts." +
                            "\nWarning: Not enough space is available to move all of it. " + (assetsSize - totalFreeSpace) / 1024 + " GB of your assets will remain in " + userEmail + ".";

                } else {
                    text[0] = "All of your assets in " + userEmail + " will be moved to other accounts.";
                    ableToMoveAllAssets[0] = true;
                }
            }



            int finalTotalFreeSpace = totalFreeSpace;
            MainActivity.activity.runOnUiThread(() -> {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                    builder.setMessage(text[0]);
                    builder.setTitle("Unlink Drive Account");

                    builder.setPositiveButton("Proceed", (dialog, id) -> {
                        dialog.dismiss();
                        if (otherAccountsSize[0] == 0) {
                            String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);
                            Drive service = GoogleDrive.initializeDrive(accessToken);
                            GoogleDrive.unlinkSingleAccount(userEmail,service,ableToMoveAllAssets[0]);
                        }else{
                            GoogleDrive.moveFromSourceToDestinationAccounts(userEmail,ableToMoveAllAssets[0],(assetsSize - finalTotalFreeSpace));
                        }

                        System.out.println("finish moving files");
                    });

                    builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

                    builder.setCancelable(false);

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (Exception e) {
                    LogHandler.saveLog("Failed to show move drive files dialog : " + e.getLocalizedMessage(), true);
                }
            });

        }catch(Exception e){
            LogHandler.saveLog("Failed to calculate asset size and drives free space : " + e.getLocalizedMessage(), true);
            return false;
        }
        return wantToUnlink[0];
    }

    public static void handleSignInFailure(ActivityResultLauncher<Intent> signInToBackUpLauncher){
        MainActivity.activity.runOnUiThread(() -> {
            LinearLayout backupButtonsLinearLayout = MainActivity.activity.findViewById(R.id.backUpAccountsButtons);
            View child = backupButtonsLinearLayout.getChildAt(
                    backupButtonsLinearLayout.getChildCount() - 1);
            if(child instanceof Button){
                Button bt = (Button) child;
                bt.setText("ADD A BACK UP ACCOUNT");
            }
            UIHandler.updateButtonsListeners(signInToBackUpLauncher);
        });
    }



    //---------------------------------- //---------------------------------- Device Buttons //---------------------------------- //----------------------------------


    public void setupDeviceButtons(){
        ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromDB();
        for (DeviceHandler device : devices) {
            if (!buttonExistsInUI(device.getDeviceId())) {
                System.out.println("creating button for device " + device.getDeviceName());
                View newDeviceButtonView = createNewDeviceButtonView(MainActivity.activity, device);
                uiHelper.deviceButtons.addView(newDeviceButtonView);
            }
        }
    }

    private static boolean buttonExistsInUI(String deviceId){
        LinearLayout deviceButtons = uiHelper.deviceButtons;
        int deviceButtonsCount = deviceButtons.getChildCount();
        for(int i=0 ; i < deviceButtonsCount ; i++){
            LinearLayout deviceButtonView = (LinearLayout) deviceButtons.getChildAt(i);
            int deviceViewChildrenCount = deviceButtonView.getChildCount();
            for (int j = 0; j <= deviceViewChildrenCount; j++) {
                View deviceButtonChild = deviceButtonView.getChildAt(j);
                if (deviceButtonChild instanceof Button){
                    if(deviceButtonChild.getContentDescription().equals(deviceId)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isCurrentDevice(DeviceHandler device) {
        return device.getDeviceId().equals(MainActivity.androidUniqueDeviceIdentifier);
    } // for more data

    private static void setListenerToDeviceButtons(Button button, DeviceHandler device){
        button.setOnClickListener( view -> {
            LinearLayout detailsView = getDetailsView(button);
            if (detailsView.getVisibility() == View.VISIBLE){
                detailsView.setVisibility(View.GONE);
                return;
            }
            String type = "device";
            if (isCurrentDevice(device)){
                System.out.println("" +button.getContentDescription());
                type = "ownDevice";
            }
            PopupMenu popupMenu = setPopUpMenuOnButton(MainActivity.activity, button,type);
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.unlink) {
                    button.setText("signing out...");
                    button.setClickable(false);
//                    GoogleCloud.startUnlinkThreads(buttonText, item, button);
                    button.setClickable(true);
                }else if (item.getItemId() == R.id.details){
                    detailsView.setVisibility(View.VISIBLE);
                }
                return true;
            });
            popupMenu.show();
            });
    }

    private static Button createNewDeviceButton(Context context, DeviceHandler device) {
        Button newDeviceButton = new Button(context);
        newDeviceButton.setText(device.getDeviceName());
        newDeviceButton.setContentDescription(device.getDeviceId());
        addEffectsToDeviceButton(newDeviceButton);
        setListenerToDeviceButtons(newDeviceButton, device);
        return newDeviceButton;
    }

    private static void addEffectsToDeviceButton(Button androidDeviceButton){
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(2000);
        androidDeviceButton.startAnimation(fadeIn);
        UIHelper uiHelper = new UIHelper();
        androidDeviceButton.setBackgroundResource(uiHelper.deviceBackgroundResource);
        Drawable deviceButtonDrawable = uiHelper.deviceDrawable;
        Drawable deviceButtonMenu = uiHelper.threeDotMenuDrawable;
        
        androidDeviceButton.setCompoundDrawablesWithIntrinsicBounds
                (deviceButtonDrawable, null, null, null);

        androidDeviceButton.setTextColor(uiHelper.buttonTextColor);
        androidDeviceButton.setTextSize(12);

        androidDeviceButton.setPadding(40,0,150,0);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        MainActivity.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                200
        );
        if (!androidDeviceButton.getContentDescription().equals(MainActivity.androidUniqueDeviceIdentifier)) {
            layoutParams.topMargin = 35;
        }
        androidDeviceButton.setLayoutParams(layoutParams);
    }

    public LinearLayout createNewDeviceButtonView(Context context, DeviceHandler device) {
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        Button button = createNewDeviceButton(context, device);

        LinearLayout chartInnerLayout = createDeviceDetailsLayout(context);

        ImageView pieChartArrowDown = createArrowDownImageView(context);

        PieChart pieChart = createPieChart(context);

        TextView directoryUsages = createDirectoryUsageTextView(context);

        chartInnerLayout.addView(pieChartArrowDown);
        chartInnerLayout.addView(pieChart);
        chartInnerLayout.addView(directoryUsages);

        layout.addView(button);
        layout.addView(chartInnerLayout);

        return layout;
    }

    private static PieChart createPieChart(Context context) {
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart);
        configurePieChartData(pieChart, new JsonObject());
        configurePieChartLegend(pieChart);
        configurePieChartInteractions(pieChart);
        pieChart.invalidate();
        return pieChart;
    }

    private static void configurePieChartDimensions(PieChart pieChart) {
        pieChart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        MainActivity.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int chartHeight = (int) (displayMetrics.heightPixels * 0.25);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                displayMetrics.widthPixels, chartHeight
        );

        pieChart.setLayoutParams(layoutParams);
    }

    private static void configurePieChartData(PieChart pieChart, JsonObject storageInfo) {
        StorageHandler storageHandler = new StorageHandler();
        double freeSpace = storageHandler.getFreeSpace();
        double totalStorage = storageHandler.getTotalStorage();
        double mediaStorage = Double.parseDouble(MainActivity.dbHelper.getPhotosAndVideosStorage());
        double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) freeSpace, "Free Space(GB)"));
        entries.add(new PieEntry((float) mediaStorage, "Media(GB)"));
        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others(GB)"));

        PieDataSet dataSet = new PieDataSet(entries, null);
        int[] colors = {
                Color.parseColor("#1E88E5"),
                Color.parseColor("#64B5F6"),
                Color.parseColor("#B3E5FC")
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.parseColor("#212121"));
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setDrawHoleEnabled(false);
    }

    private static void configurePieChartLegend(PieChart pieChart) {
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);
    }

    private static void configurePieChartInteractions(PieChart pieChart) {
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                handlePieChartSelection(pieChart,(int) h.getX());
            }

            @Override
            public void onNothingSelected() {

            }
        });
    }

    private static void handlePieChartSelection(PieChart pieChart, int index) {
        PieData pieData = pieChart.getData();
        PieDataSet pieDataSet = (PieDataSet) pieData.getDataSet();
        String label = pieDataSet.getEntryForIndex(index).getLabel();

        if ("Media(GB)".equals(label)) {
//            displayDirectoryUsage();
        } else {

        }
    }

    private static LinearLayout createDeviceDetailsLayout(Context context) {
        LinearLayout chartInnerLayout = new LinearLayout(context);
        chartInnerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        chartInnerLayout.setOrientation(LinearLayout.VERTICAL);
        chartInnerLayout.setGravity(Gravity.CENTER);
        chartInnerLayout.setPadding(8, 8, 8, 8);
        chartInnerLayout.setElevation(4f);
        chartInnerLayout.setBackgroundResource(R.drawable.border_background);
        chartInnerLayout.setVisibility(View.GONE);
        return chartInnerLayout;
    }

    private static ImageView createArrowDownImageView(Context context){
        ImageView pieChartArrowDown = new ImageView(context);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(25, 70);
        arrowParams.setMargins(0, 0, -5, 0);
        pieChartArrowDown.setLayoutParams(arrowParams);
        pieChartArrowDown.setBackgroundResource(R.drawable.arrowdown);
        return pieChartArrowDown;
    }

    private static TextView createDirectoryUsageTextView(Context context){
        TextView directoryUsages = new TextView(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(3, 25, 3, 0);
        directoryUsages.setLayoutParams(textParams);
//        directoryUsages.setFontFamily(ResourcesCompat.getFont(context, R.font.sans_serif));
        directoryUsages.setGravity(Gravity.CENTER);
        directoryUsages.setTextSize(12);
        return directoryUsages;
    }

    private static void changeDeviceDetailsVisibility(View currentView){
        LinearLayout deviceButtonView = (LinearLayout) currentView.getParent();
        int deviceViewChildrenCount = deviceButtonView.getChildCount();
        for (int j = 0; j < deviceViewChildrenCount; j++) {
            View view = deviceButtonView.getChildAt(j);
            if (!(view instanceof Button)) {
                if (view.getVisibility() == View.VISIBLE) {
                    view.setVisibility(View.GONE);
                }else{
                }
                return;
            }
        }
    }

    private static LinearLayout getDetailsView(Button button){
        LinearLayout deviceButtonView = (LinearLayout) button.getParent();
        int deviceViewChildrenCount = deviceButtonView.getChildCount();
        for (int j = 0; j < deviceViewChildrenCount; j++) {
            View view = deviceButtonView.getChildAt(j);
            if (!(view instanceof Button)) {
                return (LinearLayout) view;
            }
        }
        return null;
    }

//    public static void pieChartHandler(){
//        if(uiHelper.pieChart.getVisibility() == View.VISIBLE){
//            UIHandler.configurePieChartData();
//            uiHelper.pieChart.invalidate();
//            if(uiHelper.directoryUsages.getVisibility() == View.VISIBLE){
//                UIHandler.displayDirectoryUsage();
//            }
//        }
//    }

/*
    private static void displayDirectoryUsage() {
        directoryUsages.setVisibility(View.VISIBLE);
        HashMap<String, String> dirHashMap = StorageHandler.directoryUIDisplay();
        StringBuilder usageText = new StringBuilder();

        for (Map.Entry<String, String> entry : dirHashMap.entrySet()) {
            usageText.append(String.format("%-10s: %s GB\n", entry.getKey(), entry.getValue()));
        }
        directoryUsages.setText(usageText.toString());
        directoryUsages.setTextColor(Color.parseColor("#212121"));
    }
*/


 /*
    private static void configurePieChartData(PieChart pieChart) {
        StorageHandler storageHandler = new StorageHandler();

        double freeSpace = storageHandler.getFreeSpace();
        double totalStorage = storageHandler.getTotalStorage();
        double mediaStorage = Double.parseDouble(MainActivity.dbHelper.getPhotosAndVideosStorage());
        double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) freeSpace, "Free Space(GB)"));
        entries.add(new PieEntry((float) mediaStorage, "Media(GB)"));
        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others(GB)"));

        PieDataSet dataSet = new PieDataSet(entries, null);
        int[] colors = {
                Color.parseColor("#1E88E5"),
                Color.parseColor("#64B5F6"),
                Color.parseColor("#B3E5FC")
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.parseColor("#212121"));
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setDrawHoleEnabled(false);
    }
*/

    //---------------------------------- //---------------------------------- //---------------------------------- //----------------------------------

    public static void handleDeactivatedUser(){
        try{
            MainActivity.activity.runOnUiThread(() -> Toast.makeText(MainActivity.activity.getApplicationContext(),
                    "you're deActivated, Call support", Toast.LENGTH_SHORT).show());
            MainActivity.activity.finish();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }
}
