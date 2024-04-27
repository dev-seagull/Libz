package com.example.cso;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

public class Upgrade {

    public static void versionHandler(SharedPreferences preferences){
        int savedVersionCode = preferences.getInt("currentVersionCode", -1); // Default to -1 if not found
        int currentVersionCode = BuildConfig.VERSION_CODE;
        if (savedVersionCode == -1){
//            deleteProfileTableContent();
//            deleteAccountsTableContent();
        }
        else if (savedVersionCode <= currentVersionCode) {
            switch (savedVersionCode){
                case 13:
                    upgrade_13_to_14();
                    break;
                case 14:
                    upgrade_14_to_15();
                    break;
                case 15:
                    upgrade_15_to_16();
                    break;
                case 16:
                    upgrade_16_to_17();
                    break;
                case 17:
                    upgrade_17_to_18();
                case 18:
                    upgrade_18_to_19();
                case 19:
                    upgrade_19_to_20();
                default:
                    lastVersion();
            }
        } else if (savedVersionCode > currentVersionCode) {
            Toast.makeText(MainActivity.activity, "Please install last version of App", Toast.LENGTH_SHORT).show();
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("currentVersionCode", currentVersionCode);
        editor.apply();
    }


    public static void upgrade_17_to_18() {
        MainActivity.activity.runOnUiThread(() -> {
            Toast.makeText(MainActivity.activity, "you are upgraded from version 17 to version 18 by Upgrader", Toast.LENGTH_SHORT).show();
        });
        deleteDeviceTableContent();
//        deleteAccountsTableContent();
        upgrade_18_to_19();
    }


    public static void lastVersion() {
        MainActivity.activity.runOnUiThread(() -> {
            Toast.makeText(MainActivity.activity, "You are upgraded to last version", Toast.LENGTH_SHORT).show();
        });
//        deleteAccountsTableContent();
    }


    public static void upgrade_16_to_17() {
        MainActivity.activity.runOnUiThread(() -> {
            Toast.makeText(MainActivity.activity, "you are upgraded from version 16 to version 17 by Upgrader", Toast.LENGTH_SHORT).show();
            deleteDeviceTableContent();
        });
        upgrade_17_to_18();
    }

    public static void upgrade_15_to_16() {
        MainActivity.activity.runOnUiThread(() -> {
            Toast.makeText(MainActivity.activity, "you are upgraded from version 15 to version 16 by Upgrader", Toast.LENGTH_SHORT).show();
        });
        upgrade_16_to_17();
    }

    public static void upgrade_14_to_15() {
        dropProfileIdColumn();
        deleteProfileTableContent();
        upgrade_15_to_16();
    }

    public static void upgrade_13_to_14(){
        cutFromUserProfileToAccounts();
        upgrade_14_to_15();
    }

    public static void upgrade_18_to_19(){
        DBHelper.removeColumn("folderId","ACCOUNTS");
        upgrade_19_to_20();
    }

    public static void upgrade_19_to_20(){
        DBHelper.removeColumn("profileId","ACCOUNTS");
        DBHelper.dropTable("PROFILE");
    }

    public static void dropProfileIdColumn() {
        SQLiteDatabase db = MainActivity.dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            String createNewTableQuery = "CREATE TABLE IF NOT EXISTS ACCOUNTS_NEW ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "userEmail TEXT, "
                    + "type TEXT CHECK (type IN ('primary','backup')), "
                    + "refreshToken TEXT, "
                    + "accessToken TEXT, "
                    + "totalStorage REAL, "
                    + "usedStorage REAL, "
                    + "usedInDriveStorage REAL, "
                    + "UsedInGmailAndPhotosStorage REAL);";
            db.execSQL(createNewTableQuery);
            String copyDataQuery = "INSERT INTO ACCOUNTS_NEW (id, userEmail, type, refreshToken, accessToken, totalStorage, usedStorage, usedInDriveStorage, UsedInGmailAndPhotosStorage) "
                    + "SELECT id, userEmail, type, refreshToken, accessToken, totalStorage, usedStorage, usedInDriveStorage, UsedInGmailAndPhotosStorage FROM ACCOUNTS;";
            db.execSQL(copyDataQuery);
            String dropOldTableQuery = "DROP TABLE ACCOUNTS;";
            db.execSQL(dropOldTableQuery);
            String renameTableQuery = "ALTER TABLE ACCOUNTS_NEW RENAME TO ACCOUNTS;";
            db.execSQL(renameTableQuery);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to drop profileId column from accounts table: " + e.getLocalizedMessage());
        } finally {
            db.endTransaction();
        }
    }
    public static void deleteAndroidTableContent(){
        try{
            MainActivity.dbHelper.getWritableDatabase().beginTransaction();
            String deleteAndroidContent = "DELETE FROM ANDROID ;";
            MainActivity.dbHelper.getWritableDatabase().execSQL(deleteAndroidContent);
            MainActivity.dbHelper.getWritableDatabase().setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to delete Android table because in (deleteAndroidTableContent) : " + e.getLocalizedMessage());
        }finally {
            MainActivity.dbHelper.getWritableDatabase().endTransaction();
        }
    }

    public static void deleteProfileTableContent(){
        try{
            MainActivity.dbHelper.getWritableDatabase().beginTransaction();
            String deleteProfileContent = "DELETE FROM PROFILE ;";
            MainActivity.dbHelper.getWritableDatabase().execSQL(deleteProfileContent);
            MainActivity.dbHelper.getWritableDatabase().setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to delete profile table because in (deleteProfileTableContent) : " + e.getLocalizedMessage());
        }finally {
            MainActivity.dbHelper.getWritableDatabase().endTransaction();
        }
    }

    public static void deleteAccountsTableContent(){
        try{
            MainActivity.dbHelper.getWritableDatabase().beginTransaction();
            String deleteAccountsContent = "DROP TABLE IF EXISTS ACCOUNTS ;";
            MainActivity.dbHelper.getWritableDatabase().execSQL(deleteAccountsContent);
            MainActivity.dbHelper.getWritableDatabase().setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to delete Accounts table because in (deleteAccountsTableContent) : " + e.getLocalizedMessage());
        }finally {
            MainActivity.dbHelper.getWritableDatabase().endTransaction();
        }
    }

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
            }finally {
                if(cursor != null){
                    cursor.close();
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get profileId from profile  : " + e.getLocalizedMessage());
        }
        return profileId;
    }


    public static void deleteDeviceTableContent(){
        try{
            MainActivity.dbHelper.getWritableDatabase().beginTransaction();
            String deleteDeviceContent = "DELETE FROM DEVICE ;";
            MainActivity.dbHelper.getWritableDatabase().execSQL(deleteDeviceContent);
            MainActivity.dbHelper.getWritableDatabase().setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to delete Device table because in (deleteDeviceTableContent) : " + e.getLocalizedMessage());
        }finally {
            MainActivity.dbHelper.getWritableDatabase().endTransaction();
        }
    }
}
