package com.example.cso;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.example.cso.UI.Devices;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeviceStatusSync {
    public static String TAG  = "DeviceStatusSync";
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    public static long timeInterval = 8 * 60 * 60 * 1000;

    public static void uploadDeviceStatusJsonFileToAccounts(Context context) {
        DBHelper.getInstance(context);
        new Thread(() -> {
            String fileName = "DeviceStatus_" + getAndroidId(context) + ".json";
            String[] columnsName = new String[]{"userEmail", "refreshToken", "type"};
            List<String[]> accounts = DBHelper.getAccounts(columnsName);
            JsonObject deviceStatusJson = createDeviceStatusJson();
            Log.d("storageSync","created JSON storage for upload : " + deviceStatusJson);

            for (String[] account : accounts) {
                String userEmail = account[0];
                String refreshToken = account[1];
                String type = account[2];
                if (type.equals("backup")) {
                    String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
                    Log.d("storageSync","uploading to " + userEmail + "with access token : " + accessToken);
                    uploadDeviceStatusJsonFile(userEmail, accessToken, deviceStatusJson, fileName);
                }
            }
        }).start();
    }

    public static void uploadDeviceStatusJsonFile(String userEmail, String accessToken, JsonObject deviceStatus, String fileName) {
        Thread uploadDeviceStatusJsonFileThread = new Thread(() -> {
            Drive service = GoogleDrive.initializeDrive(accessToken);
            String folderName = GoogleDriveFolders.profileFolderName;
            String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, accessToken, true);
            Log.d(TAG,"profileFolderId: " + profileFolderId);
            String uploadFileId ;
            try{
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);

                String contentString = deviceStatus.toString();
                Log.d(TAG,"deviceStatus Content : " + contentString);
                ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", contentString);

                uploadFileId = searchForExistingFile(service,fileName,profileFolderId);
                Log.d(TAG,"searching for older file result : " + uploadFileId);
                if (uploadFileId == null || uploadFileId.isEmpty()) {
                    Log.d(TAG,"upload file start");
                    fileMetadata.setParents(java.util.Collections.singletonList(profileFolderId));
                    com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();
                    uploadFileId = uploadedFile.getId();
                    Log.d(TAG,"upload file finished : " + uploadFileId);
                }else{
                    Log.d(TAG,"updating file start");
                    service.files().update(uploadFileId, fileMetadata, mediaContent).execute();
                    Log.d(TAG,"updating file finished");
                }

                Log.d(TAG , "Upload file is : "+ uploadFileId);
            }catch (Exception e){
                Log.d(TAG,"failed to upload file : " + e.getLocalizedMessage());
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });

        uploadDeviceStatusJsonFileThread.start();
        try{
            uploadDeviceStatusJsonFileThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static String getAndroidId(Context context){
        return Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ANDROID_ID);
    }

    public static JsonObject createDeviceStatusJson(){
        JsonObject[] jsonObjects = {new JsonObject()};
        Thread createDeviceStatusJsonThread = new Thread(() -> {
            Date currentTime = new Date();
            String dateString = sdf.format(currentTime);
            JsonObject deviceStatusJson = new JsonObject();
            deviceStatusJson.add("storageStatus", createStorageStatusJson());
            deviceStatusJson.add("assetsLocationStatus", createAssetsLocationStatusJson());
            deviceStatusJson.add("assetsSourceStatus", createAssetsSourceStatusJson());
            deviceStatusJson.addProperty("updateTime", dateString);
            Log.d(TAG,"Full device status : " + deviceStatusJson);
            jsonObjects[0] = deviceStatusJson;
        });
        createDeviceStatusJsonThread.start();
        try{
            createDeviceStatusJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,TAG);
        }
        return jsonObjects[0];
    }

    public static JsonObject createStorageStatusJson(){
        JsonObject[] jsonObjects = {new JsonObject()};
        Thread createStorageStatusJsonThread = new Thread(() -> {
            StorageHandler storageHandler = new StorageHandler();
            double totalStorage = storageHandler.getTotalStorage();
            double freeSpace = storageHandler.getFreeSpace();
            double usedSpace = totalStorage - freeSpace;

            double mediaStorage = DBHelper.getPhotosAndVideosStorageOnThisDevice();
            double syncedAssetsStorage = DBHelper.getSizeOfSyncedAssetsOnThisDevice();

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("totalStorage", totalStorage);
            jsonObject.addProperty("freeSpace", freeSpace);
            jsonObject.addProperty("mediaStorage", mediaStorage);
            jsonObject.addProperty("usedSpace", usedSpace);
            jsonObject.addProperty("syncedAssetsStorage", syncedAssetsStorage);
            Log.d("AreaSquareChart","storageStatus : " + jsonObject);
            jsonObjects[0] = jsonObject;
        });
        createStorageStatusJsonThread.start();
        try{
            createStorageStatusJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"AreaSquareChart");
        }
        return jsonObjects[0];
    }

    public static JsonObject createAssetsLocationStatusJson(){
        JsonObject[] jsonObjects = {new JsonObject()};
        Thread createAssetsLocationStatusJsonThread = new Thread(() -> {
            ArrayList<String[]> files = (ArrayList<String[]>) DBHelper.getAndroidTableOnThisDevice(
                    new String[]{"assetId","fileSize"}, MainActivity.androidUniqueDeviceIdentifier);
            if (files.isEmpty()){
                return;
            }
            HashMap<String, Double> locationSizes = new HashMap<>();
            List<String[]> accounts = DBHelper.getAccounts(new String[]{"userEmail","type"});
            double totalAccSize = 0.0;
            for (String[] account : accounts){
                String userEmail = account[0];
                String type = account[1];
                if (type.equals("backup")){
                    locationSizes.put(userEmail,0.0);
                    double accSizeOnDevice = DBHelper.getSizeOfSyncedAssetsFromAccountWithoutDistinctAndroid(userEmail);
                    Log.d("UserEmailSize", userEmail + ": " + accSizeOnDevice);
                    totalAccSize = totalAccSize + accSizeOnDevice;
                    locationSizes.put(userEmail,accSizeOnDevice);
                }
            }

            double mediaStorage = DBHelper.getPhotosAndVideosStorageOnThisDevice();
            double syncedAssetsStorage = DBHelper.getSizeOfSyncedAssetsOnThisDevice();
            double unsynced = ( mediaStorage - syncedAssetsStorage ) * 1024;
            locationSizes.put("UnSynced", unsynced);
            JsonObject assetsLocationSize = new JsonObject();
            for (Map.Entry<String, Double> entry : locationSizes.entrySet()){
                assetsLocationSize.addProperty(entry.getKey(), entry.getValue());
            }
            Log.d(TAG,"assetsLocationStatus : " + assetsLocationSize);
            jsonObjects[0] = assetsLocationSize;
        });
        createAssetsLocationStatusJsonThread.start();
        try{
            createAssetsLocationStatusJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,TAG);
        }
        return jsonObjects[0];
    }

    public static JsonObject createAssetsSourceStatusJson() {
        JsonObject[] assetsSourceSizeJson = new JsonObject[]{new JsonObject()};
        Thread createAssetsSourceStatusJsonThread = new Thread(() -> {
            String rootPath = "/storage/emulated/0";
            HashMap<String, List<String>> folderTypeMap = new HashMap<>();

            folderTypeMap.put("Screenshots", List.of("DCIM/Screenshots",
                    "Pictures/Screenshots", "Screenshots"));
            folderTypeMap.put("Screen Recorder", List.of("DCIM/ScreenRecorder",
                    "Pictures/ScreenRecorder", "ScreenRecorder"));
            folderTypeMap.put("Camera", List.of("DCIM/Camera", "Camera", "Pictures/Camera"));
            folderTypeMap.put("Documents", List.of("Documents"));
            folderTypeMap.put("Downloads", List.of("Download", "Downloads"));
            folderTypeMap.put("Movies", List.of("Movies"));
            folderTypeMap.put("Telegram", List.of("Telegram"));
            folderTypeMap.put("WhatsApp", List.of("WhatsApp"));

            for (String folderType : folderTypeMap.keySet()) {
                double totalFolderSize = 0;

                for (String folderPath : folderTypeMap.get(folderType)) {
                    String pattern = rootPath + "/" + folderPath + "/%.%";
                    double folderSize = DBHelper.getRootFolderMediaSize(pattern);
                    totalFolderSize += folderSize;
                }

                if (totalFolderSize > 0) {
                    assetsSourceSizeJson[0].addProperty(folderType, totalFolderSize);
                }
            }
            // this is for root
            double folderSize = DBHelper.getRootFolderMediaExcludeChildrenSize(rootPath);
            if (folderSize > 0) {
                assetsSourceSizeJson[0].addProperty("Root", folderSize);
            }

            Log.d(TAG, "assetsSourceSize : " + assetsSourceSizeJson[0]);
        });

        createAssetsSourceStatusJsonThread.start();

        try {
            createAssetsSourceStatusJsonThread.join();
        } catch (Exception e) {
            LogHandler.crashLog(e,TAG);
        }

        return assetsSourceSizeJson[0];
    }

    public static JsonObject getDeviceStatusJsonFile(String deviceId) {
        JsonObject[] jsonObjects = {new JsonObject()};
        Thread getDeviceStatusJsonFileThread = new Thread(() -> {
            String fileName = "DeviceStatus_" + deviceId + ".json";
            jsonObjects[0] = SharedPreferencesHandler.getDeviceStatus(fileName);
            if (jsonObjects[0] != null) {
                Log.d(TAG, "data already have saved ;Reading content from " + fileName);
                if (shouldDownloadBasedOnUpdateTime(fileName)) {
                    Log.d(TAG, "File needs to be updated");
                    jsonObjects[0] = downloadDeviceStatusJsonFromAccounts(fileName);
                }
            }else{
                Log.d(TAG, "File does not exist. Downloading from accounts");
                jsonObjects[0] = downloadDeviceStatusJsonFromAccounts(fileName);
            }
        });
        getDeviceStatusJsonFileThread.start();
        try{
            getDeviceStatusJsonFileThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,TAG);
        }
        return jsonObjects[0];
    }

    public static JsonObject downloadDeviceStatusJsonFromAccounts(String fileName){
        JsonObject[] jsonObjects = {new JsonObject()};
        Thread downloadDeviceStatusJsonFromAccountsThread = new Thread(() -> {
            String[] columnsName = new String[]{"userEmail", "refreshToken", "type"};
            List<String[]> accounts = DBHelper.getAccounts(columnsName);
            for (String[] account : accounts) {
                String userEmail = account[0];
                String refreshToken = account[1];
                String type = account[2];
                if (type.equals("backup")) {
                    Log.d(TAG,"downloading from " + userEmail);
                    String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
                    Log.d(TAG,"downloading from " + userEmail + "with access token : " + accessToken);
                    jsonObjects[0] = downloadDeviceStatusJson(userEmail,accessToken,fileName);
                    Log.d(TAG,"download result of account " + userEmail + " : " +  jsonObjects[0]);
                    if (jsonObjects[0] != null) {
                        SharedPreferencesHandler.setDeviceStatus(fileName, jsonObjects[0]);
                        break;
                    }
                }
            }
        });
        downloadDeviceStatusJsonFromAccountsThread.start();
        try{
            downloadDeviceStatusJsonFromAccountsThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,TAG);
        }
        return jsonObjects[0];
    }

    public static JsonObject downloadDeviceStatusJson(String userEmail, String accessToken, String fileName) {
        JsonObject[] jsonObjects = {new JsonObject()};
        Thread downloadDeviceStatusJsonThread = new Thread(() -> {
            Drive service = GoogleDrive.initializeDrive(accessToken);

            String folderName = GoogleDriveFolders.profileFolderName;
            String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, accessToken, true);
            Log.d(TAG,"downloading -- profileFolderId: " + profileFolderId);
            try {
                String fileId = searchForExistingFile(service, fileName,profileFolderId);
                Log.d(TAG,"searching for file result : " + fileId);
                if (fileId != null) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        service.files().get(fileId)
                                .executeMediaAndDownloadTo(outputStream);

                        String jsonString = outputStream.toString();
                        jsonObjects[0] = JsonParser.parseString(jsonString).getAsJsonObject();
                        Log.d(TAG, "Downloaded storage JSON file: " + jsonString);
                    }catch (Exception e){
                        LogHandler.crashLog(e,TAG);
                    }finally {
                        outputStream.close();
                    }
                }
            } catch (Exception e) {
                LogHandler.crashLog(e,TAG);
            }
        });
        downloadDeviceStatusJsonThread.start();
        try{
            downloadDeviceStatusJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,TAG);
        }
        return jsonObjects[0];
    }

    public static String searchForExistingFile(Drive service, String fileName, String folderId){
        String[] fileId = {null};
        Thread searchForExistingFileThread = new Thread(() -> {
            Log.d(TAG, "searchForExistingFile");
            try {
                List<File> files = service.files().list()
                        .setQ("name='" + fileName + "' and '" + folderId + "' in parents")
                        .setFields("files(id)")
                        .execute()
                        .getFiles();

                if (!files.isEmpty()) {
                    fileId[0] = files.get(0).getId();
                    Log.d(TAG, "older device status file found");
                }else{
                    Log.d(TAG, "older device status file not found");
                }
            } catch (IOException e) {
                LogHandler.crashLog(e,TAG);
            }
        });
        searchForExistingFileThread.start();
        try{
            searchForExistingFileThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,TAG);
        }
        return fileId[0];
    }

    public static boolean shouldDownloadBasedOnUpdateTime(String fileName) {
        boolean[] shouldDownload = {true};
        Thread shouldDownloadBasedOnUpdateTimeThread = new Thread(() -> {
            try{
                JsonObject jsonObject = SharedPreferencesHandler.getDeviceStatus(fileName);
                String updateTime = jsonObject.get("updateTime").getAsString();

                Date updateTimeDate = sdf.parse(updateTime);
                Date currentTime = new Date();
                long timeDifference = currentTime.getTime() - updateTimeDate.getTime();
                if (timeDifference < timeInterval){
                    shouldDownload[0] = false;
                }
            }catch (Exception e){
                LogHandler.crashLog(e,TAG);
            }
        });
        shouldDownloadBasedOnUpdateTimeThread.start();
        try{
            shouldDownloadBasedOnUpdateTimeThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,TAG);
        }
        return shouldDownload[0];
    }

    public static String getDeviceStatusLastUpdateTime(String deviceId){
        SimpleDateFormat showDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault());
        try{
            Log.d(TAG,"getDeviceStatusLastUpdateTime Started");
            if (Devices.isCurrentDevice(deviceId)){
                Date now = new Date();
                return "as of " + showDateFormat.format(now);
            }
            JsonObject deviceStatus = getDeviceStatusJsonFile(deviceId);
            String updateTime = deviceStatus.get("updateTime").getAsString();
            Log.d(TAG,"for device : " + deviceId + " update time is : " + updateTime);
            Date updateTimeDate = sdf.parse(updateTime);
            return "as of " + showDateFormat.format(updateTimeDate);
        }catch (Exception e){
            LogHandler.crashLog(e,TAG);
        }
        return "Updated Long time ago";
    }

}
