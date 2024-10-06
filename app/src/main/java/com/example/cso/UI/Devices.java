package com.example.cso.UI;

import static com.example.cso.MainActivity.activity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.cso.Android;
import com.example.cso.DeviceHandler;
import com.example.cso.DeviceStatusSync;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

public class Devices {

    public static int deviceButtonsId;

    public static LinearLayout createParentLayoutForDeviceButtons(Activity activity){
        LinearLayout parentLayout = new LinearLayout(activity);
        parentLayout.setOrientation(LinearLayout.VERTICAL);
        parentLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, UI.dpToPx(24),0,0);
        parentLayout.setLayoutParams(params);
        deviceButtonsId = View.generateViewId();
        parentLayout.setId(deviceButtonsId);
        return parentLayout;
    }

    public static void setupDeviceButtons(Activity activity){
        LinearLayout deviceButtons = activity.findViewById(deviceButtonsId);
        ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromDB();
        for (DeviceHandler device : devices) {
            if (!deviceButtonExistsInUI(device.getDeviceId(), activity)) {
                View newDeviceButtonView = createNewDeviceMainView(activity, device);
                MainActivity.activity.runOnUiThread(() -> deviceButtons.addView(newDeviceButtonView));
            }
        }
    }

    public static boolean deviceButtonExistsInUI(String deviceId, Activity activity){
        LinearLayout deviceButtonsLinearLayout = activity.findViewById(deviceButtonsId);
        int deviceButtonsChildCount = deviceButtonsLinearLayout.getChildCount();
        for(int i=0 ; i < deviceButtonsChildCount ; i++){
            View deviceButtonsChildView = deviceButtonsLinearLayout.getChildAt(i);
            CharSequence contentDescription = deviceButtonsChildView.getContentDescription();
            if(contentDescription != null){
                if(contentDescription.toString().equalsIgnoreCase(deviceId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static LinearLayout createNewDeviceMainView(Context context, DeviceHandler device) {
        LinearLayout layout = new LinearLayout(context);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(48,0,48,32);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        RelativeLayout buttonFrame = createNewDeviceButtonLayout(context, device);
        FrameLayout frameLayout = Details.createFrameLayoutForButtonDetails(context,"device",device.getDeviceId());

        activity.runOnUiThread(() -> {
            layout.addView(buttonFrame);
            layout.addView(frameLayout);
            layout.setContentDescription(device.getDeviceId());
        });
        return layout;
    }

    public static RelativeLayout createNewDeviceButtonLayout(Context context, DeviceHandler device) {
        RelativeLayout layout = new RelativeLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        reInitializeDeviceButtonsLayout(layout,context,device);
        return layout;
    }

    public static void reInitializeDeviceButtonsLayout(RelativeLayout layout,Context context, DeviceHandler device){
        layout.removeAllViews();
        Button newDeviceButton = createNewDeviceButton(context,device);
        Button threeDotButton = createNewDeviceThreeDotsButton(context,device.getDeviceId(),newDeviceButton);

        layout.addView(newDeviceButton);
        layout.addView(threeDotButton);
    }

    public static Button createNewDeviceButton(Context context,DeviceHandler device) {
        Button newDeviceButton = new Button(context);
        newDeviceButton.setText(device.getDeviceName());
        newDeviceButton.setContentDescription(device.getDeviceId());
        addEffectsToDeviceButton(newDeviceButton, context);
        newDeviceButton.setId(View.generateViewId());

        RelativeLayout.LayoutParams deviceButtonParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                Math.min(UI.getDeviceHeight(context) / 14,120)
        );
        deviceButtonParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        newDeviceButton.setLayoutParams(deviceButtonParams);

        setListenerToDeviceButtons(newDeviceButton,device);
        return newDeviceButton;
    }

    public static void addEffectsToDeviceButton(Button androidDeviceButton, Context context){
        Drawable deviceDrawable = MainActivity.currentTheme.deviceIcon;
        androidDeviceButton.setCompoundDrawablesWithIntrinsicBounds
                (deviceDrawable, null, null, null);

        UI.addGradientEffectToButton(androidDeviceButton,MainActivity.currentTheme.deviceButtonColors);

        androidDeviceButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        androidDeviceButton.setTextColor(MainActivity.currentTheme.primaryTextColor);
        androidDeviceButton.setTextSize(12);
        androidDeviceButton.setPadding(40,0,150,0);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        MainActivity.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UI.getDeviceHeight(context) / 18
        );
        androidDeviceButton.setLayoutParams(layoutParams);
    }

    public static void setListenerToDeviceButtons(Button button, DeviceHandler device){
        button.setOnClickListener( view -> {
            if (MainActivity.isAnyProccessOn) {// clickable false
                return;
            }
            FrameLayout detailsView = Details.getDetailsView(button);
            if (detailsView.getVisibility() == View.VISIBLE) {
                detailsView.setVisibility(View.GONE);
            } else {
                detailsView.setVisibility(View.VISIBLE);
            }

            RelativeLayout parent = (RelativeLayout) view.getParent();
            reInitializeDeviceButtonsLayout(parent,activity,device);
        });
    }

    public static Button createNewDeviceThreeDotsButton(Context context,String deviceId, Button deviceButton){
        Button newThreeDotButton = new Button(context);
        newThreeDotButton.setContentDescription(deviceId + "threeDot");
        addEffectsToThreeDotButton(newThreeDotButton,context);
        setListenerToDeviceThreeDotButtons(newThreeDotButton, deviceId);

        deviceButton.getHeight();
        int buttonSize =Math.min(UI.getDeviceHeight(context) / 20, 84);
        deviceButton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int parentHeight = deviceButton.getMeasuredHeight();
        int topMargin = (parentHeight - buttonSize) / 2 - 8;

        RelativeLayout.LayoutParams threeDotButtonParams = new RelativeLayout.LayoutParams(buttonSize, buttonSize);
        threeDotButtonParams.addRule(RelativeLayout.ALIGN_TOP, deviceButton.getId());
        threeDotButtonParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        threeDotButtonParams.topMargin = topMargin;
        threeDotButtonParams.rightMargin = 10;

        newThreeDotButton.setLayoutParams(threeDotButtonParams);
        newThreeDotButton.setVisibility(View.VISIBLE);
        newThreeDotButton.bringToFront();
        return newThreeDotButton;
    }

    public static void addEffectsToThreeDotButton(Button threeDotButton, Context context){
        threeDotButton.setBackgroundResource(MainActivity.currentTheme.threeDotButtonId);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                UI.getDeviceHeight(context) / 20,
                UI.getDeviceHeight(context) / 20
        );

        threeDotButton.setLayoutParams(layoutParams);
    }

    public static void setListenerToDeviceThreeDotButtons(Button button, String deviceId){
        button.setOnClickListener(view -> {
            String type = "device";
            if (isCurrentDevice(deviceId)){
                type = "ownDevice";
            }
            try{
                PopupMenu popupMenu = setPopUpMenuOnButton(activity, button,type);
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.unlink) {

                    }
                    return true;
                });
                popupMenu.show();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
    }

    public static PopupMenu setPopUpMenuOnButton(Activity activity, Button button, String type) {
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

    public static boolean isCurrentDevice(String deviceId) {
        return deviceId.equals(MainActivity.androidUniqueDeviceIdentifier);
    } // for more data

    public static LinearLayout createChartForStorageStatus(Context context, String deviceId) {
        LinearLayout layout = Details.createInnerDetailsLayout(context);
        JsonObject data = getStorageStatus(deviceId);
        Log.d("DeviceStatusSync", "storage data : " + data);
        View areaSquareChart = AreaSquareChart.createStorageChart(context,data);
        layout.addView(areaSquareChart);
        return layout;
    }

    public static JsonObject getStorageStatus(String deviceId) {
        FutureTask<JsonObject> futureTask = new FutureTask<>(() -> {
            if (isCurrentDevice(deviceId)) {
                return DeviceStatusSync.createStorageStatusJson();
            } else {
                JsonObject json = DeviceStatusSync.getDeviceStatusJsonFile(deviceId);
                if (json != null && !json.isJsonNull()) {
                    return json.getAsJsonObject("storageStatus");
                }
                return null;
            }
        });
        new Thread(futureTask).start();
        try {
            return futureTask.get();
        } catch (Exception e) {
            LogHandler.crashLog(e,"DeviceStatusSync");
            return null;
        }
    }

    public static LinearLayout createChartForSyncedAssetsLocationStatus(Context context, String deviceId){
        LinearLayout layout = Details.createInnerDetailsLayout(context);
        JsonObject data = getSyncedAssetsLocationStatus(deviceId);
        Log.d("DeviceStatusSync", "assets location data : " + data);
        LinearLayout treeMapChartLayout = CustomTreeMapChart.createChart(context, data);
        layout.addView(treeMapChartLayout);
        return layout;
    }

    public static JsonObject getSyncedAssetsLocationStatus(String deviceId) {
        FutureTask<JsonObject> futureTask = new FutureTask<>(() -> {
            if (isCurrentDevice(deviceId)) {
                return DeviceStatusSync.createAssetsLocationStatusJson();
            } else {
                JsonObject json = DeviceStatusSync.getDeviceStatusJsonFile(deviceId);
                if (json != null && !json.isJsonNull()) {
                    return json.getAsJsonObject("assetsLocationStatus");
                }
                return null;
            }
        });
        new Thread(futureTask).start();
        try {
            return futureTask.get();
        } catch (Exception e) {
            LogHandler.crashLog(e,"DeviceStatusSync");
            return null;
        }
    }

    public static LinearLayout createChartForSourceStatus(Context context, String deviceId){
        List<PackageInfo> packagesInfo = Android.getInstalledApps();
        for(PackageInfo packageInfo: packagesInfo){
            File file = Android.getAppDirectories(packageInfo);
            Android.getMediaSizeFromDirectory(file);
        }

        LinearLayout layout = Details.createInnerDetailsLayout(context);
        JsonObject data = getAssetsSourceStatus(deviceId);
        Log.d("DeviceStatusSync", "assets source data : " + data);
        PieChart chartLayout = Details.createPieChartForDeviceSourceStatus(context, data);
        layout.addView(chartLayout);
        return layout;
    }

    public static JsonObject getAssetsSourceStatus(String deviceId) {
        FutureTask<JsonObject> futureTask = new FutureTask<>(() -> {
            if (isCurrentDevice(deviceId)) {
                return DeviceStatusSync.createAssetsSourceStatusJson();
            } else {
                JsonObject json = DeviceStatusSync.getDeviceStatusJsonFile(deviceId);
                if (json != null && !json.isJsonNull()) {
                    return json.getAsJsonObject("assetsSourceStatus");
                }
                return null;
            }
        });
        new Thread(futureTask).start();
        try {
            return futureTask.get();
        } catch (Exception e) {
            LogHandler.crashLog(e,"DeviceStatusSync");
            return null;
        }
    }
}
