package com.example.cso;

import java.util.ArrayList;
import java.util.List;

public class Sync {

    public static void syncAndroidFiles(){
        try{
            StorageHandler storageHandler = new StorageHandler();
            double amountSpaceToFreeUp = storageHandler.getAmountSpaceToFreeUp();
            String[] id = {""};
            String[] fileName = {""};
            String[] filePath = {""};
            String[] device = {""};
            String[] fileSize = {""};
            String[] fileHash = {""};
            String[] dateModified = {""};
            String[] memeType = {""};
            String[] assetId = {""};
            String[] selected_accounts_columns = {"userEmail","type", "totalStorage","usedStorage", "accessToken","folderId"};
            List<String[]> account_rows = DBHelper.getAccounts(selected_accounts_columns);
            final boolean[] isBackedUp = {false};
            for(String[] account_row: account_rows){
                String[] userEmail = {account_row[0]};
                String[] type = {account_row[1]};
                String[] totalStorage = {account_row[2]};
                String[] usedStorage = {account_row[3]};
                String[] accessToken = {account_row[4]};
                String[] folderId = {account_row[5]};
                if(type[0].equals("backup")){
                    double driveFreeSpace = Double.valueOf(totalStorage[0]) - Double.valueOf(usedStorage[0]);
                    ArrayList<String> file_hashes = new ArrayList<>() ;
                    String[] selected_android_columns = {"id", "fileName", "filePath", "device",
                            "fileSize", "fileHash", "dateModified", "memeType","assetId"};
                    List<String[]> android_rows =  MainActivity.dbHelper.getAndroidTable(selected_android_columns);
                    Upload upload = new Upload();
                    Upload.sortAndroidItems(android_rows);
                    for(String[] android_row: android_rows) {
                        System.out.println("------ - - - - - - -sort checking data modified : " + android_row[6]
                                + " file name: " + android_row[1]);
                    }
//                    for(String[] android_row: android_rows){
//                        id[0] = android_row[0];
//                        fileName[0] = android_row[1];
//                        filePath[0] = android_row[2];
//                        fileSize[0] = android_row[4];
//                        fileHash[0] = android_row[5];
//                        dateModified[0] = android_row[6];
//                        memeType[0] = android_row[7];
//                        assetId[0] = android_row[8];
//                        System.out.println("Check android Item : " + filePath[0] + "\nWith\n"+
//                                "Account: " + userEmail[0] + "\n" + "driveFreeSpace: " + driveFreeSpace + "\n");
//                        boolean isDeleted = false;
//                        if(!file_hashes.contains(fileHash[0])) {
//                            file_hashes.add(fileHash[0]);
//                            if (!DBHelper.androidFileExistsInDrive(Long.valueOf(assetId[0]), fileHash[0])) {
//                                if (driveFreeSpace > Double.valueOf(fileSize[0])) {
//                                    isBackedUp[0] = false;
//                                    Thread backupThread = new Thread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            System.out.println("uploading "+fileName[0] + " to drive");
//                                            isBackedUp[0] = upload.backupAndroidToDrive(Long.valueOf(id[0]),fileName[0],
//                                                    filePath[0],fileHash[0],memeType[0],assetId[0],accessToken[0],userEmail[0],folderId[0]);
//                                            System.out.println("uplaoded "+fileName[0] + " to drive is finished");
//                                        }
//                                    });
//                                    backupThread.start();
//                                    try {
//                                        backupThread.join();
//                                    } catch (Exception e) {
//                                        LogHandler.saveLog("Failed to join backup thread when syncing android file.", true);
//                                    }
//                                    if (isBackedUp[0]) {
//                                        driveFreeSpace = driveFreeSpace - Double.valueOf(fileSize[0]);
//                                        if (amountSpaceToFreeUp > 0) {
//                                            Thread deleteRedundantDrive = new Thread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    String[] columns = {"accessToken","userEmail", "type"};
//                                                    List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columns);
//
//                                                    for(String[] account_row : account_rows) {
//                                                        String type = account_row[2];
//                                                        if(type.equals("backup")){
//                                                            String userEmail = account_row[1];
//                                                            String accessToken = account_row[0];
//                                                            ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
//                                                            ArrayList<String> driveFileIds = new ArrayList<>();
//
//                                                            for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
//                                                                String fileId = driveMediaItem.getId();
//                                                                driveFileIds.add(fileId);
//                                                            }
//                                                            MainActivity.dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
//                                                        }
//                                                    }
//                                                }
//                                            });
//                                            deleteRedundantDrive.start();
//                                            try {
//                                                deleteRedundantDrive.join();
//                                            } catch (Exception e) {
//                                                LogHandler.saveLog("Failed to join delete " +
//                                                        " redundant drive thread when syncing android file.", true);
//                                            }
//                                            if(DBHelper.androidFileExistsInDrive(Long.valueOf(assetId[0]), fileHash[0])){
//                                                isDeleted = Android.deleteAndroidFile(filePath[0],assetId[0],fileHash[0]
//                                                        ,fileSize[0],fileName[0]);
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            else {
//                                if (amountSpaceToFreeUp > 0) {
//                                    Thread deleteRedundantDrive = new Thread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            String[] columns = {"accessToken","userEmail", "type"};
//                                            List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columns);
//                                            for(String[] account_row : account_rows) {
//                                                String type = account_row[2];
//                                                if(type.equals("backup")){
//                                                    String userEmail = account_row[1];
//                                                    String accessToken = account_row[0];
//                                                    ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
//                                                    ArrayList<String> driveFileIds = new ArrayList<>();
//
//                                                    for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
//                                                        String fileId = driveMediaItem.getId();
//                                                        driveFileIds.add(fileId);
//                                                    }
//                                                    MainActivity.dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
//                                                }
//                                            }
//                                        }
//                                    });
//                                    deleteRedundantDrive.start();
//                                    try {
//                                        deleteRedundantDrive.join();
//                                    } catch (Exception e) {
//                                        LogHandler.saveLog("Failed to join delete " +
//                                                " redundant drive thread when syncing android file.", true);
//                                    }
//                                    if(DBHelper.androidFileExistsInDrive(Long.valueOf(assetId[0]), fileHash[0])){
//                                        isDeleted = Android.deleteAndroidFile(filePath[0],assetId[0],fileHash[0]
//                                                ,fileSize[0],fileName[0]);
//                                    }
//                                }
//                            }
//                        }else{
//                            System.out.println("android duplicate file hash: " + fileHash[0]);
//                        }
//                        System.out.println("isBackedUp: " + isBackedUp[0]);
//                        System.out.println("isDeleted: " + isDeleted);
//
//                        if(isDeleted){
//                            amountSpaceToFreeUp = amountSpaceToFreeUp - Double.valueOf(fileSize[0]);
//                        }
//                    }
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to sync files: " + e.getLocalizedMessage(), true);
        }
    }
}
