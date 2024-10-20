package com.example.cso;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.cso.UI.UI;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;

public class Sync {
//    private static boolean isAllOfAccountsFull = true;
    public static boolean isJsonChangeCheckRunning = false;
    public static boolean isAnyBackUpAccountExistsToastShown = false;
    public static boolean isInternetConnectionToastShown = false;
    public static boolean isAllOfAccountsFullToastShown = false;
    public static long lastToastTime = 0;
    public static boolean isAllOfAccountsFull = true;
    public static long toastInterval = 10000;
    public static double amountToFreeUp;
    public static double currentDriveFreeSpace;
    public static void syncAndroidFiles(Activity activity){
        Log.d("Threads","startSyncThread started");
        Thread syncThread =  new Thread( () -> {
            try{
                List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","type",
                        "totalStorage","usedStorage", "refreshToken","usedStorage","totalStorage"});
                for(String[] account_row: account_rows){
                    if (!TimerService.isTimerRunning){
                        Log.d("service","sync Stopped suddenly");
                        break;
                    }
                    String type = account_row[1];
                    if(type.equals("backup")) {
                        currentDriveFreeSpace = GoogleDrive.calculateDriveFreeSpace(account_row);
                        Log.d("service","free space of " + account_row[0] + " : " + currentDriveFreeSpace);
                        if(currentDriveFreeSpace > 50){
                            isAllOfAccountsFull = false;
                            syncAndroidToBackupAccount(account_row[0], account_row[4], activity);
                        }
                    }
                }
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastToastTime >= toastInterval) {
                    lastToastTime = currentTime;
                    isAllOfAccountsFullToastShown = false;
                }
                if (isAllOfAccountsFull){
                    if (!isAllOfAccountsFullToastShown){
                        isAllOfAccountsFullToastShown = true;
                        MainActivity.activity.runOnUiThread(()->{

                        });
                    }
                }
                Log.d("service","end of check for account existence and capacity");
            }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e);  }
        });

        syncThread.start();
        try{
            syncThread.join();
        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }

        Log.d("Threads","startSyncThread finished");
    }

    private static void syncAndroidToBackupAccount(String userEmail, String refreshToken, Activity activity){
        Log.d("Threads","syncAndroidToBackupAccount  method started");
        try{
            String folderName = GoogleDriveFolders.assetsFolderName;
            String syncedAssetsSubFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, null, false);
            if(syncedAssetsSubFolderId == null){
                Log.d("service","asset folder not found");
                return;
            }

            List<String[]> sortedAndroidFiles = getSortedAndroidFiles();
            amountToFreeUp = StorageHandler.getAmountSpaceToFreeUp();
            Log.d("service","init amount space to free up : " + amountToFreeUp);

            int checkedLimitFilesCount = 0 ;
            double checkedLimitFilesSize = 0 ;

            new Thread( () -> checkForStatusChanges(activity)).start();

            for (String[] androidRow : sortedAndroidFiles) {
                if (!TimerService.isTimerRunning){
                    Log.d("service","sync Stopped suddenly");
                    break;
                }

                double fileSize = Double.parseDouble(androidRow[4]);
                checkedLimitFilesSize += fileSize;
                checkedLimitFilesCount ++;
                if (checkedLimitFilesSize >= 1000 || checkedLimitFilesCount >= 40){
                    checkedLimitFilesSize = 0;
                    checkedLimitFilesCount = 0;
                    new Thread( () -> checkForStatusChanges(activity)).start();
                    String internetConnectionStatus = getInternetStatusForSync(activity);
                    boolean anyBackUpaAccountExists = DBHelper.anyBackupAccountExists();
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastToastTime >= toastInterval) {
                        lastToastTime = currentTime;
                        isAnyBackUpAccountExistsToastShown = false;
                        isInternetConnectionToastShown = false;
                    }
                    if (!anyBackUpaAccountExists) {
                        isInternetConnectionToastShown = false;
                        String message = "There is no backup account to sync!";
                        if(!isAnyBackUpAccountExistsToastShown) {
                            isAnyBackUpAccountExistsToastShown = true;
                            UI.makeToast(message);
                        }
                        break;
                    }
                    if(!internetConnectionStatus.equals("wifi") && !internetConnectionStatus.equals("data")) {
                        isAnyBackUpAccountExistsToastShown = false;
                        String message = internetConnectionStatus;
                        if (!isInternetConnectionToastShown) {
                            isInternetConnectionToastShown = true;
                            UI.makeToast(message);
                        }
                        break;
                    }
                    isAnyBackUpAccountExistsToastShown = false;
                    isInternetConnectionToastShown = false;
                }

                syncAndroidFile(androidRow, userEmail, refreshToken, syncedAssetsSubFolderId, activity);
            }
        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
    }


    private static void syncAndroidFile(String[] androidRow, String userEmail, String refreshToken,
                                        String syncedAssetsFolderId,Activity activity){
        Thread syncAndroidFileThread = new Thread( () -> {
            try{
                Log.d("Threads","syncAndroidFileThread started");
                String fileName = androidRow[1];
                String filePath = androidRow[2];
                String device = androidRow[3];
                String fileSize = androidRow[4];
                String fileHash = androidRow[5];
                Long assetId = Long.valueOf(androidRow[8]);

                if (!DBHelper.androidFileExistsInDrive(assetId, fileHash)){
                    Log.d("file","file was not in drive : "  + fileName);
                    if(currentDriveFreeSpace > Double.parseDouble(fileSize) + 10) {
                        Log.d("file","has enough space to upload to drive : "  + fileName);
                        String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
                        boolean isBackedUp = uploadAndroidToDrive(androidRow, userEmail, accessToken, syncedAssetsFolderId);
                        if (isBackedUp) {
                            Log.d("file","backed up : "  + fileName);
                            Log.d("service" ,fileName + " is backedup");
                            GoogleDrive.startUpdateDriveFilesThread();
                            currentDriveFreeSpace -= Double.parseDouble(fileSize);

                            if(amountToFreeUp > 0){
                                Log.d("file","should delete after backup : "  + fileName);
                                GoogleDrive.deleteRedundantDriveFilesFromAccount(userEmail);
                                if (DBHelper.androidFileExistsInDrive(assetId, fileHash)) {
                                    boolean isDeleted = Android.deleteAndroidFile(device,filePath, String.valueOf(assetId)
                                            , fileHash, fileSize, fileName, activity);
                                    if (isDeleted) {
                                        amountToFreeUp -= Double.parseDouble(fileSize);
                                        Log.d("file","deleted after backup : "  + fileName);
                                        Log.d("service" ,fileName + " is deleted");
                                    }
                                }
                            }
                        }
                    }
                }else {
                    Log.d("file","already in drive : "  + fileName);
                    if (amountToFreeUp > 0) {
                        Log.d("file","have to delete : "  + fileName);
                        GoogleDrive.deleteRedundantDriveFilesFromAccount(userEmail);
                        if (DBHelper.androidFileExistsInDrive(assetId, fileHash)) {
                            Log.d("file","file already in drive and have to delete : "  + fileName);
                            boolean isDeleted = Android.deleteAndroidFile(device, filePath, androidRow[8], fileHash
                                    , fileSize, androidRow[1], activity);
                            if (isDeleted) {
                                Log.d("file","file deleted : "  + fileName);
                                Log.d("service" ,fileName + " is deleted");
                                amountToFreeUp -= Double.parseDouble(fileSize);
                            }
                        }
                    }
                }
                Log.d("service","amount needed to free up after sync one file : " + amountToFreeUp);
            }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
        });

        syncAndroidFileThread.start();
        try{
            syncAndroidFileThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        Log.d("Threads","syncAndroidFileThread finished");
    }

    private static String getInternetStatusForSync(Context context) {
        boolean isWifiOnlySwitchOn = SharedPreferencesHandler.getWifiOnlySwitchState();
        String internetStatus = InternetManager.getInternetStatus(context);

        Log.d("service","isWifiOnlySwitchOn : " + isWifiOnlySwitchOn);
        Log.d("service","internetStatus : " + internetStatus);
        if(isWifiOnlySwitchOn){
            if(internetStatus.equals("wifi")){
                return "wifi";
            }else {
                return "No wifi connection!";
            }
        }else {
            if(internetStatus.equals("noInternet")){
                return "noInternet";
            }else{
                return internetStatus;
            }
        }
    }


    private static boolean uploadAndroidToDrive(String[] androidRow, String userEmail
            , String accessToken, String syncedAssetsFolderId){
        boolean[] isBackedUp = {false};
        try{
            Thread backupThread = new Thread(() -> {
                Long fileId = Long.valueOf(androidRow[0]);
                String fileName = androidRow[1];
                String filePath = androidRow[2];
                String fileHash = androidRow[5];
                String mimeType = androidRow[7];
                String assetId = androidRow[8];
                MainActivity.syncDetailsStatus = "Syncing " + fileName + " to " + userEmail + " ...";
                UI.makeToast("Syncing " + fileName + " to " + userEmail + " ...");
//                SyncDetails.setSyncStatusDetailsTextView(activity, false);
                isBackedUp[0] = BackUp.backupAndroidToDrive(fileId,fileName, filePath,fileHash,mimeType,assetId,
                        accessToken,userEmail,syncedAssetsFolderId);
                MainActivity.syncDetailsStatus = "";
            });

            backupThread.start();
            try {
                backupThread.join();
            } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }

        return isBackedUp[0];
    }


    private static List<String[]> getSortedAndroidFiles(){
        Log.d("service","getSortedAndroidFiles started");
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
        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
        Log.d("service7","getSortedAndroidFiles finished");
        return unique_android_rows;
    }

    public static void stopSync(Activity activity){
        try{
            TimerService.isTimerRunning = false;
            if (TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on")) {
                MainActivity.activity.getApplicationContext().stopService(MainActivity.serviceIntent);
            }
        }catch (Exception e) {  LogHandler.crashLog(e,"Service4"); }
    }

    public static void startSync(Context context){
        try{
            if (TimerService.isMyServiceRunning(context, TimerService.class).equals("off")){
                context.startService(MainActivity.serviceIntent);
            }
        }catch (Exception e) { LogHandler.crashLog(e,"Service5"); }
    }

    public static void startSync(Context context, Intent serviceIntent){
        try{
            if (TimerService.isMyServiceRunning(context, TimerService.class).equals("off")){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                }else{
                    context.startService(serviceIntent);
                }
            }
        }catch (Exception e) { LogHandler.crashLog(e,"Service6"); }
    }



    public static void checkForStatusChanges(Activity activity){
        Thread checkForStatusChangesThread = new Thread(() -> {
            Log.d("service","check for status changes");
            boolean isDeactivated = Deactivation.isDeactivationFileExists();
            Log.d("service","is deActivated : " + isDeactivated);
            if (isDeactivated){ UI.handleDeactivatedUser(); }
            Log.d("service","checkSupportBackupRequired");
            Support.backupDatabase(activity);
            if (isJsonChangeCheckRunning){
                return;
            }
            isJsonChangeCheckRunning = true;
            boolean isJsonHasChanged = Profile.hasJsonChanged();
            Log.d("service","is json has changed : " + isJsonHasChanged);
            if (isJsonHasChanged){
                try {
                    Thread t = new Thread(() -> {
                        Log.d("jsonChange","wait for upload complete");
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    t.start();
                    try {
                        t.join();
                    }catch (Exception e){
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Log.d("jsonChange","after upload complete");
                    DBHelper.updateDatabaseBasedOnJson();
                    UI.update("after json has changed in Sync checkStatusChange");
                } catch (Exception e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            }
            Sync.isJsonChangeCheckRunning = false;
        });
        checkForStatusChangesThread.start();
        try{
            checkForStatusChangesThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

}
