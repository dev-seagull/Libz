package com.example.cso;

import android.content.Intent;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Profile {
    public static JsonObject createProfileMapContent(String userName){
        JsonObject resultJson = new JsonObject();
        try{
            JsonArray profileJson = createProfileJson(userName);
            JsonArray backupAccountsJson = createBackUpAccountsJson();
            JsonArray primaryAccountsJson = createPrimaryAccountsJson();
            JsonArray deviceInfoJson = createDeviceInfoJson();

            resultJson.add("profile", profileJson);
            resultJson.add("backupAccounts", backupAccountsJson);
            resultJson.add("primaryAccounts", primaryAccountsJson);
            resultJson.add("deviceInfo", deviceInfoJson);
        }catch (Exception e){
            LogHandler.saveLog("Failed to create profile map content : " + e.getLocalizedMessage() , true);
        }finally {
            return  resultJson;
        }
    }

    private static JsonArray createProfileJson(String userName){
        JsonArray profileJson = new JsonArray();
        try{
            JsonObject profile = new JsonObject();
            profile.addProperty("userName", userName);
            profileJson.add(profile);
        }catch (Exception e){
            LogHandler.saveLog("Failed to create profile json : " + e.getLocalizedMessage(), true);
        }
        finally {
            return profileJson;
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<JsonObject> uploadTask = () -> {
            JsonObject resultJson = null;
            try{
                String folderName = "stash_user_profile";
                String stashUserProfileFolderId = GoogleDrive.createOrGetSubDirectoryInStashSyncedAssetsFolder(userEmail,folderName, true, accessToken);

                Drive service = GoogleDrive.initializeDrive(accessToken);
                if(stashUserProfileFolderId != null && !stashUserProfileFolderId.isEmpty()) {
                    FileList fileList = service.files().list()
                            .setQ("name contains 'profileMap_' and '" + stashUserProfileFolderId + "' in parents")
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute();
                    List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
                    for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                        for (int i = 0; i < 3; i++) {
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            service.files().get(existingFile.getId())
                                    .executeMediaAndDownloadTo(outputStream);
                            String jsonString = outputStream.toString();
                            resultJson = JsonParser.parseString(jsonString).getAsJsonObject();
                            outputStream.close();
                            if (resultJson != null) {
                                return resultJson;
                            }
                        }
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to read profile map content : " + e.getLocalizedMessage(), true);
            }
            return null;
        };

        Future<JsonObject> future = null;
        JsonObject resultJson = new JsonObject();
        try{
            future = executor.submit(uploadTask);
        }catch (Exception e){
            LogHandler.saveLog("Failed to submit executor: " + e.getLocalizedMessage(), true);
        }
        try {
            resultJson = future.get();
        } catch (Exception e) {
            LogHandler.saveLog("error when downloading user profile : " + e.getLocalizedMessage());
        }
        return resultJson;
    }


    private static boolean isNewJsonProfile(JsonObject resultJson, String userEmail){
        try{
            JsonArray backupAccountsArray = resultJson.getAsJsonArray("backupAccounts");
            for (JsonElement element : backupAccountsArray) {
                JsonObject accountObject = element.getAsJsonObject();
                String backupEmail = accountObject.get("backupEmail").getAsString();
                if (!backupEmail.equals(userEmail)) {
                    System.out.println("A new email found:" + backupEmail);
                    return true;
                }
            }

            if (resultJson.has("deviceInfo")) {
                JsonArray deviceInfoArray = resultJson.getAsJsonArray("deviceInfo");
                for (JsonElement element : deviceInfoArray) {
                    JsonObject deviceInfoObject = element.getAsJsonObject();
                    String deviceIdentifier = deviceInfoObject.get("deviceId").getAsString();
                    if (!MainActivity.androidUniqueDeviceIdentifier.equals(deviceIdentifier)) {
                        System.out.println("A new device found:" + deviceIdentifier);
                        return true;
                    }
                }
            }

        }catch (Exception e){
            LogHandler.saveLog("Failed to check if it's a new profile: " + e.getLocalizedMessage(), true);
        }
        return false;
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
        Thread backUpJsonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    if (signInResult.getHandleStatus()) {
                        String userEmail = signInResult.getUserEmail();
                        GoogleCloud.Tokens tokens = signInResult.getTokens();
                        String refreshToken = tokens.getRefreshToken();
                        String accessToken = tokens.getAccessToken();
                        Double totalStorage = signInResult.getStorage().getTotalStorage();
                        Double usedStorage = signInResult.getStorage().getUsedStorage();
                        Double usedInDriveStorage = signInResult.getStorage().getUsedInDriveStorage();
                        Double usedInGmailAndPhotosStorage = signInResult.getStorage().getUsedInGmailAndPhotosStorage();

                        MainActivity.dbHelper.insertIntoAccounts(userEmail, "backup", refreshToken,accessToken,
                                totalStorage, usedStorage, usedInDriveStorage, usedInGmailAndPhotosStorage);
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
                            String uploadedFileId = setAndCreateProfileMapContent(service,profileFolderId,account_row[0]);
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

    private static String setAndCreateProfileMapContent(Drive service,String profileFolderId, String userEmail){
        String uploadFileId = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date();
        String currentDate = formatter.format(date);
        String fileName = "profileMap_" + currentDate + ".json";
        try{
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(java.util.Collections.singletonList(profileFolderId));
            String content = Profile.createProfileMapContent(userEmail).toString();
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

        final boolean[] isSignedout = {false};
        final boolean[] isBackedUp = {false};

        Thread detachLinkedAccountsProfileJsonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                JsonArray backupAccounts =  profileMapContent.get("backupAccounts").getAsJsonArray();
                JsonObject editedProfileContent = editProfileJson(profileMapContent,userEmail);

                for (int i = 0;i < backupAccounts.size();i++){
                    JsonObject backupAccount = backupAccounts.get(i).getAsJsonObject();
                    String linkedUserEmail = backupAccount.get("backupEmail").getAsString();
                    String refreshToken = backupAccount.get("refreshToken").getAsString();
                    if (linkedUserEmail.equals(userEmail)){
                        continue;
                    }
                    backUpProfileMapToLinkedAccounts(editedProfileContent,refreshToken,userEmail);
                }
            }
        });


        Thread backUpJsonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //                                    MainActivity.dbHelper.deleteFromAccountsTable(buttonText, "backup");
                //                                    MainActivity.dbHelper.deleteAccountFromDriveTable(buttonText);
                //                                    dbHelper.deleteRedundantAsset();
                //                                    isBackedUp[0] = MainActivity.dbHelper.backUpProfileMap(true,buttonText);
                //                                    System.out.println("isBackedUp " + isBackedUp[0]);

                synchronized (this){
                    notify();
                }
            }
        });

        Thread signOutThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (backUpJsonThread){
                    try {
                        backUpJsonThread.join();
                    } catch (InterruptedException e) {
                        LogHandler.saveLog("Failed to join back up json thread : "
                                + e.getLocalizedMessage(), true);
                    }
                }
//                                                if(isBackedUp[0]){
//                                                    isSignedout[0] = googleCloud.signOut(buttonText);
//                                                    System.out.println("isSignedOut " + isSignedout[0]);
//                                                }
            }
        });

        Thread uiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (signOutThread){
                    try {
                        signOutThread.join();
                    } catch (Exception e) {
                        LogHandler.saveLog(
                                "Failed to join sign out thread : "
                                        + e.getLocalizedMessage(), true
                        );
                    }
                }
