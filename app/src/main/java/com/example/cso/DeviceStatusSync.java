package com.example.cso;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceStatusSync {

    public static void uploadDeviceStatusJsonFileToAccounts(Context context) {
        DBHelper.getInstance(context);
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
    }

    public static void uploadDeviceStatusJsonFile(String userEmail, String accessToken, JsonObject deviceStatus, String fileName) {
        Thread uploadDeviceStatusJsonFileThread = new Thread(() -> {
            Drive service = GoogleDrive.initializeDrive(accessToken);
            String folderName = GoogleDriveFolders.profileFolderName;
            String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, accessToken, true);
            Log.d("DeviceStatusSync","profileFolderId: " + profileFolderId);
            String uploadFileId ;
            try{
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);

                String contentString = deviceStatus.toString();
                Log.d("DeviceStatusSync","deviceStatus Content : " + contentString);
                ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", contentString);

                uploadFileId = searchForExistingFile(service,fileName,profileFolderId);
                Log.d("DeviceStatusSync","searching for older file result : " + uploadFileId);
                if (uploadFileId == null || uploadFileId.isEmpty()) {
                    Log.d("DeviceStatusSync","upload file start");
                    fileMetadata.setParents(java.util.Collections.singletonList(profileFolderId));
                    com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();
                    uploadFileId = uploadedFile.getId();
                    Log.d("DeviceStatusSync","upload file finished : " + uploadFileId);
                }else{
                    Log.d("DeviceStatusSync","updating file start");
                    service.files().update(uploadFileId, fileMetadata, mediaContent).execute();
                    Log.d("DeviceStatusSync","updating file finished");
                }

                Log.d("DeviceStatusSync" , "Upload file is : "+ uploadFileId);
            }catch (Exception e){
                Log.d("DeviceStatusSync","failed to upload file : " + e.getLocalizedMessage());
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
        JsonObject deviceStatusJson = new JsonObject();
        deviceStatusJson.add("storageStatus", createStorageStatusJson());
        deviceStatusJson.add("assetsLocationStatus", createAssetsLocationStatusJson());
        deviceStatusJson.add("assetsSourceStatus", createAssetsSourceStatusJson());
        Log.d("DeviceStatusSync","Full device status : " + deviceStatusJson);
        return deviceStatusJson;
    }

    public static JsonObject createStorageStatusJson(){
        StorageHandler storageHandler = new StorageHandler();
        double freeSpace = storageHandler.getFreeSpace();
        double totalStorage = storageHandler.getTotalStorage();
        double mediaStorage = Double.parseDouble(DBHelper.getPhotosAndVideosStorage());
        double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("freeSpace", freeSpace);
        jsonObject.addProperty("mediaStorage", mediaStorage);
        jsonObject.addProperty("usedSpaceExcludingMedia", usedSpaceExcludingMedia);
        Log.d("DeviceStatusSync","storageStatus : " + jsonObject);
        return jsonObject;
    }

    public static JsonObject createAssetsLocationStatusJson(){
        ArrayList<String[]> files = (ArrayList<String[]>) DBHelper.getAndroidTable(new String[]{"assetId","fileSize"});
        HashMap<String, Double> locationSizes = new HashMap<>();
        List<String[]> accounts = DBHelper.getAccounts(new String[]{"userEmail","type"});
        for (String[] account : accounts){
            String userEmail = account[0];
            String type = account[1];
            if (type.equals("backup")){
                locationSizes.put(userEmail,0.0);
                List<String[]> driveFiles = DBHelper.getDriveTable(new String[]{"assetId"},userEmail);
                for (String[] driveFile : driveFiles){
                    String driveFileAssetId = driveFile[0];
                    for (String[] androidFile : files){
                        String androidFileAssetId = androidFile[0];
                        if (androidFileAssetId.equals(driveFileAssetId)){
                            Double fileSize = Double.parseDouble(androidFile[1]);
                            locationSizes.put(userEmail,locationSizes.get(userEmail) + fileSize);
                            files.remove(androidFile);
                            break;
                        }
                    }
                }
            }
        }
        JsonObject assetsLocationSize = new JsonObject();
        for (Map.Entry<String, Double> entry : locationSizes.entrySet()){
            assetsLocationSize.addProperty(entry.getKey(), entry.getValue());
        }
        Log.d("DeviceStatusSync","assetsLocationStatus : " + assetsLocationSize);
        return assetsLocationSize;
    }

    public static JsonObject createAssetsSourceStatusJson(){
        List<String[]> files = DBHelper.getAndroidTable(new String[]{"filePath","fileSize"});
        HashMap<String, Double> sourceSizeMap = new HashMap<>();

        String[] sources = {"Telegram", "Photos" , "Downloads" , "Camera" ,
                "Screenshots", "Screen Recorded"};

        for (String[] file : files){
            String filePath = file[0].toLowerCase();
            Double fileSize = Double.parseDouble(file[1]);
            String sourceName = "Others";

            for (String source : sources){
                String keyword = "/" + source.toLowerCase() + "/";
                if (filePath.contains(keyword)){
                    sourceName = source;
                    break;
                }
            }

            if (sourceName.equals("Others")){
                String[] splitPath = filePath.split("/");
                sourceName = "Others/" + splitPath[splitPath.length-1];
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sourceSizeMap.put(sourceName, sourceSizeMap.getOrDefault(sourceName, 0.0) + fileSize);
            }
        }

        JsonObject assetsSourceSize = new JsonObject();
        for (Map.Entry<String, Double> entry : sourceSizeMap.entrySet()){
            assetsSourceSize.addProperty(entry.getKey(), entry.getValue());
        }
        Log.d("DeviceStatusSync","assetsSourceSize : " + assetsSourceSize);
        return assetsSourceSize;
    }

    public static JsonObject downloadDeviceStatusJsonFileFromAccounts(String deviceId) {
        String[] columnsName = new String[]{"userEmail", "refreshToken", "type"};
        List<String[]> accounts = DBHelper.getAccounts(columnsName);
        for (String[] account : accounts) {
            String userEmail = account[0];
            String refreshToken = account[1];
            String type = account[2];
            if (type.equals("backup")) {
                Log.d("DeviceStatusSync","downloading from " + userEmail);
                String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
                Log.d("DeviceStatusSync","downloading from " + userEmail + "with access token : " + accessToken);
                JsonObject result = downloadDeviceStatusJson(userEmail,accessToken,deviceId);
                Log.d("DeviceStatusSync","download result of account " + userEmail + " : " +  result);
                if (result!= null) {
                    return result;
                }
            }
        }
        return null;
    }

    public static JsonObject downloadDeviceStatusJson(String userEmail, String accessToken, String deviceId) {
        JsonObject[] jsonObjects = {null};
        Thread downloadDeviceStatusJsonThread = new Thread(() -> {
            Drive service = GoogleDrive.initializeDrive(accessToken);
            String fileName = "Storage_" + deviceId + ".json";
            String folderName = GoogleDriveFolders.profileFolderName;
            String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, accessToken, true);
            Log.d("DeviceStatusSync","downloading -- profileFolderId: " + profileFolderId);
            try {
                String fileId = searchForExistingFile(service, fileName,profileFolderId);
                Log.d("DeviceStatusSync","searching for file result : " + fileId);
                if (fileId != null) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        service.files().get(fileId)
                                .executeMediaAndDownloadTo(outputStream);

                        String jsonString = outputStream.toString();
                        jsonObjects[0] = JsonParser.parseString(jsonString).getAsJsonObject();
                        Log.d("DeviceStatusSync", "Downloaded storage JSON file: " + jsonString);
                    }catch (Exception e){
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }finally {
                        outputStream.close();
                    }
                }
            } catch (IOException e) {
                Log.d("DeviceStatusSync","Error downloading storage JSON file: " + e.getLocalizedMessage());
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        downloadDeviceStatusJsonThread.start();
        try{
            downloadDeviceStatusJsonThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return jsonObjects[0];
    }

    public static String searchForExistingFile(Drive service, String fileName, String folderId){
        String[] fileId = {null};
        Thread searchForExistingFileThread = new Thread(() -> {
            Log.d("DeviceStatusSync", "searchForExistingFile");
            try {
                List<File> files = service.files().list()
                        .setQ("name='" + fileName + "' and '" + folderId + "' in parents")
                        .setFields("files(id)")
                        .execute()
                        .getFiles();

                if (!files.isEmpty()) {
                    fileId[0] = files.get(0).getId();
                    Log.d("DeviceStatusSync", "older device status file found");
                }else{
                    Log.d("DeviceStatusSync", "older device status file not found");
                }
            } catch (IOException e) {
                Log.d("DeviceStatusSync","Error searching for existing file: " + e.getLocalizedMessage());
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        searchForExistingFileThread.start();
        try{
            searchForExistingFileThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return fileId[0];
    }

}
