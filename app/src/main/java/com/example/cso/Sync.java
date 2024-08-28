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
                List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","type",
                        "totalStorage","usedStorage", "refreshToken","usedStorage","totalStorage"});
                boolean accountExists = DBHelper.anyBackupAccountExists();
                boolean isAllOfAccountsFull = true;

                for(String[] account_row: account_rows){
                    double freeSpace = GoogleDrive.calculateDriveFreeSpace(account_row);
                    Log.d("service","free space of " + account_row[0] + " : " + freeSpace);
                    boolean isAccountFull = true;
                    if(freeSpace > 50) {
                        isAccountFull = false;
                    }

                    if(!isAccountFull){
                        isAllOfAccountsFull = false;
                        String type = account_row[1];
                        if(type.equals("backup")){
                        syncAndroidToBackupAccount(freeSpace,account_row[0], account_row[4],
                                context, activity);
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

    private static void syncAndroidToBackupAccount(double driveFreeSpace, String userEmail,
                                                   String refreshToken, Context context,
                                                   Activity activity){
        Log.d("Threads","syncAndroidToBackupAccount  method started");
        try{
            String folderName = GoogleDriveFolders.assetsFolderName;
            String syncedAssetsSubFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, null, false);
            if(syncedAssetsSubFolderId == null){
                Log.d("service","asset folder not found");
                return;
            }

            List<String[]> sortedAndroidFiles = getSortedAndroidFiles();

            for (String[] androidRow : sortedAndroidFiles) {
                double amountSpaceToFreeUp = StorageHandler.getAmountSpaceToFreeUp();
                Log.d("service","amountSpaceToFreeUp : " + amountSpaceToFreeUp);
                syncAndroidFile(androidRow, userEmail, refreshToken, syncedAssetsSubFolderId,
                        driveFreeSpace, amountSpaceToFreeUp, context, activity);
            }
        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
    }


    private static void syncAndroidFile(String[] androidRow, String userEmail, String refreshToken,
                                        String syncedAssetsFolderId, double driveFreeSpace,
                                        double amountSpaceToFreeUp, Context context, Activity activity){
        final double[] finalAmountSpaceToFreeUp = {amountSpaceToFreeUp};
        final double[] finalDriveFreeSpace = {driveFreeSpace};
        Thread syncAndroidFileThread = new Thread( () -> {
            try{
                Log.d("Threads","syncAndroidFileThread started");

                if (shouldSyncFile(context)){
                    String fileName = androidRow[1];
                    String filePath = androidRow[2];
                    String fileSize = androidRow[4];
                    String fileHash = androidRow[5];
                    Long assetId = Long.valueOf(androidRow[8]);

                    MainActivity.activity.runOnUiThread( () -> {
                        //fill animation here
                    });
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
                }
            }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
        });

        syncAndroidFileThread.start();
        try{
            syncAndroidFileThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        Log.d("Threads","syncAndroidFileThread finished");
    }

    private static boolean shouldSyncFile(Context context) {
        boolean isWifiOnlySwitchOn = SharedPreferencesHandler.getWifiOnlySwitchState();
        boolean isWifiConnected = InternetManager.getInternetStatus(context).equals("wifi");

        return isWifiOnlySwitchOn && isWifiConnected || !isWifiOnlySwitchOn;
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
        }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }

        return unique_android_rows;
    }

    public static void stopSync(){
        try{
            MainActivity.activity.getApplicationContext().stopService(MainActivity.serviceIntent);
        }catch (Exception e) {  FirebaseCrashlytics.getInstance().recordException(e); }
    }

    public static void startSync(Activity activity){
        try{
            activity.getApplicationContext().startService(MainActivity.serviceIntent);
        }catch (Exception e) {  FirebaseCrashlytics.getInstance().recordException(e); }
    }
}
