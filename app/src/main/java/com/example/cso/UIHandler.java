package com.example.cso;

import static com.example.cso.MainActivity.signInToBackUpLauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
import com.google.android.material.navigation.NavigationView;
import com.google.api.services.drive.Drive;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class UIHandler {
    private static int buttonTextColor = Color.WHITE;
    private static int buttonTransparentTextColor = Color.argb(128, 255, 255, 255);

    public static void initAppUI(Activity activity){
        initializeDrawerLayout(activity);
        setupDeviceButtons(activity);
        initializeSyncButton(activity);
        initializeWifiOnlyButton(activity);
        handleSyncDetailsButton(activity);
        setupAccountButtons(activity); // init
    }

    public static void initializeWifiOnlyButton(Activity activity){
        ImageButton wifiButton = activity.findViewById(R.id.wifiButton);
        TextView wifiButtonText = activity.findViewById(R.id.wifiButtonText);
        boolean[] wifiOnlyState = {SharedPreferencesHandler.getWifiOnlySwitchState()};
        if(wifiOnlyState[0]){
            wifiButtonText.setTextColor(buttonTextColor);
        }else{
            wifiButtonText.setTextColor(buttonTransparentTextColor);
        }
        updateSyncAndWifiButtonBackground(wifiButton, wifiOnlyState[0], activity);

        wifiButton.setOnClickListener(view -> {
                handleWifiOnlyButtonClick(activity);
        });
    }

    private static void handleWifiOnlyButtonClick(Activity activity){
        ImageButton wifiButton = activity.findViewById(R.id.wifiButton);
        TextView wifiButtonText = activity.findViewById(R.id.wifiButtonText);
        boolean currentWifiOnlyState = toggleWifiOnlyOnState();
        if(currentWifiOnlyState){
            wifiButtonText.setTextColor(buttonTextColor);
        }else{
            wifiButtonText.setTextColor(buttonTransparentTextColor);
        }
        updateSyncAndWifiButtonBackground(wifiButton,currentWifiOnlyState, activity);
    }

    public static void initializeSyncButton(Activity activity){
        LiquidFillButton syncButton = activity.findViewById(R.id.syncButton);
        boolean[] syncState = {SharedPreferencesHandler.getSyncSwitchState()};
        boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");
        if(syncState[0] && isServiceRunning){
            startSyncButtonAnimation(activity);
        }else{
            SharedPreferencesHandler.setSwitchState("syncSwitchState",false,MainActivity.preferences);
            syncState[0] = false;
        }
        updateSyncAndWifiButtonBackground(syncButton, syncState[0], activity);

        syncButton.setOnClickListener(view -> {
            handleSyncButtonClick(activity);
        });
    }

    public static void startSyncButtonAnimation(Activity activity){
        activity.runOnUiThread(() -> {
            LiquidFillButton syncButton = activity.findViewById(R.id.syncButton);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                syncButton.startFillAnimation();
            }
        });

    }

    public static void handleSyncButtonClick(Activity activity){
        LiquidFillButton syncButton = activity.findViewById(R.id.syncButton);
        boolean currentSyncState = toggleSyncState();
        boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");
        if(currentSyncState){
            startSyncIfNotRunning(isServiceRunning, activity);
        }else{
            stopSyncIfRunning(isServiceRunning, activity);
        }
        updateSyncAndWifiButtonBackground(syncButton,currentSyncState, activity);
    }

    private static void startSyncIfNotRunning(boolean isServiceRunning, Activity activity){
        try{
            if(!isServiceRunning){
                Sync.startSync(activity);
            }
//            startSyncButtonAnimation(activity);
        }catch (Exception e){}
    }

    private static void stopSyncIfRunning(boolean isServiceRunning, Activity activity){
        try{
            if(isServiceRunning){
                Sync.stopSync();
            }
            stopSyncButtonAnimation(activity);
        }catch (Exception e){}
    }

    public static void stopSyncButtonAnimation(Activity activity){
        activity.runOnUiThread(() -> {
            LiquidFillButton syncButton = activity.findViewById(R.id.syncButton);
            syncButton.endFillAnimation();
        });

    }

    private static RotateAnimation createContinuousRotateAnimation() {
        RotateAnimation continuousRotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        continuousRotate.setDuration(4000);
        continuousRotate.setStartOffset(1000);
        continuousRotate.setRepeatCount(Animation.INFINITE);
        return continuousRotate;
    }

    private static void updateSyncButtonRotationState(RotateAnimation animation, boolean isSyncOn, Activity activity) {
        TextView syncButtonText = activity.findViewById(R.id.syncButtonText);
        if (isSyncOn) {
            syncButtonText.startAnimation(animation);
        } else {
            syncButtonText.clearAnimation();
        }
    }

    public static boolean toggleSyncState() {
        try{
            boolean state = SharedPreferencesHandler.getSyncSwitchState();
            SharedPreferencesHandler.setSwitchState("syncSwitchState",!state,MainActivity.preferences);
            return !state;
        }catch (Exception e){}
        return false;
    }

    private static boolean toggleWifiOnlyOnState() {
        boolean previousState = SharedPreferencesHandler.getWifiOnlySwitchState();
        SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",!previousState,MainActivity.preferences);
        return !previousState;
    }

    private static void updateSyncAndWifiButtonBackground(View button, Boolean state, Activity activity) {
        try{
            TextView syncButtonText = activity.findViewById(R.id.syncButtonText);
            TextView wifiButtonText = activity.findViewById(R.id.wifiButtonText);
            TextView textView;
            if (button.getId() == R.id.syncButton){
                textView = syncButtonText;
            }else{
                textView = wifiButtonText;
            }
            int backgroundResource;
            int textColor;
            if (state){
                backgroundResource = R.drawable.circular_button_on;
                textColor = buttonTextColor;
            }else{
                backgroundResource = R.drawable.circular_button_off;
                textColor = buttonTransparentTextColor;
            }
            textView.setTextColor(textColor);
            button.setBackgroundResource(backgroundResource);
        }catch (Exception e){}
    }

    public static void initializeDrawerLayout(Activity activity){
        setupDrawerToggle(activity);
        setMenuItems(activity);
        setupInfoButton(activity);
    }

    private static void setupDrawerToggle(Activity activity){
        DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                activity, drawerLayout, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    private static void setMenuItems(Activity activity) {
        try{
            PackageInfo pInfo = activity.getApplicationContext()
                    .getPackageManager().getPackageInfo(activity.getApplicationContext()
                            .getPackageName(), 0);
            setMenuItemTitle(R.id.navMenuItem1, "Version: " + pInfo.versionName, activity);
            setMenuItemTitle(R.id.navMenuItem2, "Device id: " + MainActivity.androidUniqueDeviceIdentifier, activity);
        }catch (Exception e) {
        }
    }

    private static void setMenuItemTitle(int menuItemId, String text, Activity activity) {
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

    private static void setupInfoButton(Activity activity) {
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


    private static void handleStatistics(String deviceId){
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
                    handleSyncDetailsButton(activity);
                    handleStatistics(MainActivity.androidUniqueDeviceIdentifier);
                });
            }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
        });
        updateUIThread.start();
    }


    public static void displayLinkProfileDialog(JsonObject resultJson, GoogleCloud.SignInResult signInResult){
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
                            new Thread(() -> Profile.startSignInToProfileThread(resultJson,signInResult)).start();

                        })

                        .setNegativeButton("Cancel", (dialog, id) -> {
                            Log.d("signInToBackUpLauncher","Cancel pressed");

                            UIHandler.setupAccountButtons(MainActivity.activity); // cancel login
                            dialog.dismiss();
                        }).setCancelable(false);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
    }

    public static void showMoveDriveFilesDialog(String userEmail, Activity activity){
        try {
            double totalFreeSpace = Unlink.getTotalLinkedCloudsFreeSpace(userEmail);
            boolean isSingleAccountUnlink = Unlink.isSingleUnlink(userEmail);

            Log.d("Unlink", "Free storage of accounts except " + userEmail + " is " + totalFreeSpace);
            Log.d("Unlink", "Is single account unlink: " + isSingleAccountUnlink);

            int assetsSize = GoogleDrive.getAssetsSizeOfDriveAccount(userEmail);
            Log.d("Unlink", "getAssetsSizeOfDriveAccountThread finished for " + userEmail + ":" + assetsSize);

            MainActivity.activity.runOnUiThread(() -> {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                    boolean isAbleToMoveAllAssets = handleUnlinkBuilderTitleAndMessage(builder,userEmail,
                            isSingleAccountUnlink,assetsSize,totalFreeSpace);

                    builder.setPositiveButton("Proceed", (dialog, id) -> {
                        Log.d("Unlink", "Proceed pressed");
                        dialog.dismiss();
                        if (isSingleAccountUnlink) {
                            new Thread(() -> {
                                Log.d("Unlink", "Just unlink from single account ");
                                String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);
                                Drive service = GoogleDrive.initializeDrive(accessToken);
                                Log.d("Unlink", "Drive and access token : " + accessToken + service);
                                Unlink.unlinkSingleAccount(userEmail, service,false);
                            }).start();
                        }else{
                            new Thread( () -> {
                                Unlink.unlinkAccount(userEmail,isAbleToMoveAllAssets, activity);
                            }).start();
                        }
                    }).setNegativeButton("Cancel", (dialog, id) -> {
                        UIHandler.setupAccountButtons(activity); // cancel unlink
                        Log.d("Unlink", "end of unlink: canceled");
                        dialog.dismiss();
                    });

                    builder.setCancelable(false);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
            });

        }catch(Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static boolean handleUnlinkBuilderTitleAndMessage(AlertDialog.Builder builder,String userEmail,
                                                           boolean isSingleAccountUnlink, double assetsSize,
                                                           double totalFreeSpace){
        boolean isAbleToMoveAllAssets = false;
        try{
            if (isSingleAccountUnlink){
                builder.setTitle("No other available account");
                builder.setMessage("Caution : All of your assets in " + userEmail + " will be out of sync.");
            }else{
                if (totalFreeSpace < assetsSize) {
                    builder.setTitle("Not enough space");
                    builder.setMessage("Approximately " + totalFreeSpace / 1024 + " GB out of " + assetsSize / 1024 + " GB of your assets in " + userEmail + " will be moved to other available accounts." +
                            + (assetsSize - totalFreeSpace) / 1024 + " GB of your assets will be out of sync.");

                } else {
                    builder.setTitle("Unlink Backup Account");
                    builder.setMessage("All of your assets in " + userEmail + " will be moved to your other available accounts.");
                    isAbleToMoveAllAssets = true;
                }
            }
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return isAbleToMoveAllAssets;
    }


    //---------------------------------- //---------------------------------- Device Buttons //---------------------------------- //----------------------------------

    public static void setupDeviceButtons(Activity activity){
        LinearLayout deviceButtons = activity.findViewById(R.id.deviceButtons);
        ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromDB();
        for (DeviceHandler device : devices) {
            if (!buttonExistsInUI(device.getDeviceId(), activity)) {
                System.out.println("creating button for device " + device.getDeviceName());
                View newDeviceButtonView = createNewDeviceButtonView(activity, device);
                deviceButtons.addView(newDeviceButtonView);
            }
        }
    }

    private static boolean buttonExistsInUI(String deviceId, Activity activity){
        LinearLayout deviceButtons = activity.findViewById(R.id.deviceButtons);
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
            if (MainActivity.isAnyProccessOn){// clickable false
                return;
            }
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
                    MainActivity.isAnyProccessOn = true; // unlink device
//                    GoogleCloud.startUnlinkThreads(buttonText, item, button);
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
        addEffectsToDeviceButton(newDeviceButton, context);
        setListenerToDeviceButtons(newDeviceButton, device);
        return newDeviceButton;
    }

    private static void addEffectsToDeviceButton(Button androidDeviceButton, Context context){
        int deviceBackgroundResource = R.drawable.gradient_color_bg;
        Drawable deviceDrawable = context.getResources()
                .getDrawable(R.drawable.android_device_icon);
        Drawable threeDotMenuDrawable = context.getApplicationContext().getResources()
                .getDrawable(R.drawable.three_dot_menu);
        //        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
//        fadeIn.setDuration(2000);
//        androidDeviceButton.startAnimation(fadeIn);
        androidDeviceButton.setBackgroundResource(deviceBackgroundResource);
        Drawable deviceButtonDrawable = deviceDrawable;
        Drawable deviceButtonMenu = threeDotMenuDrawable;
        
        androidDeviceButton.setCompoundDrawablesWithIntrinsicBounds
                (deviceButtonDrawable, null, null, null);

        androidDeviceButton.setTextColor(buttonTextColor);
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

    public static LinearLayout createNewDeviceButtonView(Context context, DeviceHandler device) {
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

        PieChart pieChart = createPieChartForDevice(context,device);

        TextView directoryUsages = createDirectoryUsageTextView(context);

        chartInnerLayout.addView(pieChartArrowDown);
        chartInnerLayout.addView(pieChart);
        chartInnerLayout.addView(directoryUsages);

        layout.addView(button);
        layout.addView(chartInnerLayout);

        return layout;
    }

    private static PieChart createPieChartForDevice(Context context, DeviceHandler device) {
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart);
        configurePieChartDataForDevice(pieChart, device);
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

    private static void configurePieChartDataForDevice(PieChart pieChart, DeviceHandler device) {
        JsonObject storageData = getDeviceStorageData(device);
        double freeSpace = storageData.get("freeSpace").getAsDouble() * 1000;
        double mediaStorage = storageData.get("mediaStorage").getAsDouble() * 1000;
        double usedSpaceExcludingMedia = storageData.get("usedSpaceExcludingMedia").getAsDouble() * 1000;

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) freeSpace, "Free Space"));
        entries.add(new PieEntry((float) mediaStorage, "Media"));
        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others"));

        PieDataSet dataSet = new PieDataSet(entries, null);
        int[] colors = {
                Color.parseColor("#1E88E5"),
                Color.parseColor("#64B5F6"),
                Color.parseColor("#304194")
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setValueFormatter(new PieChartValueFormatter());

        // Enable value lines and set value positions
        dataSet.setDrawValues(true);
        dataSet.setValueLinePart1OffsetPercentage(80f); // Offset of the line
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueLineWidth(2f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);


        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setDrawHoleEnabled(false);
    }

    private static JsonObject getDeviceStorageData(DeviceHandler device){
        JsonObject storageData = new JsonObject();
        StorageHandler storageHandler = new StorageHandler();
        double freeSpace = storageHandler.getFreeSpace();
        double totalStorage = storageHandler.getTotalStorage();
        double mediaStorage = Double.parseDouble(DBHelper.getPhotosAndVideosStorage());
        double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;
        storageData.addProperty("freeSpace",freeSpace);
        storageData.addProperty("mediaStorage", mediaStorage);
        storageData.addProperty("usedSpaceExcludingMedia", usedSpaceExcludingMedia);
//
//        if (isCurrentDevice(device)){
//            StorageHandler storageHandler = new StorageHandler();
//            double freeSpace = storageHandler.getFreeSpace();
//            double totalStorage = storageHandler.getTotalStorage();
//            double mediaStorage = Double.parseDouble(DBHelper.getPhotosAndVideosStorage());
//            double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;
//            storageData.addProperty("freeSpace",freeSpace);
//            storageData.addProperty("mediaStorage", mediaStorage);
//            storageData.addProperty("usedSpaceExcludingMedia", usedSpaceExcludingMedia);
//
//        }else{
//            storageData = StorageSync.downloadStorageJsonFileFromAccounts(device);
//        }
        return storageData;
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

    //---------------------------------- //---------------------------------- Account Button //---------------------------------- //----------------------------------

    public static void setupAccountButtons(Activity activity){ // define
        activity.runOnUiThread(() -> {
            LinearLayout backupAccountsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
            backupAccountsLinearLayout.removeAllViews();
            String[] columnsList = {"userEmail", "type", "refreshToken"};
            List<String[]> accountRows = DBHelper.getAccounts(columnsList);
            for (String[] accountRow : accountRows) {
                String userEmail = accountRow[0];
                String type = accountRow[1];
                if (type.equals("primary")) {
                } else if (type.equals("backup")) {
                    if (!accountButtonExistsInUI(userEmail)){
                        LinearLayout newAccountButtonView = createNewAccountButtonView(activity, userEmail);
                        backupAccountsLinearLayout.addView(newAccountButtonView);
                    }
                }
            }
            // add a back up account button
            if (!accountButtonExistsInUI("add a back up account")){
                LinearLayout newAccountButtonView = createNewAccountButtonView(activity,"add a back up account");
                backupAccountsLinearLayout.addView(newAccountButtonView);
            }
            MainActivity.isAnyProccessOn = false; // setup account buttons
        });
        setupDeviceButtons(activity);
    }

    private static LinearLayout createNewAccountButtonView(Activity context, String userEmail){
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setContentDescription(userEmail);

        Button button = createBackUpAccountButton(MainActivity.activity);
        button.setText(userEmail);
        setListenerToAccountButton(button,MainActivity.activity);


        LinearLayout chartInnerLayout = createDeviceDetailsLayout(context);

        ImageView pieChartArrowDown = createArrowDownImageView(context);

        PieChart pieChart = createPieChartForAccount(context,userEmail);

        TextView directoryUsages = createDirectoryUsageTextView(context);

        chartInnerLayout.addView(pieChartArrowDown);
        chartInnerLayout.addView(pieChart);
        chartInnerLayout.addView(directoryUsages);

        layout.addView(button);
        layout.addView(chartInnerLayout);

        return layout;
    }

    private static boolean accountButtonExistsInUI(String userEmail){ // need change
        LinearLayout backupButtonsLinearLayout = MainActivity.activity.findViewById(R.id.backUpAccountsButtons);
        int backupButtonsCount = backupButtonsLinearLayout.getChildCount();
        for(int i=0 ; i < backupButtonsCount ; i++){
            View backupButtonChild = backupButtonsLinearLayout.getChildAt(i);
            if(backupButtonChild.getContentDescription().toString().equalsIgnoreCase(userEmail)){
                return true;
            }
        }
        return false;
    }

    private static PieChart createPieChartForAccount(Activity context,String userEmail){
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart);
        configurePieChartDataForAccount(pieChart, userEmail);
        configurePieChartLegend(pieChart);
        configurePieChartInteractions(pieChart);
        pieChart.invalidate();
        return pieChart;
    }

    private static void configurePieChartDataForAccount(PieChart pieChart, String userEmail) {
        String[] columns = new String[] {"totalStorage","usedStorage","userEmail","type"};
        List<String[]> account_rows = DBHelper.getAccounts(columns);
        double freeSpace =0;
        double usedStorage = 0;
        for (String[] account : account_rows){
            if (account[2].equals(userEmail) && account[3].equals("backup")){
                double totalStorage = Double.parseDouble(account[0]);
                usedStorage = Double.parseDouble(account[1]);
                freeSpace = totalStorage - usedStorage;
                break;
            }
        }
//        JsonObject storageData = getDeviceStorageData(device);
//        double freeSpace = storageData.get("freeSpace").getAsDouble();
//        double mediaStorage = storageData.get("mediaStorage").getAsDouble();
//        double usedSpaceExcludingMedia = storageData.get("usedSpaceExcludingMedia").getAsDouble();

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) freeSpace, "Free Space"));
        entries.add(new PieEntry((float) usedStorage, "Used Storage"));
