package com.example.cso;

import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Profile {
    public static JsonObject createProfileMapContentBasedOnDB(){
        JsonObject resultJson = new JsonObject();
        try{
            JsonArray backupAccountsJson = createBackUpAccountsJson();
            JsonArray deviceInfoJson = createDeviceInfoJsonBasedDB();

            resultJson.add("backupAccounts", backupAccountsJson);
            resultJson.add("deviceInfo", deviceInfoJson);
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }finally {
            return  resultJson;
        }
    }

    private static JsonArray createDeviceInfoJsonBasedDB(){
        JsonArray deviceInfoJson = new JsonArray();
        try{
            ArrayList<DeviceHandler> deviceList = DeviceHandler.getDevicesFromDB();
            if(deviceList != null && !deviceList.isEmpty()){
                for (DeviceHandler device: deviceList){
                    JsonObject deviceInfo = new JsonObject();
                    deviceInfo.addProperty("deviceName", device.getDeviceName());
                    deviceInfo.addProperty("deviceId", device.getDeviceId());
                    deviceInfoJson.add(deviceInfo);
                }
            }

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


    public static JsonObject readProfileMapContent(String userEmail, String accessToken) {
        JsonObject[] resultJson = {null};
        Thread readProdileMapContentThread = new Thread(() -> {
            try{
                Drive service = GoogleDrive.initializeDrive(accessToken);
                List<com.google.api.services.drive.model.File> existingFiles = getFilesInProfileFolder(userEmail, service, accessToken);
                for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                    for (int i = 0; i < 3; i++) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        try {
                            service.files().get(existingFile.getId())
                                    .executeMediaAndDownloadTo(outputStream);

                            String jsonString = outputStream.toString();
                            resultJson[0] = JsonParser.parseString(jsonString).getAsJsonObject();
                            if(resultJson[0] != null){
                                break;
                            }
                        }catch (Exception e){
                            FirebaseCrashlytics.getInstance().recordException(e);
                        }finally {
                            outputStream.close();
                        }
                    }
                }
            }catch (Exception e){
                System.out.println("errr is r: " + e.getLocalizedMessage());
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });

        readProdileMapContentThread.start();
        try{
            readProdileMapContentThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        return resultJson[0];
    }

    public static String readProfileMapName(String userEmail, String accessToken) {
        String[] resultJsonName = {null};

        Thread readProdileMapContentThread = new Thread( () -> {
            try{
                Log.d("Threads","Map file name searching started");
                Drive service = GoogleDrive.initializeDrive(accessToken);
                List<com.google.api.services.drive.model.File> existingFiles = getFilesInProfileFolder(userEmail, service, accessToken);

                for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                    resultJsonName[0] = existingFile.getName();
                    Log.d("jsonChange","Map files found: " + resultJsonName[0]);
                    if (resultJsonName[0]!= null && !resultJsonName[0].isEmpty() && resultJsonName[0].contains("profileMap_")) {
                        Log.d("Threads","Map file name searching finished");
                        break;
                    }
                }
                Log.d("Threads","Map file name searching finished");
            }catch (Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
        });

        readProdileMapContentThread.start();
        try {
            readProdileMapContentThread.join();
        } catch (InterruptedException e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return resultJsonName[0];
    }

    private static List<com.google.api.services.drive.model.File> getFilesInProfileFolder(String userEmail, Drive service, String accessToken){
        final List<com.google.api.services.drive.model.File>[] existingFiles = new List[]{new ArrayList<>()};
        Thread getFilesInProfileFolderThread = new Thread(() -> {
            try {
                String folderName = GoogleDriveFolders.profileFolderName;
                String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, accessToken, true);

                if (profileFolderId != null && !profileFolderId.isEmpty()) {
                    FileList fileList = service.files().list()
                            .setQ("name contains 'profileMap_' and '" + profileFolderId + "' in parents")
                            .setSpaces("drive")
                            .setFields("files(id,name)")
                            .execute();
                    List<File> files = fileList.getFiles();
                    existingFiles[0].addAll(files);
                    Log.d("folder", "n files in profile folder: " + existingFiles[0].size());
                }
            }catch (Exception e){
                System.out.println("errr is f:"  +e.getLocalizedMessage());
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });

        getFilesInProfileFolderThread.start();
        try {
            getFilesInProfileFolderThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return existingFiles[0];
    }

    public static boolean isLinkedToAccounts(JsonObject resultJson, String userEmail){
        boolean[] isLinkedToAccounts = {false};
        Thread isLinkedToAccountsThread = new Thread(() -> {
            try{
                if(resultJson != null){
                    ArrayList<String> backupAccountsInDevice = DBHelper.getBackupAccountsInDevice(userEmail);

                    JsonArray backupAccountsArray = resultJson.getAsJsonArray("backupAccounts");
                    isLinkedToAccounts[0] = checkNewBackupEmails(backupAccountsArray,backupAccountsInDevice);
                    if(!isLinkedToAccounts[0]){
                        if (resultJson.has("deviceInfo")) {
                            JsonArray deviceInfoArray = resultJson.getAsJsonArray("deviceInfo");
                            isLinkedToAccounts[0] = checkNewDevices(deviceInfoArray);
                        }
                    }
                }
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });

        isLinkedToAccountsThread.start();
        try {
            isLinkedToAccountsThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        return isLinkedToAccounts[0];
    }

    private static boolean checkNewDevices(JsonArray deviceInfoArray){
        boolean isLinked = false;

        ArrayList<DeviceHandler> currentDeviceIdList = MainActivity.dbHelper.getDevicesFromDB();
        for (JsonElement element : deviceInfoArray) {
            JsonObject deviceInfoObject = element.getAsJsonObject();
            String deviceIdentifier = deviceInfoObject.get("deviceId").getAsString();
            if (!currentDeviceIdList.contains(deviceIdentifier) && !deviceIdentifier.equals(MainActivity.androidUniqueDeviceIdentifier)) {
                Log.d("signInToBackUpLauncher","A new device found: " + deviceIdentifier);
                isLinked = true;
            }
        }

        return isLinked;
    }

    private static boolean checkNewBackupEmails(JsonArray backupAccountsArray, ArrayList<String> backupAccountsInDevice) {
        boolean isLinked = false;

        for (JsonElement element : backupAccountsArray) {
            JsonObject accountObject = element.getAsJsonObject();
            String backupEmail = accountObject.get("backupEmail").getAsString();
            if (!backupAccountsInDevice.contains(backupEmail)) {
                Log.d("signInToBackUpLauncher","A new email found: " + backupEmail);
                isLinked = true;
            }
        }
        return isLinked;
    }


    private static void deleteProfileFile(String userEmail, String accessToken, boolean deleteOlderProfileFiles){
        Thread deleteProfileFileThread = new Thread(() -> {
            try {
                Drive service = GoogleDrive.initializeDrive(accessToken);
                String folderName = GoogleDriveFolders.profileFolderName;
                String folderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, accessToken, true);

                FileList fileList = service.files().list()
                        .setQ("name contains 'profileMap' and '" + folderId + "' in parents")
                        .setSpaces("drive")
                        .setFields("files(id, name)")
                        .execute();

                Date date = SharedPreferencesHandler.getJsonModifiedTime(MainActivity.preferences);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US);
                String currentDate = formatter.format(date);
                String fileName = "profileMap_" + currentDate + ".json";

                List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
                for(com.google.api.services.drive.model.File profileFile: existingFiles){
                    if (deleteOlderProfileFiles && !profileFile.getName().equals(fileName)) {
                        service.files().delete(profileFile.getId()).execute();
                    } else if (!deleteOlderProfileFiles && profileFile.getName().equals(fileName)) {
                        service.files().delete(profileFile.getId()).execute();
                        break;
                    }
                }
            }catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        deleteProfileFileThread.start();
        try{
            deleteProfileFileThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
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
            Drive service = GoogleDrive.initializeDrive(accessToken);
            List<com.google.api.services.drive.model.File> existingFiles = getFilesInProfileFolder(userEmail, service, accessToken);
            if (existingFiles.size() == 1) {
                return true;
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to check deletion status of profile files: " + e.getLocalizedMessage(), true);
        }
        return false;
    }

    public static boolean backUpJsonFile(JsonObject resultJson, GoogleCloud.SignInResult signInResult, ActivityResultLauncher<Intent> signInToBackUpLauncher){
        boolean[] isBackedUp = {false};
        Thread backUpJsonThread = new Thread(() -> {
            try{
                if (signInResult.getHandleStatus()) {
                    Drive service = GoogleDrive.initializeDrive(signInResult.getTokens().getAccessToken());
                    String folderName = GoogleDriveFolders.profileFolderName;
                    String profileFolderId = GoogleDriveFolders.getSubFolderId(signInResult.getUserEmail(), folderName,
                            signInResult.getTokens().getAccessToken(), true);

                    String uploadedFileId = setAndCreateProfileMapContent(service,profileFolderId,"profile", resultJson);
                    if (uploadedFileId == null | uploadedFileId.isEmpty()) {
                        Log.d("error","Failed to upload profileMap, it's null");
                    }else{
                        isBackedUp[0] = true;
                    }
                }else{
                    Log.d("signInToBackUpLauncher","Back up launcher failed with status : " + signInResult.getHandleStatus());
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

    private static String setAndCreateProfileMapContent(Drive service,String profileFolderId,
                                                        String loginStatus, Object attachedFile){
        final String[] uploadFileId = {""};
        Thread setAndCreateProfileMapContentThread = new Thread( () -> {
            try{
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                Date date = SharedPreferencesHandler.getJsonModifiedTime(MainActivity.preferences);
                String currentDate = formatter.format(date);
                String fileName = "profileMap_" + currentDate + ".json";
                try{
                    com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                    fileMetadata.setName(fileName);
                    fileMetadata.setParents(java.util.Collections.singletonList(profileFolderId));

                    JsonObject content = prepareProfileMapContent(loginStatus, attachedFile);

                    String contentString = content.toString();
                    ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", contentString);

                    com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();

                    uploadFileId[0] = uploadedFile.getId();
                    Log.d("profileMapContent" , "Upload file is : "+ uploadFileId[0]);
                }catch (Exception e){
                    LogHandler.saveLog("Failed to set profile map content:" + e.getLocalizedMessage(), true);
                }
            }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        });

        setAndCreateProfileMapContentThread.start();
        try{
            setAndCreateProfileMapContentThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return uploadFileId[0];
    }

    private static JsonObject prepareProfileMapContent(String loginStatus, Object attachedFile){
        final JsonObject[] content = {new JsonObject()};
        Thread prepareProfileMapContentThread = new Thread( () -> {
            try {
                if(loginStatus.equals("unlink")) {
                    JsonObject initialContent = createProfileMapContentBasedOnDB();
                    String unlinkedEmail = (String) attachedFile;
                    content[0] = Profile.removeAccountFromJson(initialContent, unlinkedEmail);

                } else if(loginStatus.equals("profile")){
                    JsonObject resultJson = (JsonObject) attachedFile;
                    content[0] = addDeviceInfoToJson(resultJson);
                } else if(loginStatus.equals("login")) {
                    JsonObject initialContent = createProfileMapContentBasedOnDB();
                    JsonObject resultJson = (JsonObject) attachedFile;
                    content[0] = addAccountToJson(initialContent, resultJson);
                }
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });

        prepareProfileMapContentThread.start();
        try{
            prepareProfileMapContentThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return content[0];
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

    public static boolean hasJsonChanged(){
        boolean hasChanged = false;
        try{
            List<String[]> accounts = DBHelper.getAccounts(new String[]{"userEmail","type","refreshToken"});

            for (String[] account : accounts) {
                if (account[1].equals("backup")) {
                    String accessToken = MainActivity.googleCloud.updateAccessToken(account[2]).getAccessToken();
                    String resultJsonName = readProfileMapName(account[0],accessToken);

                    if (resultJsonName!= null || !resultJsonName.isEmpty()){
                        Log.d("jsonChange","Json file not found");
                        continue;
                    }

                    Date lastModifiedDateOfJson = convertFileNameToTimeStamp(resultJsonName);
                    Date lastModifiedDateOfPreferences =  SharedPreferencesHandler.getJsonModifiedTime(MainActivity.preferences);
                    Log.d("jsonChange","lastModifiedDateOfJson: " + lastModifiedDateOfJson.toString());
                    Log.d("jsonChange","lastModifiedDateOfPreferences: " + lastModifiedDateOfPreferences.toString());

                    if (lastModifiedDateOfJson!= null && lastModifiedDateOfPreferences!= null){
                        hasChanged = lastModifiedDateOfJson.after(lastModifiedDateOfPreferences);
                    } else {
                        hasChanged = false;
                    }
                }
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return hasChanged;
    }

    public static Date convertFileNameToTimeStamp(String fileName){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US);
            String timestamp = fileName.substring(fileName.indexOf('_') + 1, fileName.lastIndexOf('.'));
            return dateFormat.parse(timestamp);
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
            return null;
        }
    }

    public void startSignInToProfileThread(ActivityResultLauncher<Intent> signInToBackUpLauncher, View[] child,
                                           JsonObject resultJson, GoogleCloud.SignInResult signInResult){
        Log.d("signInToBackUpLauncher","Adding linked accounts started");
        Thread addingLinkedAccountsThread = new Thread(() -> {
            try {
                boolean isDone = false;
                ArrayList<GoogleCloud.SignInResult> linkedAccounts = MainActivity.googleCloud.signInLinkedAccounts(resultJson, signInResult.getUserEmail());
                if(linkedAccounts != null) {
                    Log.d("signInToBackUpLauncher", "Handling backups started");
                    boolean isAllBackedUp = handleBackups(resultJson, signInToBackUpLauncher, linkedAccounts);
                    Log.d("signInToBackUpLauncher", "isAllBackedUp: " + isAllBackedUp);
                    if (isAllBackedUp) {
                        Log.d("signInToBackUpLauncher", "handleDeviceInsertion started");
                        handleDeviceInsertion(resultJson);
                        Log.d("signInToBackUpLauncher", "handleDeviceInsertion finished");

                        Log.d("signInToBackUpLauncher", "handleNewAccounts started");
                        handleNewAccounts(linkedAccounts);
                        Log.d("signInToBackUpLauncher", "handleNewAccounts finished");

                        GoogleDrive.startThreads();
                        isDone = true;
                    }
                }
                Log.d("signInToBackUpLauncher","Adding linked accounts finished");
            } catch (Exception e) {
                LogHandler.saveLog("Failed to add linked accounts: " + e.getLocalizedMessage(), true);
            }
        });
        addingLinkedAccountsThread.start();
    }

    private static void handleDeviceInsertion(JsonObject resultJson){
        Thread handleDeviceInsertionThread =  new Thread(() -> {
            try{
                ArrayList<DeviceHandler> devices = DeviceHandler.getDevicesFromJson(resultJson);
                for (DeviceHandler device : devices) {
                    Log.d("signInToBackUpLauncher","Inserted " + device.getDeviceId() +  " into device table");
                    DeviceHandler.insertIntoDeviceTable(device.deviceName, device.deviceId);
                }
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        handleDeviceInsertionThread.start();
        try{
            handleDeviceInsertionThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static void handleNewAccounts(ArrayList<GoogleCloud.SignInResult> signInLinkedAccountsResult){
        Thread handleAccountInsertionThread =  new Thread(() -> {
           try{
               Log.d("Threads" ,"handleAccountInsertionThread started");
               for (GoogleCloud.SignInResult signInLinkedAccountResult : signInLinkedAccountsResult) {
                   String userEmail = signInLinkedAccountResult.getUserEmail();
                   String accessToken = signInLinkedAccountResult.getTokens().getAccessToken();

                   Profile.deleteProfileFile(userEmail, accessToken, true);

                   MainActivity.dbHelper.insertIntoAccounts(signInLinkedAccountResult.getUserEmail(),
                           "backup", signInLinkedAccountResult.getTokens().getRefreshToken(),
                           signInLinkedAccountResult.getTokens().getAccessToken(),
                           signInLinkedAccountResult.getStorage().getTotalStorage(),
                           signInLinkedAccountResult.getStorage().getUsedStorage(),
                           signInLinkedAccountResult.getStorage().getUsedInDriveStorage(),
                           signInLinkedAccountResult.getStorage().getUsedInGmailAndPhotosStorage());
               }
               Log.d("Threads" ,"handleAccountInsertionThread finished");
           }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e) ;}
        });
        handleAccountInsertionThread.start();
        try{
            handleAccountInsertionThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e) ;}
    }

    private boolean handleBackups(JsonObject resultJson, ActivityResultLauncher<Intent> signInToBackUpLauncher
            , ArrayList<GoogleCloud.SignInResult> linkedAccounts) {
        final boolean[] isBackedUp = {false};
        ArrayList<GoogleCloud.SignInResult> backedUpSignInResults = new ArrayList<>();
        Thread handleBackupsThread =  new Thread(() -> {
            for (GoogleCloud.SignInResult signInLinkedAccountResult : linkedAccounts) {
                isBackedUp[0] = Profile.backUpJsonFile(resultJson, signInLinkedAccountResult, signInToBackUpLauncher);
                if (!isBackedUp[0]) {
                    handleBackupFailure(backedUpSignInResults);
                    break;
                } else {
                    backedUpSignInResults.add(signInLinkedAccountResult);
                }
            }
        });
        handleBackupsThread.start();
        try{
            handleBackupsThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        return isBackedUp[0];
    }

    private static void handleBackupFailure(ArrayList<GoogleCloud.SignInResult> backedUpSignInResults){
        for(GoogleCloud.SignInResult backedUpSignInResult: backedUpSignInResults){
            String userEmail = backedUpSignInResult.getUserEmail();
            String accessToken = backedUpSignInResult.getTokens().getAccessToken();
            Profile.deleteProfileFile(userEmail, accessToken, false);
        }
    }

    public void loginSingleAccount(GoogleCloud.SignInResult signInResult){
        Thread loginSingleAccountThread = new Thread(() -> {
            List<String[]> existingAccounts = DBHelper.getAccounts(new String[]{"userEmail", "type", "refreshToken"});
            existingAccounts.add(new String[]{signInResult.getUserEmail(), "backup", signInResult.getTokens().getRefreshToken()});

            boolean isBackedUp = false;
            ArrayList<String[]> backedUpAccounts = new ArrayList<>();
            JsonObject newAccountJson = createNewAccountJson(signInResult);

            for (String[] existingAccount : existingAccounts) {
                if (existingAccount[1].equals("backup")) {
                    Log.d("signInToBackUpLauncher","backupJsonFileToExistingAccount started");
                    isBackedUp = Profile.backupJsonFileToExistingAccount(newAccountJson,existingAccount[0],
                            existingAccount[2],"login",signInResult.getTokens().getAccessToken());
                    Log.d("signInToBackUpLauncher","backupJsonFileToExistingAccount finished: " + isBackedUp);
                    if (!isBackedUp){
                        handleLoginToSingleAccountFailure(backedUpAccounts);
                        break;
                    }else{
                        backedUpAccounts.add(existingAccount);
                    }
                }
            }

            if(isBackedUp){
                handleLoginToSingleAccountSuccess(backedUpAccounts,signInResult);
                GoogleDrive.startThreads();
            }else{
                LogHandler.saveLog("login with back up launcher failed with response code : " + signInResult.getHandleStatus());
            }
        });

        loginSingleAccountThread.start();
        try {
            loginSingleAccountThread.join();
        }catch (InterruptedException e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static void handleLoginToSingleAccountSuccess(List<String[]> backedUpAccounts,GoogleCloud.SignInResult signInResult){
        Thread handleLoginToSingleAccountSuccessThread = new Thread( () -> {
            Log.d("Threads" ,"handleLoginToSingleAccountSuccessThread started");
            try{
                for(String[] backedUpExistingAccount: backedUpAccounts){
                    String userEmail = backedUpExistingAccount[0];
                    String accessToken = MainActivity.googleCloud.updateAccessToken(backedUpExistingAccount[2]).getAccessToken();
                    Profile.deleteProfileFile(userEmail, accessToken, false);
                }

                MainActivity.dbHelper.insertIntoAccounts(signInResult.getUserEmail(),
                        "backup",signInResult.getTokens().getRefreshToken(),
                        signInResult.getTokens().getAccessToken(),
                        signInResult.getStorage().getTotalStorage(),
                        signInResult.getStorage().getUsedStorage(),
                        signInResult.getStorage().getUsedInDriveStorage(),
                        signInResult.getStorage().getUsedInGmailAndPhotosStorage());
                Log.d("Threads" ,"handleLoginToSingleAccountSuccessThread finished");
            }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        });
        handleLoginToSingleAccountSuccessThread.start();
        try{
            handleLoginToSingleAccountSuccessThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static void handleLoginToSingleAccountFailure(List<String[]> backedUpAccounts){
        for(String[] backedUpExistingAccount: backedUpAccounts){
            String userEmail = backedUpExistingAccount[0];
            String accessToken = MainActivity.googleCloud.updateAccessToken(backedUpExistingAccount[2]).getAccessToken();
            Profile.deleteProfileFile(userEmail, accessToken, false);
        }
    }

    private static JsonObject createNewAccountJson(GoogleCloud.SignInResult signInResult) {
        JsonObject newAccountJson = new JsonObject();
        try{
            newAccountJson.addProperty("backupEmail", signInResult.getUserEmail());
            newAccountJson.addProperty("refreshToken", signInResult.getTokens().getRefreshToken());
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        return newAccountJson;
    }

    public static boolean backupJsonFileToExistingAccount(Object attachedFile, String userEmail, String refreshToken, String loginStatus,
                                                          String accessToken) {
        boolean[] isBackedUp = {false};
        final String[] finalAccessToken = {null};
        Thread backUpJsonThread = new Thread(() -> {
            try{
                if(loginStatus.equals("login")){
                    finalAccessToken[0] = accessToken;
                }else if (loginStatus.equals("unlink")){
                    finalAccessToken[0] = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                }
                Drive service = GoogleDrive.initializeDrive(finalAccessToken[0]);
                String folderName = GoogleDriveFolders.profileFolderName;
                String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, finalAccessToken[0], true);
                String uploadedFileId = setAndCreateProfileMapContent(service,profileFolderId,loginStatus, attachedFile);
                if (uploadedFileId == null | uploadedFileId.isEmpty()) {
                    LogHandler.saveLog("Failed to upload profileMap from Android to backup because it's null");
                }else{
                    isBackedUp[0] = true;
                }
            }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e);}
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

    public static JsonObject removeAccountFromJson(JsonObject existingProfile, String unlinkedEmail){
        JsonArray existingAccounts = existingProfile.getAsJsonArray("backupAccounts");
        JsonArray remainingAccounts = new JsonArray();
        for (JsonElement accountJson :existingAccounts){
            if (!accountJson.getAsJsonObject().get("backupEmail").getAsString().equals(unlinkedEmail)){
                remainingAccounts.add(accountJson);
            }
        }
        existingProfile.remove("backupAccounts");
        existingProfile.add("backupAccounts", remainingAccounts);
        return existingProfile;
    }


    public void validateProfile(){
        validateDevices();
        validateAccounts();
    }

    public void validateDevices(){
        try{
            ArrayList<DeviceHandler> currentDevices = MainActivity.dbHelper.getDevicesFromDB();
//        readProfileMapContent()
            JsonObject jsonObject = new JsonObject();
            ArrayList<DeviceHandler> jsonDevices = getDevicesFromProfileJson(jsonObject);
            for(DeviceHandler device: currentDevices){
                if(!jsonDevices.contains(device)){
                    //unlink
                }
            }
            for(DeviceHandler device: jsonDevices){
                if(!currentDevices.contains(device)){
                    //link
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to validate devices: " + e.getLocalizedMessage(), true);
        }
    }

    public void validateAccounts(){
        ArrayList<String> currentAccounts = new ArrayList<>();
        try{
            List<String []> accounts = DBHelper.getAccounts(new String[]{"userEmail","type"});
            for(String[] account: accounts){
                String type = account[1];
                if(type.equals("backup")){
                    String userEmail = account[0];
                    currentAccounts.add(userEmail);
                }
            }
//        readProfileMapContent()
            JsonObject jsonObject = new JsonObject();
            ArrayList<String> jsonAccounts = getBackUpAccountsFromProfileJson(jsonObject);
            for(String currentAccount: currentAccounts){
                if(!jsonAccounts.contains(currentAccount)){
                    //unlink
                }
            }
            for(String jsonAccount: jsonAccounts){
                if(!currentAccounts.contains(jsonAccount)){
                    //link
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to validate accounts: " + e.getLocalizedMessage(), true);
        }
    }

    public ArrayList<DeviceHandler> getDevicesFromProfileJson(JsonObject profileJson){
        ArrayList<DeviceHandler> devices = new ArrayList<>();
        try{
            if (profileJson.has("deviceInfo")) {
                JsonArray deviceInfoArray = profileJson.getAsJsonArray("deviceInfo");
                for (JsonElement element : deviceInfoArray) {
                    JsonObject deviceInfoObject = element.getAsJsonObject();
                    String deviceIdentifier = deviceInfoObject.get("deviceId").getAsString();
                    String deviceName = deviceInfoObject.get("deviceName").getAsString();
                    devices.add(new DeviceHandler(deviceName,deviceIdentifier));
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to getDevicesFromProfileJson: " + e.getLocalizedMessage(), true);
        }finally {
            return devices;
        }
    }

    public ArrayList<String> getBackUpAccountsFromProfileJson(JsonObject profileJson){
        ArrayList<String> backUpAccounts = new ArrayList<>();
        try{
            JsonArray backupAccountsArray = profileJson.getAsJsonArray("backupAccounts");
            for (JsonElement element : backupAccountsArray) {
                JsonObject accountObject = element.getAsJsonObject();
                String backupEmail = accountObject.get("backupEmail").getAsString();
                backUpAccounts.add(backupEmail);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to getBackUpAccountsFromProfileJson: " + e.getLocalizedMessage(), true);
        }finally {
            return backUpAccounts;
        }
    }

    public static boolean backupJsonToRemainingAccounts(String unlinkedUserEmail){
        ArrayList<String[]> existingAccounts = (ArrayList<String[]>) DBHelper.getAccounts(new String[]{"userEmail", "type", "refreshToken"});
        ArrayList<String[]> backedUpAccounts = new ArrayList<>();
        boolean isBackedUp = false;
        for(String[] account: existingAccounts){
            if (account[1].equals("backup") && !account[0].equals(unlinkedUserEmail)){
                isBackedUp = backupJsonFileToExistingAccount(unlinkedUserEmail, account[0], account[2],"unlink", null);
                if (!isBackedUp){
                    for(String[] backedUpExistingAccount: backedUpAccounts){
                        String userEmail = backedUpExistingAccount[0];
                        String accessToken = MainActivity.googleCloud.updateAccessToken(backedUpExistingAccount[2]).getAccessToken();
                        Profile.deleteProfileFile(userEmail, accessToken, false);
                    }
                    break;
                }else{
                    backedUpAccounts.add(account);
                }
            }
        }

        if(isBackedUp) {
            for (String[] backedUpExistingAccount : existingAccounts) {
                String userEmail = backedUpExistingAccount[0];
                String accessToken = MainActivity.googleCloud.updateAccessToken(backedUpExistingAccount[2]).getAccessToken();
                Profile.deleteProfileFile(userEmail, accessToken, true);
            }
        }
        return isBackedUp;
    }

}

