package com.example.cso;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class UIHelper {
    Activity activity = MainActivity.activity;
    final NavigationView navigationView = activity.findViewById(R.id.navigationView);
    final AppCompatButton infoButton = activity.findViewById(R.id.infoButton);
    final DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
    final ImageButton syncButton = activity.findViewById(R.id.syncButton);
    final TextView syncButtonText = activity.findViewById(R.id.syncButtonText);
    final ImageButton wifiButton = activity.findViewById(R.id.wifiButton);
    final LinearLayout backupAccountsButtonsLayout= activity.findViewById(R.id.backUpAccountsButtons);
    final TextView syncMessageTextView = activity.findViewById(R.id.syncMessageTextView);
    final LinearLayout deviceButtons = activity.findViewById(R.id.deviceButtons);
    final ColorStateList backupAccountButtonColor = ColorStateList.valueOf(Color.parseColor("#388E3C"));
    final Button androidDeviceButton = activity.findViewById(R.id.androidDeviceButton);
    final SwitchMaterial syncSwitchMaterialButton = activity.findViewById(R.id.syncSwitchMaterial);
    final PieChart pieChart = MainActivity.activity.findViewById(R.id.pieChart);
    final TextView directoryUsages = MainActivity.activity.findViewById(R.id.directoryUsages);
    public static ColorStateList primaryAccountButtonColor = ColorStateList.valueOf(Color.parseColor("#0D47A1"));
    public static ImageView waitingSyncGif = MainActivity.activity.findViewById(R.id.waitingSyncGif);
    public static ColorStateList offSwitchMaterialThumb  = ColorStateList.valueOf(Color.parseColor("#808080"));
    public static ColorStateList offSwitchMaterialTrack  = ColorStateList.valueOf(Color.parseColor("#808080"));
    public static ColorStateList onSwitchMaterialThumb  = ColorStateList.valueOf(Color.GREEN);
    public static ColorStateList onSwitchMaterialTrack  = ColorStateList.valueOf(Color.GREEN);
    final public int buttonTextColor = Color.WHITE;
    public static Drawable driveImage = MainActivity.activity.getApplicationContext().getResources()
            .getDrawable(R.drawable.googledriveimage);

}
