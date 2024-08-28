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
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class StorageSync {

    public static SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    public static String getAndroidId(Context context){
        return Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ANDROID_ID);
    }

    public static JsonObject createStorageJson(Context context){
        StorageHandler storageHandler = new StorageHandler();
        double freeSpace = storageHandler.getFreeSpace();
        double totalStorage = storageHandler.getTotalStorage();
        DBHelper dbHelper = DBHelper.getInstance(context);
        double mediaStorage = Double.parseDouble(DBHelper.getPhotosAndVideosStorage());
        double usedSpaceExcludingMedia = totalStorage - freeSpace - mediaStorage;

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("freeSpace", freeSpace);
        jsonObject.addProperty("mediaStorage", mediaStorage);
        jsonObject.addProperty("usedSpaceExcludingMedia", usedSpaceExcludingMedia);
        jsonObject.addProperty("updatedAt", formatter.format(System.currentTimeMillis()));
        return jsonObject;
    }

    public static void uploadStorageJsonFile(Context context, String userEmail, String accessToken, JsonObject storageJson) {
        Thread uploadStorageJsonFileThread = new Thread(() -> {
            Drive service = GoogleDrive.initializeDrive(accessToken);
            String fileName = "Storage_" + getAndroidId(context) + ".json";
            String folderName = GoogleDriveFolders.profileFolderName;
            String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, accessToken, true);
            Log.d("storageSync","profileFolderId: " + profileFolderId);
            String uploadFileId = "";
            try{
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);

                String contentString = storageJson.toString();
                Log.d("storageSync","storageJson Content : " + contentString);
                ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", contentString);

                uploadFileId = searchForExistingFile(service,fileName,profileFolderId);
                Log.d("storageSync","searching for older file result : " + uploadFileId);
                if (uploadFileId == null || uploadFileId.isEmpty()) {
                    Log.d("storageSync","upload file start");
                    fileMetadata.setParents(java.util.Collections.singletonList(profileFolderId));
                    com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();
                    uploadFileId = uploadedFile.getId();
                    Log.d("storageSync","upload file finished : " + uploadFileId);
                }else{
                    Log.d("storageSync","updating file start");
                    service.files().update(uploadFileId, fileMetadata, mediaContent).execute();
                    Log.d("storageSync","updating file finished");
                }

                Log.d("storage json" , "Upload file is : "+ uploadFileId);
            }catch (Exception e){
                Log.d("storageSync","failed to upload file : " + e.getLocalizedMessage());
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
            Log.d("storageSync","downloading -- profileFolderId: " + profileFolderId);
            try {
                String fileId = searchForExistingFile(service, fileName,profileFolderId);
                Log.d("storageSync","searching for file result : " + fileId);
                if (fileId != null) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        service.files().get(fileId)
                                .executeMediaAndDownloadTo(outputStream);

                        String jsonString = outputStream.toString();
                        jsonObjects[0] = JsonParser.parseString(jsonString).getAsJsonObject();
                        Log.d("storage json", "Downloaded storage JSON file: " + jsonString);
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
        DBHelper dbHelper = new DBHelper(context);
        String[] columnsName = new String[]{"userEmail", "refreshToken", "type"};
        List<String[]> accounts = DBHelper.getAccounts(columnsName);
        JsonObject storageJson = createStorageJson(context);
        Log.d("storageSync","created JSON storage for upload : " + storageJson);

        for (String[] account : accounts) {
            String userEmail = account[0];
            String refreshToken = account[1];
            String type = account[2];
            if (type.equals("backup")) {
                String accessToken = new GoogleCloud().updateAccessToken(refreshToken).getAccessToken();
                Log.d("storageSync","uploading to " + userEmail + "with access token : " + accessToken);
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
                Log.d("storageSync","downloading from " + userEmail);
                String accessToken = new GoogleCloud().updateAccessToken(refreshToken).getAccessToken();
                Log.d("storageSync","downloading from " + userEmail + "with access token : " + accessToken);
                JsonObject result = downloadStorageJsonFile(userEmail,accessToken,device);
                Log.d("storageSync","download result of account " + userEmail + " : " +  result);
                if (result!= null) {
                    return result;
                }
            }
        }
        return null;
    }

}
