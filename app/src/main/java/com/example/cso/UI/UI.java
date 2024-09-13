package com.example.cso.UI;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.cso.DBHelper;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;

public class UI {

    public static void update(){
        Accounts.setupAccountButtons(MainActivity.activity);
        Devices.setupDeviceButtons(MainActivity.activity);
        MainActivity.isAnyProccessOn = false; // UI.update() is called
    }

    public static void initAppUI(Activity activity){
        initializeDrawerLayout(activity);
        SyncButton.initializeSyncButton(activity);
        WifiOnlyButton.initializeWifiOnlyButton(activity);
        SyncDetails.handleSyncDetailsButton(activity);
        update();
    }

    public static void handleStatistics(String deviceId){
        int total_Assets_count = DBHelper.countAssets();
        int total_android_assets = DBHelper.countAndroidAssetsOnThisDevice(deviceId);
        int android_synced_assets_count = DBHelper.countAndroidSyncedAssetsOnThisDevice(deviceId);
        int unsynced_android_assets =  total_android_assets - android_synced_assets_count;
        int synced_assets = total_Assets_count -(DBHelper.countAndroidAssets() - DBHelper.countAndroidUnsyncedAssets());

        System.out.println("total_Assets_count : " + total_Assets_count +
                        "\ntotal_android_assets : " + total_android_assets +
                        "\nandroid_synced_assets_count : " +  android_synced_assets_count +
                        "\nunsynced_android_assets : " + unsynced_android_assets +
//                "\nsynced_assets : " + synced_assets +
                        "\nDbhelper.countAndroidAssets() : " + DBHelper.countAndroidAssets() +
                        "\nDbhelper.countAndroidUnsyncedAssets() : " + DBHelper.countAndroidUnsyncedAssets()
        );
        DBHelper.getAndroidSyncedAssetsOnThisDevice();

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
                    SyncDetails.handleSyncDetailsButton(activity);
                    handleStatistics(MainActivity.androidUniqueDeviceIdentifier);
                });
            }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
        });
        updateUIThread.start();
    }

    public static void initializeDrawerLayout(Activity activity){
        setupDrawerToggle(activity);
        setMenuItems(activity);
        setupInfoButton(activity);
    }

    public static void setupDrawerToggle(Activity activity){
        DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                activity, drawerLayout, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    public static void setMenuItems(Activity activity) {
        try{
            PackageInfo pInfo = activity.getApplicationContext()
                    .getPackageManager().getPackageInfo(activity.getApplicationContext()
                            .getPackageName(), 0);
            setMenuItemTitle(R.id.navMenuItem1, "Version: " + pInfo.versionName, activity);
            setMenuItemTitle(R.id.navMenuItem2, "Device id: " + MainActivity.androidUniqueDeviceIdentifier, activity);
        }catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void setMenuItemTitle(int menuItemId, String text, Activity activity) {
        NavigationView navigationView = activity.findViewById(R.id.navigationView);
        MenuItem menuItem = navigationView.getMenu().findItem(menuItemId);
        SpannableString centeredText = new SpannableString(text);
        centeredText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        centeredText.setSpan(new ForegroundColorSpan(Color.parseColor("#202124")),
                0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        centeredText.setSpan(new TypefaceSpan("sans-serif-light"),
                0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        menuItem.setTitle(centeredText);
    }

    public static void setupInfoButton(Activity activity) {
        DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        Button infoButton = activity.findViewById(R.id.infoButton);
        infoButton.setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });
    }

    public static void handleDeactivatedUser(){
        try{
            MainActivity.activity.runOnUiThread(() -> Toast.makeText(MainActivity.activity.getApplicationContext(),
                    "you're deActivated, Call support", Toast.LENGTH_SHORT).show());
            MainActivity.activity.finish();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
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
//
//    public static void displayDirectoryUsage() {
//        directoryUsages.setVisibility(View.VISIBLE);
//        HashMap<String, String> dirHashMap = StorageHandler.directoryUIDisplay();
//        StringBuilder usageText = new StringBuilder();
//
//        for (Map.Entry<String, String> entry : dirHashMap.entrySet()) {
//            usageText.append(String.format("%-10s: %s GB\n", entry.getKey(), entry.getValue()));
//        }
//        directoryUsages.setText(usageText.toString());
//        directoryUsages.setTextColor(Color.parseColor("#212121"));
//    }
//
//
//    public static void configurePieChartData(PieChart pieChart) {
//        StorageHandler storageHandler = new StorageHandler();
//
//        double freeSpace = storageHandler.getFreeSpace();
//        double totalStorage = storageHandler.getTotalStorage();
//        double mediaStorage = Double.parseDouble(MainActivity.dbHelper.getPhotosAndVideosStorage());
//        double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;
//
//        ArrayList<PieEntry> entries = new ArrayList<>();
//        entries.add(new PieEntry((float) freeSpace, "Free Space(GB)"));
//        entries.add(new PieEntry((float) mediaStorage, "Media(GB)"));
//        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others(GB)"));
//
//        PieDataSet dataSet = new PieDataSet(entries, null);
//        int[] colors = {
//                Color.parseColor("#1E88E5"),
//                Color.parseColor("#64B5F6"),
//                Color.parseColor("#B3E5FC")
//        };
//        dataSet.setColors(colors);
//        dataSet.setValueTextColor(Color.parseColor("#212121"));
//        dataSet.setValueTextSize(14f);
//
//        PieData data = new PieData(dataSet);
//        pieChart.setData(data);
//        pieChart.getDescription().setEnabled(false);
//        pieChart.setDrawEntryLabels(true);
//        pieChart.setDrawHoleEnabled(true);
//        pieChart.setDrawHoleEnabled(false);
//    }


    //---------------------------------- //---------------------------------- //---------------------------------- //----------------------------------


}
