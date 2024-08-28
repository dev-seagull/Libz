package com.example.cso;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;

public class Sync {
//    private static boolean isAllOfAccountsFull = true;
    public static void syncAndroidFiles(Context context, Activity activity){
        Log.d("Threads","startSyncThread started");
        Thread syncThread =  new Thread( () -> {
            try{
//                double amountSpaceToFreeUp = StorageHandler.freeStorageUpdater();

                List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","type",
                        "totalStorage","usedStorage", "refreshToken","usedStorage","totalStorage"});
                boolean accountExists = DBHelper.anyBackupAccountExists();
                boolean isAllOfAccountsFull = true;

                for(String[] account_row: account_rows){
                    double usedStorage = Double.parseDouble(account_row[5]);
                    Double totalStorage = Double.parseDouble(account_row[6]);
                    boolean isAccountFull = GoogleDrive.isBackUpAccountFull(totalStorage,usedStorage);
                    if(!isAccountFull){
                        isAllOfAccountsFull = false;
                        String type = account_row[1];
                        if(type.equals("backup")){
//                        syncAndroidToBackupAccount(account_row,amountSpaceToFreeUp,context, activity);
                        }
                    }
                }

                Log.d("service","isAllOfAccountsFull : " + isAllOfAccountsFull);
                Log.d("service", "any backup account exists: " + accountExists);

                if(!InternetManager.isInternetReachable("https://drive.google.com")){
                    MainActivity.activity.runOnUiThread( () -> {
                        Log.d("service","no internet connection");
                        Toast.makeText(activity,
                                "No internet connection!",
                                Toast.LENGTH_LONG).show();
                    });
                }
                else if(isAllOfAccountsFull && accountExists){
                    MainActivity.activity.runOnUiThread(() -> {
                        Toast.makeText(activity,
                                "Sync failed! You are running out of space." +
                                        " Add more back up accounts.",
                                Toast.LENGTH_LONG).show();
                    });
                } else if(!accountExists){
                    MainActivity.activity.runOnUiThread(() -> {
                        Toast.makeText(activity,
                                "There is no backup account to sync!",
                                Toast.LENGTH_LONG).show();
                    });
                }

            }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e);  }
        });

        syncThread.start();
        try{
            syncThread.join();
        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }

        Log.d("Threads","startSyncThread finished");
    }

    private static void syncAndroidToBackupAccount(String[] accountRow, double amountSpaceToFreeUp,
                                                   Context context, Activity activity){
        try{
            String userEmail = accountRow[0];
            String refreshToken = accountRow[4];
            String folderName = GoogleDriveFolders.assetsFolderName;
            String syncedAssetsSubFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, null, false);
            if(syncedAssetsSubFolderId == null){
                Log.d("folders","asset folder not found");
                return;
            }

            double driveFreeSpace = calculateDriveFreeSpace(accountRow);
            System.out.println("This is drive free space " + driveFreeSpace);

            if (driveFreeSpace <= 100){
                return;
            }else{
                //should be global
                boolean isAllOfAccountsFull = false;
            }

            List<String[]> sortedAndroidFiles = getSortedAndroidFiles();

            for (String[] androidRow : sortedAndroidFiles) {
                StorageHandler.freeStorageUpdater();
                syncAndroidFile(androidRow, userEmail, refreshToken, syncedAssetsSubFolderId,
                        driveFreeSpace, amountSpaceToFreeUp, context, activity);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to sync android to backup accounts: "  + e.getLocalizedMessage(), true);
        }
    }


    private static void syncAndroidFile(String[] androidRow, String userEmail, String refreshToken,
                                        String syncedAssetsFolderId, double driveFreeSpace,
                                        double amountSpaceToFreeUp, Context context, Activity activity){
        try{
            String fileName = androidRow[1];
            String fileHash = androidRow[5];
            String fileSize = androidRow[4];
            String filePath = androidRow[2];
            Long assetId = Long.valueOf(androidRow[8]);

            boolean isWifiOnlySwitchOn = SharedPreferencesHandler.getWifiOnlySwitchState();
            if (!Thread.currentThread().isInterrupted() && (isWifiOnlySwitchOn && InternetManager.getInternetStatus(context).equals("wifi")) || (!isWifiOnlySwitchOn) ){
                MainActivity.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UIHelper uiHelper = new UIHelper();
                        uiHelper.syncMessageTextView.setVisibility(View.VISIBLE);
                        uiHelper.syncMessageTextView.setText("Syncing is in progress");
                        try{
                            if(!MainActivity.activity.isDestroyed()){
//                                Glide.with(MainActivity.activity).asGif().load(R.drawable.gifwaiting).into(UIHelper.waitingSyncGif);
//                                UIHelper.waitingSyncGif.setVisibility(View.VISIBLE);
                            }
                        }catch (Exception e){
                            System.out.println(e.getLocalizedMessage());
                        }
                    }
                });
                if (!DBHelper.androidFileExistsInDrive(assetId, fileHash)){
                    String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
                    if(driveFreeSpace > Double.parseDouble(fileSize)) {
                        boolean isBackedUp = uploadAndroidToDrive(androidRow, userEmail, accessToken, syncedAssetsFolderId);
                        if (isBackedUp) {
                            GoogleDrive.startUpdateDriveFilesThread();
                            driveFreeSpace -= Double.parseDouble(fileSize);
                            if(amountSpaceToFreeUp > 0){
                                GoogleDrive.deleteRedundantDriveFilesFromAccount(userEmail);
                                if (DBHelper.androidFileExistsInDrive(Long.valueOf(androidRow[8]), fileHash)) {
                                    boolean isDeleted = Android.deleteAndroidFile(filePath, String.valueOf(assetId), fileHash
                                            , fileSize, fileName, activity);
                                    if (isDeleted) {
                                        amountSpaceToFreeUp -= Double.parseDouble(fileSize);
                                        LogHandler.saveLog(fileName + " is deleted",false);
                                    }
                                }
                            }

                        }
                    }
                }else {
                    if (amountSpaceToFreeUp > 0) {
                        GoogleDrive.deleteRedundantDriveFilesFromAccount(userEmail);
                        if (DBHelper.androidFileExistsInDrive(Long.valueOf(androidRow[8]), fileHash)) {
                            boolean isDeleted = Android.deleteAndroidFile(filePath, androidRow[8], fileHash
                                    , fileSize, androidRow[1], activity);
                            if (isDeleted) {
                                amountSpaceToFreeUp -= Double.parseDouble(fileSize);
                                LogHandler.saveLog(fileName + " is deleted",false);
                                System.out.println(fileName + " is deleted");
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to sync android file: " + e.getLocalizedMessage());
        }
    }

    private static boolean uploadAndroidToDrive(String[] androidRow, String userEmail
            , String accessToken, String syncedAssetsFolderId){
        boolean[] isBackedUp = {false};
        try{
            Thread backupThread = new Thread(() -> {
                Long fileId = Long.valueOf(androidRow[0]);
                String fileName = androidRow[1];
                System.out.println("Uploading to drive : " + fileName + " " + DBHelper.countAndroidAssets());
                String filePath = androidRow[2];
                String fileHash = androidRow[5];
                String mimeType = androidRow[7];
                String assetId = androidRow[8];
                BackUp backUp = new BackUp();
                isBackedUp[0] = backUp.backupAndroidToDrive(fileId,fileName, filePath,fileHash,mimeType,assetId,
                        accessToken,userEmail,syncedAssetsFolderId);
            });

            backupThread.start();
            try {
                backupThread.join();
            } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }

        return isBackedUp[0];
    }


    private static List<String[]> getSortedAndroidFiles(){
        String[] selected_android_columns = {"id", "fileName", "filePath", "device",
                "fileSize", "fileHash", "dateModified", "memeType","assetId"};

        List<String[]> unique_android_rows = new ArrayList<>();
        ArrayList<String> file_hashes = new ArrayList<>() ;

        try{
            List<String[]> android_rows =  DBHelper.getAndroidTable(selected_android_columns);
            BackUp.sortAndroidItems(android_rows);
            for(String[] android_row: android_rows){
                String fileHash = android_row[5];
                if(!file_hashes.contains(fileHash)){
                    file_hashes.add(fileHash);
                    unique_android_rows.add(android_row);
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("get sorted android files failed: " + e.getLocalizedMessage(), true );
        }

        return unique_android_rows;
    }

    public static double calculateDriveFreeSpace(String[] accountRow){
        try{
            String[] totalStorage = {accountRow[2]};
            String[] usedStorage = {accountRow[3]};
            return Double.parseDouble(totalStorage[0]) - Double.parseDouble(usedStorage[0]);
        }catch (Exception e) {
            LogHandler.saveLog("Failed to calculate drive free space: " + e.getLocalizedMessage(), true);
            return 0;
        }
    }

    public static void stopSync(){
        MainActivity.activity.getApplicationContext().stopService(MainActivity.serviceIntent);
    }

    public static void startSync(Activity activity){
        activity.getApplicationContext().startService(MainActivity.serviceIntent);
    }


}
