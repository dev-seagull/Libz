package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.SharedPreferencesHandler;
import com.example.cso.Sync;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class WifiOnlyButton {

    public static int wifiOnlyButtonId ;

    public static void initializeWifiOnlyButton(Activity activity){
        SwitchMaterial wifiButton = activity.findViewById(wifiOnlyButtonId);
        boolean[] wifiOnlyState = {SharedPreferencesHandler.getWifiOnlySwitchState()};
        updateSyncAndWifiButtonBackground(wifiButton, wifiOnlyState[0]);

        wifiButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            handleWifiOnlyButtonClick(activity);
        });
    }

    public static void handleWifiOnlyButtonClick(Activity activity){
        boolean currentWifiOnlyState = toggleWifiOnlyOnState();
        updateSyncAndWifiButtonBackground(activity.findViewById(wifiOnlyButtonId),currentWifiOnlyState);
    }

    public static boolean toggleWifiOnlyOnState() {
        boolean previousState = SharedPreferencesHandler.getWifiOnlySwitchState();
        SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",!previousState, MainActivity.preferences);
        return !previousState;
    }

    public static void updateSyncAndWifiButtonBackground(View button, Boolean state) {
        try{
            SwitchMaterial switchButton = (SwitchMaterial) button;
            if (state){
                int color =  MainActivity.currentTheme.OnSyncButtonGradientStart;
                switchButton.setTrackTintList(ColorStateList.valueOf(color));
                switchButton.setThumbTintList(ColorStateList.valueOf(color));
                switchButton.setChecked(true);
//                addGradientOnToWifiButton((SwitchMaterial) button);
            }else{
                int color =  MainActivity.currentTheme.OffSyncButtonGradientStart;
                switchButton.setTrackTintList(ColorStateList.valueOf(color));
                switchButton.setThumbTintList(ColorStateList.valueOf(color));
                switchButton.setChecked(false);
//                addGradientOnToWifiButton(((SwitchMaterial) button));
            }
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);}
    }

    public static void addGradientOffToWifiButton(ImageButton actionButton){
        GradientDrawable firstLayer = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {MainActivity.currentTheme.OffSyncButtonGradientStart,
                        MainActivity.currentTheme.OffSyncButtonGradientEnd}
        );
        firstLayer.setShape(GradientDrawable.OVAL);
        firstLayer.setSize(UI.dpToPx(104), UI.dpToPx(104));
        firstLayer.setCornerRadius(UI.dpToPx(52));

//        ShapeDrawable secondLayer = new ShapeDrawable(new OvalShape());
//        secondLayer.getPaint().setColor(android.graphics.Color.parseColor("#B0BEC5"));
//        secondLayer.setPadding((int) UI.dpToPx(4), (int) UI.dpToPx(4), (int) UI.dpToPx(4), (int) UI.dpToPx(4));
//
//        GradientDrawable secondLayerStroke = new GradientDrawable();
//        secondLayerStroke.setShape(GradientDrawable.OVAL);
//        secondLayerStroke.setStroke((int) UI.dpToPx(2), android.graphics.Color.parseColor("#90A4AE"));
//        secondLayerStroke.setCornerRadius(UI.dpToPx(50));
//
//        GradientDrawable thirdLayer = new GradientDrawable(
//                GradientDrawable.Orientation.BOTTOM_TOP,
//                new int[]{android.graphics.Color.parseColor("#B0BEC5"), android.graphics.Color.parseColor("#90A4AE")}
//        );
//        thirdLayer.setShape(GradientDrawable.OVAL);
//        thirdLayer.setCornerRadius(UI.dpToPx(50));

//        Drawable[] layers = {firstLayer, secondLayerStroke, thirdLayer};
//        LayerDrawable layerDrawable = new LayerDrawable(layers);

        actionButton.setBackground(firstLayer);
    }

    public static void addGradientOnToWifiButton(SwitchMaterial actionSwitch){
        GradientDrawable firstLayer = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {MainActivity.currentTheme.OnSyncButtonGradientStart,
                        MainActivity.currentTheme.OnSyncButtonGradientEnd}
        );
        firstLayer.setShape(GradientDrawable.RECTANGLE);
        firstLayer.setCornerRadius(UI.dpToPx(20));
        actionSwitch.setBackground(firstLayer);

        if (actionSwitch.isChecked()) {
            actionSwitch.setThumbTintList(ColorStateList.valueOf(Color.GREEN));
        } else {
            actionSwitch.setThumbTintList(ColorStateList.valueOf(Color.RED));
        }
    }


    public static FrameLayout createCircularWifiButtonContainer(Context context) {
        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                UI.dpToPx(100),
                UI.dpToPx(100)
        );
        frameParams.setMargins(UI.dpToPx(8), UI.dpToPx(8), UI.dpToPx(8), UI.dpToPx(8));
        frameLayout.setLayoutParams(frameParams);

        SwitchMaterial wifiSwitch = new SwitchMaterial(context);
        wifiOnlyButtonId = View.generateViewId();
        wifiSwitch.setId(wifiOnlyButtonId);
        FrameLayout.LayoutParams switchParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        wifiSwitch.setLayoutParams(switchParams);
        wifiSwitch.setText("Wifi only");
        wifiSwitch.setTextColor(ContextCompat.getColor(context, R.color.textColor));

        frameLayout.addView(wifiSwitch);

        return frameLayout;
    }


}
