package com.example.cso;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Looper;
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
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIHandler {
    public static TextView directoryUsages = MainActivity.activity.findViewById(R.id.directoryUsages);
    public static UIHelper uiHelper = new UIHelper();
    private boolean isWifiOnlyOn = false;
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
            if (SharedPreferencesHandler.getWifiOnlySwitchState()){
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

    public void addAbackUpAccountToUI(Activity activity, boolean isBackedUp,
                                             ActivityResultLauncher<Intent> signInToBackUpLauncher, View[] child,
                                             GoogleCloud.signInResult signInResult){
        Thread uiThread = new Thread(() -> {
            try{
                if (isBackedUp) {
                    activity.runOnUiThread(() -> {
                        LinearLayout backupButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                        Button newBackupLoginButton = MainActivity.googleCloud.createBackUpLoginButton(backupButtonsLinearLayout);
                        newBackupLoginButton.setBackgroundTintList(uiHelper.backupAccountButtonColor);
                        child[0] = backupButtonsLinearLayout.getChildAt(
                                backupButtonsLinearLayout.getChildCount() - 2);
                        LogHandler.saveLog(signInResult.getUserEmail()
                                + " has logged in to the backup account", false);

                        if (child[0] instanceof Button) {
                            Button bt = (Button) child[0];
                            bt.setText(signInResult.getUserEmail());
                            bt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D47A1")));
                        }
                        initializeDeviceButton(true);
                        updateButtonsListeners(signInToBackUpLauncher);
                    });
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to add a backup account to ui: " + e.getLocalizedMessage(), true);
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

        uiHelper.wifiButton.setOnClickListener(view -> {
            boolean isSyncOn = SharedPreferencesHandler.getSyncSwitchState();
            if(isSyncOn){
                toggleWifiOnlyOnState();
                updateButtonBackground(uiHelper.wifiButton, isWifiOnlyOn);
            }else{
                try{
                    Toast.makeText(uiHelper.activity.getApplicationContext(),
                            "First turn the sync button on!" , Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    System.out.println("Failed to make toast make in initializeWifiOnlyButton: " + e.getLocalizedMessage());
                }
            }
        });
    }

    public void initializeSyncButton(){
        RotateAnimation continuousRotate = createContinuousRotateAnimation();
        uiHelper.syncButton.setOnClickListener(view -> {
            toggleSyncState();
            boolean syncState = SharedPreferencesHandler.getSyncSwitchState();
            if(syncState){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!TimerService.isMyServiceRunning(uiHelper.activity.getApplicationContext(),TimerService.class).equals("on")){
                        uiHelper.activity.getApplicationContext().startService(MainActivity.serviceIntent);
                        updateButtonBackground(uiHelper.wifiButton, isWifiOnlyOn);
                        SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",true,MainActivity.preferences);
                    }
                }
            }else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (TimerService.isMyServiceRunning(uiHelper.activity.getApplicationContext(),TimerService.class).equals("on")){
                        uiHelper.activity.getApplicationContext().stopService(MainActivity.serviceIntent);
                    }
                }
            }

            updateSyncButtonRotationState(continuousRotate, syncState);

            updateButtonBackground(uiHelper.syncButton, syncState);
        });
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

    private void toggleSyncState() {
        boolean state = SharedPreferencesHandler.getSyncSwitchState();
        SharedPreferencesHandler.setSwitchState("syncSwitchState",!state,MainActivity.preferences);
    }

    private void toggleWifiOnlyOnState() {
        isWifiOnlyOn = !isWifiOnlyOn;
    }

    private void updateButtonBackground(ImageButton button, Boolean state) {
        int backgroundResource = state ? R.drawable.circular_button_on : R.drawable.circular_button_off;
        button.setBackgroundResource(backgroundResource);
    }

    public static void initializeDrawerLayout(Activity activity){
        setupDrawerToggle();
        setMenuItems();
        setupInfoButton();
    }

    private static void setupDrawerToggle(){
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                uiHelper.activity, uiHelper.drawerLayout, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        uiHelper.drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    private static void setMenuItems() {
        setMenuItemTitle(R.id.navMenuItem1, "Version: " + BuildConfig.VERSION_NAME);
        setMenuItemTitle(R.id.navMenuItem2, "Device id: " + MainActivity.androidUniqueDeviceIdentifier);
    }

    private static void setMenuItemTitle(int menuItemId, String text) {
        MenuItem menuItem = uiHelper.navigationView.getMenu().findItem(menuItemId);
        SpannableString centeredText = new SpannableString(text);
        centeredText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
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
        initializeSyncButton();
        initializeWifiOnlyButton();
        initializeAccountButtons(googleCloud);
        initializeDeviceButton(false);
        initializeSddAnAccountButton(googleCloud);
    }

    private void initializeSddAnAccountButton(GoogleCloud googleCloud){
//          LinearLayout primaryAccountsButtonsLayout= findViewById(R.id.primaryAccountsButtons);
//           Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryAccountsButtonsLayout);
//           newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        Button newBackupLoginButton = googleCloud.createBackUpLoginButton(uiHelper.backupAccountsButtonsLayout);
        newBackupLoginButton.setBackgroundTintList(uiHelper.backupAccountButtonColor);
    }

    private void initializeAccountButtons(GoogleCloud googleCloud) {
        String[] columnsList = {"userEmail", "type", "refreshToken"};
        List<String[]> accountRows = DBHelper.getAccounts(columnsList);
        for (String[] accountRow : accountRows) {
            String userEmail = accountRow[0];
            String type = accountRow[1];
            if (type.equals("primary")) {
                // LinearLayout primaryLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
                // Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryLinearLayout);
                // newGoogleLoginButton.setText(userEmail);
            } else if (type.equals("backup")) {
                LinearLayout backupLinearLayout = uiHelper.activity.findViewById(R.id.backUpAccountsButtons);
                Button newGoogleLoginButton = googleCloud.createBackUpLoginButton(backupLinearLayout);
                newGoogleLoginButton.setText(userEmail);
            }
        }
    }


    private static boolean buttonExistsInUI(LinearLayout deviceButtons, String deviceId){
        int deviceButtonsCount = deviceButtons.getChildCount();
        for(int i=1; i < deviceButtonsCount ; i++){
            View childView = deviceButtons.getChildAt(i);
            if(childView instanceof Button){
                if(childView.getContentDescription().equals(deviceId)){
                    return true;
                }
            }
        }
        return false;
    }

    public void initializeDeviceButton(boolean linkedDeviceButton){
        uiHelper.activity.runOnUiThread(()->{
            if(!linkedDeviceButton){
                setupAndroidDeviceButton();
            }else{
                setupLinkedDeviceButtons();
            }
        });
    }

    private void setupAndroidDeviceButton(){
        uiHelper.androidDeviceButton.setText(MainActivity.androidDeviceName);
        uiHelper.androidDeviceButton.setContentDescription(MainActivity.androidUniqueDeviceIdentifier);
        addEffectsToDeviceButton(uiHelper.androidDeviceButton);
    }

    private void setupLinkedDeviceButtons(){
        ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromDB();
        for (DeviceHandler device : devices) {
            if (!isCurrentDevice(device) && !buttonExistsInUI(uiHelper.deviceButtons, device.getDeviceId())) {
                Button newDeviceButton = createNewDeviceButton(uiHelper.activity, device);
                uiHelper.deviceButtons.addView(newDeviceButton);
            }
        }
    }

    private static boolean isCurrentDevice(DeviceHandler device) {
        return device.getDeviceId().equals(MainActivity.androidUniqueDeviceIdentifier);
    }

    private static Button createNewDeviceButton(Activity activity, DeviceHandler device) {
        Button newDeviceButton = new Button(activity);
        newDeviceButton.setText(device.getDeviceName());
        newDeviceButton.setContentDescription(device.getDeviceId());
        addEffectsToDeviceButton(newDeviceButton);
        return newDeviceButton;
    }

    private static void addEffectsToDeviceButton(Button androidDeviceButton){
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(2000);
        androidDeviceButton.startAnimation(fadeIn);
        androidDeviceButton.setPadding(40,0,150,0);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        uiHelper.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int windowWidth = displayMetrics.widthPixels;
        int buttonWidth = (int) (windowWidth * 0.6);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                buttonWidth,
                200
        );
        androidDeviceButton.setLayoutParams(layoutParams);
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
        setupPieChart();
        int total_Assets_count = MainActivity.dbHelper.countAssets();
        int total_android_assets = MainActivity.dbHelper.countAndroidAssetsOnThisDevice();
        int android_synced_assets_count = MainActivity.dbHelper.countAndroidSyncedAssetsOnThisDevice();
        int unsynced_android_assets =  total_android_assets - android_synced_assets_count;
        int synced_assets = total_Assets_count -(MainActivity.dbHelper.countAndroidAssets() - MainActivity.dbHelper.countAndroidUnsyncedAssets());

        System.out.println("total_Assets_count : " + total_Assets_count +
                "\ntotal_android_assets : " + total_android_assets +
                "\nandroid_synced_assets_count : " +  android_synced_assets_count +
                "\nunsynced_android_assets : " + unsynced_android_assets +
                "\nsynced_assets : " + synced_assets +
                "\nMainActivity.dbHelper.countAndroidAssets() : " + MainActivity.dbHelper.countAndroidAssets() +
                "\nMainActivity.dbHelper.countAndroidUnsyncedAssets() : " + MainActivity.dbHelper.countAndroidUnsyncedAssets()
                );
        MainActivity.dbHelper.getAndroidSyncedAssetsOnThisDevice();

        BarChart barChart = MainActivity.activity.findViewById(R.id.barChart);
        barChart.setVisibility(View.VISIBLE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        int windowWidth = displayMetrics.widthPixels;
        int windowHeight = displayMetrics.heightPixels;
        int chartWidth = windowWidth;
        int chartHeight = (int) (windowHeight * 0.25);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(10,200,10,200);
        barChart.setLayoutParams(layoutParams);

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

        barChart.setData(barData);

        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true); // Display values on bars
        barChart.setMaxVisibleValueCount(2);
        barChart.setPinchZoom(false);
        barChart.setHorizontalScrollBarEnabled(false);
        barChart.setVerticalScrollBarEnabled(false);
        barChart.setFitBars(true);
        barChart.getDescription().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGranularity(1f);
        xAxis.setDrawLabels(false);
        leftAxis.setDrawLabels(false);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setDrawAxisLine(false);
        rightAxis.setGranularity(1f);
        rightAxis.setDrawLabels(false);


//        barChart.setBackgroundColor(getResources().getColor(android.R.color.white)); // Set background color

        barChart.invalidate();    }

    private static void setupPieChart() {
        StorageHandler storageHandler = new StorageHandler();
        PieChart pieChart = MainActivity.activity.findViewById(R.id.pieChart);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        MainActivity.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int windowWidth = displayMetrics.widthPixels;
        int windowHeight = displayMetrics.heightPixels;
        int chartWidth = windowWidth;
        int chartHeight = (int) (windowHeight * 0.25);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                chartWidth,
                chartHeight
        );
        pieChart.setLayoutParams(layoutParams);

        double freeSpace = storageHandler.getFreeSpace();
        double totalStorage = storageHandler.getTotalStorage();
        double mediaStorage = Double.parseDouble(MainActivity.dbHelper.getPhotosAndVideosStorage());

        double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) freeSpace, "Free Space(GB)"));
        entries.add(new PieEntry((float) mediaStorage, "Media(GB)"));
        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others(GB)"));

        PieDataSet dataSet = new PieDataSet(entries, "Click on Media chart for details");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) h.getX();
                PieData pieData = pieChart.getData();
                PieDataSet pieDataSet = (PieDataSet) pieData.getDataSet();
                String label = pieDataSet.getEntryForIndex(index).getLabel();
                if (label.equals("Media(GB)")) {
                    directoryUsages.setVisibility(View.VISIBLE);
                    HashMap<String,String> dirHashMap = StorageHandler.directoryUIDisplay();
                    directoryUsages.setText("");
                    for (Map.Entry<String, String> entry : dirHashMap.entrySet()) {
                        directoryUsages.append(entry.getKey() + ": " + entry.getValue() + " GB\n");
                    }
                }else{
                    directoryUsages.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected() {
                directoryUsages.setVisibility(View.GONE);
            }
        });

        pieChart.setDrawEntryLabels(false);
        pieChart.invalidate();
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

    private void handleDeviceButtons(){

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
                            } else if (buttonText.equals("Wait")){
                                button.setText("add a back up account");
                            } else {
                                PopupMenu popupMenu = setPopUpMenuOnButton(activity, button);
                                popupMenu.setOnMenuItemClickListener(item -> {
                                    if (item.getItemId() == R.id.unlink) {
                                        try {
                                            button.setText("Wait");
                                        }catch (Exception e){}

                                        GoogleCloud.startUnlinkThreads(buttonText, item, button);
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

    private static PopupMenu setPopUpMenuOnButton(Activity activity, Button button){
        PopupMenu popupMenu = new PopupMenu(activity.getApplicationContext(), button, Gravity.CENTER);
        popupMenu.getMenuInflater().inflate(R.menu.account_button_menu, popupMenu.getMenu());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupMenu.setGravity(Gravity.CENTER);
        }
        return popupMenu;
    }

    public static void displayLinkProfileDialog(ActivityResultLauncher<Intent> signInToBackUpLauncher, View[] child,
                                                JsonObject resultJson,GoogleCloud.signInResult signInResult){
        MainActivity.activity.runOnUiThread(() -> {
            try{

                LogHandler.saveLog("@@@" + "signin result is : " +signInResult.getUserEmail() +" is handled : "+ signInResult.getHandleStatus(),false);
                String userEmail = signInResult.getUserEmail();

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                builder.setMessage(userEmail + " belongs to another profile.\nWe can add the " +
                        "corresponding profile which includes linked accounts to " + userEmail + ".\n" +
                        "If you like to add " + userEmail + " alone, you have to sign this out from the previous profile.");
                builder.setTitle("Add Profile");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    System.out.println("is display1 in main thread: "  + Looper.getMainLooper().isCurrentThread());
                }

                builder.setPositiveButton("Add", (dialog, id) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        System.out.println("is display2 in main thread: "  + Looper.getMainLooper().isCurrentThread());
                    }
                    dialog.dismiss();
                    Profile profile = new Profile();
                    profile.startSignInToProfileThread(signInToBackUpLauncher,child,resultJson,signInResult);
                });

                builder.setNegativeButton("Don't add", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }});

                builder.setCancelable(false);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }catch (Exception e){
                LogHandler.saveLog("Failed to display link profile dialog : " + e.getLocalizedMessage(), true);
            }
        });
    }

    public void reInitializeButtons(Activity activity,GoogleCloud googleCloud){
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

        initializeButtons(googleCloud);

//            Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryLinearLayout);
//            newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        Button newBackupLoginButton = googleCloud.createBackUpLoginButton(backupLinearLayout);
        newBackupLoginButton.setBackgroundTintList(uiHelper.backupAccountButtonColor);
    }

    public static boolean showMoveDriveFilesDialog(String userEmail){
        boolean[] wantToUnlink = {false};
        try {
            List<String[]> accounts = DBHelper.getAccounts(new String[]{"userEmail","totalStorage","usedStorage"});
            int totalFreeSpace = 0;
            for (String[] account : accounts) {
                if (!account[0].equals(userEmail)){
                    int totalStorage = Integer.parseInt(account[1]);
                    int usedStorage = Integer.parseInt(account[2]);
                    totalFreeSpace += totalStorage - usedStorage;
                }
            }
            int assetsSize = GoogleDrive.getAssetsSizeOfDriveAccount(userEmail);
            String[] text = {""};
            if (totalFreeSpace < assetsSize){ // bad situation
                text[0] = "We will move " + totalFreeSpace /1025 + " GB out of " + assetsSize/1024 + " GB of your assets in "+ userEmail +" to other accounts." +
                        "\nWarning : there is not enough space to move all of it. We wont be responsible for "+ (assetsSize - totalFreeSpace)/1024 + " GB of your assets that remain in "+userEmail+"." ;
            }else{
                text[0] = "We will move all your assets in "+ userEmail +" to other accounts." ;
            }

            MainActivity.activity.runOnUiThread(() -> {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                    builder.setMessage(text[0]);
                    builder.setTitle("Unlink Drive Account");



                    builder.setPositiveButton("Unlink", (dialog, id) -> {
                        dialog.dismiss();
                        System.out.println("wantToUnlink : " + "true");
                        GoogleDrive.moveFilesBetweenAccounts(userEmail);
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

}
