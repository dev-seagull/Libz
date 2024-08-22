package com.example.cso;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import com.github.mikephil.charting.charts.BarChart;
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
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIHandler {
    public static TextView directoryUsages = MainActivity.activity.findViewById(R.id.directoryUsages);
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

    public static void updateDirectoriesUsages(){
        HashMap<String,String> dirHashMap = StorageHandler.directoryUIDisplay();
        directoryUsages.setText("");
        for (Map.Entry<String, String> entry : dirHashMap.entrySet()) {
            directoryUsages.append(entry.getKey() + ": " + entry.getValue() + " GB\n");
        }
    }


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

    public static void pieChartHandler(){
        if(uiHelper.pieChart.getVisibility() == View.VISIBLE){
            UIHandler.configurePieChartData();
            uiHelper.pieChart.invalidate();
            if(uiHelper.directoryUsages.getVisibility() == View.VISIBLE){
                UIHandler.displayDirectoryUsage();
            }
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

    private static boolean toggleDeviceButtonClickState(){
        boolean previousState = SharedPreferencesHandler.getCurrentDeviceClickedState();
        SharedPreferencesHandler.setSwitchState("currentDeviceClicked",!previousState,MainActivity.preferences);
        return !previousState;
    }


    private static void hidePieChart(){
        uiHelper.pieChart.setVisibility(View.GONE);
        uiHelper.chartInnerLayout.setVisibility(View.GONE);
        uiHelper.pieChartArrowDown.setVisibility(View.GONE);
        uiHelper.directoryUsages.setVisibility(View.GONE);
    }

    public void setupDeviceButtons(){
        ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromDB();
        for (DeviceHandler device : devices) {
            if (!buttonExistsInUI(uiHelper.deviceButtons, device.getDeviceId())) {
                Button newDeviceButton = createNewDeviceButton(MainActivity.activity, device);
                uiHelper.deviceButtons.addView(newDeviceButton);
            }
        }
    }

    private static boolean isCurrentDevice(DeviceHandler device) {
        return device.getDeviceId().equals(MainActivity.androidUniqueDeviceIdentifier);
    }

    private static void setListenerToDeviceButtons(Button button, DeviceHandler device){
        button.setOnClickListener(
                view -> {
                    if (isCurrentDevice(device)) {
                        boolean deviceButtonClickState = toggleDeviceButtonClickState();
                        if(!deviceButtonClickState){
                            setupPieChart();
                        }else{
                            hidePieChart();
                        }
                    }else{
                        //popup menu
                    }
                }
        );
    }

    private static Button createNewDeviceButton(Activity activity, DeviceHandler device) {
        Button newDeviceButton = new Button(activity);
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
        Drawable loginButtonLeftDrawable = uiHelper.deviceDrawble;
        androidDeviceButton.setCompoundDrawablesWithIntrinsicBounds
                (loginButtonLeftDrawable, null, null, null);

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

    private static void handeSyncSwitchMaterialButton(UIHelper uiHelper, Activity activity){
        if (!TimerService.isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
//            uiHelper.syncSwitchMaterialButton.setChecked(false);
//            uiHelper.syncSwitchMaterialButton.setThumbTintList(UIHelper.offSwitchMaterialThumb);
//            uiHelper.syncSwitchMaterialButton.setTrackTintList(UIHelper.offSwitchMaterialTrack);
        }else{
//            uiHelper.syncSwitchMaterialButton.setChecked(true);
//            uiHelper.syncSwitchMaterialButton.setThumbTintList(UIHelper.onSwitchMaterialThumb);
//            uiHelper.syncSwitchMaterialButton.setTrackTintList(UIHelper.onSwitchMaterialTrack);
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

        BarChart barChart = MainActivity.activity.findViewById(R.id.barChart);
//        barChart.setVisibility(View.VISIBLE);
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
        uiHelper.pieChart.setVisibility(View.VISIBLE);
        uiHelper.chartInnerLayout.setVisibility(View.VISIBLE);
        uiHelper.pieChartArrowDown.setVisibility(View.VISIBLE);
        setupPieChartDimensions();
        configurePieChartData();
        configurePieChartLegend();
        configurePieChartInteractions();
        uiHelper.pieChart.invalidate();
    }

    private static void setupPieChartDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        MainActivity.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int chartHeight = (int) (displayMetrics.heightPixels * 0.25);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                displayMetrics.widthPixels, chartHeight
        );

        uiHelper.pieChart.setLayoutParams(layoutParams);
    }

    private static void configurePieChartData() {
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
        uiHelper.pieChart.setData(data);
        uiHelper.pieChart.getDescription().setEnabled(false);
        uiHelper.pieChart.setDrawEntryLabels(true);
        uiHelper.pieChart.setDrawHoleEnabled(true);
        uiHelper.pieChart.setDrawHoleEnabled(false);
    }

    private static void configurePieChartLegend() {
        Legend legend = uiHelper.pieChart.getLegend();
        legend.setEnabled(false);
    }

    private static void configurePieChartInteractions() {
        uiHelper.pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                handlePieChartSelection((int) h.getX());
            }

            @Override
            public void onNothingSelected() {
                directoryUsages.setVisibility(View.GONE);
            }
        });
    }

    private static void handlePieChartSelection(int index) {
        PieData pieData = uiHelper.pieChart.getData();
        PieDataSet pieDataSet = (PieDataSet) pieData.getDataSet();
        String label = pieDataSet.getEntryForIndex(index).getLabel();

        if ("Media(GB)".equals(label)) {
            displayDirectoryUsage();
        } else {
            directoryUsages.setVisibility(View.GONE);
        }
    }

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

    public static void startUpdateUIThread(Activity activity){
        LogHandler.saveLog("Starting startUpdateUIThread", false);
        Thread updateUIThread =  new Thread(() -> {
            try{
                UIHelper uiHelper = new UIHelper();
                activity.runOnUiThread(() -> {
                    handeSyncSwitchMaterialButton(uiHelper, activity);
                    handleStatistics(MainActivity.androidUniqueDeviceIdentifier);
                });
            }catch (Exception e){
                LogHandler.saveLog("Failed to run on ui thread : " + e.getLocalizedMessage() , true);
            }
        });
        updateUIThread.start();
        LogHandler.saveLog("Finished startUpdateUIThread", false);
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
                            if (MainActivity.isAnyProccessOn) {
                                return;
                            }
                            MainActivity.isAnyProccessOn = true;
                            String buttonText = button.getText().toString().toLowerCase();
                            if (buttonText.equals("add a back up account")) {
                                button.setText("signing in ...");
                                button.setClickable(false);
                                MainActivity.googleCloud.signInToGoogleCloud(signInToBackUpLauncher);
                                button.setClickable(true);
                            } else if (buttonText.equals("signing in ...")){
                                button.setText("add a back up account");
                            } else if (buttonText.equals("signing out...")){
                                button.setText(button.getContentDescription());
                            } else {
                                button.setContentDescription(buttonText);
                                try{
                                    PopupMenu popupMenu = setPopUpMenuOnButton(activity, button);
                                    popupMenu.setOnMenuItemClickListener(item -> {
                                        if (item.getItemId() == R.id.unlink) {
                                            button.setText("signing out...");
                                            button.setClickable(false);
                                            GoogleCloud.startUnlinkThreads(buttonText, item, button);
                                            button.setClickable(true);
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

    private static PopupMenu setPopUpMenuOnButton(Activity activity, Button button){
        PopupMenu popupMenu = new PopupMenu(activity.getApplicationContext(), button, Gravity.CENTER);
        popupMenu.getMenuInflater().inflate(R.menu.account_button_menu, popupMenu.getMenu());
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
            for (String[] account : accounts) {
                if (!account[0].equals(userEmail)){
                    int totalStorage = Integer.parseInt(account[1]);
                    int usedStorage = Integer.parseInt(account[2]);
                    totalFreeSpace += totalStorage - usedStorage;
                }
            }

            int assetsSize = GoogleDrive.getAssetsSizeOfDriveAccount(userEmail);

            String[] text = {""};
            boolean[] ableToMoveAllAssets = {false};
            if (totalFreeSpace < assetsSize) {
                text[0] = "Approximately " + totalFreeSpace / 1024 + " GB out of " + assetsSize / 1024 + " GB of your assets in " + userEmail + " will be moved to other accounts." +
                        "\nWarning: Not enough space is available to move all of it. " + (assetsSize - totalFreeSpace) / 1024 + " GB of your assets will remain in " + userEmail + ".";

            } else {
                text[0] = "All of your assets in " + userEmail + " will be moved to other accounts.";
                ableToMoveAllAssets[0] = true;
            }


            int finalTotalFreeSpace = totalFreeSpace;
            MainActivity.activity.runOnUiThread(() -> {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                    builder.setMessage(text[0]);
                    builder.setTitle("Unlink Drive Account");

                    builder.setPositiveButton("Proceed", (dialog, id) -> {
                        dialog.dismiss();
                        System.out.println("wantToUnlink : " + "true");
                        GoogleDrive.moveFromSourceToDestinationAccounts(userEmail,ableToMoveAllAssets[0],(assetsSize - finalTotalFreeSpace));
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
}
