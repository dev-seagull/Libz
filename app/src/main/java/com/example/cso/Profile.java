package com.example.cso;

import android.database.Cursor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class Profile {

    public static boolean profileExists(){
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM USERPROFILE WHERE type = 'profile')";
        Cursor cursor = MainActivity.dbHelper.dbReadable.rawQuery(sqlQuery, null);
        boolean exists = false;
        if(cursor != null && cursor.moveToFirst()){
            int result = cursor.getInt(0);
            if(result == 1){
                exists = true;
            }
        }
        cursor.close();
        return exists;
    }


    public static JsonObject createProfileMapContent(){
        List<String[]> userProfiles = DBHelper.getUserProfile(new String[]{"userEmail","type","refreshToken"});
        JsonObject userProfileJson = new JsonObject();
        JsonArray backupAccountsJson = new JsonArray();
        JsonArray primaryAccountsJson = new JsonArray();
        JsonObject backUpDBJson = new JsonObject();
        for (String[] userProfile : userProfiles){
            switch (userProfile[1]) {
                case "profile":
                    String userProfileUserName = userProfile[0];
                    userProfileJson.addProperty("userName", userProfileUserName);
                    String userProfilePassword = userProfile[2];
                    userProfileJson.addProperty("password", userProfilePassword);
                    break;
                case "backup":
                    JsonObject backupAccount = new JsonObject();
                    backupAccount.addProperty("backupEmail", userProfile[0]);
                    backupAccount.addProperty("refreshToken", userProfile[2]);
                    backupAccountsJson.add(backupAccount);
                    break;
                case "primary":
                    JsonObject primaryAccount = new JsonObject();
                    primaryAccount.addProperty("primaryEmail", userProfile[0]);
                    primaryAccount.addProperty("refreshToken", userProfile[2]);
                    primaryAccountsJson.add(primaryAccount);
                    break;
            }
        }

        String sqlQuery = "SELECT * FROM BACKUPDB";
        Cursor cursor = MainActivity.dbHelper.dbReadable.rawQuery(sqlQuery, null);
        if(cursor != null && cursor.moveToFirst()){
            int userEmailColumnIndex = cursor.getColumnIndex("userEmail");
            int fileIdColumnIndex = cursor.getColumnIndex("fileId");
            if(userEmailColumnIndex >= 0 && fileIdColumnIndex >= 0){
                String dbUserEmail = cursor.getString(userEmailColumnIndex);
                String dbFileId = cursor.getString(fileIdColumnIndex);
                backUpDBJson.addProperty("dbUserEmail", dbUserEmail);
                backUpDBJson.addProperty("dbFileId", dbFileId);
            }
        }

        JsonObject resultJson = new JsonObject();
        resultJson.add("userProfile", userProfileJson);
        resultJson.add("backupAccounts", backupAccountsJson);
        resultJson.add("primaryAccounts", primaryAccountsJson);
        cursor.close();
        return  resultJson;
    }

}