//        entries.add(new PieEntry((float) usedSpaceExcludingMedia, "Others(GB)"));

        PieDataSet dataSet = new PieDataSet(entries, null);
        int[] colors = {
                Color.parseColor("#1E88E5"),
                Color.parseColor("#304194")
//                ,Color.parseColor("#B3E5FC")
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setValueFormatter(new PieChartValueFormatter());

        // Enable value lines and set value positions
        dataSet.setDrawValues(true);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setDrawHoleEnabled(false);
    }

    public static Button createBackUpAccountButton(Activity activity){
        Button newLoginButton = new Button(activity);
        addEffectsToAccountButton(newLoginButton, activity);
        return newLoginButton;
    }

    public static void addEffectsToAccountButton(Button newLoginButton, Activity activity){
        Drawable loginButtonLeftDrawable = activity.getApplicationContext().getResources()
                .getDrawable(R.drawable.googledriveimage);;
        newLoginButton.setCompoundDrawablesWithIntrinsicBounds
                (loginButtonLeftDrawable, null, null, null);
        newLoginButton.setBackgroundResource(R.drawable.gradient_purple);
        newLoginButton.setGravity(Gravity.CENTER);
        newLoginButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        newLoginButton.setVisibility(View.VISIBLE);
        newLoginButton.setPadding(40,0,150,0);
        newLoginButton.setTextSize(10);
        newLoginButton.setTextColor(buttonTextColor);
        newLoginButton.setBackgroundResource(R.drawable.gradient_purple);
        newLoginButton.setId(View.generateViewId());

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                200
        );
        layoutParams.setMargins(0,20,0,16);
        newLoginButton.setLayoutParams(layoutParams);

