package com.example.cso;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class StorageSync {

    public static String getAndroidId(Context context){
        return Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ANDROID_ID);
    }

    public static JsonObject createStorageJson(Context context){
        StorageHandler storageHandler = new StorageHandler();
        double freeSpace = storageHandler.getFreeSpace();
        double totalStorage = storageHandler.getTotalStorage();
        DBHelper dbHelper = DBHelper.getInstance(context);
        double mediaStorage = Double.parseDouble(dbHelper.getPhotosAndVideosStorage());
        double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("freeSpace", freeSpace);
        jsonObject.addProperty("mediaStorage", mediaStorage);
        jsonObject.addProperty("usedSpaceExcludingMedia", usedSpaceExcludingMedia);
        jsonObject.addProperty("updatedAt", System.currentTimeMillis());
        return jsonObject;
    }

    public static void uploadStorageJsonFile(Context context, String userEmail, String accessToken, JsonObject storageJson) {
        Thread uploadStorageJsonFileThread = new Thread(() -> {
            Drive service = GoogleDrive.initializeDrive(accessToken);
            String fileName = "Storage_" + getAndroidId(context) + ".json";
            String folderName = GoogleDriveFolders.profileFolderName;
            String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, accessToken, true);
            String uploadFileId = "";
            try{
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(java.util.Collections.singletonList(profileFolderId));

                String contentString = storageJson.toString();
                ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", contentString);

                uploadFileId = searchForExistingFile(service,fileName,profileFolderId);
                if (uploadFileId == null || uploadFileId.isEmpty()) {
                    com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();
                    uploadFileId = uploadedFile.getId();
                }else{
                    service.files().update(uploadFileId, fileMetadata, mediaContent).execute();
                }

                Log.d("storage json" , "Upload file is : "+ uploadFileId);
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });

        uploadStorageJsonFileThread.start();
        try{
            uploadStorageJsonFileThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static String searchForExistingFile(Drive service, String fileName, String folderId){
        String[] fileId = {null};
        Thread searchForExistingFileThread = new Thread(() -> {
            try {
                List<File> files = service.files().list()
                        .setQ("name='" + fileName + "' and '" + folderId + "' in parents")
                        .setFields("files(id)")
                        .execute()
                        .getFiles();

                if (!files.isEmpty()) {
                    fileId[0] = files.get(0).getId();
                }
            } catch (IOException e) {
                System.out.println("Error searching for existing file: " + e.getLocalizedMessage());
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

    public static JsonObject downloadStorageJsonFile(String userEmail, String accessToken, DeviceHandler device) {
        JsonObject[] jsonObjects = {null};
        Thread downloadStorageJsonThread = new Thread(() -> {
            Drive service = GoogleDrive.initializeDrive(accessToken);
            String fileName = "Storage_" + device.getDeviceId() + ".json";
            String folderName = GoogleDriveFolders.profileFolderName;
            String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, accessToken, true);
            try {
                String fileId = searchForExistingFile(service, fileName,profileFolderId);
                if (fileId != null) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        service.files().get(fileId)
                                .executeMediaAndDownloadTo(outputStream);

                        String jsonString = outputStream.toString();
                        jsonObjects[0] = JsonParser.parseString(jsonString).getAsJsonObject();
                    }catch (Exception e){
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }finally {
                        outputStream.close();
                    }
                }
            } catch (IOException e) {
                System.out.println("Error downloading storage JSON file: " + e.getLocalizedMessage());
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        downloadStorageJsonThread.start();
        try{
            downloadStorageJsonThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return jsonObjects[0];
    }

    public static void uploadStorageJsonFileToAccounts(Context context) {
        String[] columnsName = new String[]{"userEmail", "refreshToken", "type"};
        DBHelper dbHelper = DBHelper.getInstance(context);
        List<String[]> accounts = DBHelper.getAccounts(columnsName);
        JsonObject storageJson = createStorageJson(context);

        for (String[] account : accounts) {
            String userEmail = account[0];
            String refreshToken = account[1];
            String type = account[2];
            if (type.equals("backup")) {
                GoogleCloud googleCloud = new GoogleCloud( (FragmentActivity) context);
                String accessToken = googleCloud.updateAccessToken(refreshToken).getAccessToken();
                uploadStorageJsonFile(context, userEmail, accessToken, storageJson);
            }
        }
    }

    public static JsonObject downloadStorageJsonFileFromAccounts(DeviceHandler device) {
        String[] columnsName = new String[]{"userEmail", "refreshToken", "type"};
        List<String[]> accounts = DBHelper.getAccounts(columnsName);
        for (String[] account : accounts) {
            String userEmail = account[0];
            String refreshToken = account[1];
            String type = account[2];
            if (type.equals("backup")) {
                String accessToken = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                JsonObject result = downloadStorageJsonFile(userEmail,accessToken,device);
                if (result!= null) {
                    return result;
                }
            }
        }
        return null;
    }

}
