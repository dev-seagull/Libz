package com.example.cso;

import android.content.Context;

import com.jaredrummler.android.device.DeviceName;

import java.io.File;

public class UserDataAnalysis {
    public static void sendUserDataAnalysisEmail(Context context) {
        String dataBasePath = context.getDatabasePath(DBHelper.NEW_DATABASE_NAME).getPath();
        File databaseFile = new File(dataBasePath);
        Support.sendEmail("This is user data analysis for : " + MainActivity.androidUniqueDeviceIdentifier + " , "
                + MainActivity.androidDeviceName,databaseFile);
        DBHelper.backUpDataBaseToDrive(context);
    }
}
