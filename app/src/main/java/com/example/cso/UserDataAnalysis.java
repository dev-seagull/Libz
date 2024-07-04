package com.example.cso;

import android.content.Context;

import com.jaredrummler.android.device.DeviceName;

import java.io.File;

public class UserDataAnalysis {
    public static String supportEmail = MainActivity.activity.getResources().getString(R.string.supportEmail);
    public static boolean sendUserDataAnalysisEmail(Context context) {
        String dataBasePath = context.getDatabasePath(DBHelper.NEW_DATABASE_NAME).getPath();
        File databaseFile = new File(dataBasePath);
        boolean isSent  = Support.sendEmail("This is user data analysis for : " + MainActivity.androidDeviceName + " , "
                + DeviceName.getDeviceName(),databaseFile);
        return isSent;
    }
}
