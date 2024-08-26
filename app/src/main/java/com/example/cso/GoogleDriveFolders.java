package com.example.cso;

import android.util.Log;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GoogleDriveFolders {
    public static String parentFolderName = "stash_synced_assets";
    public static String assetsFolderName = "assets";
    public static String profileFolderName = "stash_user_profile";
    public static String databaseFolderName = "libz_database";

    public static void initializeAllFolders(String userEmail, String accessToken){
        System.out.println("initializing all folders for user " + userEmail + "with access token " + accessToken);
        initializeParentFolder(userEmail,accessToken);
        initializeProfileFolder(userEmail,accessToken);
        initializeDatabaseFolder(userEmail,accessToken);
        initializeAssetsFolder(userEmail,accessToken);
    }
    public static void initializeParentFolder(String userEmail, String accessToken){
        Drive service = GoogleDrive.initializeDrive(accessToken);
        String syncAssetsFolderId = searchForParentFolder(service);
        if (syncAssetsFolderId != null){
            String finalSyncAssetsFolderId = syncAssetsFolderId;
            HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                put("parentFolderId", finalSyncAssetsFolderId);
            }};
            MainActivity.dbHelper.updateAccounts(userEmail,updatedValues, "backup");
        }else{
            File folder_metadata = new File();
            try{
                folder_metadata.setName(parentFolderName);
                folder_metadata.setMimeType("application/vnd.google-apps.folder");
                File folder = service.files().create(folder_metadata)
                        .setFields("id")
                        .execute();

                syncAssetsFolderId = folder.getId();
                Log.d("folder","stash_synced_assets id:" + syncAssetsFolderId);
                String finalSyncAssetsFolderId = syncAssetsFolderId;
                HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                    put("parentFolderId", finalSyncAssetsFolderId);
                }};
                MainActivity.dbHelper.updateAccounts(userEmail,updatedValues, "backup");

            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
    }

    public static String searchForParentFolder(Drive service){
        try{
            FileList files = service.files().list().execute();
            for (File file : files.getFiles()) {
                if (file.getName().equals(parentFolderName)){
                    return file.getId();
                }
            }
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
        return null;
    }

    public static String getParentFolderId(String userEmail){
        String syncAssetsFolderId = DBHelper.getParentFolderIdFromDB(userEmail);
        if (syncAssetsFolderId != null){
            return syncAssetsFolderId;
        }else{
            String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);
            initializeParentFolder(userEmail,accessToken);
            return DBHelper.getParentFolderIdFromDB(userEmail);
        }
    }


    public static void initializeAssetsFolder(String userEmail, String accessToken){
        Drive service = GoogleDrive.initializeDrive(accessToken);
        String assetsFolderId = "";
        File folder_metadata = new File();
        try{
            folder_metadata.setName(assetsFolderName);
            folder_metadata.setMimeType("application/vnd.google-apps.folder");
            folder_metadata.setParents(Collections.singletonList(getParentFolderId(userEmail)));
            File folder = service.files().create(folder_metadata)
                    .setFields("id")
                    .execute();

            assetsFolderId = folder.getId();
            Log.d("folder","assetsFolderId id:" + assetsFolderId);
            String finalAssetsFolderId = assetsFolderId;
            HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                put("assetsFolderId", finalAssetsFolderId);
            }};
            MainActivity.dbHelper.updateAccounts(userEmail,updatedValues, "backup");

        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static String getAssetsFolderId(String userEmail){
        String assetsFolderId = DBHelper.getAssetsFolderId(userEmail);
        if (assetsFolderId != null){
            return assetsFolderId;
        }else{
            String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);
            initializeAssetsFolder(userEmail,accessToken);
            return DBHelper.getAssetsFolderId(userEmail);
        }
    }


    public static void initializeProfileFolder(String userEmail, String accessToken){
        Drive service = GoogleDrive.initializeDrive(accessToken);
        String profileFolderId = "";
        File folder_metadata = new File();
        try{
            folder_metadata.setName(profileFolderName);
            folder_metadata.setMimeType("application/vnd.google-apps.folder");
            folder_metadata.setParents(Collections.singletonList(getParentFolderId(userEmail)));
            File folder = service.files().create(folder_metadata)
                    .setFields("id")
                    .execute();

            profileFolderId = folder.getId();
            Log.d("folder","profileFolderId id:" + profileFolderId);
            String finalProfileFolderId = profileFolderId;
            HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                put("profileFolderId", finalProfileFolderId);
            }};
            MainActivity.dbHelper.updateAccounts(userEmail,updatedValues, "backup");

        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static String getProfileFolderId(String userEmail){
        String profileFolderId = DBHelper.getProfileFolderIdFromDB(userEmail);
        if (profileFolderId != null){
            return profileFolderId;
        }else{
            String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);
            initializeProfileFolder(userEmail,accessToken);
            return DBHelper.getProfileFolderIdFromDB(userEmail);
        }
    }

    public static void initializeDatabaseFolder(String userEmail,String accessToken){
        Drive service = GoogleDrive.initializeDrive(accessToken);
        String databaseFolderId = "";
        File folder_metadata = new File();
        try{
            folder_metadata.setName(databaseFolderName);
            folder_metadata.setMimeType("application/vnd.google-apps.folder");
            folder_metadata.setParents(Collections.singletonList(getParentFolderId(userEmail)));
            File folder = service.files().create(folder_metadata)
                    .setFields("id")
                    .execute();

            databaseFolderId = folder.getId();
            Log.d("folder","databaseFolderId id:" + databaseFolderId);
            String finalDatabaseFolderId = databaseFolderId;
            HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                put("databaseFolderId", finalDatabaseFolderId);
            }};
            MainActivity.dbHelper.updateAccounts(userEmail,updatedValues, "backup");

        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static String getDatabaseFolderId(String userEmail){
        String databaseFolderId = DBHelper.getDatabaseFolderIdFromDB(userEmail);
        if (databaseFolderId != null){
            return databaseFolderId;
        }else{
            String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);
            initializeDatabaseFolder(userEmail,accessToken);
            return DBHelper.getDatabaseFolderIdFromDB(userEmail);
        }
    }



}
