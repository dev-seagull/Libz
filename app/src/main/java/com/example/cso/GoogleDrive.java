package com.example.cso;

import static com.example.cso.MainActivity.signInToBackUpLauncher;

import android.app.ProgressDialog;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import net.sqlcipher.database.SQLiteDebug;

import java.io.IOException;
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

    public static int CURRENT_DRIVE_ACCOUNT_INDEX = 0;

    public GoogleDrive() {}
    public static String createOrGetStashSyncedAssetsFolderInDrive(String userEmail, boolean isLogin, String accessToken){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String syncAssetsFolderIdStr = null;
        Callable<String> createFolderTask = () -> {
            String syncAssetsFolderId = null;
            try {
                String driveBackupAccessToken = "";
                if(isLogin){
                    driveBackupAccessToken = accessToken;
                }else{
                    List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","refreshToken"});
                    for(String[] account_row: account_rows){
                        String selectedUserEmail = account_row[0];
                        if(selectedUserEmail.equals(userEmail)){
                            String driveBackupRefreshToken = account_row[1];
                            driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();
                        }
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

    public static String createOrGetSubDirectoryInStashSyncedAssetsFolder(String userEmail, String folderName, boolean isLogin, String accessToken){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String folderIdStr = null;
        Callable<String> createFolderTask = () -> {
            String folderId = null;
            try {
                String driveBackupAccessToken = "";
                if(isLogin){
                    driveBackupAccessToken = accessToken;
                }else{
                    List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","refreshToken"});
                    for(String[] account_row: account_rows){
                        String selectedUserEmail = account_row[0];
                        if(selectedUserEmail.equals(userEmail)){
                            String driveBackupRefreshToken = account_row[1];
                            driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();
                        }
                    }
                }

                Drive service = initializeDrive(driveBackupAccessToken);
                String parentFolderId =  GoogleDrive.createOrGetStashSyncedAssetsFolderInDrive(userEmail, isLogin, driveBackupAccessToken);
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
            File folder_metadata =
                    new File();
            String folderName = "stash_synced_assets";
            folder_metadata.setName(folderName);
            folder_metadata.setMimeType("application/vnd.google-apps.folder");
            File folder = service.files().create(folder_metadata).setFields("id").execute();
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

    public static ArrayList<DriveAccountInfo.MediaItem> getMediaItems(String userEmail, boolean isLogin, String accessToken) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final ArrayList<DriveAccountInfo.MediaItem> mediaItems = new ArrayList<>();
        Callable<ArrayList<DriveAccountInfo.MediaItem>> backgroundTask = () -> {
            try {
                String backupAccessToken = "";
                if(isLogin){
                    backupAccessToken = accessToken;
                }else{
                    String[] selected_accounts_columns = {"userEmail","refreshToken"};
                    List<String[]> account_rows = DBHelper.getAccounts(selected_accounts_columns);
                    for (String[] account_row : account_rows) {
                        if (account_row[0].equals(userEmail)) {
                            backupAccessToken = MainActivity.googleCloud.updateAccessToken(account_row[1]).getAccessToken();
                        }
                    }
                }
                String assetsSubFolderId = createOrGetSubDirectoryInStashSyncedAssetsFolder(userEmail,"assets", isLogin, backupAccessToken);
                if (assetsSubFolderId == null){
                    LogHandler.saveLog("No folder was found in Google Drive back up account",false);
                    return mediaItems;
                }

                Drive driveService = initializeDrive(backupAccessToken);
                String nextPageToken = null;
                do{
                    FileList result = driveService.files().list()
                            .setFields("files(id, name, sha256Checksum),nextPageToken")
                            .setQ("'" +assetsSubFolderId + "' in parents and trashed=false")
                            .setPageToken(nextPageToken)
                            .execute();
                    List<File> files = result.getFiles();
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

    public static List<File> getDriveFolderFiles(Drive service, String folderId){
        List<File> files = null;
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
                        ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(userEmail, false, null);
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
                ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(userEmail, false, null);
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
                        ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(userEmail, false, null);
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

    public static void startUpdateDriveStorageThread() {
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

                String parentFolderId =  GoogleDrive.createOrGetStashSyncedAssetsFolderInDrive(account_row[1], false, null);
                if (parentFolderId == null){
                    LogHandler.saveLog("No parent folder was found in Google Drive back up account when creating sub directory and refactoring",true);
                }
                FileList stashDatabase_result = getSubDirectoryInStashSyncedAssetsFolder(service,"libz_database",parentFolderId);
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

    public static void moveFromSourceToDestinationAccounts(String sourceUserEmail,boolean ableToMoveAllAssets,int movableSize){
        boolean[] hasMovedAllAssets = {false};
        Thread moveFromSourceToDestinationAccountsThread = new Thread(() -> {

            System.out.println("Starting to move files between accounts");
            List<String[]> accounts_rows = DBHelper.getAccounts(new String[]{"refreshToken","userEmail"});
            Drive sourceDriveService = null;
            for (String[] account_row: accounts_rows){
                if (account_row[1].equals(sourceUserEmail)){
                    String refreshToken = account_row[0];
                    String accessToken = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                    sourceDriveService = initializeDrive(accessToken);
                    break;
                }
            }

            if (sourceDriveService == null) {
                LogHandler.saveLog("Source drive service could not be initialized for user: " + sourceUserEmail, true);
                hasMovedAllAssets[0] = false;
                return ;
            }

            System.out.println("source drive service created");
            String[] driveColumns = {"fileHash", "id","assetId", "fileId", "fileName", "userEmail"};
            List<String[]> drive_rows = MainActivity.dbHelper.getDriveTable(driveColumns, sourceUserEmail);
            CURRENT_DRIVE_ACCOUNT_INDEX = 0;
            ArrayList<Object> driveDestinationObject = getDestinationDrive(sourceUserEmail,accounts_rows);
            Drive destinationDriveService = (Drive) driveDestinationObject.get(0);
            String destinationUserEmail = (String) driveDestinationObject.get(1);

            int count_of_retry = 0;

            System.out.println("starting to loop drive files");

            for (int i = 0; i < drive_rows.size(); i++){
                String[] drive_row = drive_rows.get(i);
                String fileId = drive_row[3];
                System.out.println("moving file: " + drive_row[4]);
                String moveResult = moveFileBetweenAccounts(sourceDriveService, destinationDriveService,sourceUserEmail,destinationUserEmail, fileId);
                System.out.println("move result: " + moveResult);
                if (moveResult.equals("failed")){
                    CURRENT_DRIVE_ACCOUNT_INDEX+=1;
                    if (CURRENT_DRIVE_ACCOUNT_INDEX == accounts_rows.size()){
                        CURRENT_DRIVE_ACCOUNT_INDEX = 0 ;
                    }
                    driveDestinationObject = getDestinationDrive(sourceUserEmail,accounts_rows);
                    destinationDriveService = (Drive) driveDestinationObject.get(0);
                    destinationUserEmail = (String) driveDestinationObject.get(1);
                    i--;
                    count_of_retry++;
                    if (count_of_retry >= accounts_rows.size()){
                        if (!ableToMoveAllAssets){
                            hasMovedAllAssets[0] = true;
                            break;
                        }
                        hasMovedAllAssets[0] = false;
                    }
                }else{
                    //get size of file and check movable size
                    hasMovedAllAssets[0] = true;
                    count_of_retry =0;
                }
            }


            boolean isBackedUpAndDeleted = false;
            if (hasMovedAllAssets[0]){
                System.out.println("starting to back up json to remaining accounts");
                isBackedUpAndDeleted = Profile.backupJsonToRemainingAccounts(sourceUserEmail);
                System.out.println("result of backup to remainging accounrs : " + isBackedUpAndDeleted);
            }

            if(isBackedUpAndDeleted){
                String syncAssetsFolderId;
                FileList result = getStashSyncedAssetsFolderInDrive(sourceDriveService);
                if (result != null && !result.getFiles().isEmpty()) {
                    syncAssetsFolderId = result.getFiles().get(0).getId();
                    System.out.println("sync asset folder id:" + syncAssetsFolderId);
                }else{
                    syncAssetsFolderId = createStashSyncedAssetFolder(sourceDriveService);
                }
                System.out.println("starting to recursively delete files ");
                recursivelyDeleteFolder(sourceDriveService,syncAssetsFolderId,ableToMoveAllAssets);
                boolean isRevoked = false;
                System.out.println("starting to revoke ");
                isRevoked = GoogleCloud.startInvalidateTokenThread(sourceUserEmail);
                if (isRevoked){
                    System.out.println("starting to delete account and related asset");
                    MainActivity.dbHelper.deleteAccountAndRelatedAssets(sourceUserEmail);
                    GoogleDrive.startThreads();
                }
            }
        });

        moveFromSourceToDestinationAccountsThread.start();

    }


    public static ArrayList<Object> getDestinationDrive(String sourceUserEmail,List<String[]> accounts_rows){
//        Drive[] driveServices = {null};
        ArrayList<Object> driveDestinationObject = new ArrayList<>();
        Thread getDestinationDriveThread = new Thread(() -> {
            Drive destinationDriveService = null;
            for (String[] account_row: accounts_rows){
                if (account_row[1].equals(sourceUserEmail)){
                    continue;
                }
                if (CURRENT_DRIVE_ACCOUNT_INDEX >0){
                    CURRENT_DRIVE_ACCOUNT_INDEX-=1;
                    continue;
                }
                if (CURRENT_DRIVE_ACCOUNT_INDEX == 0){
                    String refreshToken = account_row[0];
                    String accessToken = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                    destinationDriveService = initializeDrive(accessToken);
                    driveDestinationObject.add(destinationDriveService);
                    driveDestinationObject.add(account_row[1]);
                    System.out.println("changing destination account to : " + account_row[1]);
//                    driveServices[0] = destinationDriveService;
                }
            }
        });
        getDestinationDriveThread.start();
        try {
            getDestinationDriveThread.join();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to join get free drive thread: " + e.getLocalizedMessage(), true);
        }

        return driveDestinationObject;
    }


    public static String moveFileBetweenAccounts(Drive sourceAccount, Drive destinationAccount, String sourceUserEmail, String destinationUserEmail, String fileId) {
        String[] moveResult = {"failed"};
        Thread moveFileBetweenAccountsThread = new Thread(() -> {
            File fileMetadata = null;
            String[] asset = new String[0];
            try {
                fileMetadata = sourceAccount.files().get(fileId).execute();

                Permission destinationUserPermission = new Permission().setType("user").setRole("writer")//maybe owner is better
                        .setEmailAddress(destinationUserEmail + "@gmail.com");

                sourceAccount.permissions().create(fileMetadata.getId(), destinationUserPermission).execute();

                File copiedFile = new File();
                copiedFile.setName(fileMetadata.getName());
                copiedFile.setMimeType(fileMetadata.getMimeType());

                File newFile = destinationAccount.files().copy(fileId, copiedFile).execute();
                System.out.println("File copied successfully to the destination account with ID: " + newFile.getId());
                asset = DBHelper.getAssetByDriveFileId(fileId);
                if (asset == null || newFile.getId() == null || newFile.getId().isEmpty()) {
                    LogHandler.saveLog("Failed to get asset for fileId: " + fileId, true);
                    return;
                }
            }catch (Exception e) {
                LogHandler.saveLog("Failed to move file between accounts: " + e.getLocalizedMessage() + " and " + e.getMessage(), true);
            }try {
                MainActivity.dbHelper.insertTransactionsData(sourceUserEmail, fileMetadata.getName(), destinationUserEmail
                        ,asset[0], "Transfer", asset[1]);

                sourceAccount.files().delete(fileId).execute();
                System.out.println("Original file deleted from the source account.");

                moveResult[0] = "success";
            } catch (Exception e) {
                LogHandler.saveLog("Failed to delete file from source account: " + e.getLocalizedMessage(), true);
            }

        });
        moveFileBetweenAccountsThread.start();
        try {
            moveFileBetweenAccountsThread.join();
        }catch (Exception e){
            LogHandler.saveLog("failed to join moveFileBetweenAccountsThread : " + e.getLocalizedMessage());
        }
        return moveResult[0];
    }



    public static int getAssetsSizeOfDriveAccount(String userEmail){
        int totalSize = 0;
        try{
            List<String[]> accounts_rows = DBHelper.getAccounts(new String[]{"refreshToken","userEmail"});
            Drive service = null;
            for (String[] account_row: accounts_rows){
                if (account_row[1].equals(userEmail)){
                    String accessToken = MainActivity.googleCloud.updateAccessToken(account_row[0]).getAccessToken();
                    service = initializeDrive(accessToken);
                }
            }
            String[] driveColumns = {"fileHash", "id","assetId", "fileId", "fileName", "userEmail"};
            List<String[]> drive_rows = MainActivity.dbHelper.getDriveTable(driveColumns, userEmail);
            for (String[] drive_row: drive_rows){
                String fileId = drive_row[3];
                int fileSize = service.files().get(fileId).size();
                totalSize += fileSize;
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get assets size of drive account: " + e.getLocalizedMessage(), true);
        }
        return totalSize;
    }


    public static void recursivelyDeleteFolder(Drive service,String folderId, boolean completeMove) {
        try {
            String query = "'" + folderId + "' in parents and trashed = false";
            Drive.Files.List request = service.files().list().setQ(query);
            boolean deleteThisFolder = true;
            do {
                FileList fileList = request.execute();

                for (File file : fileList.getFiles()) {
                    if (file.getMimeType().equals("application/vnd.google-apps.folder")) {
                        if (file.getName().equals("assets") && !completeMove){
                            deleteThisFolder = false;
                            continue;
                        }
                        recursivelyDeleteFolder(service, file.getId(),completeMove);
                    } else {
                        service.files().delete(file.getId()).execute();
                    }
                }
                request.setPageToken(fileList.getNextPageToken());
            } while (request.getPageToken() != null && !request.getPageToken().isEmpty());

            if (deleteThisFolder){
                service.files().delete(folderId).execute();
            }
        } catch (Exception e) {
            LogHandler.saveLog("failed to delete files and folders from drive : " + e.getLocalizedMessage()) ;
        }
    }

    public static void cleanDriveFolders() {
        Thread cleanDriveFoldersThread = new Thread(() -> {
            try{
                System.out.println("start of cleaning drive folders");
                List<String[]> accounts_rows = DBHelper.getAccounts(new String[]{"refreshToken","userEmail","parentFolderId"});
                Drive service;
                for (String[] account_row: accounts_rows){
                    String accessToken = MainActivity.googleCloud.updateAccessToken(account_row[0]).getAccessToken();
                    service = initializeDrive(accessToken);
                    String parentFolderId = account_row[2];
                    System.out.println("stored parentFolderId: " + parentFolderId);
                    if (parentFolderId != null && !parentFolderId.isEmpty()){
                        continue;
                    }

                    FileList result;
                    try{
                        String query = "mimeType='application/vnd.google-apps.folder' and name='stash_synced_assets' and trashed=false";
                        result = service.files().list()
                                .setQ(query)
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute();
                        parentFolderId = result.getFiles().get(0).getId();
                        System.out.println("founded parentFolderId: " + parentFolderId);
                        for (File file : result.getFiles()){
                            if (!file.getId().equals(parentFolderId)){
                                System.out.println("moving files between parent folders: " + file.getId() + " and " + parentFolderId);
                                moveFilesBetweenFolders(service, file.getId(), parentFolderId);
                                System.out.println("deleting file: " + file.getId());
                                service.files().delete(file.getId()).execute();
                                System.out.println("deleted file: " + file.getId());
                            }
                        }
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to get stash synced folder in drive", true);
                    }

                    String finalParentFolderId = parentFolderId;
                    HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                        put("parentFolderId", finalParentFolderId);
                    }};
                    MainActivity.dbHelper.updateAccounts(account_row[1],updatedValues, "backup");
                    System.out.println("new parentFolderId: " + parentFolderId + " updated");
                    String assetsFolderId = "";
                    try{
                        String query = "mimeType='application/vnd.google-apps.folder' and name='assets' and '" + parentFolderId + "' in parents and trashed=false";
                        result = service.files().list()
                                .setQ(query)
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute();
                        assetsFolderId = result.getFiles().get(0).getId();
                        for (File file : result.getFiles()){
                            if (!file.getId().equals(assetsFolderId)){
                                moveFilesBetweenFolders(service, file.getId(), assetsFolderId);
                                service.files().delete(file.getId()).execute();
                            }
                        }
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to get stash synced folder in drive", true);
                    }

                    String profileFolderId = "";
                    try{
                        String query = "mimeType='application/vnd.google-apps.folder' and name='stash_user_profile' and '" + parentFolderId + "' in parents and trashed=false";
                        result = service.files().list()
                                .setQ(query)
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute();
                        profileFolderId = result.getFiles().get(0).getId();
                        for (File file : result.getFiles()){
                            if (!file.getId().equals(profileFolderId)){
                                moveFilesBetweenFolders(service, file.getId(), profileFolderId);
                                service.files().delete(file.getId()).execute();
                            }
                        }
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to get stash synced folder in drive", true);
                    }

                    String databaseFolderId = "";
                    try{
                        String query = "mimeType='application/vnd.google-apps.folder' and name='stash_database' and '" + parentFolderId + "' in parents and trashed=false";
                        result = service.files().list()
                                .setQ(query)
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute();
                        databaseFolderId = result.getFiles().get(0).getId();
                        for (File file : result.getFiles()){
                            if (!file.getId().equals(databaseFolderId)){
                                moveFilesBetweenFolders(service, file.getId(), databaseFolderId);
                                service.files().delete(file.getId()).execute();
                            }
                        }
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to get stash synced folder in drive", true);
                    }

                    String finalAssetsFolderId = assetsFolderId;
                    updatedValues = new HashMap<String, Object>() {{
                        put("assetsFolderId", finalAssetsFolderId);
                    }};
                    MainActivity.dbHelper.updateAccounts(account_row[1],updatedValues, "backup");

                    String finalProfileFolderId = profileFolderId;
                    updatedValues = new HashMap<String, Object>() {{
                        put("profileFolderId", finalProfileFolderId);
                    }};
                    MainActivity.dbHelper.updateAccounts(account_row[1],updatedValues, "backup");

                    String finalDatabaseFolderId = databaseFolderId;
                    updatedValues = new HashMap<String, Object>() {{
                        put("databaseFolderId", finalDatabaseFolderId);
                    }};
                    MainActivity.dbHelper.updateAccounts(account_row[1],updatedValues, "backup");

                    deleteOldDatabaseFiles(service);
                    deleteOldProfileFiles(service);
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to clean drive folders: " + e.getLocalizedMessage(), true);
            }
        });
        cleanDriveFoldersThread.start();
        try {
            cleanDriveFoldersThread.join();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to join cleanDriveFoldersThread : " + e.getLocalizedMessage());
        }
    }


    public static void moveFilesBetweenFolders(Drive service, String sourceFolderId, String destinationFolderId) {
        try {
            String query = "'" + sourceFolderId + "' in parents and trashed = false";
            Drive.Files.List request = service.files().list().setQ(query);

            do {
                FileList fileList = request.execute();

                for (File file : fileList.getFiles()) {
                    if (file.getMimeType().equals("application/vnd.google-apps.folder")) {
                        moveFilesBetweenFolders(service, file.getId(), destinationFolderId);
                    } else {
                        File fileMetadata = new File();

                        List<String> previousParents = new ArrayList<>();
                        previousParents.add(sourceFolderId);

                        fileMetadata.setParents(Collections.singletonList(destinationFolderId));

                        service.files().update(file.getId(), fileMetadata)
                                .setRemoveParents(String.join(",", previousParents))
                                .setAddParents(destinationFolderId)
                                .execute();
                    }
                }
                request.setPageToken(fileList.getNextPageToken());
            } while (request.getPageToken() != null && !request.getPageToken().isEmpty());
        } catch (Exception e) {
            LogHandler.saveLog("Failed to move files between folders in drive: " + e.getLocalizedMessage());
        }
    }

    public static void deleteOldDatabaseFiles(Drive service) {
        try {
            String fileName = "stashDatabase.db";
            String query = "name='" + fileName + "' and trashed=false and orderBy createdTime desc";
            List<File> result = service.files().list().setQ(query).execute().getFiles();
            boolean lastFile = true;
            for (File file : result) {
                if (lastFile){
                    lastFile = false;
                    break;
                }
                service.files().delete(file.getId()).execute();
            }
        }catch (Exception e) {
            LogHandler.saveLog("Failed to delete old database files from drive: " + e.getLocalizedMessage());
        }
    }

    public static void deleteOldProfileFiles(Drive service) {
        try {
            String fileName = "profileMap_";
            String query = "name  contains '" + fileName + "' and trashed=false and orderBy createdTime desc";
            List<File> result = service.files().list().setQ(query).execute().getFiles();
            boolean lastFile = true;
            for (File file : result) {
                if (lastFile){
                    lastFile = false;
                    break;
                }
                service.files().delete(file.getId()).execute();
            }

            String fileName2 = "profileMap.json";
            query = "name='" + fileName2 + "' and trashed=false";
            result = service.files().list().setQ(query).execute().getFiles();
            for (File file : result) {
                service.files().delete(file.getId()).execute();
            }

        }catch (Exception e) {
            LogHandler.saveLog("Failed to delete old profile files from drive: " + e.getLocalizedMessage());
        }
    }
}

