package com.example.cso;

import java.util.ArrayList;
import java.util.List;

public class Sync {

    public static void syncAndroidFiles(){
        try{
            StorageHandler storageHandler = new StorageHandler();
            double amountSpaceToFreeUp = storageHandler.getAmountSpaceToFreeUp();

            String[] selected_accounts_columns = {"userEmail","type", "totalStorage","usedStorage", "accessToken","folderId"};
            List<String[]> account_rows = DBHelper.getAccounts(selected_accounts_columns);

            for(String[] account_row: account_rows){
                if(account_row[1].equals("backup")){
                    double driveFreeSpace = Double.valueOf(account_row[2]) - Double.valueOf(account_row[3]);
                    ArrayList<String> file_hashes = new ArrayList<>() ;
                    String[] selected_android_columns = {"id", "fileName", "filePath", "device",
                            "fileSize", "fileHash", "dateModified", "memeType","assetId"};
                    List<String[]> android_rows =  MainActivity.dbHelper.getAndroidTable(selected_android_columns);
                    Upload upload = new Upload();
                    Upload.sortAndroidItems(android_rows);
                    for(String[] android_row: android_rows){
                        boolean isDeleted = false;
                        if(!file_hashes.contains(android_row[5])) {
                            file_hashes.add(android_row[5]);
                            if (!DBHelper.androidFileExistsInDrive(Long.valueOf(android_row[8]), android_row[5])) {
                                if (driveFreeSpace > Double.valueOf(android_row[4])) {
                                    final boolean[] isBackedUp = {false};
                                    Thread backupThread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            isBackedUp[0] = upload.backupAndroidToDrive(Long.valueOf(account_row[0]), account_row[1], account_row[2],
                                                    account_row[5], account_row[7],
                                                    account_row[8], account_row[4],
                                                    account_row[0], account_row[5]);
                                        }
                                    });
                                    backupThread.start();
                                    try {
                                        backupThread.join();
                                    } catch (Exception e) {
                                        LogHandler.saveLog("Failed to join backup thread when syncing android file.", true);
                                    }
                                    if (isBackedUp[0]) {
                                        driveFreeSpace = driveFreeSpace - Double.valueOf(android_row[4]);
                                        if (amountSpaceToFreeUp > 0) {
                                            Thread deleteRedundantDrive = new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    String[] columns = {"accessToken","userEmail", "type"};
                                                    List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columns);

                                                    for(String[] account_row : account_rows) {
                                                        String type = account_row[2];
                                                        if(type.equals("backup")){
                                                            String userEmail = account_row[1];
                                                            String accessToken = account_row[0];
                                                            ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                                            ArrayList<String> driveFileIds = new ArrayList<>();

                                                            for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                                                                String fileId = driveMediaItem.getId();
                                                                driveFileIds.add(fileId);
                                                            }
                                                            MainActivity.dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
                                                        }
                                                    }
                                                }
                                            });
                                            deleteRedundantDrive.start();
                                            try {
                                                deleteRedundantDrive.join();
                                            } catch (Exception e) {
                                                LogHandler.saveLog("Failed to join delete " +
                                                        " redundant drive thread when syncing android file.", true);
                                            }
                                            if(DBHelper.androidFileExistsInDrive(Long.valueOf(android_row[8]), android_row[5])){
                                                isDeleted = Android.deleteAndroidFile(android_row[2],account_row[8],android_row[5]
                                                        ,account_row[4],android_row[1]);
                                            }
                                        }
                                    }
                                }
                            }else {
                                if (amountSpaceToFreeUp > 0) {
                                    Thread deleteRedundantDrive = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String[] columns = {"accessToken","userEmail", "type"};
                                            List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columns);

                                            for(String[] account_row : account_rows) {
                                                String type = account_row[2];
                                                if(type.equals("backup")){
                                                    String userEmail = account_row[1];
                                                    String accessToken = account_row[0];
                                                    ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                                    ArrayList<String> driveFileIds = new ArrayList<>();

                                                    for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                                                        String fileId = driveMediaItem.getId();
                                                        driveFileIds.add(fileId);
                                                    }
                                                    MainActivity.dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
                                                }
                                            }
                                        }
                                    });
                                    deleteRedundantDrive.start();
                                    try {
                                        deleteRedundantDrive.join();
                                    } catch (Exception e) {
                                        LogHandler.saveLog("Failed to join delete " +
                                                " redundant drive thread when syncing android file.", true);
                                    }
                                    if(DBHelper.androidFileExistsInDrive(Long.valueOf(android_row[8]), android_row[5])){
                                        isDeleted = Android.deleteAndroidFile(android_row[2],account_row[8],android_row[5]
                                                ,account_row[4],android_row[1]);
                                    }
                                }
                            }
                        }
                        if(isDeleted){
                            amountSpaceToFreeUp = amountSpaceToFreeUp - Double.valueOf(android_row[4]);
                        }
                    }
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to sync files: " + e.getLocalizedMessage(), true);
        }
    }
}
