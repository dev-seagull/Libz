package com.example.cso.UI;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cso.BackUp;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.example.cso.SharedPreferencesHandler;
import com.example.cso.Sync;
import com.example.cso.TimerService;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class SyncButton {

    public static LiquidFillButton syncButton = MainActivity.activity.findViewById(R.id.syncButton);
    public static int warningTextViewId;
    public static void initializeSyncButton(Activity activity){

        boolean[] syncState = {SharedPreferencesHandler.getSyncSwitchState()};
        boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");
        if(syncState[0] && isServiceRunning){
            startSyncButtonAnimation(activity);
        }else{
            SharedPreferencesHandler.setSwitchState("syncSwitchState",false, MainActivity.preferences);
            syncState[0] = false;
        }
        updateSyncAndWifiButtonBackground(syncButton, syncState[0], activity);

        syncButton.setOnClickListener(view -> {
            handleSyncButtonClick(activity);
        });
    }

    public static void startSyncButtonAnimation(Activity activity){
        activity.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                syncButton.startFillAnimation();
            }
        });

    }

    public static void handleSyncButtonClick(Activity activity){
        boolean currentSyncState = toggleSyncState();
        boolean isServiceRunning = TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on");
        if(currentSyncState){
            startSyncIfNotRunning(isServiceRunning, activity);
        }else{
            TextView warningText = MainActivity.activity.findViewById(R.id.warningText);
            warningText.setText("");
            stopSyncIfRunning(isServiceRunning, activity);
        }
        updateSyncAndWifiButtonBackground(syncButton,currentSyncState, activity);
    }

    public static void startSyncIfNotRunning(boolean isServiceRunning, Activity activity){
        try{
            if(!isServiceRunning){
                Sync.startSync(activity);
            }
//            startSyncButtonAnimation(activity);
        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e);}
    }

    public static void stopSyncIfRunning(boolean isServiceRunning, Activity activity){
        try{
            if(isServiceRunning){
                Sync.stopSync();
            }
            stopSyncButtonAnimation(activity);
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
    }

    public static void stopSyncButtonAnimation(Activity activity){
        activity.runOnUiThread(() -> {
            syncButton.endFillAnimation();
        });
    }

    public static RotateAnimation createContinuousRotateAnimation() {
        RotateAnimation continuousRotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        continuousRotate.setDuration(4000);
        continuousRotate.setStartOffset(1000);
        continuousRotate.setRepeatCount(Animation.INFINITE);
        return continuousRotate;
    }

    public static void updateSyncButtonRotationState(RotateAnimation animation,
                                                      boolean isSyncOn, Activity activity) {
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
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
        return false;
    }

    public static void updateSyncAndWifiButtonBackground(View button, Boolean state, Activity activity) {
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
            if (state){
                addGradientOnToSyncButton((LiquidFillButton) button);
            }else{
                addGradientOffToSyncButton((LiquidFillButton) button);
            }
            textView.setTextColor( MainActivity.currentTheme.primaryTextColor);
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
    }

    public static void addGradientOffToSyncButton(LiquidFillButton actionButton){
        GradientDrawable firstLayer = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {MainActivity.currentTheme.OffSyncButtonGradientStart,
                MainActivity.currentTheme.OffSyncButtonGradientEnd}
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

    public static void addGradientOnToSyncButton(LiquidFillButton actionButton){
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

//                       <TextView
//    android:id="@+id/warningText"
//    android:layout_marginTop="20dp"
//    android:layout_width="match_parent"
//    android:layout_height="wrap_content"
//    android:layout_gravity="center"
//    android:gravity="center"
//    android:visibility="gone"
//    android:textColor="@color/text_red_color"/>

    public static TextView createWarningTextView(Activity activity){
        TextView warningText = new TextView(activity);
        warningTextViewId = View.generateViewId();
        warningText.setId(warningTextViewId);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, (int) UI.dpToPx(20), 0, 0);
        warningText.setGravity(Gravity.CENTER);
        warningText.setVisibility(View.GONE);
        warningText.setTextColor(MainActivity.currentTheme.warningTextColor);
        return warningText;
    }

}
