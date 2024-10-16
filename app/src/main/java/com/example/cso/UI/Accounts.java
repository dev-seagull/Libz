package com.example.cso.UI;

import static com.example.cso.MainActivity.activity;
import static com.example.cso.MainActivity.signInToBackUpLauncher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
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

import androidx.viewpager2.widget.ViewPager2;

import com.example.cso.DBHelper;
import com.example.cso.GoogleCloud;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.github.mikephil.charting.charts.PieChart;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.internal.NonNullElementWrapperList;

import java.util.List;

public class Accounts {

    public static int accountButtonsId;

    public static LinearLayout createParentLayoutForAccountsButtons(Activity activity){
        LinearLayout parentLayout = new LinearLayout(activity);
        parentLayout.setOrientation(LinearLayout.VERTICAL);
        parentLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0,UI.dpToPx(24),0,0);
        parentLayout.setLayoutParams(params);
        accountButtonsId = View.generateViewId();
        parentLayout.setId(accountButtonsId);
        return parentLayout;
    }

    public static void setupAccountButtons(Activity activity){ // define
        activity.runOnUiThread(() -> {
            LinearLayout backupAccountsLinearLayout = activity.findViewById(accountButtonsId);
            backupAccountsLinearLayout.removeAllViews();
            String[] columnsList = {"userEmail", "type", "refreshToken"};
            List<String[]> accountRows = DBHelper.getAccounts(columnsList);
            for (String[] accountRow : accountRows) {
                String userEmail = accountRow[0];
                String type = accountRow[1];
                if (type.equals("backup")) {
                    if (accountButtonDoesNotExistsInUI(userEmail)){
                        LinearLayout newAccountButtonView = createNewAccountMainView(activity, userEmail);
                        MainActivity.activity.runOnUiThread(() ->backupAccountsLinearLayout.addView(newAccountButtonView));
                    }
                }
            }
            if (accountButtonDoesNotExistsInUI("add a back up account")){
                LinearLayout addABackupAccountButtonView = createNewAccountMainView(activity, "add a backup account");
                MainActivity.activity.runOnUiThread(() ->backupAccountsLinearLayout.addView(addABackupAccountButtonView));
            }
        });

    }

    public static boolean accountButtonDoesNotExistsInUI(String userEmail){
        LinearLayout backupButtonsLinearLayout = MainActivity.activity.findViewById(accountButtonsId);
        int backupButtonsCount = backupButtonsLinearLayout.getChildCount();
        for(int i=0 ; i < backupButtonsCount ; i++){
            View backupButtonChild = backupButtonsLinearLayout.getChildAt(i);
            CharSequence contentDescription = backupButtonChild.getContentDescription();
            if(contentDescription.toString().equalsIgnoreCase(userEmail)){
                return false;
            }
        }
        return true;
    }

    public static LinearLayout createNewAccountMainView(Activity context, String userEmail){
        LinearLayout layout = new LinearLayout(context);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(48,0,48,32);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        RelativeLayout buttonFrame = createNewAccountButtonLayout(context, userEmail);
        FrameLayout frameLayout = Details.createFrameLayoutForButtonDetails(context,"account",userEmail);

        MainActivity.activity.runOnUiThread(() -> {
            layout.addView(buttonFrame);
            layout.addView(frameLayout);
            layout.setContentDescription(userEmail);
        });
        return layout;
    }

    public static RelativeLayout createNewAccountButtonLayout(Activity context, String userEmail){
        RelativeLayout layout = new RelativeLayout(context);
        layout.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        ));

        reInitializeAccountButtonsLayout(layout,context,userEmail);
        return layout;
    }

    public static void reInitializeAccountButtonsLayout(RelativeLayout layout, Context context, String userEmail){
        layout.removeAllViews();
        Button newAccountButton = createNewAccountButton(context,userEmail);
        Button threeDotButton = createNewAccountThreeDotsButton(context,userEmail,newAccountButton);

        layout.addView(newAccountButton);
        if (!userEmail.equalsIgnoreCase("add a backup account")){
            layout.addView(threeDotButton);
        }
    }

    public static Button createNewAccountButton(Context context, String userEmail){
        Button newLoginButton = new Button(context);
        newLoginButton.setText(userEmail);
        newLoginButton.setContentDescription(userEmail);
        addEffectsToAccountButton(newLoginButton, context);
        newLoginButton.setId(View.generateViewId());

        RelativeLayout.LayoutParams accountButtonParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                Math.min(UI.getDeviceHeight(context) / 14,120)
        );
        accountButtonParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        newLoginButton.setLayoutParams(accountButtonParams);

        setListenerToAccountButton(newLoginButton,MainActivity.activity);
        return newLoginButton;
    }

    public static void addEffectsToAccountButton(Button newLoginButton, Context context){
        Drawable loginButtonLeftDrawable = context.getResources().getDrawable(R.drawable.googledriveimage);
        newLoginButton.setCompoundDrawablesWithIntrinsicBounds
                (loginButtonLeftDrawable, null, null, null);

        UI.addGradientEffectToButton(newLoginButton,MainActivity.currentTheme.accountButtonColors);

        newLoginButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        newLoginButton.setTextColor(MainActivity.currentTheme.primaryTextColor);
        newLoginButton.setTextSize(12);
        newLoginButton.setPadding(40,0,150,0);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        MainActivity.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UI.getDeviceHeight(context) / 18
        );
        newLoginButton.setLayoutParams(layoutParams);
    }

    public static void setListenerToAccountButton(Button button, Activity activity) {
        button.setOnClickListener(
            view -> {
                if (MainActivity.isAnyProccessOn) { // make clickable false
                    return;
                }
                String buttonText = button.getText().toString().toLowerCase();
                if (buttonText.equals("add a backup account")) {
                    MainActivity.isAnyProccessOn = true; // add a backup account
                    button.setText("Adding in progress ...");
                    GoogleCloud.signInToGoogleCloud(signInToBackUpLauncher, activity);
                }else{
//                    button.setText("Loading ... ");
                    FrameLayout detailsView = Details.getDetailsView(button);
                    if (detailsView.getVisibility() == View.VISIBLE){
                        detailsView.setVisibility(View.GONE);
                    } else {
                        detailsView.setVisibility(View.VISIBLE);
                    }
                    RelativeLayout parent = (RelativeLayout) view.getParent();
                    reInitializeAccountButtonsLayout(parent,activity,buttonText);
                }
            }
        );
    }

    public static Button createNewAccountThreeDotsButton(Context context,String userEmail, Button accountButton){
        Button newThreeDotButton = new Button(context);
        newThreeDotButton.setContentDescription(userEmail + "threeDot");
        addEffectsToThreeDotButton(newThreeDotButton,context);
        setListenerToAccountThreeDotButtons(newThreeDotButton, userEmail);

        accountButton.getHeight();
        int buttonSize =Math.min(UI.getDeviceHeight(context) / 20, 84);
        accountButton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int parentHeight = accountButton.getMeasuredHeight();
        int topMargin = (parentHeight - buttonSize) / 2 - 8;

        RelativeLayout.LayoutParams threeDotButtonParams = new RelativeLayout.LayoutParams(buttonSize,buttonSize);
        threeDotButtonParams.addRule(RelativeLayout.ALIGN_TOP, accountButton.getId());
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

    public static void setListenerToAccountThreeDotButtons(Button button, String userEmail) {
        button.setOnClickListener(view -> {
            try {
                PopupMenu popupMenu = setPopUpMenuOnButton(activity, (Button) view, "account");
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.unlink) {
                        MainActivity.isAnyProccessOn = true; //unlink
                        button.setText("Unlink in progress ...");
                        new Thread(() -> GoogleCloud.unlink(userEmail, activity)).start();
                    }
                    return true;
                });
                popupMenu.show();
            } catch (Exception e) {
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

    public static LinearLayout createChartForStorageStatus(Context context, String userEmail){
        LinearLayout layout = Details.createInnerDetailsLayout(context, "Storage");
        ImageView[] loadingImage = new ImageView[]{new ImageView(context)};
        new Thread(() -> {
            MainActivity.activity.runOnUiThread(() -> {
                loadingImage[0].setBackgroundResource(R.drawable.yellow_loading);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(128,128);
                params.setMargins(0,64,0,64);
                loadingImage[0].setLayoutParams(params);
                layout.addView(loadingImage[0]);
            });

            View chart = AreaSquareChartForAccount.createStorageChart(context,userEmail);

            MainActivity.activity.runOnUiThread(() -> {
                layout.addView(chart);
                layout.removeView(loadingImage[0]);

            });

        }).start();

        return layout;
    }

    public static LinearLayout createChartForSyncAndSourceStatus(Context context, String userEmail){
        LinearLayout layout = Details.createInnerDetailsLayout(context, "Devices");
        new Thread(() -> {
            ImageView[] loadingImage = new ImageView[]{new ImageView(context)};
            MainActivity.activity.runOnUiThread(() -> {
                loadingImage[0].setBackgroundResource(R.drawable.yellow_loading);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(128,128);
                params.setMargins(0,64,0,64);
                loadingImage[0].setLayoutParams(params);
                layout.addView(loadingImage[0]);
            });

            View chart = AreaSquareChartForAccount.createStorageChart(context,userEmail);

            MainActivity.activity.runOnUiThread(() -> {
                layout.addView(chart);
                layout.removeView(loadingImage[0]);

            });

        }).start();
        return layout;
    }

}
