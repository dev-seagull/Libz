package com.example.cso;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class Upgrade {

    public static void versionHandler(SharedPreferences preferences){
        int savedVersionCode = preferences.getInt("currentVersionCode", -1); // Default to -1 if not found
        int currentVersionCode = BuildConfig.VERSION_CODE;
        if (savedVersionCode == -1){
            deleteProfileTableContent();
            deleteAccountsTableContent();
        }
        else if (savedVersionCode < currentVersionCode) {
            // This means this is a new install or an upgrade. Perform necessary data migration.
            if(savedVersionCode == 13) {
                upgrade_13_to_14();
                upgrade_14_to_15();
            }
            if (savedVersionCode == 14){
                upgrade_14_to_15();
            }
        } else if (savedVersionCode > currentVersionCode) {
            // popup -> please install last version of apk
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("currentVersionCode", currentVersionCode);
        editor.apply();
    }



    public static void upgrade_14_to_15() {
        dropProfileIdColumn();
        deleteProfileTableContent();
    }

    public static void upgrade_13_to_14(){
        cutFromUserProfileToAccounts();
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
            String deleteAccountsContent = "DELETE FROM ACCOUNTS ;";
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
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get profileId from profile  : " + e.getLocalizedMessage());
        }
        return profileId;
    }
}
