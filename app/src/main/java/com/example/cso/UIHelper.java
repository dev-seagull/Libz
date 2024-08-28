package com.example.cso;

import android.annotation.SuppressLint;
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
    NavigationView navigationView = MainActivity.activity.findViewById(R.id.navigationView);
    AppCompatButton infoButton = MainActivity.activity.findViewById(R.id.infoButton);
    DrawerLayout drawerLayout = MainActivity.activity.findViewById(R.id.drawer_layout);
    ImageButton wifiButton = MainActivity.activity.findViewById(R.id.wifiButton);
    LinearLayout backupAccountsButtonsLayout= MainActivity.activity.findViewById(R.id.backUpAccountsButtons);
    TextView syncMessageTextView = MainActivity.activity.findViewById(R.id.syncMessageTextView);
    LinearLayout deviceButtons = MainActivity.activity.findViewById(R.id.deviceButtons);
    ColorStateList backupAccountButtonColor = ColorStateList.valueOf(Color.parseColor("#FFC300"));
    static ColorStateList primaryAccountButtonColor = ColorStateList.valueOf(Color.parseColor("#0D47A1"));
//    public static ImageView waitingSyncGif = MainActivity.activity.findViewById(R.id.waitingSyncGif);
    @SuppressLint("UseCompatLoadingForDrawables")
    public static Drawable driveImage = MainActivity.activity.getApplicationContext().getResources()
            .getDrawable(R.drawable.googledriveimage);
    @SuppressLint("UseCompatLoadingForDrawables")
    public Drawable deviceDrawable = MainActivity.activity.getApplicationContext().getResources().getDrawable(R.drawable.android_device_icon);
    @SuppressLint("UseCompatLoadingForDrawables")
    public Drawable threeDotMenuDrawable = MainActivity.activity.getApplicationContext().getResources().getDrawable(R.drawable.three_dot_menu);
    public int deviceBackgroundResource = R.drawable.gradient_color_bg;

}
