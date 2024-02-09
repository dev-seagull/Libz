package com.example.cso;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Profile {

    public static boolean profileMapExists(){
        boolean exists = false;
        try{
            String sqlQuery = "SELECT EXISTS(SELECT 1 FROM PROFILE)";
            Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQuery, null);
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    exists = true;
                }
            }
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to check if profile map exists : " + e.getLocalizedMessage(), true);
        }
        return exists;
    }

    public static boolean profileMapExists(String userName, String password){
        boolean exists = false;
        try{
            String sqlQuery = "SELECT EXISTS(SELECT 1 FROM PROFILE WHERE userName = ? and password = ?)";
            Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQuery, new String[]{userName,password});
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    exists = true;
                }
            }
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to check if profile map exists : " + e.getLocalizedMessage(), true);
        }
        return exists;
    }

    public static JsonObject createProfileMapContent(){
        List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","type","refreshToken"});
        JsonArray profileJson = new JsonArray();
        JsonArray backupAccountsJson = new JsonArray();
        JsonArray primaryAccountsJson = new JsonArray();
        JsonArray backUpDBJson = new JsonArray();
        JsonObject resultJson = new JsonObject();
        String profileSQLQuery = "SELECT * FROM PROFILE";
        MainActivity.dbHelper.dbReadable.rawQuery(profileSQLQuery,null);

        try{
            for (String[] account_row : account_rows){
                switch (account_row[1]) {
                    case "backup":
                        JsonObject backupAccount = new JsonObject();
                        backupAccount.addProperty("backupEmail", account_row[0]);
                        backupAccount.addProperty("refreshToken", account_row[2]);
                        backupAccountsJson.add(backupAccount);
                    case "primary":
                        JsonObject primaryAccount = new JsonObject();
                        primaryAccount.addProperty("primaryEmail", account_row[0]);
                        primaryAccount.addProperty("refreshToken", account_row[2]);
                        primaryAccountsJson.add(primaryAccount);
                }
            }

            String sqlQuery = "SELECT * FROM BACKUPDB";
            Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQuery, null);
            if(cursor != null && cursor.moveToFirst()){
                int userEmailColumnIndex = cursor.getColumnIndex("userEmail");
                int fileIdColumnIndex = cursor.getColumnIndex("fileId");
                if(userEmailColumnIndex >= 0 && fileIdColumnIndex >= 0){
                    JsonObject backUpDB = new JsonObject();
                    String dbUserEmail = cursor.getString(userEmailColumnIndex);
                    String dbFileId = cursor.getString(fileIdColumnIndex);
                    backUpDB.addProperty("dbUserEmail", dbUserEmail);
                    backUpDB.addProperty("dbFileId", dbFileId);
                    backUpDBJson.add(backUpDB);
                }
            }

            String[] selected_columns = {"userName","password","joined"};
            List<String[]> profile_rows = MainActivity.dbHelper.getProfile(selected_columns);
            for(String[] profile_row : profile_rows){
                JsonObject profile = new JsonObject();
                String userName = profile_row[0];
                String password = profile_row[1];
                profile.addProperty("userName", userName);
                profile.addProperty("password", password);
                profileJson.add(profile);
            }

            resultJson.add("profile", profileJson);
            resultJson.add("backupAccounts", backupAccountsJson);
            resultJson.add("primaryAccounts", primaryAccountsJson);
            resultJson.add("backUpDB", backUpDBJson);
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to create profile map content : " + e.getLocalizedMessage() , true);
        }
        return  resultJson;
    }


    public static JsonObject readProfileMapContent(int index_of_main_backup_account) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final int[] numbers = {index_of_main_backup_account};
        Callable<JsonObject> uploadTask = () -> {
            JsonObject result = null;
            String driveBackupAccessToken = "";

            try{
                String[] drive_backup_selected_columns = {"userEmail", "type", "accessToken"};
                List<String[]> drive_backUp_accounts = DBHelper.getAccounts(drive_backup_selected_columns);
                for (String[] drive_backUp_account : drive_backUp_accounts) {
                    if (drive_backUp_account[1].equals("backup")) {
                        numbers[0] = numbers[0] -1 ;
                        if (numbers[0] == 0){
                            driveBackupAccessToken = drive_backUp_account[2];
                            break;
                        }
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to get first back up account access token: " + e.getLocalizedMessage(), true);
            }

            Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);

            String driveFolderId = get_StashUserProfile_DriveFolderId(service);

            List<com.google.api.services.drive.model.File> files = GoogleDrive.getDriveFolderFiles(service, driveFolderId);

            boolean jsonFileExists = false;
            try{
                if(!driveFolderId.isEmpty() && driveFolderId != null){
                    if(!files.isEmpty() && files != null){
                        for (com.google.api.services.drive.model.File file : files) {
                            if ("application/json".equals(file.getMimeType()) && file.getName().startsWith("profileMap")) {

                                for (int i =0; i<3; i++){
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    service.files().get(file.getId())
                                            .executeMediaAndDownloadTo(outputStream);
                                    String jsonString = outputStream.toString();
                                    result = JsonParser.parseString(jsonString).getAsJsonObject();
                                    outputStream.close();
                                    if(result != null){
                                        jsonFileExists = true;
                                        return result;
                                    }
                                }
                                if (jsonFileExists == false) {
                                    return readProfileMapContent(numbers[0] + 1 );
                                }
                                break;
                            }
                        }
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to read json file from stash folder in drive : " + e.getLocalizedMessage(), true);
            }
            return result;
        };

        Future<JsonObject> future = null;
        JsonObject resultJson = new JsonObject();
        try{
            future = executor.submit(uploadTask);
        }catch (Exception e){
            LogHandler.saveLog("Failed to submit executor: " + e.getLocalizedMessage(), true);

        }
        try {
            resultJson = future.get();
        } catch (Exception e) {
            LogHandler.saveLog("error when downloading user profile : " + e.getLocalizedMessage());
        }
        return resultJson;
    }


    private static String get_StashUserProfile_DriveFolderId(Drive service){
        String folderId = null;
        String folder_name = "stash_user_profile";
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folder_name + "' and trashed=false";
        try{
            FileList resultList = service.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .execute();
            List<com.google.api.services.drive.model.File> folders = resultList.getFiles();
            if (folders != null && !folders.isEmpty()) {
                folderId = folders.get(0).getId();
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get stash user profile folder id from Drive : " + e.getLocalizedMessage(), true);
        }
        return folderId;
    }

    public static void insertBackupFromMap(JsonArray backupAccounts){
        String sqlQuery = "Insert into ACCOUNTS (userEmail,type,refreshToken) values (?,?,?)";
        DBHelper.dbWritable.beginTransaction();
        for (int i = 0; i < backupAccounts.size(); i++) {
            try {
                JsonObject backupAccount = backupAccounts.get(i).getAsJsonObject();
                String backupEmail = backupAccount.get("backupEmail").getAsString();
                String refreshToken = backupAccount.get("refreshToken").getAsString();
                DBHelper.dbWritable.execSQL(sqlQuery, new String[]{backupEmail, "backup", refreshToken});
                DBHelper.dbWritable.setTransactionSuccessful();
            } catch (SQLiteConstraintException e) {
                LogHandler.saveLog("SQLiteConstraintException in insert backup accounts method " + e.getLocalizedMessage(), false);
            } catch (Exception e) {
                LogHandler.saveLog("Failed to insert backup accounts data into ACCOUNTS  : " + e.getLocalizedMessage(), true);
            }
        }
        DBHelper.dbWritable.endTransaction();
    }

    public static void insertPrimaryFromMap(JsonArray primaryAccounts){
        String sqlQuery = "Insert into ACCOUNTS (userEmail,type,refreshToken) values (?,?,?)";
        DBHelper.dbWritable.beginTransaction();
        for (int i = 0; i < primaryAccounts.size(); i++) {
            try {
                JsonObject primaryAccount = primaryAccounts.get(i).getAsJsonObject();
                String primaryEmail = primaryAccount.get("primaryEmail").getAsString();
                String refreshToken = primaryAccount.get("refreshToken").getAsString();
                DBHelper.dbWritable.execSQL(sqlQuery, new String[]{primaryEmail, "primary", refreshToken});
                DBHelper.dbWritable.setTransactionSuccessful();
            } catch (SQLiteConstraintException e) {
                LogHandler.saveLog("SQLiteConstraintException in insert primary accounts method " + e.getLocalizedMessage(), false);
            } catch (Exception e) {
                LogHandler.saveLog("Failed to insert primary accounts data into ACCOUNTS : " + e.getLocalizedMessage(), true);
            }
        }
        DBHelper.dbWritable.endTransaction();
    }



}
