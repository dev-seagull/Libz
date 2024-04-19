package com.example.cso;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

public class UIHelper {

    public static ColorStateList backupAccountButtonColor = ColorStateList.valueOf(Color.parseColor("#42A5F5"));
    public static int buttonTextColor = Color.WHITE;

    public static Drawable driveImage = MainActivity.activity.getApplicationContext().getResources()
            .getDrawable(R.drawable.googledriveimage);
}
