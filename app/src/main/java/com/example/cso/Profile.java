package com.example.cso;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Profile {


    public static JsonObject createProfileMapContent(String userName){
        List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","type","refreshToken"});
        JsonArray profileJson = new JsonArray();
        JsonArray backupAccountsJson = new JsonArray();
        JsonArray primaryAccountsJson = new JsonArray();
        JsonArray backUpDBJson = new JsonArray();
        JsonObject resultJson = new JsonObject();

        String sqlQuery = "SELECT * FROM BACKUPDB";
        Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQuery, null);
        try{
            for (String[] account_row : account_rows){
                if(account_row[1].equals("backup")) {
                    JsonObject backupAccount = new JsonObject();
                    backupAccount.addProperty("backupEmail", account_row[0]);
                    backupAccount.addProperty("refreshToken", account_row[2]);
                    backupAccountsJson.add(backupAccount);}
                else if (account_row[1].equals("primary")){
                    JsonObject primaryAccount = new JsonObject();
                    primaryAccount.addProperty("primaryEmail", account_row[0]);
                    primaryAccount.addProperty("refreshToken", account_row[2]);
                    primaryAccountsJson.add(primaryAccount);
                }
            }

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

            JsonObject profile = new JsonObject();
            profile.addProperty("userName", userName);
            profileJson.add(profile);

            resultJson.add("profile", profileJson);
            resultJson.add("backupAccounts", backupAccountsJson);
            resultJson.add("primaryAccounts", primaryAccountsJson);
            resultJson.add("backUpDB", backUpDBJson);
        }catch (Exception e){
            LogHandler.saveLog("Failed to create profile map content : " + e.getLocalizedMessage() , true);
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return  resultJson;
    }

    public static JsonObject readProfileMapContent(String userEmail) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<JsonObject> uploadTask = () -> {
            JsonObject result = null;
            String driveBackupRefreshToken = "";
            String driveBackupAccessToken = "";
            try{
                String[] drive_backup_selected_columns = {"userEmail", "type", "refreshToken"};
                List<String[]> drive_backUp_accounts = DBHelper.getAccounts(drive_backup_selected_columns);
                for (String[] drive_backUp_account : drive_backUp_accounts) {
                    if (drive_backUp_account[1].equals("backup") && drive_backUp_account[0].equals(userEmail)) {
                        driveBackupRefreshToken = drive_backUp_account[2];
                        driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();
                        break;
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to get first back up account access token: " + e.getLocalizedMessage(), true);
            }

            Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);

            String profileFolderId = getDriveFolderId(service, "stash_user_profile");

            List<com.google.api.services.drive.model.File> files = GoogleDrive.getDriveFolderFiles(service, profileFolderId);

            boolean jsonFileExists = false;
            try{
                if(!profileFolderId.isEmpty() && profileFolderId != null){
                    if(!files.isEmpty() && files != null){
                        for (com.google.api.services.drive.model.File file : files) {
                            if ("application/json".equals(file.getMimeType()) && file.getName().startsWith("profileMap")) {
                                for (int i =0; i<3; i++){
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    service.files().get(file.getId())
                                            .executeMediaAndDownloadTo(outputStream);
                                    String jsonString = outputStream.toString();
                                    JsonObject resultJson = JsonParser.parseString(jsonString).getAsJsonObject();
                                    outputStream.close();
                                    if(resultJson != null) {
                                        JsonArray profileArray = resultJson.get("profile").getAsJsonArray();
                                        JsonObject profileJson = profileArray.get(0).getAsJsonObject();
                                        jsonFileExists = true;
                                        result = resultJson;
                                        return result;
                                    }
                                }
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

    private static String getDriveFolderId(Drive service, String folderName){
        String folderId = null;
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false";
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
                if(!DBHelper.accountExists(backupEmail, "backup")){
                    DBHelper.dbWritable.execSQL(sqlQuery, new String[]{backupEmail, "backup", refreshToken});
                    DBHelper.dbWritable.setTransactionSuccessful();
                }
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
                if(!DBHelper.accountExists(primaryEmail, "primary")){
                    DBHelper.dbWritable.execSQL(sqlQuery, new String[]{primaryEmail, "primary", refreshToken});
                    DBHelper.dbWritable.setTransactionSuccessful();
                }
            } catch (SQLiteConstraintException e) {
                LogHandler.saveLog("SQLiteConstraintException in insert primary accounts method " + e.getLocalizedMessage(), false);
            } catch (Exception e) {
                LogHandler.saveLog("Failed to insert primary accounts data into ACCOUNTS : " + e.getLocalizedMessage(), true);
            }
        }
        DBHelper.dbWritable.endTransaction();
    }

    public boolean syncJsonAccounts(String status, String userEmail){
        if (status.equals("sign-out")){
            final boolean[] isDeleted = {false};
                Thread deleteJsonThread = new Thread(() -> {
                    isDeleted[0] = deleteProfileJson(userEmail);
                    synchronized (this){
                        notify();
                    }
                });

                Thread backUpJsonThread = new Thread(() -> {
                    synchronized (isDeleted){
                        try {
                            deleteJsonThread.join();
                        } catch (InterruptedException e) {
                            LogHandler.saveLog("Failed to join deleteJsonThread: " + e.getLocalizedMessage());
                        }
                        if(isDeleted[0]){
                            boolean isBackedUp = MainActivity.dbHelper.backUpProfileMap(false,"");
                        }
                    }
                });
                deleteJsonThread.start();
                backUpJsonThread.start();
        } else if (status.equals("sign-in")) {
            JsonObject profileMapContent = Profile.readProfileMapContent(userEmail);
        }
        return true;
    }

    public static boolean deleteProfileJson(String userEmail) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final boolean[] isDeleted = {false};
        Callable<Boolean> uploadTask = () -> {
            try {
                String driveBackupAccessToken = "";
                String driveBackupRefreshToken = "";
                String[] selected_columns = {"userEmail", "type","refreshToken"};
                List<String[]> account_rows = MainActivity.dbHelper.getAccounts(selected_columns);
                for (String[] account_row : account_rows) {
                    if (account_row[1].equals("backup") && account_row[0].equals(userEmail)) {
                        driveBackupRefreshToken = account_row[2];
                        driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();

                        Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                        String folder_name = "stash_user_profile";
                        String folderId = null;
                        com.google.api.services.drive.model.File folder = null;

                        FileList fileList = service.files().list()
                                .setQ("mimeType='application/vnd.google-apps.folder' and name='"
                                        + folder_name + "' and trashed=false")
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute();
                        List<com.google.api.services.drive.model.File> driveFolders = fileList.getFiles();
                        for (com.google.api.services.drive.model.File driveFolder : driveFolders) {
                            folderId = driveFolder.getId();
                        }

                        if (folderId == null) {
                            com.google.api.services.drive.model.File folder_metadata =
                                    new com.google.api.services.drive.model.File();
                            folder_metadata.setName(folder_name);
                            folder_metadata.setMimeType("application/vnd.google-apps.folder");
                            folder = service.files().create(folder_metadata)
                                    .setFields("id").execute();

                            folderId = folder.getId();
                        }
                        try {
                            fileList = service.files().list()
                                    .setQ("name contains 'profileMap' and '" + folderId + "' in parents")
                                    .setSpaces("drive")
                                    .setFields("files(id)")
                                    .execute();
                            List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
                            for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                                service.files().delete(existingFile.getId()).execute();
                            }

                            fileList = service.files().list()
                                    .setQ("name contains 'profileMap' and '" + folderId + "' in parents")
                                    .setSpaces("drive")
                                    .setFields("files(id)")
                                    .execute();
                            existingFiles = fileList.getFiles();
                            System.out.println("fsize " + existingFiles.size());
                            if (existingFiles.size() == 0) {
                                isDeleted[0] = true;
                            }
                        }catch (Exception e){
                            LogHandler.saveLog("Failed to delete profileMap from backup : " + e.getLocalizedMessage() , true);
                        }
                    }
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to upload profileMap from Android to backup in deleteProfileJson : " + e.getLocalizedMessage());
            }
            return isDeleted[0];
        };

        Future<Boolean> future = executor.submit(uploadTask);
        boolean isDeletedFuture = false;
        try {
            isDeletedFuture = future.get();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete profile Content from account : " + e.getLocalizedMessage());
        }
        return isDeletedFuture;
    }

    public static boolean backUpJsonFile(GoogleCloud.signInResult signInResult, ActivityResultLauncher<Intent> signInTobackUpLauncher){
        boolean[] isBackedUp = {false};
        Thread backUpJsonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    if (signInResult.getHandleStatus() == true) {
                        String userEmail = signInResult.getUserEmail();
                        GoogleCloud.Tokens tokens = signInResult.getTokens();
                        String refreshToken = tokens.getRefreshToken();
                        String accessToken = tokens.getAccessToken();
                        Double totalStorage = signInResult.getStorage().getTotalStorage();
                        Double usedStorage = signInResult.getStorage().getUsedStorage();
                        Double usedInDriveStorage = signInResult.getStorage().getUsedInDriveStorage();
                        Double usedInGmailAndPhotosStorage = signInResult.getStorage().getUsedInGmailAndPhotosStorage();

                        MainActivity.dbHelper.insertIntoAccounts(userEmail, "backup", refreshToken,accessToken,
                                totalStorage, usedStorage, usedInDriveStorage, usedInGmailAndPhotosStorage);
                        isBackedUp[0] = MainActivity.dbHelper.backUpProfileMap(false,"");
                        System.out.println("Test of is backedUp: " + isBackedUp[0]);
                    }else{
                        LogHandler.saveLog("login with back up launcher failed with response code ");
                        MainActivity.activity.runOnUiThread(() -> {
                            LinearLayout backupButtonsLinearLayout = MainActivity.activity.findViewById(R.id.backUpAccountsButtons);
                            View child2 = backupButtonsLinearLayout.getChildAt(
                                    backupButtonsLinearLayout.getChildCount() - 1);
                            if(child2 instanceof Button){
                                Button bt = (Button) child2;
                                bt.setText("ADD A BACK UP ACCOUNT");
                            }
                            MainActivity.updateButtonsListeners(signInTobackUpLauncher);
                        });
                    }
                }catch (Exception e){
                    LogHandler.saveLog("Failed to backup json file: " + e.getLocalizedMessage(), true);
                }
            }
        });
        backUpJsonThread.start();
        try {
            backUpJsonThread.join();
        } catch (Exception e) {
            LogHandler.saveLog("failed to join backUpJsonThread in backup account: " + e.getLocalizedMessage());
        }
        return isBackedUp[0];
    }
}
