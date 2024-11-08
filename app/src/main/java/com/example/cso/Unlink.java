package com.example.cso;

import static com.example.cso.GoogleDrive.moveFileBetweenAccounts;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.example.cso.UI.Accounts;
import com.example.cso.UI.Devices;
import com.example.cso.UI.UI;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;

public class Unlink {
    public static void unlinkSingleAccount(String sourceUserEmail, Drive sourceDriveService,boolean completeMove
            , boolean needToNotify, View lastButton){
        Thread unlinkSingleAccountThread = new Thread( () -> {
            Log.d("Threads","unlinkSingleAccountThread started");
            Log.d("unlinkNotify"," needToNotify : " + needToNotify);
            if (needToNotify){
                String accessToken = DBHelper.getDriveBackupAccessToken(sourceUserEmail);
                String unlinkedFolderId = GoogleDriveFolders.getSubFolderId(sourceUserEmail,
                        GoogleDriveFolders.unlinkedFolderName,accessToken,true);
                    Log.d("unlinkNotify","create folder : " + unlinkedFolderId);
                if (unlinkedFolderId != null){
                    uploadDevicesJsonToNotify(sourceUserEmail,unlinkedFolderId);
                    Log.d("unlinkNotify","devices json uploaded");
                    DBHelper.deleteAccountAndRelatedAssets(sourceUserEmail);
                    GoogleDrive.startThreads();
                    unlinkDevices();
                    Accounts.accountNumbers = Accounts.accountNumbers - 1;
                }
            }else{
                GoogleDrive.deleteDriveFolders(sourceDriveService,sourceUserEmail,completeMove);

                boolean isRevoked = GoogleCloud.invalidateToken(sourceUserEmail);
                if (isRevoked){
                    Accounts.accountNumbers = Accounts.accountNumbers - 1;
                    DBHelper.deleteAccountAndRelatedAssets(sourceUserEmail);
                    GoogleDrive.startThreads();
                }
            }
        });
        unlinkSingleAccountThread.start();
        try{
            unlinkSingleAccountThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }finally {
            MainActivity.activity.runOnUiThread(() -> {
                if(lastButton != null){
                    lastButton.setClickable(true);
                }
            });
        }
        UI.update("end of unlink single account");
        Log.d("Threads","unlinkSingleAccountThread finished");
    }

    public static void unlinkAccount(String sourceUserEmail,boolean ableToMoveAllAssets, Activity activity, View lastButton){
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
                    Unlink.unlinkSingleAccount(sourceUserEmail,service,ableToMoveAllAssets,false, lastButton);
                    Log.d("Unlink", "end of unlink from single account (every thing is ok)");
                }
            }else{
                if (InternetManager.getInternetStatus(activity).equals("noInternet")){
                    UI.makeToast("Failed To Unlink, Check your Internet Connection !!! ");
                }else{
                    UI.makeToast("Failed To Unlink, Try Again !!! ");
                }
            }
        });

        unlinkAccountThread.start();
        try{
            unlinkAccountThread.join();
        }
        catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }finally {
            MainActivity.activity.runOnUiThread(() -> {
                lastButton.setClickable(true);
            });
        }
        UI.update("end of unlink account (not single account)");
        Log.d("Unlink", "unlinkAccountThread finished");
    }

    public static double getTotalLinkedCloudsFreeSpace(String userEmail){
        double totalFreeSpace = 0;
        try{
            List<String[]> accounts = DBHelper.getAccounts(new String[]{"userEmail","totalStorage","usedStorage","type"});

            for (String[] account : accounts) {
                String type = account[3];
                if (!account[0].equals(userEmail) && type.equals("backup")){
                    double totalStorage = Double.valueOf(account[1]);
                    double usedStorage = Double.valueOf(account[2]);
                    totalFreeSpace += totalStorage - usedStorage;
                }
            }
        }catch (Exception e) {
            LogHandler.crashLog(e,"unlink");
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

    public static void unlinkDevices(){
        ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromDB();
        for (DeviceHandler deviceHandler : devices){
            if (!Devices.isCurrentDevice(deviceHandler.deviceId)){
                DeviceHandler.deleteDevice(deviceHandler.deviceId);
            }
        }
    }

    public static void uploadDevicesJsonToNotify(String userEmail, String folderId) {
        try {
            ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromDB();
            for (DeviceHandler device : devices){
                if (!Devices.isCurrentDevice(device.getDeviceId())){
                    boolean success;
                    int counter = 5;
                    do{
                        String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);
                        Drive service = GoogleDrive.initializeDrive(accessToken);
                        Log.d("unlinkNotify","uploadDevicesJsonToNotify for "+ device.getDeviceId() + " to " + userEmail);
                        success = uploadUnlinkedDeviceFile(service,folderId,device.getDeviceId());
                        if (success){
                            break;
                        }
                        counter--;
                    } while (counter != 0);
                }
            }
        }catch (Exception e){
            LogHandler.crashLog(e,"unlinkNotify");
        }
    }

    private static boolean uploadUnlinkedDeviceFile(Drive service,String folderId, String deviceId){
        final String[] uploadFileId = {null};
        Thread setAndCreateProfileMapContentThread = new Thread( () -> {
            try{
                String fileName = "unlinked_" + deviceId + ".json";
                try{
                    com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                    fileMetadata.setName(fileName);
                    fileMetadata.setParents(java.util.Collections.singletonList(folderId));
                    ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", "{}");
                    Log.d("unlinkNotify","uploading Notify file is : " + fileName);
                    com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();

                    uploadFileId[0] = uploadedFile.getId();
                    Log.d("unlink" , "Upload file is (unlinkedDeviceFile) : "+ uploadFileId[0]);
                }catch (Exception e){
                    LogHandler.crashLog(e,"unlink");
                }
            }catch (Exception e) { LogHandler.crashLog(e,"unlink"); }
        });

        setAndCreateProfileMapContentThread.start();
        try{
            setAndCreateProfileMapContentThread.join();
        }catch (Exception e) { LogHandler.crashLog(e,"unlink"); }
        if (uploadFileId[0] == null){
            return false;
        }
        return true;
    }

    public static boolean isUnlinkedFolderExists(String userEmail, String accessToken) {
        String[] resultJsonName = {null};

        Thread readProdileMapContentThread = new Thread( () -> {
            try{
                Drive service = GoogleDrive.initializeDrive(accessToken);
                List<File> existingFiles = GoogleDriveFolders.getUnlinkedDevicesFile(userEmail, service, accessToken);
                for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                    String name = existingFile.getName();
                    Log.d("unlinkNotify","checking file : " + name);
                    existingFiles.hashCode();
                    boolean shouldDelete = (name!= null && !name.isEmpty() && name.contains(MainActivity.androidUniqueDeviceIdentifier));
                    Log.d("unlinkNotify","should delete file " + name + " : " + shouldDelete);
                    if (shouldDelete){
                        service.files().delete(existingFile.getId()).execute();
                        resultJsonName[0] = name;
                        break;
                    }
                }
                Log.d("unlinkNotify","founded file size : " + existingFiles.size());
                if (existingFiles.size() == 1){
                    Log.d("unlinkNotify","unlink single account because this is last linked device");
                    unlinkSingleAccount(userEmail,service,false,false, null);
                    Log.d("unlinkNotify","unlink single account because this is last linked device finished");
                }
            }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
        });

        readProdileMapContentThread.start();
        try {
            readProdileMapContentThread.join();
        } catch (InterruptedException e) { FirebaseCrashlytics.getInstance().recordException(e); }
        return resultJsonName[0] != null;
    }

}
