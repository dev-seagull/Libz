package com.example.cso.UI;

import static com.example.cso.MainActivity.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.cso.DBHelper;
import com.example.cso.DeviceHandler;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.StorageHandler;
import com.github.mikephil.charting.charts.PieChart;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;

import java.util.ArrayList;

public class Devices {
    public static void setupDeviceButtons(Activity activity){
        LinearLayout deviceButtons = activity.findViewById(R.id.deviceButtons);
        ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromDB();
        for (DeviceHandler device : devices) {
            if (!deviceButtonExistsInUI(device.getDeviceId(), activity)) {
                Log.d("ui","creating button for device " + device.getDeviceName());
                View newDeviceButtonView = createNewDeviceButtonView(activity, device);

                deviceButtons.addView(newDeviceButtonView);
            }
        }
    }

    public static LinearLayout createNewDeviceButtonView(Context context, DeviceHandler device) {
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

        LinearLayout chartInnerLayout = Details.createDetailsLayout(context);

        PieChart pieChart = Details.createPieChartForDevice(context,device);

        TextView directoryUsages = Details.createDirectoryUsageTextView(context);

        chartInnerLayout.addView(pieChart);
        chartInnerLayout.addView(directoryUsages);

        layout.addView(buttonFrame);
        layout.addView(chartInnerLayout);
        layout.setContentDescription(device.getDeviceId());
        return layout;
    }

    public static boolean deviceButtonExistsInUI(String deviceId, Activity activity){
        LinearLayout deviceButtonsLinearLayout = activity.findViewById(R.id.deviceButtons);
        int deviceButtonsChildCount = deviceButtonsLinearLayout.getChildCount();
        for(int i=0 ; i < deviceButtonsChildCount ; i++){
            View deviceButtonsChildView = deviceButtonsLinearLayout.getChildAt(i);
            CharSequence contentDescription = deviceButtonsChildView.getContentDescription();
            if(contentDescription != null){
                if(contentDescription.toString().equalsIgnoreCase(deviceId)) {
                    Log.d("ui", deviceId+ " exists.");
                    return true;
                }
            }
        }
        Log.d("ui", deviceId+ " doesn't exists.");
        return false;
    }

    public static boolean isCurrentDevice(DeviceHandler device) {
        return device.getDeviceId().equals(MainActivity.androidUniqueDeviceIdentifier);
    } // for more data

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
        Button threeDotButton = createNewDeviceThreeDotsButton(context,device,newDeviceButton);

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
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        deviceButtonParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        newDeviceButton.setLayoutParams(deviceButtonParams);

        setListenerToDeviceButtons(newDeviceButton,device);
        return newDeviceButton;
    }

    public static Button createNewDeviceThreeDotsButton(Context context,DeviceHandler device, Button deviceButton){
        Button newThreeDotButton = new Button(context);
        newThreeDotButton.setContentDescription(device.getDeviceId() + "threeDot");
        addEffectsToThreeDotButton(newThreeDotButton);
        setListenerToDeviceThreeDotButtons(newThreeDotButton, device);

        RelativeLayout.LayoutParams threeDotButtonParams = new RelativeLayout.LayoutParams(
                112,
                112
        );
        threeDotButtonParams.addRule(RelativeLayout.ALIGN_TOP, deviceButton.getId());
        threeDotButtonParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        newThreeDotButton.setLayoutParams(threeDotButtonParams);
        newThreeDotButton.setVisibility(View.VISIBLE);
        newThreeDotButton.bringToFront();

        return newThreeDotButton;
    }

    public static void addEffectsToDeviceButton(Button androidDeviceButton, Context context){
        Drawable deviceDrawable = context.getResources().getDrawable(R.drawable.android_device_icon);
        androidDeviceButton.setCompoundDrawablesWithIntrinsicBounds
                (deviceDrawable, null, null, null);

        androidDeviceButton.setBackgroundResource(R.drawable.gradient_color_bg);
        androidDeviceButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        androidDeviceButton.setTextColor(Tools.buttonTextColor);
        androidDeviceButton.setTextSize(12);
        androidDeviceButton.setPadding(40,0,150,0);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        MainActivity.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                170
        );
        androidDeviceButton.setLayoutParams(layoutParams);
    }

    public static void addEffectsToThreeDotButton(Button threeDotButton){
        threeDotButton.setBackgroundResource(R.drawable.three_dot_white);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                112,
                112
        );

        threeDotButton.setLayoutParams(layoutParams);
    }

    public static void setListenerToDeviceThreeDotButtons(Button button, DeviceHandler device){
        button.setOnClickListener(view -> {
            String type = "device";
            if (isCurrentDevice(device)){
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

    public static void setListenerToDeviceButtons(Button button, DeviceHandler device){
        button.setOnClickListener( view -> {
            if (MainActivity.isAnyProccessOn) {// clickable false
                return;
            }
            LinearLayout detailsView = Details.getDetailsView(button);
            if (detailsView.getVisibility() == View.VISIBLE) {
                detailsView.setVisibility(View.GONE);
            } else {
                detailsView.setVisibility(View.VISIBLE);
            }

            RelativeLayout parent = (RelativeLayout) view.getParent();
            reInitializeDeviceButtonsLayout(parent,activity,device);
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

    public static JsonObject getDeviceStorageData(DeviceHandler device){
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
}
