package com.example.cso;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

public class UIHelper {

    public static ColorStateList backupAccountButtonColor = ColorStateList.valueOf(Color.parseColor("#42A5F5"));
    public static ColorStateList idontknow =  ColorStateList.valueOf(Color.parseColor("#4CAF50"));

    public static ColorStateList addBackupAccountButtonColor = ColorStateList.valueOf(Color.parseColor("#FF5722"));
    public static ColorStateList primaryAccountButtonColor = ColorStateList.valueOf(Color.parseColor("#0D47A1"));

    public static ColorStateList offSwitchMaterialThumb  = ColorStateList.valueOf(Color.parseColor("#BF2C2C"));
    public static ColorStateList offSwitchMaterialTrack  = ColorStateList.valueOf(Color.parseColor("#850808"));
    //gray colors
    public static ColorStateList disabledSwitchMaterialThunm = ColorStateList.valueOf(Color.parseColor("#BDBDBD"));
    public static ColorStateList disabledSwitchMaterialTrack = ColorStateList.valueOf(Color.parseColor("#9E9E9E"));
    public static ColorStateList onSwitchMaterialThumb  = ColorStateList.valueOf(Color.GREEN);
    public static ColorStateList onSwitchMaterialTrack  = ColorStateList.valueOf(Color.GREEN);
    public static int buttonTextColor = Color.WHITE;

    public static Drawable driveImage = MainActivity.activity.getApplicationContext().getResources()
            .getDrawable(R.drawable.googledriveimage);



}
