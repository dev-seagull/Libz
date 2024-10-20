package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.cso.DBHelper;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.SharedPreferencesHandler;
import com.example.cso.TimerService;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;

public class UI{


    public static void update(String task){
        Log.d("UI.update","UI.update() called for : " + task);
        Accounts.setupAccountButtons(MainActivity.activity);
        Devices.setupDeviceButtons(MainActivity.activity);
        MainActivity.isAnyProccessOn = false; // UI.update() is called
    }

    public static void initAppUI(Activity activity){
        initPrimaryBackground(activity);
        initializeMainLayouts(activity);
        initializeDrawerLayout(activity);
        SyncButton.initializeSyncButton(activity);
        WifiOnlyButton.initializeWifiOnlyButton(activity);
        SyncDetails.handleSyncDetailsButton(activity);
        update("init App Ui");
    }

    public static void initPrimaryBackground(Activity activity){
        LinearLayout primaryBackgroundLinearLayout = activity.findViewById(R.id.primaryBackground);
        primaryBackgroundLinearLayout.removeView(activity.findViewById(ToolBar.toolbarButtonId));
        primaryBackgroundLinearLayout.addView(ToolBar.createCustomToolbar(activity),0);
        primaryBackgroundLinearLayout.setBackgroundColor(MainActivity.currentTheme.primaryBackgroundColor);
    }

    public static void initializeMainLayouts(Activity activity){
        LinearLayout mainLayout = activity.findViewById(R.id.mainLayout);
        mainLayout.addView(Devices.createParentLayoutForDeviceButtons(activity));
        mainLayout.addView(SyncButton.createVerticalLayoutForSyncButtonsAndStatistics(activity));
        mainLayout.addView(Accounts.createParentLayoutForAccountsButtons(activity));
    }

    public static void startUpdateUIThread(Activity activity){
        Log.d("Threads","startUpdateUIThread started");
        Thread updateUIThread =  new Thread(() -> {
            try{
//                boolean[] syncState = {SharedPreferencesHandler.getSyncSwitchState()};
//                boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");
                activity.runOnUiThread(() -> {
//                    if(! (syncState[0] & isServiceRunning)){
//                        TextView syncProgressText = activity.findViewById(SyncButton.warningTextViewId);
//                        syncProgressText.setText("");
//                    }
                    SyncDetails.handleSyncDetailsButton(activity);
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
            setMenuItemTitle(R.id.navMenuItemTheme,"Change Theme", activity);
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
        centeredText.setSpan(new ForegroundColorSpan(MainActivity.currentTheme.menuTextColor),
                0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        centeredText.setSpan(new TypefaceSpan("sans-serif-light"),
                0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        menuItem.setTitle(centeredText);
    }

    public static void setupInfoButton(Activity activity) {
        DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        Button infoButton = activity.findViewById(ToolBar.menuButtonId);
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
            UI.makeToast("you're deActivated, Call support");
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

    public static int dpToPx(float dp) {
        try{
            return (int) (dp * MainActivity.activity.getResources().getDisplayMetrics().density);
        }catch (Exception e) {
            LogHandler.crashLog(e,"ui");
        }
        return 0;
    }

    public static int getDeviceHeight(Context context){
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int getDeviceWidth(Context context){
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static void addGradientEffectToButton(Button button, int[] colors){
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(UI.dpToPx(25));
        gradientDrawable.setColors(colors);
        button.setBackground(gradientDrawable);
    }

    public static GradientDrawable createBorderInnerLayoutDrawable(Context context){
        GradientDrawable drawable = new GradientDrawable();

        drawable.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
        drawable.setColors(new int[] {
                MainActivity.currentTheme.deviceButtonColors[0]    ,
                MainActivity.currentTheme.deviceButtonColors[1]
        });

        int strokeWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2, context.getResources().getDisplayMetrics());
        drawable.setStroke(strokeWidth, Color.BLACK);

        // Set the corner radius
        float cornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
        drawable.setCornerRadius(cornerRadius);

        return drawable;
    }

    public static void makeToast(String message){
        MainActivity.activity.runOnUiThread(() -> {
            if(TimerService.isAppinForeGround){
                Toast.makeText(MainActivity.activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }



//    private void openThemeSelection() {
//        // Your method to handle theme selection, e.g., opening a dialog or activity
//        ThemeSelectionDialog dialog = new ThemeSelectionDialog();
//        dialog.show(getSupportFragmentManager(), "themeDialog");
//    }
}
