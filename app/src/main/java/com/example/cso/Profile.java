package com.example.cso;

import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Profile {
    public static JsonObject createProfileMapContent(){
        JsonObject resultJson = new JsonObject();
        try{
            JsonArray backupAccountsJson = createBackUpAccountsJson();
            JsonArray primaryAccountsJson = createPrimaryAccountsJson();
            JsonArray deviceInfoJson = createDeviceInfoJson();

            resultJson.add("backupAccounts", backupAccountsJson);
            resultJson.add("primaryAccounts", primaryAccountsJson);
            resultJson.add("deviceInfo", deviceInfoJson);
        }catch (Exception e){
            LogHandler.saveLog("Failed to create profile map content : " + e.getLocalizedMessage() , true);
        }finally {
            return  resultJson;
        }
    }

    private static JsonArray createDeviceInfoJson(){
        JsonArray deviceInfoJson = new JsonArray();
        try{
            JsonObject deviceInfo = new JsonObject();
            deviceInfo.addProperty("deviceName", MainActivity.androidDeviceName);
            deviceInfo.addProperty("deviceId", MainActivity.androidUniqueDeviceIdentifier);
            deviceInfoJson.add(deviceInfo);

        }catch (Exception e){
            LogHandler.saveLog("Failed to create back up accounts json: " + e.getLocalizedMessage(), true);
        }finally {
            return deviceInfoJson;
        }
    }

    private static JsonArray createBackUpAccountsJson(){
        JsonArray backupAccountsJson = new JsonArray();
        try{
            List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","type","refreshToken"});
            for (String[] account_row : account_rows){
                if(account_row[1].equals("backup")) {
                    JsonObject backupAccount = new JsonObject();
                    backupAccount.addProperty("backupEmail", account_row[0]);
                    backupAccount.addProperty("refreshToken", account_row[2]);
                    backupAccountsJson.add(backupAccount);
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to create back up accounts json: " + e.getLocalizedMessage(), true);
        }finally {
            return backupAccountsJson;
        }
    }

    private static JsonArray createPrimaryAccountsJson(){
        JsonArray primaryAccountsJson = new JsonArray();
        try{
            List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","type","refreshToken"});
            for (String[] account_row : account_rows){
                if (account_row[1].equals("primary")){
                    JsonObject primaryAccount = new JsonObject();
                    primaryAccount.addProperty("primaryEmail", account_row[0]);
                    primaryAccount.addProperty("refreshToken", account_row[2]);
                    primaryAccountsJson.add(primaryAccount);
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to create primary accounts json: " + e.getLocalizedMessage(), true);
        }finally {
            return primaryAccountsJson;
        }
    }

    public static JsonObject readProfileMapContent(String userEmail, String accessToken) {
        JsonObject[] resultJson = {null};
        Thread readProdileMapContentThread = new Thread(() -> {
            try{
                Drive service = GoogleDrive.initializeDrive(accessToken);
                List<com.google.api.services.drive.model.File> existingFiles = getFilesInProfileFolder(service,userEmail, accessToken);
                for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                    for (int i = 0; i < 3; i++) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        service.files().get(existingFile.getId())
                                .executeMediaAndDownloadTo(outputStream);

                        String jsonString = outputStream.toString();
                        resultJson[0] = JsonParser.parseString(jsonString).getAsJsonObject();

                        LogHandler.saveLog("resultJson is: " + resultJson[0].toString() + " [Thread: " + Thread.currentThread().getName() + "]", false);

                        outputStream.close();
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to read profile map content : " + e.getLocalizedMessage(), true);
            }
        });
        readProdileMapContentThread.start();
        try{
            readProdileMapContentThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join readProdileMapContentThread: " + e.getLocalizedMessage(), true);
        }
        return resultJson[0];
    }

    public static String readProfileMapName(String userEmail, String accessToken) {
        String[] resultJsonName = {null};
        Thread readProdileMapContentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    System.out.println("access token in method readProfileMapName: " + accessToken);
                    Drive service = GoogleDrive.initializeDrive(accessToken);
                    System.out.println("user email in method readProfileMapName: " + userEmail);
                    List<com.google.api.services.drive.model.File> existingFiles = getFilesInProfileFolder(service,userEmail, accessToken);
                    System.out.println("size of files in profile folder: " + existingFiles.size());
                    for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                        resultJsonName[0] = existingFile.getName();
                        System.out.println("existing file name: " + existingFile.getName());
                        if (resultJsonName[0]!= null && !resultJsonName[0].isEmpty() && resultJsonName[0].contains("profileMap_")) {
                            System.out.println("result json name : " + resultJsonName[0] + " found. Exiting loop. ");
                            break;
                        }
                    }
                }catch (Exception e){
                    LogHandler.saveLog("Failed to read profile map content : " + e.getLocalizedMessage(), true);
                }
            }
        });
        readProdileMapContentThread.start();
        try {
            readProdileMapContentThread.join();
        } catch (InterruptedException e) {
            LogHandler.saveLog("Interrupted while waiting for read profile map content thread to finish: " + e.getLocalizedMessage());
        }
        return resultJsonName[0];
    }

    private static List<com.google.api.services.drive.model.File> getFilesInProfileFolder(Drive service, String userEmail, String accessToken){
        List<com.google.api.services.drive.model.File> existingFiles = null;
        try {
            String folderName = "stash_user_profile";
            String stashUserProfileFolderId = GoogleDrive.createOrGetSubDirectoryInStashSyncedAssetsFolder(userEmail, folderName, true, accessToken);
            System.out.println("stash user profile folder id: " + stashUserProfileFolderId);
            if (stashUserProfileFolderId != null && !stashUserProfileFolderId.isEmpty()) {
                FileList fileList = service.files().list()
                        .setQ("name contains 'profileMap_' and '" + stashUserProfileFolderId + "' in parents")
                        .setSpaces("drive")
                        .setFields("files(id,name)")
                        .execute();
                existingFiles = fileList.getFiles();
                System.out.println("size of files in profile folder: " + existingFiles.size());
                return existingFiles;
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get files in profile folder : " + e.getLocalizedMessage(), true);
        }
        return existingFiles;
    }

    public static boolean isLinkedToAccounts(JsonObject resultJson, String userEmail){
        final boolean[] isLinkedToAccounts = {false};
        Thread isLinkedToAccountsThread = new Thread(() -> {
            try{
                if(resultJson != null){
                    List<String[]> accountRows = DBHelper.getAccounts(new String[]{"userEmail", "type"});
                    List<String> backupAccountsInDevice = new ArrayList<>();
                    for (String[] accountRow : accountRows) {
                        if (accountRow[1].equals("backup")){
                            backupAccountsInDevice.add(accountRow[0]);
                        }

                    }
                    backupAccountsInDevice.add(userEmail);
                    System.out.println("backup accounts in device: " + backupAccountsInDevice);

                    JsonArray backupAccountsArray = resultJson.getAsJsonArray("backupAccounts");
                    System.out.println("backup accounts in json: " + backupAccountsArray);
                    for (JsonElement element : backupAccountsArray) {
                        JsonObject accountObject = element.getAsJsonObject();
                        String backupEmail = accountObject.get("backupEmail").getAsString();
                        if (!backupAccountsInDevice.contains(backupEmail)) {
                            System.out.println("A new email found:" + backupEmail);
                            isLinkedToAccounts[0] = true;
                        }
                    }

                    if (resultJson.has("deviceInfo")) {
                        JsonArray deviceInfoArray = resultJson.getAsJsonArray("deviceInfo");
                        for (JsonElement element : deviceInfoArray) {
                            JsonObject deviceInfoObject = element.getAsJsonObject();
                            String deviceIdentifier = deviceInfoObject.get("deviceId").getAsString();
                            if (!MainActivity.androidUniqueDeviceIdentifier.equals(deviceIdentifier)) {
                                System.out.println("A new device found:" + deviceIdentifier);
                                isLinkedToAccounts[0] = true;
                            }
                        }
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to check if it's a new profile: " + e.getLocalizedMessage(), true);
            }
        });
        isLinkedToAccountsThread.start();
        try {
            isLinkedToAccountsThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join sign in to isLinkedToAccounts thread : " + e.getLocalizedMessage(), true);
        }
        LogHandler.saveLog("isLinked is: " + isLinkedToAccounts[0], false);
        return isLinkedToAccounts[0];
    }

    public static boolean deleteProfileJson(String userEmail) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final boolean[] isDeleted = {false};
        Callable<Boolean> uploadTask = () -> {
            try {
                String driveBackupAccessToken;
                String driveBackupRefreshToken;
                String[] selected_columns = {"userEmail", "type","refreshToken"};
                List<String[]> account_rows = DBHelper.getAccounts(selected_columns);
                for (String[] account_row : account_rows) {
                    if (account_row[1].equals("backup") && account_row[0].equals(userEmail)) {
                        driveBackupRefreshToken = account_row[2];
                        driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();

                        Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                        String folder_name = "stash_user_profile";
                        String folderId = GoogleDrive.createOrGetSubDirectoryInStashSyncedAssetsFolder(userEmail,folder_name, false, null);
                        deleteProfileFiles(service, folderId);
                        isDeleted[0] = checkDeletionStatus(service, folderId);
                    }
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to upload profileMap from Android to backup in deleteProfileJson : " + e.getLocalizedMessage());
            }
            return isDeleted[0];
        };

        Future<Boolean> future = executor.submit(uploadTask);
        boolean isDeletedFuture = false;
        try {
            isDeletedFuture = future.get();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete profile Content from account : " + e.getLocalizedMessage());
        }
        return isDeletedFuture;
    }

    private static void deleteProfileFiles(Drive service, String folderId){
        try {
            FileList fileList = service.files().list()
                    .setQ("name contains 'profileMap' and '" + folderId + "' in parents")
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute();
            List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
            for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                service.files().delete(existingFile.getId()).execute();
            }
        }catch (Exception e) {
            LogHandler.saveLog("Failed to delete profile files: " + e.getLocalizedMessage(), true);
        }
    }

    private static boolean checkDeletionStatus(Drive service, String folderId){
        try{
            FileList fileList = service.files().list()
                    .setQ("name contains 'profileMap' and '" + folderId + "' in parents")
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute();
            List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
            if (existingFiles.size() == 0) {
                return true;
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to check deletion status of profile files: " + e.getLocalizedMessage(), true);
        }
        return false;
    }

    public static boolean backUpJsonFile(GoogleCloud.signInResult signInResult, ActivityResultLauncher<Intent> signInToBackUpLauncher){
        boolean[] isBackedUp = {false};
        Thread backUpJsonThread = new Thread(() -> {
            try{
                if (signInResult.getHandleStatus()) {
                    String userEmail = signInResult.getUserEmail();
                    GoogleCloud.Tokens tokens = signInResult.getTokens();
                    String refreshToken = tokens.getRefreshToken();
                    String accessToken = tokens.getAccessToken();


                    isBackedUp[0] = Profile.backUpProfileMap(false,"");
                }else{
                    LogHandler.saveLog("login with back up launcher failed with response code : " + signInResult.getHandleStatus());
                    MainActivity.activity.runOnUiThread(() -> {
                        LinearLayout backupButtonsLinearLayout = MainActivity.activity.findViewById(R.id.backUpAccountsButtons);
                        View child2 = backupButtonsLinearLayout.getChildAt(
                                backupButtonsLinearLayout.getChildCount() - 1);
                        if(child2 instanceof Button){
                            Button bt = (Button) child2;
                            bt.setText("ADD A BACK UP ACCOUNT");
                        }
                        UIHandler.updateButtonsListeners(signInToBackUpLauncher);
                    });
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to backup json file: " + e.getLocalizedMessage(), true);
            }
        });
        backUpJsonThread.start();
        try {
            backUpJsonThread.join();
        } catch (Exception e) {
            LogHandler.saveLog("failed to join backUpJsonThread in backup account: " + e.getLocalizedMessage());
        }
        return isBackedUp[0];
    }

    public static boolean backUpProfileMap(boolean hasRemoved, String signedOutEmail) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final boolean[] isBackedUp = {false};
        Callable<Boolean> uploadTask = () -> {
            try {
                String driveBackupAccessToken;
                String driveBackupRefreshToken;
                String[] selected_columns = {"userEmail", "type", "refreshToken"};
                List<String[]> account_rows = DBHelper.getAccounts(selected_columns);
                int backUpAccountCounts = 0;
                for (String[] account_row : account_rows) {
                    if (hasRemoved && account_row[0].equals(signedOutEmail)) {
                        continue;
                    }
                    if (account_row[1].equals("backup")){
                        backUpAccountCounts ++;
                        driveBackupRefreshToken = account_row[2];
                        driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();
                        Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                        String folder_name = "stash_user_profile";
                        String profileFolderId = GoogleDrive.createOrGetSubDirectoryInStashSyncedAssetsFolder(account_row[0],folder_name, false, null);
                        deleteProfileFiles(service, profileFolderId);
                        boolean isDeleted = checkDeletionStatus(service,profileFolderId);
                        if(isDeleted){
                            String uploadedFileId = setAndCreateProfileMapContent(service,profileFolderId);
                            if (uploadedFileId == null | uploadedFileId.isEmpty()) {
                                LogHandler.saveLog("Failed to upload profileMap from Android to backup because it's null");
                            }else{
                                isBackedUp[0] = true;
                            }
                        }
                    }
                }
                if(backUpAccountCounts == 0){
                    isBackedUp[0] = true;
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to upload profileMap from Android to backup in backUpProfileMap: " + e.getLocalizedMessage());
            }
            return isBackedUp[0];
        };

        Future<Boolean> future = executor.submit(uploadTask);
        boolean isBackedUpFuture = false;
        try{
            isBackedUpFuture = future.get();
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return isBackedUpFuture;
    }

    private static String setAndCreateProfileMapContent(Drive service,String profileFolderId){
        String uploadFileId = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = SharedPreferencesHandler.getJsonModifiedTime(MainActivity.preferences);
        String currentDate = formatter.format(date);
        String fileName = "profileMap_" + currentDate + ".json";
        try{
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(java.util.Collections.singletonList(profileFolderId));
            String content = Profile.createProfileMapContent().toString();
            ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", content);
            com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            uploadFileId = uploadedFile.getId();
        }catch (Exception e){
            LogHandler.saveLog("Failed to set profile map content:" + e.getLocalizedMessage(), true);
        }finally {
            return uploadFileId;
        }
    }

    public static void detachAccount(JsonObject profileMapContent,String userEmail){
        Thread detachLinkedAccountsProfileJsonThread = new Thread(() -> {
            JsonArray backupAccounts =  profileMapContent.get("backupAccounts").getAsJsonArray();
            JsonObject editedProfileContent = editProfileJson(profileMapContent,userEmail);

            for (int i = 0;i < backupAccounts.size();i++){
                JsonObject backupAccount = backupAccounts.get(i).getAsJsonObject();
                String linkedUserEmail = backupAccount.get("backupEmail").getAsString();
                String refreshToken = backupAccount.get("refreshToken").getAsString();
                if (linkedUserEmail.equals(userEmail)){
                    continue;
                }
                LogHandler.saveLog("Starting to detach account with email (backUpProfileMapToLinkedAccounts) : " + linkedUserEmail,false);
                backUpProfileMapToLinkedAccounts(editedProfileContent,refreshToken,userEmail);
                LogHandler.saveLog("Finished detaching account with email (backUpProfileMapToLinkedAccounts) : " + linkedUserEmail,false);
            }
        });

        detachLinkedAccountsProfileJsonThread.start();
        try {
            detachLinkedAccountsProfileJsonThread.join();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to join detachLinkedAccountsProfileJsonThread: " + e.getLocalizedMessage());
        }
    }

    public static JsonObject editProfileJson(JsonObject profileJson, String emailToDelete) {
        try {
            JsonArray backupAccounts = profileJson.get("backupAccounts").getAsJsonArray();
            profileJson.remove("backupAccounts");
            JsonArray updatedBackupAccounts = new JsonArray();

            for (int i = 0; i < backupAccounts.size(); i++) {
                JsonObject accountObject = backupAccounts.get(i).getAsJsonObject();
                if (!accountObject.get("backupEmail").getAsString().equals(emailToDelete)) {
                    updatedBackupAccounts.add(accountObject);
                }
            }

            profileJson.add("backupAccounts", updatedBackupAccounts);
            return profileJson;

        } catch (Exception e) {
            LogHandler.saveLog("Failed to edit profile json : " + e.getLocalizedMessage());
            return null;
        }
    }

    private static String uploadProfileContent(Drive service, String profileFolderId, JsonObject profileContent){
        String uploadFileId = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = SharedPreferencesHandler.getJsonModifiedTime(MainActivity.preferences);
        String currentDate = formatter.format(date);
        String fileName = "profileMap_" + currentDate + ".json";
        try{
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(java.util.Collections.singletonList(profileFolderId));
            String content = profileContent.toString();
            ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", content);
            com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            uploadFileId = uploadedFile.getId();
        }catch (Exception e){
            LogHandler.saveLog("Failed to set profile map content:" + e.getLocalizedMessage(), true);
        }finally {
            return uploadFileId;
        }
    }

    public static boolean backUpProfileMapToLinkedAccounts(JsonObject profileContent,String refreshToken,String userEmail) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final boolean[] isBackedUp = {false};
        Callable<Boolean> uploadTask = () -> {
            try {
                String driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                String folder_name = "stash_user_profile";
                String profileFolderId = GoogleDrive.createOrGetSubDirectoryInStashSyncedAssetsFolder(userEmail,folder_name, true, driveBackupAccessToken);
                deleteProfileFiles(service, profileFolderId);
                boolean isDeleted = checkDeletionStatus(service,profileFolderId);
                if(isDeleted){
                    String uploadedFileId = uploadProfileContent(service,profileFolderId, profileContent);
                    if (uploadedFileId == null | uploadedFileId.isEmpty()) {
                        LogHandler.saveLog("Failed to upload profileMap from Android to backup because it's null");
                    }else{
                        isBackedUp[0] = true;
                    }
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to upload profileMap from Android to backup in backUpProfileMap: " + e.getLocalizedMessage());
            }
            return isBackedUp[0];
        };

        Future<Boolean> future = executor.submit(uploadTask);
        boolean isBackedUpFuture = false;
        try{
            isBackedUpFuture = future.get();
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return isBackedUpFuture;
    }

    public static boolean hasJsonChanged(){
        List<String[]> accounts = DBHelper.getAccounts(new String[]{"userEmail","type","refreshToken"});
        for (String[] account : accounts) {
            if (account[1].equals("backup")) {
                String resultJsonName = readProfileMapName(account[0], MainActivity.googleCloud.updateAccessToken(account[2]).getAccessToken());
                if (resultJsonName!= null &&!resultJsonName.isEmpty()){
                    continue;
                }
                Date lastModifiedDateOfJson = convertFileNameToTimeStamp(resultJsonName);
                Date lastModifiedDateOfPreferences =  SharedPreferencesHandler.getJsonModifiedTime(MainActivity.preferences);
                if (lastModifiedDateOfJson!= null && lastModifiedDateOfPreferences!= null && lastModifiedDateOfJson.after(lastModifiedDateOfPreferences)){
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public static Date convertFileNameToTimeStamp(String fileName){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = fileName.substring(fileName.indexOf('_') + 1, fileName.lastIndexOf('.'));
            return dateFormat.parse(timestamp);
        }catch (Exception e){
            LogHandler.saveLog("Failed to convert filename ("+fileName +") to timestamp : " + e.getLocalizedMessage(), true);
            return null;
        }
    }


}
