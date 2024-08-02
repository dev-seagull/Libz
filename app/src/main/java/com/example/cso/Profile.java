package com.example.cso;

import static com.example.cso.MainActivity.signInToBackUpLauncher;

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
                List<com.google.api.services.drive.model.File> existingFiles = getFilesInProfileFolder(userEmail, accessToken);
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
                    System.out.println("user email in method readProfileMapName: " + userEmail);
                    List<com.google.api.services.drive.model.File> existingFiles = getFilesInProfileFolder(userEmail, accessToken);
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

    private static List<com.google.api.services.drive.model.File> getFilesInProfileFolder(String userEmail, String accessToken){
        List<com.google.api.services.drive.model.File> existingFiles = null;
        try {
            Drive service = GoogleDrive.initializeDrive(accessToken);

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

    private static void deleteProfileFile(String userEmail, String accessToken, boolean deleteOlderProfileFiles){
        Thread deleteProfileFileThread = new Thread(() -> {
            try {
                Drive service = GoogleDrive.initializeDrive(accessToken);
                String folder_name = "stash_user_profile";
                String folderId = GoogleDrive.createOrGetSubDirectoryInStashSyncedAssetsFolder(userEmail
                        , folder_name, true, accessToken);

                FileList fileList = service.files().list()
                        .setQ("name contains 'profileMap' and '" + folderId + "' in parents")
                        .setSpaces("drive")
                        .setFields("files(id, name)")
                        .execute();

                Date date = SharedPreferencesHandler.getJsonModifiedTime(MainActivity.preferences);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String currentDate = formatter.format(date);
                String fileName = "profileMap_" + currentDate + ".json";
                List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
                for(com.google.api.services.drive.model.File profileFile: existingFiles){
                    System.out.println("Older name:" + profileFile.getName() + " new name: " + fileName);
                    if(deleteOlderProfileFiles){
                        if(!profileFile.getName().equals(fileName)){
                            service.files().delete(profileFile.getId()).execute();
                        }
                    }else {
                        if(profileFile.getName().equals(fileName)){
                            service.files().delete(profileFile.getId()).execute();
                            break;
                        }
                    }
                }
            }catch (Exception e) {
                LogHandler.saveLog("Failed to delete profile files: " + e.getLocalizedMessage(), true);
            }
        });
        deleteProfileFileThread.start();
        try{
            deleteProfileFileThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join deleteOlderProfileFileThread: " + e.getLocalizedMessage(), true);
        }
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

    private static boolean checkDeletionStatusAfterProfileLogin(String userEmail,String accessToken){
        try{
            List<com.google.api.services.drive.model.File> existingFiles = getFilesInProfileFolder(userEmail,accessToken);
            if (existingFiles.size() == 1) {
                return true;
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to check deletion status of profile files: " + e.getLocalizedMessage(), true);
        }
        return false;
    }

    public static boolean backUpJsonFile(JsonObject resultJson, GoogleCloud.signInResult signInResult, ActivityResultLauncher<Intent> signInToBackUpLauncher){
        boolean[] isBackedUp = {false};
        Thread backUpJsonThread = new Thread(() -> {
            try{
                if (signInResult.getHandleStatus()) {
                    Drive service = GoogleDrive.initializeDrive(signInResult.getTokens().getAccessToken());
                    String folder_name = "stash_user_profile";
                    String profileFolderId = GoogleDrive.
                            createOrGetSubDirectoryInStashSyncedAssetsFolder(signInResult.getUserEmail(),folder_name
                                    , true, signInResult.getTokens().getAccessToken());

                    LogHandler.saveLog("@@@" + "profile folder id for account " + signInResult.getUserEmail() + " : "+profileFolderId,false);
                    String uploadedFileId = setAndCreateProfileMapContent(service,profileFolderId,"profile", resultJson);
                    if (uploadedFileId == null | uploadedFileId.isEmpty()) {
                        LogHandler.saveLog("Failed to upload profileMap from Android to backup because it's null");
                    }else{
                        isBackedUp[0] = true;
                    }
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
                            String uploadedFileId = setAndCreateProfileMapContent(service,profileFolderId,"signout",null);
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

    private static String setAndCreateProfileMapContent(Drive service,String profileFolderId,String loginStatus, JsonObject resultJson){
        String uploadFileId = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = SharedPreferencesHandler.getJsonModifiedTime(MainActivity.preferences);
        String currentDate = formatter.format(date);
        String fileName = "profileMap_" + currentDate + ".json";
        LogHandler.saveLog("@@@" + "name of json file is : "+ fileName,false);
        try{
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(java.util.Collections.singletonList(profileFolderId));
            JsonObject content = null;
            if(loginStatus.equals("signout")){
                content = Profile.createProfileMapContent();
            }else if(loginStatus.equals("profile")){
                content = addDeviceInfoToJson(resultJson);
            }else if(loginStatus.equals("login")){
                content = Profile.createProfileMapContent();
                content = Profile.addAccountToJson(content,resultJson);
            }
            String contentString = content.toString();
            ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", contentString);
            com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            uploadFileId = uploadedFile.getId();
            LogHandler.saveLog("@@@" + "profile map upload file id is : "+ uploadFileId,false);
        }catch (Exception e){
            LogHandler.saveLog("Failed to set profile map content:" + e.getLocalizedMessage(), true);
        }finally {
            return uploadFileId;
        }
    }

     public static JsonObject addDeviceInfoToJson(JsonObject profileJson) {
        try {
            JsonArray deviceInfo = profileJson.get("deviceInfo").getAsJsonArray();
            profileJson.remove("deviceInfo");

            JsonObject newDeviceInfo = new JsonObject();
            newDeviceInfo.addProperty("deviceName", MainActivity.androidDeviceName);
            newDeviceInfo.addProperty("deviceId", MainActivity.androidUniqueDeviceIdentifier);
            deviceInfo.add(newDeviceInfo);

            profileJson.add("deviceInfo", deviceInfo);
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

    public static void startSignInToProfileThread(ActivityResultLauncher<Intent> signInToBackUpLauncher, View[] child,
                                                  JsonObject resultJson,GoogleCloud.signInResult signInResult){
        LogHandler.saveLog("Start adding linked accounts thread", false);
        Thread addingLinkedAccountsThread = new Thread(() -> {
            try {
                boolean isSignedIn = false;
                ArrayList<GoogleCloud.signInResult> signInLinkedAccountsResult =
                        MainActivity.googleCloud.signInLinkedAccounts(resultJson, signInResult.getUserEmail());
                if (signInLinkedAccountsResult != null && !signInLinkedAccountsResult.isEmpty()){
                    isSignedIn = true;
                }
                signInLinkedAccountsResult.add(signInResult);

                LogHandler.saveLog("@@@" + "can sign in to linked accounts (size of signin results): " + signInLinkedAccountsResult.size(),false);

                boolean isBackedUp = false;
                ArrayList<GoogleCloud.signInResult> backedUpSignInResults = new ArrayList<>();
                if(isSignedIn){
                    for (GoogleCloud.signInResult signInLinkedAccountResult: signInLinkedAccountsResult) {
                        isBackedUp = Profile.backUpJsonFile(resultJson,signInLinkedAccountResult, signInToBackUpLauncher);
                        if (!isBackedUp){
                            for(GoogleCloud.signInResult backedUpSignInResult: backedUpSignInResults){
                                String userEmail = backedUpSignInResult.getUserEmail();
                                String accessToken = backedUpSignInResult.getTokens().getAccessToken();
                                Profile.deleteProfileFile(userEmail,accessToken,false);
                            }
                            break;
                        }else{
                            backedUpSignInResults.add(signInLinkedAccountResult);
                        }
                    }
                }

                LogHandler.saveLog("@@@" + "can back up to all accounts : " + isBackedUp,false);
                if(isBackedUp && isSignedIn){
                    ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromJson(resultJson);
                    for(DeviceHandler device: devices){
                        LogHandler.saveLog("@@@" + "found device " + device.deviceName + " in result json",false);
                        DeviceHandler.insertIntoDeviceTable(device.deviceName,device.deviceId);
                    }

                    for (GoogleCloud.signInResult signInLinkedAccountResult: signInLinkedAccountsResult){
                        String userEmail = signInLinkedAccountResult.getUserEmail();
                        String accessToken = signInLinkedAccountResult.getTokens().getAccessToken();
                        Profile.deleteProfileFile(userEmail, accessToken, true);

                        LogHandler.saveLog("Starting insertIntoAccounts for linked accounts",false);
                        MainActivity.dbHelper.insertIntoAccounts(signInLinkedAccountResult.getUserEmail(),
                                "backup",signInLinkedAccountResult.getTokens().getRefreshToken(),
                                signInLinkedAccountResult.getTokens().getAccessToken(),
                                signInLinkedAccountResult.getStorage().getTotalStorage(),
                                signInLinkedAccountResult.getStorage().getUsedStorage(),
                                signInLinkedAccountResult.getStorage().getUsedInDriveStorage(),
                                signInLinkedAccountResult.getStorage().getUsedInGmailAndPhotosStorage());
                        LogHandler.saveLog("Finished to insertIntoAccounts for linked accounts",false);

                        LogHandler.saveLog("Starting addAbackUpAccountToUI thread",false);
                        UIHandler.addAbackUpAccountToUI(MainActivity.activity,true,signInToBackUpLauncher,
                                child,signInLinkedAccountResult);
                        LogHandler.saveLog("Finished addAbackUpAccountToUI thread for linked account",false);
                    }

                    LogHandler.saveLog("Starting Drive threads",false);
                    GoogleDrive.startThreads();
                    LogHandler.saveLog("Finished Drive threads",false);
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    System.out.println("is display4 in main thread: "  + Looper.getMainLooper().isCurrentThread());
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to add linked accounts: " + e.getLocalizedMessage(), true);
            }
        });
        addingLinkedAccountsThread.start();
    }

    public static void linkToAccounts(GoogleCloud.signInResult signInResult,View[] child){
        Thread linkToAccountsThread = new Thread(() -> {
            ArrayList<String[]> existingAccounts = (ArrayList<String[]>) DBHelper.getAccounts(new String[]{"userEmail", "type", "refreshToken"});
            existingAccounts.add(new String[]{signInResult.getUserEmail(), "backup", signInResult.getTokens().getRefreshToken()});

            boolean isBackedUp = false;
            ArrayList<String[]> backedUpSignInResults = new ArrayList<>();
            JsonObject newAccountJson = new JsonObject();
            newAccountJson.addProperty("backupEmail", signInResult.getUserEmail());
            newAccountJson.addProperty("refreshToken", signInResult.getTokens().getRefreshToken());
            for (String[] existingAccount : existingAccounts) {
                if (existingAccount[1].equals("backup")) {
                    isBackedUp = Profile.backupJsonFileToExistingAccounts(newAccountJson,existingAccount[0], existingAccount[2]);
                    if (!isBackedUp){
                        for(String[] backedUpExistingAccount: backedUpSignInResults){
                            String userEmail = backedUpExistingAccount[0];
                            String accessToken = MainActivity.googleCloud.updateAccessToken(backedUpExistingAccount[2]).getAccessToken();
                            Profile.deleteProfileFile(userEmail, accessToken, false);
                        }
                        break;
                    }else{
                        backedUpSignInResults.add(existingAccount);
                    }
                }
            }


            if(isBackedUp){
                for(String[] backedUpExistingAccount: existingAccounts){
                    String userEmail = backedUpExistingAccount[0];
                    String accessToken = MainActivity.googleCloud.updateAccessToken(backedUpExistingAccount[2]).getAccessToken();
                    Profile.deleteProfileFile(userEmail, accessToken, true);
                }
                MainActivity.dbHelper.insertIntoAccounts(signInResult.getUserEmail(),
                        "backup",signInResult.getTokens().getRefreshToken(),
                        signInResult.getTokens().getAccessToken(),
                        signInResult.getStorage().getTotalStorage(),
                        signInResult.getStorage().getUsedStorage(),
                        signInResult.getStorage().getUsedInDriveStorage(),
                        signInResult.getStorage().getUsedInGmailAndPhotosStorage());

                UIHandler.addAbackUpAccountToUI(MainActivity.activity,true,signInToBackUpLauncher,
                        child,signInResult);

                LogHandler.saveLog("Starting Drive threads",false);
                GoogleDrive.startThreads();
                LogHandler.saveLog("Finished Drive threads",false);
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                System.out.println("is display4 in main thread: "  + Looper.getMainLooper().isCurrentThread());
            }

        });
        linkToAccountsThread.start();
        try {
            linkToAccountsThread.join();
        }catch (InterruptedException e) {
            LogHandler.saveLog("Failed to link to accounts: " + e.getLocalizedMessage(), true);
        }
    }

    public static boolean backupJsonFileToExistingAccounts(JsonObject newAccountJson, String userEmail, String refreshToken) {
        boolean[] isBackedUp = {false};
        Thread backUpJsonThread = new Thread(() -> {
            try{
                String accessToken = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                Drive service = GoogleDrive.initializeDrive(accessToken);
                String folder_name = "stash_user_profile";
                String profileFolderId = GoogleDrive.createOrGetSubDirectoryInStashSyncedAssetsFolder(userEmail,folder_name, true, accessToken);
                String uploadedFileId = setAndCreateProfileMapContent(service,profileFolderId,"login", newAccountJson);
                if (uploadedFileId == null | uploadedFileId.isEmpty()) {
                    LogHandler.saveLog("Failed to upload profileMap from Android to backup because it's null");
                }else{
                    isBackedUp[0] = true;
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


    public static JsonObject addAccountToJson(JsonObject existingProfile,JsonObject newAccount){
        JsonArray existingAccounts = existingProfile.getAsJsonArray("backupAccounts");
        existingAccounts.add(newAccount);
        existingProfile.remove("backupAccounts");
        existingProfile.add("backupAccounts", existingAccounts);
        return existingProfile;
    }
    
}
