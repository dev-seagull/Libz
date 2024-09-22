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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeviceStatusSync {

    public static String rootPath = MainActivity.activity.getPackageResourcePath();
    public static java.io.File rootDirectory = new java.io.File(rootPath);
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    public static long timeInterval = 1 * 2 * 60 * 1000;

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
        JsonObject[] jsonObjects = {null};
        Thread createDeviceStatusJsonThread = new Thread(() -> {
            Date currentTime = new Date();
            String dateString = sdf.format(currentTime);
            JsonObject deviceStatusJson = new JsonObject();
            deviceStatusJson.add("storageStatus", createStorageStatusJson());
            deviceStatusJson.add("assetsLocationStatus", createAssetsLocationStatusJson());
            deviceStatusJson.add("assetsSourceStatus", createAssetsSourceStatusJson());
            deviceStatusJson.addProperty("updateTime", dateString);
            Log.d("DeviceStatusSync","Full device status : " + deviceStatusJson);
            jsonObjects[0] = deviceStatusJson;
        });
        createDeviceStatusJsonThread.start();
        try{
            createDeviceStatusJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
        return jsonObjects[0];
    }

    public static JsonObject createStorageStatusJson(){
        JsonObject[] jsonObjects = {null};
        Thread createStorageStatusJsonThread = new Thread(() -> {
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
            jsonObjects[0] = jsonObject;
        });
        createStorageStatusJsonThread.start();
        try{
            createStorageStatusJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
        return jsonObjects[0];
    }

    public static JsonObject createAssetsLocationStatusJson(){
        JsonObject[] jsonObjects = {null};
        Thread createAssetsLocationStatusJsonThread = new Thread(() -> {
            ArrayList<String[]> files = (ArrayList<String[]>) DBHelper.getAndroidTable(new String[]{"assetId","fileSize"});
            if (files.isEmpty()){
                return;
            }
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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    locationSizes.put(userEmail,locationSizes.getOrDefault(userEmail,0.0) + fileSize);
                                }
                                files.remove(androidFile);
                                break;
                            }
                        }
                    }
                }
            }
            for (String[] file : files){
                Double fileSize = Double.parseDouble(file[1]);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    locationSizes.put("UnSynced",locationSizes.getOrDefault("UnSynced", 0.0) + fileSize);
                }
            }
            JsonObject assetsLocationSize = new JsonObject();
            for (Map.Entry<String, Double> entry : locationSizes.entrySet()){
                assetsLocationSize.addProperty(entry.getKey(), entry.getValue());
            }
            Log.d("DeviceStatusSync","assetsLocationStatus : " + assetsLocationSize);
            jsonObjects[0] = assetsLocationSize;
        });
        createAssetsLocationStatusJsonThread.start();
        try{
            createAssetsLocationStatusJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
        return jsonObjects[0];
    }

    public static JsonObject createAssetsSourceStatusJson(){
        JsonObject[] jsonObjects = {null};
        Thread createAssetsSourceStatusJsonThread = new Thread(() -> {
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
            jsonObjects[0] = assetsSourceSize;
        });
        return jsonObjects[0];
    }

    public static JsonObject getDeviceStatusJsonFile(String deviceId) {
        JsonObject[] jsonObjects = {null};
        Thread getDeviceStatusJsonFileThread = new Thread(() -> {
            String fileName = "DeviceStatus_" + deviceId + ".json";

            if (doesFileExist(fileName)) {
                Log.d("DeviceStatusSync", "File already exists. Reading content from " + fileName);
                if (shouldDownloadBasedOnUpdateTime(fileName)) {
                    Log.d("DeviceStatusSync", "File needs to be updated");
                    jsonObjects[0] = downloadDeviceStatusJsonFromAccounts(fileName,"update");
                } else {
                    jsonObjects[0] = readFileContentAsJson(fileName);
                }
            }else{
                Log.d("DeviceStatusSync", "File does not exist. Downloading from accounts");
                jsonObjects[0] = downloadDeviceStatusJsonFromAccounts(fileName,"create");
            }
        });
        getDeviceStatusJsonFileThread.start();
        try{
            getDeviceStatusJsonFileThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
        return jsonObjects[0];
    }

    public static JsonObject downloadDeviceStatusJsonFromAccounts(String fileName, String status){
        JsonObject[] jsonObjects = {null};
        Thread downloadDeviceStatusJsonFromAccountsThread = new Thread(() -> {
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
                    jsonObjects[0] = downloadDeviceStatusJson(userEmail,accessToken,fileName);
                    Log.d("DeviceStatusSync","download result of account " + userEmail + " : " +  jsonObjects[0]);
                    if (jsonObjects[0] != null) {
                        if (status.equals("create")){
                            createFileWithJson(fileName, jsonObjects[0]);
                        }else if(status.equals("update")){
                            updateFileWithJson(fileName, jsonObjects[0]);
                        }
                        break;
                    }
                }
            }
        });
        downloadDeviceStatusJsonFromAccountsThread.start();
        try{
            downloadDeviceStatusJsonFromAccountsThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
        return jsonObjects[0];
    }

    public static JsonObject downloadDeviceStatusJson(String userEmail, String accessToken, String fileName) {
        JsonObject[] jsonObjects = {null};
        Thread downloadDeviceStatusJsonThread = new Thread(() -> {
            Drive service = GoogleDrive.initializeDrive(accessToken);

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
                        LogHandler.crashLog(e,"DeviceStatusSync");
                    }finally {
                        outputStream.close();
                    }
                }
            } catch (Exception e) {
                LogHandler.crashLog(e,"DeviceStatusSync");
            }
        });
        downloadDeviceStatusJsonThread.start();
        try{
            downloadDeviceStatusJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
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
                LogHandler.crashLog(e,"DeviceStatusSync");
            }
        });
        searchForExistingFileThread.start();
        try{
            searchForExistingFileThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
        return fileId[0];
    }

    public static boolean doesFileExist(String fileName) {
        java.io.File file = new java.io.File(rootDirectory, fileName);
        return file.exists();
    }

    public static void createFileWithJson(String fileName, JsonObject jsonObject) {
        Thread createFileWithJsonThread = new Thread(() -> {
            java.io.File file = createNewDeviceStatusFile(fileName);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
                String[] lines = jsonObject.toString().split(System.lineSeparator());
                for (String existingLine : lines) {
                    writer.write(existingLine);
                    writer.newLine();
                }
                Log.d("DeviceStatusSync", "File " + fileName + " created with content.");
            }catch (Exception e){
                LogHandler.crashLog(e,"DeviceStatusSync");
            }
        });
        createFileWithJsonThread.start();
        try{
            createFileWithJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
    }

    public static JsonObject readFileContentAsJson(String fileName) {
        JsonObject[] jsonObjects = {null};
        Thread readFileContentAsJsonThread = new Thread(() -> {
            java.io.File file = new java.io.File(rootDirectory, fileName);
            String content = "";
            if (!file.exists()) {
                Log.d("DeviceStatusSync", "File " + fileName + " does not exist.");
                return ;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content += line;
                }
            } catch (Exception e) {
                LogHandler.crashLog(e,"DeviceStatusJson");
                return ;
            }

            jsonObjects[0] = JsonParser.parseString(content).getAsJsonObject();
        });
        readFileContentAsJsonThread.start();
        try{
            readFileContentAsJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
        return jsonObjects[0];


    }

    public static java.io.File createNewDeviceStatusFile(String fileName){
        java.io.File[] files = {null};
        Thread createNewDeviceStatusFileThread = new Thread(() -> {
            java.io.File file = new java.io.File(rootDirectory, fileName);
            if (!file.exists()){
                try{
                    boolean resultOfCreation = file.createNewFile();
                    if (resultOfCreation) {
                        if (file.exists()){
                            files[0] = file;
                        }
                    }else {
                        boolean resultOfDeletion = file.delete();
                        files[0] = createNewDeviceStatusFile(fileName);
                    }
                }catch (Exception e){
                    LogHandler.crashLog(e,"DeviceStatusSync");
                }
            }else{
                try{
                    boolean resultOfCreation = file.createNewFile();
                    if (resultOfCreation) {
                        if (file.exists()){
                            files[0] = file;
                        }
                    }else {
                        boolean resultOfDeletion = file.delete();
                        files[0] = createNewDeviceStatusFile(fileName);
                    }
                }catch (Exception e){
                    LogHandler.crashLog(e,"DeviceStatusSync");
                }
            }
        });
        createNewDeviceStatusFileThread.start();
        try{
            createNewDeviceStatusFileThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
        return files[0];
    }

    public static void updateFileWithJson(String fileName, JsonObject jsonObject) {
        Thread updateFileWithJsonThread = new Thread( () -> {
            java.io.File file = new java.io.File(rootDirectory, fileName);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,false)))) { // `false` overwrites existing content
                String[] lines = jsonObject.toString().split(System.lineSeparator());
                for (String existingLine : lines) {
                    writer.write(existingLine);
                    writer.newLine();
                }
                Log.d("DeviceStatusSync", "File " + fileName + " updated with new content.");
            } catch (Exception e) {
                Log.d("DeviceStatusSync", "Error while updating file: " + e.getMessage());
            }
        });
        updateFileWithJsonThread.start();
        try{
            updateFileWithJsonThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
    }

    public static boolean shouldDownloadBasedOnUpdateTime(String fileName) {
        boolean[] shouldDownload = {true};
        Thread shouldDownloadBasedOnUpdateTimeThread = new Thread(() -> {
            java.io.File file = new java.io.File(rootDirectory, fileName);
            String content = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content += line;
                }
                JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
                String updateTime = jsonObject.get("updateTime").getAsString();

                Date updateTimeDate = sdf.parse(updateTime);
                Date currentTime = new Date();
                long timeDifference = currentTime.getTime() - updateTimeDate.getTime();
                if (timeDifference < timeInterval){
                    shouldDownload[0] = false;
                }
            }catch (Exception e){
                LogHandler.crashLog(e,"DeviceStatusSync");
            }
        });
        shouldDownloadBasedOnUpdateTimeThread.start();
        try{
            shouldDownloadBasedOnUpdateTimeThread.join();
        }catch (Exception e){
            LogHandler.crashLog(e,"DeviceStatusSync");
        }
        return shouldDownload[0];
    }

}
