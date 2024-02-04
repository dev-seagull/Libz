package com.example.cso;

import android.database.sqlite.SQLiteConstraintException;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class upgrade {


    public static boolean upgradeDataBaseFrom1to2(){
        Boolean flag = false;
        cutFromUserProfileToAccounts();
        String deleteMessage = updateLoginAndSignUp();
        if(deleteMessage.equals("Done with deleting profile maps.")){
            flag = true;
        }
        return flag;
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

    public static String updateLoginAndSignUp(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<String> uploadTask = () -> {
            String deletedFilesMessage = "";
            try {
                String driveBackupAccessToken = "";
                String[] selected_columns = {"userEmail", "type", "accessToken"};
                List<String[]> account_rows = MainActivity.dbHelper.getAccounts(selected_columns);
                for (String[] account_row : account_rows) {
                    if (account_row[1].equals("backup")) {
                        driveBackupAccessToken = account_row[2];

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
                        for(com.google.api.services.drive.model.File driveFolder: driveFolders){
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

                        fileList = service.files().list()
                                .setQ("name contains 'profileMap' and '" + folderId + "' in parents")
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute();
                        List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
                        for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                            service.files().delete(existingFile.getId()).execute();
                        }
                        deletedFilesMessage = "Done with deleting profile maps.";
                    }
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to delete profileMaps when upgrading : " + e.getLocalizedMessage());
            }
            return deletedFilesMessage;
        };

        Future<String> future = executor.submit(uploadTask);
        String uploadFileIdFuture = new String();
        try{
            uploadFileIdFuture = future.get();
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return uploadFileIdFuture;
    }

}
