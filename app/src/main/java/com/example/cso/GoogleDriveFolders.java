package com.example.cso;

import android.util.Log;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class GoogleDriveFolders {
    public static String parentFolderName = "libz_app";
    public static String assetsFolderName = "assets";
    public static String profileFolderName = "profile";
    public static String databaseFolderName = "database";
    public static String oldParentFolderName = "stash_synced_assets";
    public static String oldProfileFolderName = "stash_user_profile";
    public static String oldDatabaseFolderName = "libz_database";
    public static String oldAssetsFolderName = "assets";

    public static void initializeFolders(String userEmail, String accessToken){
        initializeParentFolder(userEmail,accessToken);
        initializeProfileFolder(userEmail,accessToken);
        initializeDatabaseFolder(userEmail,accessToken);
        initializeAssetsFolder(userEmail,accessToken);
    }
    public static void initializeParentFolder(String userEmail, String accessToken){
        Thread initializeParentFolderThread = new Thread(() -> {
            try{
                Drive service = GoogleDrive.initializeDrive(accessToken);
                String parentFolderId = getParentFolderIdFromDrive(service);

                Log.d("folders", "parent folder id:" + parentFolderId);

                if(parentFolderId == null){
                    parentFolderId = createParentFolder(service);
                }

                Log.d("folders", "parent folder id:" + parentFolderId);

                String finalParentFolderId = parentFolderId;
                HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                    put("parentFolderId", finalParentFolderId);
                }};
                MainActivity.dbHelper.updateAccounts(userEmail, updatedValues, "backup");

                ArrayList<String> subFolders = new ArrayList<>(Arrays.asList
                        (profileFolderName, databaseFolderName, assetsFolderName));
                for(String subFolder: subFolders){
                    initializeSubFolders(service,parentFolderId,subFolder);
                }
            } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        });

        initializeParentFolderThread.start();
        try{
            initializeParentFolderThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }


    private static void initializeSubFolders(Drive service,String parentFolderId, String folderName){
        Thread initializeSubFoldersThread = new Thread( () -> {
            String folderId = getSubFolderIdFromDrive(service,folderName,parentFolderId);
            Log.d("folders", "Folder :" + folderName + " and id: " + folderId);

            if(folderId == null){

            }

            Log.d("folders", "Folder :" + folderName + " and id: " + folderId);
        });

        initializeSubFoldersThread.start();
        try{
            initializeSubFoldersThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static String getSubFolderIdFromDrive(Drive service, String folderName, String parentFolderId){
        final String[] folderId = {null};
        Thread getSubFolderIdFromDriveThread = new Thread( () -> {
            try{
                String query = String.format("mimeType='application/vnd.google-apps.folder' " +
                        "and name='%s' and '%s' in parents and trashed=false", folderName, parentFolderId);
                FileList result = service.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id)")
                        .execute();

                if (!result.getFiles().isEmpty()) {
                    folderId[0] = result.getFiles().get(0).getId();
                }
            }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        });

        getSubFolderIdFromDriveThread.start();
        try{
            getSubFolderIdFromDriveThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return folderId[0];
    }

    private static String createParentFolder(Drive service){
        final String[] folderId = {null};
        Thread createParentFolderThread = new Thread( () -> {
            try{
                File folder_metadata = new File();
                folder_metadata.setName(parentFolderName);
                folder_metadata.setMimeType("application/vnd.google-apps.folder");

                File folder = service.files().create(folder_metadata)
                        .setFields("id")
                        .execute();

                folderId[0] = folder.getId();
            }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
        });

        createParentFolderThread.start();
        try{
            createParentFolderThread.join();
        }catch (Exception e)  { FirebaseCrashlytics.getInstance().recordException(e); }

        return folderId[0];
    }

    public static String getParentFolderIdFromDrive(Drive service){
        final String[] folderId = {null};
        Thread getParentFolderThread = new Thread( () -> {
            try{
                FileList files = service.files().list().execute();
                for (File file : files.getFiles()) {
                    if (file.getName().equals(parentFolderName)){
                        folderId[0] = file.getId();
                    }
                }
            }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
        });

        getParentFolderThread.start();
        try{
            getParentFolderThread.join();
        }catch (Exception e)  { FirebaseCrashlytics.getInstance().recordException(e); }

        return folderId[0];
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
        final String[] profileFolderId = {""};
        Thread initializeParentFolderThread = new Thread(() -> {
            try{
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });

        initializeParentFolderThread.start();
        try{

        }catch (Exception e) {FirebaseCrashlytics.getInstance().recordException(e); }
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