//        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
//        fadeIn.setDuration(1000);
//        newLoginButton.startAnimation(fadeIn);
    }

    public static void setListenerToAccountButton(Button button, Activity activity) {
        button.setOnClickListener(
                view -> {
                    if (MainActivity.isAnyProccessOn) { // make clickable false
                        return;
                    }
                    LinearLayout detailsView = getDetailsView(button);
                    if (detailsView.getVisibility() == View.VISIBLE){
                        detailsView.setVisibility(View.GONE);
                        return;
                    }
                    String buttonText = button.getText().toString().toLowerCase();
                    if (buttonText.equals("add a back up account")) {
                        MainActivity.isAnyProccessOn = true; // add a backup account
                        button.setText("signing in ...");
                        GoogleCloud.signInToGoogleCloud(signInToBackUpLauncher, activity);
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
                                    MainActivity.isAnyProccessOn = true;//unlink
                                    button.setText("signing out...");
                                    new Thread(() -> GoogleCloud.unlink(buttonText, activity)).start();
                                }else if(item.getItemId() == R.id.details){
                                    detailsView.setVisibility(View.VISIBLE);
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

    private static void handleSyncDetailsButton(Activity activity){
        double percentageOfSyncedAssets = DBHelper.getPercentageOfSyncedAssets();
        TextView syncBuuonDetailstextView = activity.findViewById(R.id.syncDetailsButtonText);
        activity.runOnUiThread( () -> {
            syncBuuonDetailstextView.setText(String.format("%d", Math.round(percentageOfSyncedAssets)));
            Typeface dseg7classic_regular = ResourcesCompat.getFont(activity,R.font.dseg7classic_regular);
            syncBuuonDetailstextView.setTypeface(dseg7classic_regular);
            syncBuuonDetailstextView.setScaleX(1.75f);
            syncBuuonDetailstextView.setScaleY(1.75f);
            syncBuuonDetailstextView.setGravity(View.TEXT_ALIGNMENT_CENTER);
        });
    }
}
