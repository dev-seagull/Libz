package com.example.cso;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Upgrade {


    public static boolean upgradeDataBaseFrom1to2(){
        cutFromUserProfileToAccounts();
        return true;
    }

//    public static  void version_12(){
//        int version = 0;
//        if (version < 10){
//            version_11();
//        }
        // do version 12 tasks
//    }


    public static void cutFromUserProfileToAccounts(){
        try{
            MainActivity.dbHelper.getWritableDatabase().beginTransaction();
            String removeProfileEnum = "DELETE FROM userProfile WHERE type = 'profile';";
            MainActivity.dbHelper.getWritableDatabase().execSQL(removeProfileEnum);

            String copyData = "INSERT INTO accounts (profileId, userEmail, type, refreshToken, accessToken, " +
                    "totalStorage, usedStorage, usedInDriveStorage, UsedInGmailAndPhotosStorage) " +
                    "SELECT 0, userEmail, type, refreshToken, accessToken, " +
                    "totalStorage, usedStorage, usedInDriveStorage, UsedInGmailAndPhotosStorage FROM USERPROFILE;";
            MainActivity.dbHelper.getWritableDatabase().execSQL(copyData);

            String dropUserprofileTable = "DROP TABLE IF EXISTS USERPROFILE;";
            MainActivity.dbHelper.getWritableDatabase().execSQL(dropUserprofileTable);
            MainActivity.dbHelper.getWritableDatabase().setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to drop USERPROFILE : " + e.getLocalizedMessage());
        }finally {
            MainActivity.dbHelper.getWritableDatabase().endTransaction();
        }
    }

    public static String updateProfileIdsInAccounts(){
        String profileId = "";
        try {
            String getProfileIdQuery = "SELECT id FROM PROFILE limit 1;";
            Cursor cursor = DBHelper.dbReadable.rawQuery(getProfileIdQuery, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    profileId = cursor.getString(idIndex);
                }
            }
            String updateProfileIdQuery = "UPDATE ACCOUNTS SET profileId = ? ;";
            try {
                DBHelper.dbWritable.execSQL(updateProfileIdQuery, new String[]{profileId});
            } catch (SQLiteConstraintException e) {
                LogHandler.saveLog("Failed to update profileId : " + e.getLocalizedMessage());
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get profileId from profile  : " + e.getLocalizedMessage());
        }
        return profileId;
    }
}
