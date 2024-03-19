package com.example.cso;

import android.os.Build;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GoogleDrive {

    public GoogleDrive() {}

    public static class MediaItem{
        private String mediaItemId;
        private String mediaItemName;
        private  String mediaItemHash;
        private Long videoDuration;

        public MediaItem(String mediaItemId, String mediaItemName,
                         String mediaItemHash, Long videoDuration) {
            this.mediaItemId = mediaItemId;
            this.mediaItemName = mediaItemName;
            this.mediaItemHash = mediaItemHash;
            this.videoDuration = videoDuration;
        }
        public String getMediaItemId() {return mediaItemId;}
        public String getMediaItemName() {return mediaItemName;}
        public Long getVideoDuration() {return videoDuration;}
        public String getMediaItemHash() {return mediaItemHash;}
    }

    public static String[] goodEmailAccount(){
        // some check like capacity
        String[] drive_backup_selected_columns = {"userEmail", "type", "accessToken","folderId"};
        List<String[]> drive_backUp_accounts = MainActivity.dbHelper.getAccounts(drive_backup_selected_columns);
        try {
            for (String[] drive_backUp_account : drive_backUp_accounts) {
                if (drive_backUp_account[1].equals("backup")) {
                    return  drive_backUp_account;
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("error in getting backup accounts from db : " + e.getLocalizedMessage());
        }
        return null;
    }


    public static void createStashSyncedAssetsFolderInDrive(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> createFolderTask = () -> {
            try{
                String[] selected_columns = {"userEmail","type","accessToken","folderId"};
                List<String[]> account_rows = MainActivity.dbHelper.getAccounts(selected_columns);
                String folderId = null;
                for (String[] account_row : account_rows){
                    if (!account_row[1].equals("backup")){
                        continue;
                    }
                    if (account_row[3] != null){
                        continue;
                    }
                    try{
                            String driveBackupAccessToken = account_row[2];
                            Drive service = initializeDrive(driveBackupAccessToken);

                            String folder_name = "stash_synced_assets";
                            com.google.api.services.drive.model.File folder = null;

                            FileList fileList = service.files().list()
                                    .setQ("mimeType='application/vnd.google-apps.folder' and name='"
                                            + folder_name + "'")
                                    .setSpaces("drive")
                                    .setFields("files(id)")
                                    .execute();
                            List<com.google.api.services.drive.model.File> driveFolders = fileList.getFiles();
                            for(com.google.api.services.drive.model.File driveFolder: driveFolders){
                                folderId = driveFolder.getId();
                            }

                            if (folderId == null) {
                                com.google.api.services.drive.model.File folder_metadata =
                                        new com.google.api.services.drive.model.File();
                                folder_metadata.setName(folder_name);
                                folder_metadata.setMimeType("application/vnd.google-apps.folder");
                                folder = service.files().create(folder_metadata)
                                        .setFields("id").execute();

                                folderId = folder.getId();
                            }
                            if (folderId == null){
                                LogHandler.saveLog("Failed to create folder in google drive " + account_row[0],true);
                            }

                            DBHelper.updateFileIdInAccounts(folderId,account_row[0]);

                        }catch (Exception e){
                            LogHandler.saveLog("Failed to create stash synced assets folders in drive : " +
                                    e.getLocalizedMessage(), true);
                        }
                    }
            }catch (Exception e){
                LogHandler.saveLog("Failed to create or get stash synced assets folder id : " + e.getLocalizedMessage(), true);
            }
            return true;
        };
        Future<Boolean> future = executor.submit(createFolderTask);
        boolean listOfAllFoldersCreated = false;
        try{
            listOfAllFoldersCreated = future.get();
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
    }

    public static ArrayList<DriveAccountInfo.MediaItem> getMediaItems(String accessToken) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        final ArrayList<DriveAccountInfo.MediaItem> mediaItems = new ArrayList<>();
        Callable<ArrayList<DriveAccountInfo.MediaItem>> backgroundTask = () -> {
            try {
                String[] drive_backup_selected_columns = {"userEmail", "type", "accessToken","folderId"};
                String folderId ="";
                List<String[]> drive_backUp_accounts = MainActivity.dbHelper.getAccounts(drive_backup_selected_columns);
                for (String[] account_row:drive_backUp_accounts){
                    if (account_row[2].equals(accessToken)){
                        folderId = account_row[3];
                    }
                }
                if (folderId == null){
                    LogHandler.saveLog("No folder was found in Google Drive back up account",false);
                    return mediaItems;
                }
                final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
                final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                HttpRequestInitializer httpRequestInitializer = request -> {
                    System.out.println("access token for get files : " + accessToken);
                    request.getHeaders().setAuthorization("Bearer " + accessToken);
                    request.getHeaders().setContentType("application/json");
                };

                Drive driveService = new Drive.Builder(netHttpTransport, jsonFactory, httpRequestInitializer)
                        .setApplicationName("cso").build();
                String nextPageToken = null;
                do{
                    System.out.println("page token is: " + nextPageToken);
                    FileList result = driveService.files().list()
                            .setFields("files(id, name, sha256Checksum),nextPageToken")
                            .setQ("'" +folderId + "' in parents")
                            .setPageToken(nextPageToken)
                            .execute();
                    System.out.println("checking folderId : "+ folderId + " and trashed = false" +
                            "files Are : ");
                    List<com.google.api.services.drive.model.File> files = result.getFiles();
                    if (files != null && !files.isEmpty()) {
                        for (File file : files) {
                            System.out.println("File " + file.getName());
                            if (GooglePhotos.isVideo(GoogleCloud.getMemeType(file.getName())) |
                                    GooglePhotos.isImage(GoogleCloud.getMemeType(file.getName()))){
                                DriveAccountInfo.MediaItem mediaItem = new DriveAccountInfo.MediaItem(file.getName(),
                                        file.getSha256Checksum().toLowerCase(), file.getId());
                                mediaItems.add(mediaItem);
                            }else{
                                System.out.println("File " + file.getName() + " is not a media item 000000000000000000000000000000000000000");
                            }
                        }
                    }    nextPageToken = result.getNextPageToken();
                }while (nextPageToken != null);

                if(mediaItems.size() != 0 ){
                    LogHandler.saveLog( mediaItems.size() + " files were found in Google Drive back up account",false);
                }else{
                    LogHandler.saveLog("No file was found in Google Drive back up account",false);
                }
                return mediaItems;
            }catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
                LogHandler.saveLog("Error when trying to get files from google drive: " + e.getLocalizedMessage());
            }
            return mediaItems;
        };
        Future<ArrayList<DriveAccountInfo.MediaItem>> future = executor.submit(backgroundTask);
        ArrayList<DriveAccountInfo.MediaItem> uploadFileIDs_fromFuture = null;
        try {
            uploadFileIDs_fromFuture = future.get();
        } catch (Exception e) {
            LogHandler.saveLog("Error when trying to get drive files from future: " + e.getLocalizedMessage());
        }
        return uploadFileIDs_fromFuture;
    }

    public static void deleteDuplicatedMediaItems(String accessToken, String userEmail){
        String[] driveColumns = {"fileHash", "id","assetId", "fileId", "fileName", "userEmail"};
        List<String[]> drive_rows = MainActivity.dbHelper.getDriveTable(driveColumns, userEmail);

        Map<String, Integer> assetIdCount = new HashMap<>();

        for (String[] drive_row : drive_rows) {
            String assetId = drive_row[2];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                assetIdCount.put(assetId, assetIdCount.getOrDefault(assetId, 0) + 1);
            }
        }

//        for (Map.Entry<String, Integer> entry : assetIdCount.entrySet()) {
//            LogHandler.saveLog("assetId: " + entry.getKey() + ", Count: " + entry.getValue(),false);
//        }

        ArrayList<String> fileHashChecker = new ArrayList<>();
        for(String[] drive_row: drive_rows){
            String fileHash = drive_row[0];
            String id= drive_row[1];
            String assetId = drive_row[2];
            String fileId = drive_row[3];
            String fileName = drive_row[4];
            if(fileHashChecker.contains(fileHash)){
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Callable<Boolean> backgroundTask = () -> {
                   Boolean[] isDeleted = new Boolean[1];
                    try{
                        System.out.println("for test the drive file id is:  " + fileId);

                        URL url = new URL("https://www.googleapis.com/drive/v3/files/" + fileId);

                        for(int i=0; i<3; i++){
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("DELETE");
                            connection.setRequestProperty("Content-type", "application/json");
                            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                            int responseCode = connection.getResponseCode();
                            LogHandler.saveLog("responseCode of deleting duplicate drive : " + responseCode,false);
                            if(responseCode == HttpURLConnection.HTTP_NO_CONTENT){
                                LogHandler.saveLog("Deleting Duplicated file in backup drive with fileId :" +
                                        fileId  + " and assetId : " + assetId + " and fileName : " +
                                        fileName,false);
                                isDeleted[0] = true;
                                break;
                            }else{
                                LogHandler.saveLog("Retrying to delete duplicated file " + fileName+
                                        "from Drive back up account" +
                                        " with response code of " + responseCode);
                                isDeleted[0] = false;
                            }
                        }
                    }catch (Exception e){
                        LogHandler.saveLog("error in deleting duplicated media items in drive: " + e.getLocalizedMessage());
                    }
                    System.out.println("is deleted for test check check: " + isDeleted[0]);
                    return isDeleted[0];
                };
                Future<Boolean> future = executor.submit(backgroundTask);
                Boolean isDeletedFuture = false;
                try{
                    isDeletedFuture = future.get();
                }catch (Exception e ){
                    LogHandler.saveLog("Exception when trying to delete file from drive back up: " + e.getLocalizedMessage());
                }
                System.out.println("is deleted future for test check check: " + isDeletedFuture);
                if(isDeletedFuture == true){
                    MainActivity.dbHelper.deleteFileFromDriveTable(fileHash, id, assetId, fileId , userEmail);
                }

            }else if(!fileHashChecker.contains(fileHash)){
                fileHashChecker.add(fileHash);

            }
        }
    }

    public static Drive initializeDrive(String accessToken){
        Drive service = null;
        try{
            NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
            String bearerToken = "Bearer " + accessToken;
            HttpRequestInitializer requestInitializer = request -> {
                request.getHeaders().setAuthorization(bearerToken);
                request.getHeaders().setContentType("application/json");
            };
            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                    .setApplicationName("cso")
                    .build();

        }catch (Exception e){
            LogHandler.saveLog("Failed to initialize DRIVE : " + e.getLocalizedMessage(), true);
        }
        return service;
    }

    public static List<com.google.api.services.drive.model.File> getDriveFolderFiles(Drive service, String folderId){
        List<com.google.api.services.drive.model.File> files = null;
        try{
            String query = "'" + folderId + "' in parents and trashed=false";
            FileList resultJson = service.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .execute();

             files = resultJson.getFiles();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get drive folder files : " + e.getLocalizedMessage(), true);
        }
        return files;
    }

}