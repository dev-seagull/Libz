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
            String sqlQuery = "SELECT EXISTS(SELECT 1 FROM USERPROFILE WHERE type = 'profile')";
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

    public static JsonObject createProfileMapContent(){
        List<String[]> userProfiles = DBHelper.getUserProfile(new String[]{"userEmail","type","refreshToken"});
        JsonObject userProfileJson = new JsonObject();
        JsonArray backupAccountsJson = new JsonArray();
        JsonArray primaryAccountsJson = new JsonArray();
        JsonObject backUpDBJson = new JsonObject();
        JsonObject resultJson = new JsonObject();
        try{
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
            Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQuery, null);
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

            resultJson.add("userProfile", userProfileJson);
            resultJson.add("backupAccounts", backupAccountsJson);
            resultJson.add("primaryAccounts", primaryAccountsJson);
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to create profile map content : " + e.getLocalizedMessage() , true);
        }
        return  resultJson;
    }


    public static JsonObject readProfileMapContent() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<JsonObject> uploadTask = () -> {
            JsonObject result = null;
            String driveBackupAccessToken = "";

            try{
                String[] drive_backup_selected_columns = {"userEmail", "type", "accessToken"};
                List<String[]> drive_backUp_accounts = DBHelper.getUserProfile(drive_backup_selected_columns);
                for (String[] drive_backUp_account : drive_backUp_accounts) {
                    if (drive_backUp_account[1].equals("backup")) {
                        driveBackupAccessToken = drive_backUp_account[2];
                        break;
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to get first back up account access token: " + e.getLocalizedMessage(), true);
            }

            Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);

            String driveFolderId = get_StashUserProfile_DriveFolderId(service);

            List<com.google.api.services.drive.model.File> files = GoogleDrive.getDriveFolderFiles(service, driveFolderId);


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
                                        break;
                                    }
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


    public static void insertProfile(String username, String password, Context context){
        String sqlQuery = "Insert into USERPROFILE (userEmail,type,refreshToken) values (?,?,?)";
        DBHelper.dbWritable.beginTransaction();
        try {
            String newPass = Hash.calculateSHA256(password,context);
            DBHelper.dbWritable.execSQL(sqlQuery, new String[]{username,"profile",newPass});
            DBHelper.dbWritable.setTransactionSuccessful();
        }catch (SQLiteConstraintException e) {
            LogHandler.saveLog("SQLiteConstraintException in insert profile method "+ e.getLocalizedMessage(),false);
        }catch (Exception e) {
            LogHandler.saveLog("Failed to insert profile data into USERPROFILE." + e.getLocalizedMessage(), true);
        }
        finally {
            DBHelper.dbWritable.endTransaction();
        }
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
        String sqlQuery = "Insert into USERPROFILE (userEmail,type,refreshToken) values (?,?,?)";
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
                LogHandler.saveLog("Failed to insert backup accounts data into USERPROFILE." + e.getLocalizedMessage(), true);
            }
        }
        DBHelper.dbWritable.endTransaction();
    }

    public static void insertPrimaryFromMap(JsonArray primaryAccounts){
        String sqlQuery = "Insert into USERPROFILE (userEmail,type,refreshToken) values (?,?,?)";
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
                LogHandler.saveLog("Failed to insert primary accounts data into USERPROFILE." + e.getLocalizedMessage(), true);
            }
        }
        DBHelper.dbWritable.endTransaction();
    }



}