//                                                runOnUiThread(() -> {
//                                                    if (isBackedUp[0]) {
//                                                        try {
//                                                            item.setEnabled(false);
//                                                            ViewGroup parentView = (ViewGroup) button.getParent();
//                                                            parentView.removeView(button);
//                                                        } catch (Exception e) {
//                                                            e.printStackTrace();
//                                                        }
//                                                    } else {
//                                                        try {
//                                                            button.setText(buttonText);
//                                                        } catch (Exception e) {
//                                                            e.printStackTrace();
//                                                        }
//                                                    }
//                                                });
            }
        });

//                                        deleteProfileJsonThread.start();
//        backUpJsonThread.start();
//        signOutThread.start();
//        uiThread.start();
        detachLinkedAccountsProfileJsonThread.start();

        //                                            boolean isDeletedFromAccounts = dbHelper.deleteFromAccountsTable(buttonText,"backup");
        //                                            boolean isAccountDeletedFromDriveTable = dbHelper.deleteAccountFromDriveTable(buttonText);
        //                                            dbHelper.deleteRedundantAsset();
        //                                            ViewGroup parentView = (ViewGroup) button.getParent();
        //                                            parentView.removeView(button);
        //                                            googleCloud.signOut(buttonText);
    }

    public static JsonObject editProfileJson(JsonObject profileJson, String emailToDelete) {
        try {
            JsonArray backupAccounts = profileJson.get("backupAccounts").getAsJsonArray();

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
            e.printStackTrace();
            return null;
        }
    }


    private static String uploadProfileContent(Drive service,String profileFolderId, String userEmail,JsonObject profileContent){
        String uploadFileId = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date();
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
                String profileFolderId = GoogleDrive.createOrGetSubDirectoryInStashSyncedAssetsFolder(userEmail,folder_name, false, null);
                deleteProfileFiles(service, profileFolderId);
                boolean isDeleted = checkDeletionStatus(service,profileFolderId);
                if(isDeleted){
                    String uploadedFileId = uploadProfileContent(service,profileFolderId,userEmail,profileContent);
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

    public static boolean isLinkedToAccounts(String driveBackupAccessToken, String userEmail){
        boolean[] isLinked = {false};
        Thread isLinkedToAccountsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    JsonObject resultJson = Profile.readProfileMapContent(userEmail,driveBackupAccessToken);
                    if(resultJson != null){
                        isLinked[0] = isNewJsonProfile(resultJson, userEmail);
                    }
                }catch (Exception e){
                    LogHandler.saveLog("Failed to join and run isLinkedToAccounts Thread : " + e.getLocalizedMessage(), true);
                }
            }
        });
        isLinkedToAccountsThread.start();
        try {
            isLinkedToAccountsThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join sign in to isLinkedToAccounts thread : " + e.getLocalizedMessage(), true);
        }
        return isLinked[0];
    }

}
