package com.example.cso;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.cso.UI.UI;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Sync {
//    private static boolean isAllOfAccountsFull = true;
    public static boolean isJsonChangeCheckRunning = false;
    public static boolean isAnyBackUpAccountExistsToastShown = false;
    public static boolean isInternetConnectionToastShown = false;
    public static boolean isAllOfAccountsFullToastShown = false;
    public static long lastToastTime = 0;
    public static boolean isAllOfAccountsFull = true;
    public static long toastInterval = 10000;
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
                        double freeSpace = GoogleDrive.calculateDriveFreeSpace(account_row);
                        Log.d("service","free space of " + account_row[0] + " : " + freeSpace);
                        boolean isAccountFull = (freeSpace < 50);
                        if(!isAccountFull){
                            isAllOfAccountsFull = false;
                            syncAndroidToBackupAccount(freeSpace,account_row[0], account_row[4],
                                    activity);
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
                            Toast.makeText(MainActivity.activity,"Sync failed! You are running out of space." +
                                    " Add more back up accounts.", Toast.LENGTH_SHORT).show();
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

    private static void syncAndroidToBackupAccount(double driveFreeSpace, String userEmail,
                                                   String refreshToken, Activity activity){
        Log.d("Threads","syncAndroidToBackupAccount  method started");
        try{
            String folderName = GoogleDriveFolders.assetsFolderName;
            String syncedAssetsSubFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, null, false);
            if(syncedAssetsSubFolderId == null){
                Log.d("service","asset folder not found");
                return;
            }

            List<String[]> sortedAndroidFiles = getSortedAndroidFiles();
            double amountSpaceToFreeUp = StorageHandler.getAmountSpaceToFreeUp();

            int syncedFilesCount = 0 ;
            double syncedFilesSpace = 0 ;

            new Thread( () -> checkForStatusChanges(activity)).start();

            for (String[] androidRow : sortedAndroidFiles) {
                if (!TimerService.isTimerRunning){
                    Log.d("service","sync Stopped suddenly");
                    break;
                }
                String internetConnectionStatus = getInternetStatusForSync(activity);
                boolean anyBackUpaAccountExists = DBHelper.anyBackupAccountExists();
//                Log.d("service","isAllOfAccountsFull : " + isAllOfAccountsFull);
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
                        MainActivity.activity.runOnUiThread(() -> {
                            isAnyBackUpAccountExistsToastShown = true;
                            if (TimerService.isAppinForeGround){
                                Toast.makeText(MainActivity.activity, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
                }
                if(!internetConnectionStatus.equals("wifi") && !internetConnectionStatus.equals("data")) {
                    isAnyBackUpAccountExistsToastShown = false;
                    String message = internetConnectionStatus;
                    if (!isInternetConnectionToastShown) {
                        MainActivity.activity.runOnUiThread(() -> {
                            isInternetConnectionToastShown = true;
                            if (TimerService.isAppinForeGround){
                                Toast.makeText(MainActivity.activity, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
                }
                isAnyBackUpAccountExistsToastShown = false;
                isInternetConnectionToastShown = false;

                Log.d("service","amountSpaceToFreeUp : " + amountSpaceToFreeUp);
                double fileSize = Double.parseDouble(androidRow[4]);
                amountSpaceToFreeUp  = amountSpaceToFreeUp - fileSize;
                syncedFilesSpace += fileSize;
                syncedFilesCount ++;

                if (syncedFilesSpace >= 1000 || syncedFilesCount >= 40){
                    syncedFilesSpace = 0;
                    syncedFilesCount = 0;
                    new Thread( () -> checkForStatusChanges(activity)).start();
                }
                syncAndroidFile(androidRow, userEmail, refreshToken, syncedAssetsSubFolderId,
                        driveFreeSpace,amountSpaceToFreeUp , activity);
            }
        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
    }


    private static void syncAndroidFile(String[] androidRow, String userEmail, String refreshToken,
                                        String syncedAssetsFolderId, double driveFreeSpace,
                                        double amountSpaceToFreeUp, Activity activity){
        final double[] finalAmountSpaceToFreeUp = {amountSpaceToFreeUp};
        final double[] finalDriveFreeSpace = {driveFreeSpace};
        Thread syncAndroidFileThread = new Thread( () -> {
            try{
                Log.d("Threads","syncAndroidFileThread started");

                String fileName = androidRow[1];
                String filePath = androidRow[2];
                String fileSize = androidRow[4];
                String fileHash = androidRow[5];
                Long assetId = Long.valueOf(androidRow[8]);

                if (!DBHelper.androidFileExistsInDrive(assetId, fileHash)){
                    if(finalDriveFreeSpace[0] > Double.parseDouble(fileSize)) {
                        String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
                        boolean isBackedUp = uploadAndroidToDrive(androidRow, userEmail, accessToken, syncedAssetsFolderId);
                        if (isBackedUp) {
                            Log.d("service" ,fileName + " is backedup");
                            GoogleDrive.startUpdateDriveFilesThread();
                            finalDriveFreeSpace[0] -= Double.parseDouble(fileSize);

                            if(finalAmountSpaceToFreeUp[0] > 0){
                                GoogleDrive.deleteRedundantDriveFilesFromAccount(userEmail);
                                if (DBHelper.androidFileExistsInDrive(assetId, fileHash)) {
                                    boolean isDeleted = Android.deleteAndroidFile(filePath, String.valueOf(assetId), fileHash
                                            , fileSize, fileName, activity);
                                    if (isDeleted) {
                                        finalAmountSpaceToFreeUp[0] -= Double.parseDouble(fileSize);
                                        Log.d("service" ,fileName + " is deleted");
                                    }
                                }
                            }
                        }
                    }
                }else {
                    if (finalAmountSpaceToFreeUp[0] > 0) {
                        GoogleDrive.deleteRedundantDriveFilesFromAccount(userEmail);
                        if (DBHelper.androidFileExistsInDrive(assetId, fileHash)) {
                            boolean isDeleted = Android.deleteAndroidFile(filePath, androidRow[8], fileHash
                                    , fileSize, androidRow[1], activity);
                            if (isDeleted) {
                                Log.d("service" ,fileName + " is deleted");
                                finalAmountSpaceToFreeUp[0] -= Double.parseDouble(fileSize);
                            }
                        }
                    }
                }
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
//                SyncDetails.setSyncStatusDetailsTextView(activity, false);
                BackUp backUp = new BackUp();
                isBackedUp[0] = backUp.backupAndroidToDrive(fileId,fileName, filePath,fileHash,mimeType,assetId,
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
        Log.d("service","getSortedAndroidFiles finished");
        return unique_android_rows;
    }

    public static void stopSync(Activity activity){
        try{
            TimerService.isTimerRunning = false;
            if (TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on")) {
                MainActivity.activity.getApplicationContext().stopService(MainActivity.serviceIntent);
            }
        }catch (Exception e) {  LogHandler.crashLog(e,"Service"); }
    }

    public static void startSync(Activity activity){
        try{
            if (TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("off")){
                activity.getApplicationContext().startService(MainActivity.serviceIntent);
            }
        }catch (Exception e) { LogHandler.crashLog(e,"Service"); }
    }


    public static void checkForStatusChanges(Activity activity){
        Thread checkForStatusChangesThread = new Thread(() -> {
            Log.d("service","check for status changes");
            boolean isDeactivated = Deactivation.isDeactivationFileExists();
            Log.d("service","is deActivated : " + isDeactivated);
            if (isDeactivated){ UI.handleDeactivatedUser(); }
            Log.d("service","checkSupportBackupRequired");
            Support.checkSupportBackupRequired(activity);
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
                    UI.update(); // after json has changed
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
