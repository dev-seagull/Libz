package com.example.cso;

import static com.example.cso.GoogleDrive.moveFileBetweenAccounts;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.example.cso.UI.UI;
import com.google.api.services.drive.Drive;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

public class Unlink {
    public static void unlinkSingleAccount(String sourceUserEmail, Drive sourceDriveService,boolean completeMove){
        Thread unlinkSingleAccountThread = new Thread( () -> {
            Log.d("Threads","unlinkSingleAccountThread started");
            GoogleDrive.deleteDriveFolders(sourceDriveService,sourceUserEmail,completeMove);

            boolean isRevoked = GoogleCloud.invalidateToken(sourceUserEmail);
            if (isRevoked){
                DBHelper.deleteAccountAndRelatedAssets(sourceUserEmail);
                GoogleDrive.startThreads();
            }
        });
        unlinkSingleAccountThread.start();
        try{
            unlinkSingleAccountThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        UI.update(); // end of unlink single account
        Log.d("Threads","unlinkSingleAccountThread finished");
    }

    public static void unlinkAccount(String sourceUserEmail,boolean ableToMoveAllAssets, Activity activity){
        boolean[] allAssetsMovedSuccessfully = {true};
        Thread unlinkAccountThread = new Thread(() -> {
            Log.d("unlink","unlinkAccountThread started");
            GoogleDrive.startThreads();

            String sourceAccessToken = DBHelper.getDriveBackupAccessToken(sourceUserEmail);
            Drive service = GoogleDrive.initializeDrive(sourceAccessToken);

            List<String[]> accounts_rows = DBHelper.getAccounts(new String[]{"refreshToken","userEmail","type"});
            String[] driveColumns = {"fileHash", "id","assetId", "fileId", "fileName", "userEmail"};
            List<String[]> drive_rows = DBHelper.getDriveTable(driveColumns, sourceUserEmail);

            Drive targetDriveService = null;
            for (String[] drive_row : drive_rows){
                String fileId = drive_row[3];
                boolean isAssetMoved = false;
                boolean hasNetworkError = false;
                for (String[] account : accounts_rows) {
                    String type = account[2];
                    String targetUserEmail = account[1];
                    if (!type.equals("backup") || targetUserEmail.equals(sourceUserEmail)){
                        continue;
                    }

                    Log.d("unlink", "moving file " + drive_row[4] + " to " + targetUserEmail + " started");
                    if (targetDriveService == null){
                        String targetRefreshToken = account[0];
                        String targetAccessToken = GoogleCloud.updateAccessToken(targetRefreshToken).getAccessToken();
                        targetDriveService = GoogleDrive.initializeDrive(targetAccessToken);
                    }

                    String moveResult = moveFileBetweenAccounts(service, targetDriveService, sourceUserEmail, targetUserEmail, fileId);
                    Log.d("unlink", "moving file " + drive_row[4] + " to " + targetUserEmail + " finished : " + moveResult);
                    if (moveResult.equals("success")) {
                        isAssetMoved = true;
                        break;
                    }else{
                        if (moveResult.equals("failure")) {
                            hasNetworkError = true;
                        }
                        targetDriveService = null;
                    }
                }
                if (!isAssetMoved && hasNetworkError){
                    allAssetsMovedSuccessfully[0] = false;
                    if (InternetManager.getInternetStatus(activity).equals("noInternet")){
                        break;
                    }
                }
                if (!isAssetMoved && ableToMoveAllAssets) {
                    allAssetsMovedSuccessfully[0] = false;
                }
            }
            Log.d("unlink", "all files moved with result : " + allAssetsMovedSuccessfully[0]);

            if (allAssetsMovedSuccessfully[0]){
                SharedPreferencesHandler.setJsonModifiedTime(MainActivity.preferences);
                Log.d("unlink", "starting to back up json to remaining accounts");
                boolean isBackedUpAndDeleted = Profile.backupJsonToRemainingAccounts(sourceUserEmail);
                Log.d("unlink", "end of back up json to remaining accounts : " + isBackedUpAndDeleted);
                if(isBackedUpAndDeleted){
                    Log.d("Unlink", "unlink from single account after moving files and backup");
                    Unlink.unlinkSingleAccount(sourceUserEmail,service,ableToMoveAllAssets);
                    Log.d("Unlink", "end of unlink from single account (every thing is ok)");
                }
            }else{
                if (InternetManager.getInternetStatus(activity).equals("noInternet")){
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Failed To Unlink, Check your Internet Connection !!! ", Toast.LENGTH_LONG).show());
                }else{
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Failed To Unlink, Try Again !!! ", Toast.LENGTH_LONG).show());
                }
            }
        });

        unlinkAccountThread.start();
        try{
            unlinkAccountThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        UI.update(); // end of unlink account
        Log.d("Unlink", "unlinkAccountThread finished");
    }


    public static double getTotalLinkedCloudsFreeSpace(String userEmail){
        int totalFreeSpace = 0;
        try{
            List<String[]> accounts = DBHelper.getAccounts(new String[]{"userEmail","totalStorage","usedStorage","type"});

            for (String[] account : accounts) {
                String type = account[3];
                if (!account[0].equals(userEmail) && type.equals("backup")){
                    int totalStorage = Integer.parseInt(account[1]);
                    int usedStorage = Integer.parseInt(account[2]);
                    totalFreeSpace += totalStorage - usedStorage;
                }
            }
        }catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return totalFreeSpace;
    }

    public static boolean isSingleUnlink(String userEmail){
        try{
            List<String[]> accounts = DBHelper.getAccounts(new String[]{"userEmail","type"});

            for (String[] account : accounts) {
                String type = account[1];
                if (!account[0].equals(userEmail) && type.equals("backup")){
                    return false;
                }
            }
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        return true;
    }

}
