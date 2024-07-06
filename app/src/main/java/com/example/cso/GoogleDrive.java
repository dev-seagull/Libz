package com.example.cso;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GoogleDrive {

    public GoogleDrive() {}
    public static String createOrGetStashSyncedAssetsFolderInDrive(String userEmail){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String syncAssetsFolderIdStr = null;
        Callable<String> createFolderTask = () -> {
            String syncAssetsFolderId = null;
            try {
                List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","refreshToken"});
                String driveBackupAccessToken = "";
                for(String[] account_row: account_rows){
                    String selectedUserEmail = account_row[0];
                    if(selectedUserEmail.equals(userEmail)){
                        String driveBackupRefreshToken = account_row[1];
                        driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();
                    }
                }

                Drive service = initializeDrive(driveBackupAccessToken);
                FileList result = getStashSyncedAssetsFolderInDrive(service);
                if (result != null && !result.getFiles().isEmpty()) {
                    syncAssetsFolderId = result.getFiles().get(0).getId();
                    System.out.println("sync asset folder id:" + syncAssetsFolderId);
                    return syncAssetsFolderId;
                }

                syncAssetsFolderId = createStashSyncedAssetFolder(service);
            }catch (Exception e){
                LogHandler.saveLog("Error in creating stash synced assets folder in drive : "+ userEmail + e.getLocalizedMessage(),true);
            }
            return syncAssetsFolderId;
        };
        Future<String> future = executor.submit(createFolderTask);
        try{
            syncAssetsFolderIdStr = future.get();
        }catch (Exception e){
            LogHandler.saveLog("Error in creating stash synced assets folder in drive : "+ userEmail + e.getLocalizedMessage(),true);
        }
        return syncAssetsFolderIdStr;
    }

    public static String createOrGetSubDirectoryInStashSyncedAssetsFolder(String userEmail, String folderName){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String folderIdStr = null;
        Callable<String> createFolderTask = () -> {
            String folderId = null;
            try {
                List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","refreshToken"});
                String driveBackupAccessToken = "";
                for(String[] account_row: account_rows){
                    String selectedUserEmail = account_row[0];
                    if(selectedUserEmail.equals(userEmail)){
                        String driveBackupRefreshToken = account_row[1];
                        driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();
                    }
                }

                Drive service = initializeDrive(driveBackupAccessToken);
                String parentFolderId =  GoogleDrive.createOrGetStashSyncedAssetsFolderInDrive(userEmail);
                if (parentFolderId == null){
                    LogHandler.saveLog("No parent folder was found in Google Drive back up account when creating sub directory",true);
                }
                FileList result = getSubDirectoryInStashSyncedAssetsFolder(service,folderName,parentFolderId);
                if (result != null && !result.getFiles().isEmpty()) {
                    folderId = result.getFiles().get(0).getId();
                    System.out.println("sync asset folder id:" + folderId);
                    return folderId;
                }

                folderId = createSubDirectoryInStashSyncedAssetsFolder(service,folderName,parentFolderId);
            }catch (Exception e){
                LogHandler.saveLog("Error in creating stash synced assets folder in drive : "+ userEmail + e.getLocalizedMessage(),true);
            }
            return folderId;
        };
        Future<String> future = executor.submit(createFolderTask);
        try{
            folderIdStr = future.get();
        }catch (Exception e){
            LogHandler.saveLog("Error in creating stash synced assets folder in drive : "+ userEmail + e.getLocalizedMessage(),true);
        }
        return folderIdStr;
    }


    private static FileList getStashSyncedAssetsFolderInDrive(Drive service){
        FileList result = null;
        try{
            String folderName = "stash_synced_assets";
            String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and 'root' in parents and trashed=false";
            result = service.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get stash synced folder in drive", true);
        }
        return result;
    }

    private static String createStashSyncedAssetFolder(Drive service){
        String syncAssetsFolderId = "";
        try{
            com.google.api.services.drive.model.File folder_metadata =
                    new com.google.api.services.drive.model.File();
            String folderName = "stash_synced_assets";
            folder_metadata.setName(folderName);
            folder_metadata.setMimeType("application/vnd.google-apps.folder");
            com.google.api.services.drive.model.File folder = service.files().create(folder_metadata).setFields("id").execute();
            syncAssetsFolderId = folder.getId();
        }catch (Exception e){
            LogHandler.saveLog("Failed to create stash synced asset folder : " + e.getLocalizedMessage(), true);
        }
        return syncAssetsFolderId;
    }

    private static FileList getSubDirectoryInStashSyncedAssetsFolder(Drive service,String folderName, String parentFolderId){
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and '" + parentFolderId + "' in parents and trashed=false";
        FileList result = null;
        try{
            result = service.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get sub directory in stash synced asset folder :" + e.getLocalizedMessage(), true);
        }
        return result;
    }

    private static String createSubDirectoryInStashSyncedAssetsFolder(Drive service,String folderName, String parentFolderId){
        File folderMetadata = new File();
        File folder = null;
        try{
            folderMetadata.setName(folderName);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            folderMetadata.setParents(Collections.singletonList(parentFolderId));
            folder = service.files().create(folderMetadata)
                    .setFields("id")
                    .execute();
        }catch (Exception e){
            LogHandler.saveLog("Failed to create sub directory in stash synced asset folder :" + e.getLocalizedMessage(), true);
        }
        return folder.getId();
    }

    public static ArrayList<DriveAccountInfo.MediaItem> getMediaItems(String userEmail) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final ArrayList<DriveAccountInfo.MediaItem> mediaItems = new ArrayList<>();
        Callable<ArrayList<DriveAccountInfo.MediaItem>> backgroundTask = () -> {
            try {
                String assetsSubFolderId = createOrGetSubDirectoryInStashSyncedAssetsFolder(userEmail,"assets");
                if (assetsSubFolderId == null){
                    LogHandler.saveLog("No folder was found in Google Drive back up account",false);
                    return mediaItems;
                }
                String accessToken = "";
                String[] selected_accounts_columns = {"userEmail","refreshToken"};
                List<String[]> account_rows = DBHelper.getAccounts(selected_accounts_columns);
                for (String[] account_row : account_rows) {
                    if (account_row[0].equals(userEmail)) {
                        accessToken = MainActivity.googleCloud.updateAccessToken(account_row[1]).getAccessToken();
                    }
                }
                Drive driveService = initializeDrive(accessToken);
                String nextPageToken = null;
                do{
                    FileList result = driveService.files().list()
                            .setFields("files(id, name, sha256Checksum),nextPageToken")
                            .setQ("'" +assetsSubFolderId + "' in parents and trashed=false")
                            .setPageToken(nextPageToken)
                            .execute();
                    List<com.google.api.services.drive.model.File> files = result.getFiles();
                    if (files != null && !result.getFiles().isEmpty()) {
                        for (File file : files) {
                            if (Media.isVideo(GoogleCloud.getMimeType(file.getName())) |
                                    Media.isImage(GoogleCloud.getMimeType(file.getName()))){
                                DriveAccountInfo.MediaItem mediaItem = new DriveAccountInfo.MediaItem(file.getName(),
                                        file.getSha256Checksum().toLowerCase(), file.getId());
                                mediaItems.add(mediaItem);
                            }else{
                                System.out.println("File " + file.getName() + " is not a media item.");
                            }
                        }
                    }    nextPageToken = result.getNextPageToken();
                }while (nextPageToken != null);

                LogHandler.saveLog(mediaItems.size() + " files were found in Google Drive back up account",false);

                return mediaItems;
            }catch (Exception e) {
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
                        isDeleted[0] = deleteMediaItem(accessToken, fileId, assetId, fileName);
                    }catch (Exception e){
                        LogHandler.saveLog("error in deleting duplicated media items in drive: " + e.getLocalizedMessage());
                    }
                    return isDeleted[0];
                };
                Future<Boolean> future = executor.submit(backgroundTask);
                Boolean isDeletedFuture = false;
                try{
                    isDeletedFuture = future.get();
                }catch (Exception e ){
                    LogHandler.saveLog("Exception when trying to delete file from drive back up: " + e.getLocalizedMessage());
                }
                if(isDeletedFuture){
                    MainActivity.dbHelper.deleteFileFromDriveTable(fileHash, id, assetId, fileId , userEmail);
                }

            }else{
                fileHashChecker.add(fileHash);
            }
        }
    }

    private static boolean deleteMediaItem(String accessToken, String fileId, String assetId, String fileName){
        boolean isDeleted = false;
        try{
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
                    isDeleted = true;
                    break;
                }else{
                    LogHandler.saveLog("Retrying to delete duplicated file " + fileName+
                            "from Drive back up account" +
                            " with response code of " + responseCode);
                    isDeleted = false;
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to delete media item: " + e.getLocalizedMessage(), true);
        }
        return isDeleted;
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
                    .setApplicationName("stash")
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
                    .setFields("files(id, name)")
                    .execute();

             files = resultJson.getFiles();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get drive folder files : " + e.getLocalizedMessage(), true);
        }
        return files;
    }

    public static void startUpdateDriveFilesThread(){
        LogHandler.saveLog("Starting startUpdateDriveFilesThread", false);
        try {
            Thread updateDriveFilesThread = new Thread(() -> {
                List<String[]> account_rows = MainActivity.dbHelper.getAccounts(new String[]{"userEmail","type"});
                for(String[] account_row : account_rows){
                    String type = account_row[1];
                    if (type.equals("backup")){
                        String userEmail = account_row[0];
                        ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(userEmail);
                        System.out.println(driveMediaItems.size()+" drive media items found");
                        LogHandler.saveLog(driveMediaItems.size()+" drive media items found", false);
                        for(DriveAccountInfo.MediaItem driveMediaItem: driveMediaItems){
                            Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
                            if (last_insertId != -1) {
                                MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
                                        driveMediaItem.getHash(), userEmail);
                            } else {
                                LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
                            }
                        }
                    }
                }
            });
            updateDriveFilesThread.start();
            try {
                updateDriveFilesThread.join();
            } catch (Exception e) {
                LogHandler.saveLog("Failed to join update drive files thread in  " +
                            " updateDriveAccountsFilesStatus", true);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to run delete redundant drive files from account : " +e.getLocalizedMessage(), true);
        }
        LogHandler.saveLog("Finished startUpdateDriveFilesThread", false);
    }

    public static void deleteRedundantDriveFilesFromAccount(String userEmail) {
        try {
            Thread deleteRedundantDrive = new Thread(() -> {
                ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(userEmail);
                ArrayList<String> driveFileIds = new ArrayList<>();

                for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                    String fileId = driveMediaItem.getId();
                    driveFileIds.add(fileId);
                }
                MainActivity.dbHelper.deleteRedundantDriveFromDB(driveFileIds, userEmail);
            });
            deleteRedundantDrive.start();
            try {
                deleteRedundantDrive.join();
            } catch (Exception e) {
                LogHandler.saveLog("Failed to join delete " +
                        " redundant drive from accounts: " + e.getLocalizedMessage(), true);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to run delete redundant drive files from account : " +e.getLocalizedMessage(), true);
        }
    }

    private static void startDeleteRedundantDriveThread(){
        LogHandler.saveLog("Starting startDeleteRedundantDriveThread", false);
        Thread deleteRedundantDriveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<String[]> account_rows = MainActivity.dbHelper.getAccounts(new String[]{"userEmail", "type"});
                for(String[] account_row : account_rows) {
                    String type = account_row[1];
                    if(type.equals("backup")){
                        String userEmail = account_row[0];
                        ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(userEmail);
                        ArrayList<String> driveFileIds = new ArrayList<>();
                        for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                            String fileId = driveMediaItem.getId();
                            driveFileIds.add(fileId);
                        }
                        MainActivity.dbHelper.deleteRedundantDriveFromDB(driveFileIds, userEmail);
                    }
                }
            }
        });
        deleteRedundantDriveThread.start();
        try{
            deleteRedundantDriveThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join delete redundant drive thread: " + e.getLocalizedMessage(), true );
        }
        LogHandler.saveLog("Finished startDeleteRedundantDriveThread", false);
    }

    private static void startDeleteDuplicatedInDriveThread() {
        LogHandler.saveLog("Starting startDeleteDuplicatedInDriveThread", false);
        Thread deleteDuplicatedInDriveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] columns = {"refreshToken","userEmail", "type"};
                List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columns);
                for(String[] account_row : account_rows) {
                    String type = account_row[2];
                    if(type.equals("backup")){
                        String userEmail = account_row[1];
                        String refreshToken = account_row[0];
                        String accessToken = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                        GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                    }
                }
            }
        });
        deleteDuplicatedInDriveThread.start();
        try {
            deleteDuplicatedInDriveThread.join();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to join delete duplicated in drive thread: " + e.getLocalizedMessage(), true);
        }
        LogHandler.saveLog("Finished startDeleteDuplicatedInDriveThread", false);
    }

    private static void startUpdateDriveStorageThread() {
        LogHandler.saveLog("Starting startUpdateDriveStorageThread", false);
        Thread updateDriveStorage = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] columns = {"refreshToken","userEmail", "type"};
                List<String[]> account_rows = MainActivity.dbHelper.getAccounts(columns);
                for(String[] account_row : account_rows) {
                    String type = account_row[2];
//                    if(type.equals("backup")){
                        String userEmail = account_row[1];
                        String refreshToken = account_row[0];
                        String accessToken = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                        GoogleCloud.Storage storage = MainActivity.googleCloud.getStorage(new GoogleCloud.Tokens(accessToken,refreshToken));
                        Map<String, Object> updatedValues = new HashMap<String, Object>() {{
                            put("totalStorage", storage.getTotalStorage());
                        }};
                        MainActivity.dbHelper.updateAccounts(userEmail, updatedValues, type);
                        updatedValues = new HashMap<String, Object>() {{
                            put("usedStorage", storage.getUsedStorage());
                         }};
                        MainActivity.dbHelper.updateAccounts(userEmail, updatedValues, type);
                        updatedValues = new HashMap<String, Object>() {{
                            put("usedInDriveStorage", storage.getUsedInDriveStorage());
                        }};
                        MainActivity.dbHelper.updateAccounts(userEmail, updatedValues, type);
                        updatedValues = new HashMap<String, Object>() {{
                            put("UsedInGmailAndPhotosStorage", storage.getUsedInGmailAndPhotosStorage());
                         }};
                        MainActivity.dbHelper.updateAccounts(userEmail, updatedValues, type);
//                    }
                }
            }
        });
        updateDriveStorage.start();
        try {
            updateDriveStorage.join();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to join update drive storage thread: " + e.getLocalizedMessage(), true);
        }
        LogHandler.saveLog("Finished startDeleteDuplicatedInDriveThread", false);
    }

    public static void startThreads(){
        startUpdateDriveStorageThread();
        startDeleteRedundantDriveThread();
        startUpdateDriveFilesThread();
        startDeleteDuplicatedInDriveThread();
    }

    private void refactorFolders(){
        List<String[]> accounts_rows = DBHelper.getAccounts(new String[]{"refreshToken","userEmail"});
        try{
            for(String[] account_row: accounts_rows){
                String accessToken = MainActivity.googleCloud.updateAccessToken(account_row[0]).getAccessToken();
                Drive service = initializeDrive(accessToken);
                FileList stashSyncedAssetsFolders_result = getStashSyncedAssetsFolderInDrive(service);
                int stashSyncedAssetsFolders_size = stashSyncedAssetsFolders_result.getFiles().size();
                System.out.println("Number of stash synced asset folders: " + stashSyncedAssetsFolders_size);
                if (stashSyncedAssetsFolders_size > 1) {

                }

                String parentFolderId =  GoogleDrive.createOrGetStashSyncedAssetsFolderInDrive(account_row[1]);
                if (parentFolderId == null){
                    LogHandler.saveLog("No parent folder was found in Google Drive back up account when creating sub directory and refactoring",true);
                }
                FileList stashDatabase_result = getSubDirectoryInStashSyncedAssetsFolder(service,"stash_database",parentFolderId);
                int stashDatabaseFolders_size = stashDatabase_result.getFiles().size();
                System.out.println("Number of stash database folders: " + stashDatabaseFolders_size);
                if (stashDatabaseFolders_size > 1){

                }

                FileList stashProfileFolders_result = getSubDirectoryInStashSyncedAssetsFolder(service,"stash_user_profile",parentFolderId);
                int stashProfileFolders_size = stashProfileFolders_result.getFiles().size();
                System.out.println("Number of stash profile folders: " + stashProfileFolders_size);
                if (stashProfileFolders_size > 1){

                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to refactor folders: " + e.getLocalizedMessage());
        }
    }
}

