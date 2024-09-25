package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
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
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class WifiOnlyButton {

    public static int wifiOnlyButtonId ;
    public static int wifiOnlyButtonTextId ;

    public static void initializeWifiOnlyButton(Activity activity){
        ImageButton wifiButton = activity.findViewById(wifiOnlyButtonId);
        boolean[] wifiOnlyState = {SharedPreferencesHandler.getWifiOnlySwitchState()};
        updateSyncAndWifiButtonBackground(wifiButton, wifiOnlyState[0]);

        wifiButton.setOnClickListener(view -> {
            handleWifiOnlyButtonClick(activity);
        });
    }

    public static void handleWifiOnlyButtonClick(Activity activity){
        ImageButton wifiButton = activity.findViewById(wifiOnlyButtonId);
        boolean currentWifiOnlyState = toggleWifiOnlyOnState();
        updateSyncAndWifiButtonBackground(wifiButton,currentWifiOnlyState);
    }

    public static boolean toggleWifiOnlyOnState() {
        boolean previousState = SharedPreferencesHandler.getWifiOnlySwitchState();
        SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",!previousState, MainActivity.preferences);
        return !previousState;
    }

    public static void updateSyncAndWifiButtonBackground(View button, Boolean state) {
        try{
            if (state){
                addGradientOnToWifiButton((ImageButton) button);
            }else{
                addGradientOffToWifiButton((ImageButton) button);
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

    public static void addGradientOnToWifiButton(ImageButton actionButton){
        GradientDrawable firstLayer = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {MainActivity.currentTheme.OnSyncButtonGradientStart,
                        MainActivity.currentTheme.OnSyncButtonGradientEnd}
        );
        firstLayer.setShape(GradientDrawable.OVAL);
        firstLayer.setSize((int) UI.dpToPx(104), (int) UI.dpToPx(104));
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


    public static FrameLayout createCircularWifiButtonContainer(Context context) {
        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                UI.dpToPx(100),
                UI.dpToPx(100)
        );
        frameParams.setMargins(UI.dpToPx(8), UI.dpToPx(8), UI.dpToPx(8), UI.dpToPx(8));
        frameLayout.setLayoutParams(frameParams);

        ImageButton wifiButton = new ImageButton(context);
        wifiOnlyButtonId = View.generateViewId();
        wifiButton.setId(wifiOnlyButtonId);
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        wifiButton.setPadding(UI.dpToPx(16), UI.dpToPx(16), UI.dpToPx(16), UI.dpToPx(16));
        wifiButton.setLayoutParams(buttonParams);

        TextView wifiText = new TextView(context);
        wifiOnlyButtonTextId = View.generateViewId();
        wifiText.setId(wifiOnlyButtonTextId);
        wifiText.setText("Wifi only");
        wifiText.setTextSize(20);
        wifiText.setTextColor(ContextCompat.getColor(context, R.color.textColor));
        wifiText.setScaleX(0.9f);
        wifiText.setLetterSpacing(0.1f);
        wifiText.setTypeface(ResourcesCompat.getFont(context, R.font.ptsansnarrowwebregular), Typeface.BOLD);
        wifiText.setPadding(UI.dpToPx(4), UI.dpToPx(4), UI.dpToPx(4), UI.dpToPx(4));
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        wifiText.setLayoutParams(textParams);

        frameLayout.addView(wifiButton);
        frameLayout.addView(wifiText);

        return frameLayout;
    }


}
