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

    public static void initializeParentFolder(String userEmail, String accessToken, boolean shouldInitSubFolders){
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
                DBHelper.updateAccounts(userEmail, updatedValues, "backup");

                if(shouldInitSubFolders){
                    ArrayList<String> subFolders = new ArrayList<>(Arrays.asList
                            (profileFolderName, databaseFolderName, assetsFolderName));
                    for(String subFolder: subFolders){
                        initializeSubFolder(service,parentFolderId,subFolder,userEmail);
                    }
                }
            } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        });

        initializeParentFolderThread.start();
        try{
            initializeParentFolderThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static void initializeSubFolder(Drive service, String parentFolderId, String folderName, String userEmail){
        Thread initializeSubFoldersThread = new Thread( () -> {
            String folderId = getSubFolderIdFromDrive(service,folderName,parentFolderId);
            Log.d("folders", "Folder :" + folderName + " and id: " + folderId);

            if(folderId == null){
                folderId = createSubFolder(service,parentFolderId,folderName);
            }

            HashMap<String, Object> updatedValues = new HashMap<String, Object>() {};
            if(folderName.equals(GoogleDriveFolders.databaseFolderName)){
                updatedValues.put("databaseFolderId", folderId);
            }else if(folderName.equals(GoogleDriveFolders.assetsFolderName)){
                updatedValues.put("assetsFolderId", folderId);
            }else if(folderName.equals(GoogleDriveFolders.profileFolderName)){
                updatedValues.put("profileFolderId", folderId);
            }
            DBHelper.updateAccounts(userEmail, updatedValues, "backup");

            Log.d("folders", "Folder :" + folderName + " and id: " + folderId);
        });

        initializeSubFoldersThread.start();
        try{
            initializeSubFoldersThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static String createSubFolder(Drive service, String parentFolderId, String subFolderName){
        final String[] folderId = {null};
        Thread createSubFolderThread = new Thread( () -> {
           try{
               File folderMetadata = new File();
               folderMetadata.setName(subFolderName);
               folderMetadata.setMimeType("application/vnd.google-apps.folder");
               folderMetadata.setParents(Collections.singletonList(parentFolderId));

               File subfolder = service.files().create(folderMetadata)
                       .setFields("id")
                       .execute();

               folderId[0] = subfolder.getId();
           }catch (Exception e) {
               FirebaseCrashlytics.getInstance().recordException(e);
           }
        });

        createSubFolderThread.start();
        try{
            createSubFolderThread.join();
        }catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        return folderId[0];
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

    public static String getParentFolderId(String userEmail, boolean isLogin, String accessToken){
        final String[] syncAssetsFolderId = {null};
        Thread getParentFolderIdThread = new Thread( () -> {
            try{
                if(isLogin){
                    Drive service = GoogleDrive.initializeDrive(accessToken);
                    syncAssetsFolderId[0] = getParentFolderIdFromDrive(service);
                }else{
                    syncAssetsFolderId[0] = DBHelper.getParentFolderIdFromDB(userEmail);
                    if(syncAssetsFolderId[0] == null){
                        initializeParentFolder(userEmail,accessToken, false);
                        syncAssetsFolderId[0] = DBHelper.getParentFolderIdFromDB(userEmail);
                    }
                }
            }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        });

        getParentFolderIdThread.start();
        try{
            getParentFolderIdThread.join();
        }catch (Exception e) {FirebaseCrashlytics.getInstance().recordException(e);}

        return syncAssetsFolderId[0];
    }

    public static String getSubFolderId(String userEmail, String folderName, String accessToken, boolean isLogin){
        final String[] folderId = {null};
        Thread getSubFolderIdThread = new Thread( () -> {
            try{
                Drive service;

                if(isLogin){
                    service = GoogleDrive.initializeDrive(accessToken);
                    String parentFolderId = getParentFolderIdFromDrive(service);
                    folderId[0] = getSubFolderIdFromDrive(service,folderName,parentFolderId);
                }else{
                    folderId[0] = DBHelper.getSubFolderIdFromDB(userEmail,folderName);
                    if (folderId[0] == null){
                        String finalAccessToken = DBHelper.getDriveBackupAccessToken(userEmail);
                        service = GoogleDrive.initializeDrive(finalAccessToken);
                        String parentFolderId = getParentFolderIdFromDrive(service);
                        initializeSubFolder(service, parentFolderId,folderName,userEmail);
                        folderId[0] = DBHelper.getSubFolderIdFromDB(userEmail,folderName);
                    }
                }
            }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e);}
        });

        getSubFolderIdThread.start();
        try{
            getSubFolderIdThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return folderId[0];
    }

    public static void deleteSubFolder(String subFolderName, String userEmail){
        Thread deleteSubFolderThread = new Thread( () -> {
            try{

                Log.d("unlink", "deleteSubFolder" + subFolderName + "from  :" + userEmail);
                String folderId = getSubFolderId(userEmail,subFolderName,null,false);

                Log.d("unlink", "Deleting folder " + subFolderName + " : " + folderId);
                String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);

                Drive service = GoogleDrive.initializeDrive(accessToken);
                service.files().delete(folderId).execute();

            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });

        deleteSubFolderThread.start();
        try{
            deleteSubFolderThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }
}
