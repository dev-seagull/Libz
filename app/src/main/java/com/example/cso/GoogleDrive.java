package com.example.cso;

import android.app.Activity;
import android.util.Log;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

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

    public static ArrayList<DriveAccountInfo.MediaItem> getMediaItems(String userEmail, boolean isLogin, String accessToken) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ArrayList<DriveAccountInfo.MediaItem> mediaItems = new ArrayList<>();
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
                            backupAccessToken = GoogleCloud.updateAccessToken(account_row[1]).getAccessToken();
                        }
                    }
                }
                String folderName = GoogleDriveFolders.assetsFolderName;
                String assetsSubFolderId = GoogleDriveFolders.getSubFolderId(userEmail, folderName, backupAccessToken, isLogin);
                if (assetsSubFolderId == null){
                    Log.d("folders","asset folder not found");
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
        List<String[]> drive_rows = DBHelper.getDriveTable(driveColumns, userEmail);
        ArrayList<String> fileHashChecker = new ArrayList<>();

        for(String[] drive_row: drive_rows){
            String fileHash = drive_row[0];
            String id= drive_row[1];
            String assetId = drive_row[2];
            String fileId = drive_row[3];

            if(fileHashChecker.contains(fileHash)){
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Callable<Boolean> backgroundTask = () -> {
                    Boolean[] isDeleted = new Boolean[1];
                    try{
                        isDeleted[0] = deleteMediaItem(accessToken, fileId);
                    }catch (Exception e){
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                    return isDeleted[0];
                };
                Future<Boolean> future = executor.submit(backgroundTask);
                Boolean isDeletedFuture = false;
                try{
                    isDeletedFuture = future.get();
                }catch (Exception e ){
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                if(isDeletedFuture){
                    DBHelper.deleteFileFromDriveTable(fileHash, id, assetId, fileId , userEmail);
                }

            }else{
                fileHashChecker.add(fileHash);
            }
        }
    }


    private static boolean deleteMediaItem(String accessToken, String fileId){
        boolean isDeleted = false;
        try{
            URL url = new URL("https://www.googleapis.com/drive/v3/files/" + fileId);
            for(int i=0; i<3; i++){
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Content-type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                int responseCode = connection.getResponseCode();

                Log.d("backup","Deleting duplicate drive : " + responseCode);
                if(responseCode == HttpURLConnection.HTTP_NO_CONTENT){
                    return true;
                }
            }
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return isDeleted;
    }

    public static Drive initializeDrive(String accessToken){
        final Drive[] service = {null};
        Thread initializeDriveThread = new Thread(() -> {
            try{
                NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
                String bearerToken = "Bearer " + accessToken;
                HttpRequestInitializer requestInitializer = request -> {
                    request.getHeaders().setAuthorization(bearerToken);
                    request.getHeaders().setContentType("application/json");
                };
                service[0] = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                        .setApplicationName("stash")
                        .build();

            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });

        initializeDriveThread.start();
        try{
            initializeDriveThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return service[0];
    }

    public static List<File> getDriveFolderFiles(Drive service, String folderId){
        List<File> files = null;
        try{
            String query = "'" + folderId + "' in parents and trashed=false";
            FileList resultJson = service.files().list()
                    .setQ(query)
                    .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                    .execute();

            files = resultJson.getFiles();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get drive folder files : " + e.getLocalizedMessage(), true);
        }
        return files;
    }

    public static void startUpdateDriveFilesThread(){
        Log.d("Threads","startUpdateDriveFiles thread finished");
        Thread updateDriveFilesThread = new Thread(() -> {
            List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail","type"});

            try{
                for(String[] account_row : account_rows){
                    String type = account_row[1];
                    if (type.equals("backup")){
                        String userEmail = account_row[0];
                        ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(userEmail, false, null);
                        Log.d("backup",driveMediaItems.size() + " media items found in drive backup");

                        for(DriveAccountInfo.MediaItem driveMediaItem: driveMediaItems){
                            Long last_insertId = DBHelper.insertAssetData(driveMediaItem.getHash());
                            if (last_insertId != -1) {
                                DBHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
                                        driveMediaItem.getHash(), userEmail);
                            }
                        }
                    }
                }
            }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
        });
        updateDriveFilesThread.start();
        try {
            updateDriveFilesThread.join();
        } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
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
                DBHelper.deleteRedundantDriveFromDB(driveFileIds, userEmail);
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
        Log.d("Threads","startDeleteRedundantDrive thread started");
        Thread deleteRedundantDriveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<String[]> account_rows = DBHelper.getAccounts(new String[]{"userEmail", "type"});

                for(String[] account_row : account_rows) {
                    String type = account_row[1];
                    if(type.equals("backup")){
                        String userEmail = account_row[0];
                        ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(userEmail, false, null);
                        ArrayList<String> driveFileIds = new ArrayList<>();
                        for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                            driveFileIds.add( driveMediaItem.getId());
                        }
                        DBHelper.deleteRedundantDriveFromDB(driveFileIds, userEmail);
                    }
                }
            }
        });
        deleteRedundantDriveThread.start();
        try{
            deleteRedundantDriveThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        Log.d("Threads","startDeleteRedundantDrive thread finished");
    }

    private static void startDeleteDuplicatedInDriveThread() {
        Log.d("Threads","startDeleteDuplicatedInDrive thread started");
        Thread deleteDuplicatedInDriveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] columns = {"refreshToken","userEmail", "type"};
                List<String[]> account_rows = DBHelper.getAccounts(columns);

                for(String[] account_row : account_rows) {
                    String type = account_row[2];
                    if(type.equals("backup")){
                        String userEmail = account_row[1];
                        String refreshToken = account_row[0];
                        String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
                        GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                    }
                }

            }
        });

        deleteDuplicatedInDriveThread.start();
        try {
            deleteDuplicatedInDriveThread.join();
        } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        Log.d("Threads","startDeleteDuplicatedInDrive thread finished");
    }

    public static void startUpdateStorageThread() {
        Log.d("Threads","startUpdateStorage thread started");
        Thread updateDriveStorage = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] columns = {"refreshToken","userEmail", "type"};
                List<String[]> account_rows = DBHelper.getAccounts(columns);

                for(String[] account_row : account_rows) {
                    String refreshToken = account_row[0];
                    String userEmail = account_row[1];
                    String type = account_row[2];
                    String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
                    GoogleCloud.Storage storage = GoogleCloud.getStorage(new GoogleCloud.Tokens(accessToken,refreshToken));

                    if (storage != null) {
                        Map<String, Object> updatedValues = new HashMap<String, Object>() {{
                            put("totalStorage", storage.getTotalStorage());
                        }};
                        DBHelper.updateAccounts(userEmail, updatedValues, type);
                        updatedValues = new HashMap<String, Object>() {{
                            put("usedStorage", storage.getUsedStorage());
                        }};
                        DBHelper.updateAccounts(userEmail, updatedValues, type);
                        updatedValues = new HashMap<String, Object>() {{
                            put("usedInDriveStorage", storage.getUsedInDriveStorage());
                        }};
                        DBHelper.updateAccounts(userEmail, updatedValues, type);
                        updatedValues = new HashMap<String, Object>() {{
                            put("UsedInGmailAndPhotosStorage", storage.getUsedInGmailAndPhotosStorage());
                        }};
                        DBHelper.updateAccounts(userEmail, updatedValues, type);
                    }
                }
            }
        });
        updateDriveStorage.start();
        try {
            updateDriveStorage.join();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        Log.d("Threads","startUpdateStorage thread finished");
    }

    public static void startThreads(){
        startUpdateStorageThread();
        startDeleteRedundantDriveThread();
        startUpdateDriveFilesThread();
        startDeleteDuplicatedInDriveThread();
    }

    public static void moveFromSourceToDestinationAccounts(String sourceUserEmail,boolean ableToMoveAllAssets,int movableSize,
            Activity activity){
        boolean[] hasMovedAllAssets = {false};
        Thread moveFromSourceToDestinationAccountsThread = new Thread(() -> {

            System.out.println("Starting to move files between accounts");
            List<String[]> accounts_rows = DBHelper.getAccounts(new String[]{"refreshToken","userEmail"});
            Drive sourceDriveService = null;
            for (String[] account_row: accounts_rows){
                if (account_row[1].equals(sourceUserEmail)){
                    String refreshToken = account_row[0];
                    String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
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
            List<String[]> drive_rows = DBHelper.getDriveTable(driveColumns, sourceUserEmail);
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
                unlinkSingleAccount(sourceUserEmail,sourceDriveService,ableToMoveAllAssets);
            }
            UIHandler.setupAccountButtons(activity);//unlink
        });

        moveFromSourceToDestinationAccountsThread.start();
    }

    public static void unlinkSingleAccount(String sourceUserEmail, Drive sourceDriveService, boolean ableToMoveAllAssets){
        Thread unlinkSingleAccountThread = new Thread( () -> {
            String syncAssetsFolderId = GoogleDriveFolders.getParentFolderId(sourceUserEmail,false,null);
            if(syncAssetsFolderId == null){
                return;
            }
            recursivelyDeleteFolderThread(sourceDriveService,syncAssetsFolderId,ableToMoveAllAssets);
            boolean isRevoked = false;
            System.out.println("starting to revoke ");
            isRevoked = GoogleCloud.startInvalidateTokenThread(sourceUserEmail);
            if (isRevoked){
                System.out.println("starting to delete account and related asset");
                DBHelper.deleteAccountAndRelatedAssets(sourceUserEmail);
                GoogleDrive.startThreads();
            }
        });
        unlinkSingleAccountThread.start();
        try{
            unlinkSingleAccountThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
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
                    String accessToken = GoogleCloud.updateAccessToken(refreshToken).getAccessToken();
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

                String parentFolderId = DBHelper.getAssetsFolderId(destinationUserEmail);
                copiedFile.setParents(Collections.singletonList(parentFolderId));

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
                DBHelper.insertTransactionsData(sourceUserEmail, fileMetadata.getName(), destinationUserEmail
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
                    String accessToken = GoogleCloud.updateAccessToken(account_row[0]).getAccessToken();
                    service = initializeDrive(accessToken);
                }
            }
            String[] driveColumns = {"fileHash", "id","assetId", "fileId", "fileName", "userEmail"};
            List<String[]> drive_rows = DBHelper.getDriveTable(driveColumns, userEmail);
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




    public static void recursivelyDeleteFolderThread(Drive service,String folderId,boolean completeMove){
        Thread recursivelyDeleteFolderThread = new Thread(() -> {
            recursivelyDeleteFolder(service,folderId,completeMove);
        });
        recursivelyDeleteFolderThread.start();
        try{
            recursivelyDeleteFolderThread.join();
        }catch (Exception e) {FirebaseCrashlytics.getInstance().recordException(e);}
    }

    public static void cleanDriveFolders() {
        Thread cleanDriveFoldersThread = new Thread(() -> {
            try{
                List<String[]> accounts_rows = DBHelper.getAccounts(new String[]{"refreshToken","userEmail","parentFolderId","assetsFolderId","profileFolderId","databaseFolderId"});
                for (String[] account_row: accounts_rows){
                    cleanAccount(account_row);
//                    String assetsFolderId = account_row[3];
//                    cleanAssetsFolder(assetsFolderId,service,userEmail,parentFolderId);
//
//                    String profileFolderId = account_row[4];
//                    cleanProfileFolder(profileFolderId,service,userEmail,parentFolderId);
//
//                    String databaseFolderId = account_row[5];
//                    cleanDatabaseFolder(databaseFolderId,service,userEmail,parentFolderId);
//
//                    deleteOldDatabaseFiles(service,userEmail);
//                    deleteOldProfileFiles(service,userEmail);
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to clean drive folders: " + e.getLocalizedMessage(), true);
            }
        });

        cleanDriveFoldersThread.start();
        try {
            cleanDriveFoldersThread.join();
        } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static void cleanAccount(String[] account_row){
        String userEmail = account_row[1];
        String accessToken = GoogleCloud.updateAccessToken(account_row[0]).getAccessToken();
        Drive service = initializeDrive(accessToken);

        String parentFolderId = GoogleDriveFolders.getParentFolderId(userEmail,false,null);
        if(parentFolderId == null){
            return;
        }
        parentFolderId = cleanParentFolder(parentFolderId,service,userEmail);

//        String assetsFolderId = account_row[3];
//        cleanAssetsFolder(assetsFolderId,service,userEmail,parentFolderId);
//
//        String profileFolderId = account_row[4];
//        cleanProfileFolder(profileFolderId,service,userEmail,parentFolderId);
//
//        String databaseFolderId = account_row[5];
//        cleanDatabaseFolder(databaseFolderId,service,userEmail,parentFolderId);
//
//        deleteOldDatabaseFiles(service,userEmail);
//        deleteOldProfileFiles(service,userEmail);
    }

    public static String cleanParentFolder(String inputParentFolderId, Drive service, String userEmail){
        String[] parentFolderId = {inputParentFolderId};
        Thread cleanStashSyncFolderThread = new Thread(() -> {
            if (parentFolderId[0] == null || parentFolderId[0].isEmpty()){
                HashMap<String, Object> updatedValues;
                try{
                    cleanLibzFolders(service,userEmail, inputParentFolderId);

                    updatedValues = new HashMap<String, Object>() {{
                        put("parentFolderId", parentFolderId[0]);
                    }};
                    DBHelper.updateAccounts(userEmail,updatedValues, "backup");
                    System.out.println("new parentFolderId: " + parentFolderId[0] + " updated");
                }catch (Exception e){
                    LogHandler.saveLog("Failed to get stash synced folder in drive : " +e.getLocalizedMessage(), true);
                    parentFolderId[0] = null;
                }
            }
        });

        cleanStashSyncFolderThread.start();
        try {
            cleanStashSyncFolderThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join cleanStashSyncFolderThread : " + e.getLocalizedMessage());
        }
        return parentFolderId[0];
    }

    private static void cleanPreviousStashFolders(Drive service){
        cleanStashSyncedAssetsFolders(service);
    }

    private static void cleanStashSyncedAssetsFolders(Drive service){
        Thread cleanStashSyncedAssetsFoldersThreads = new Thread(() -> {
            FileList result;
            try{
                String query = "mimeType='application/vnd.google-apps.folder' and name='stash_synced_assets' and trashed=false";
                result = service.files().list()
                        .setQ(query)
                        .setOrderBy("createdTime desc")
                        .setSpaces("drive")
                        .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                        .execute();
                if (!result.getFiles().isEmpty()){

                }
                Log.d("cleanFolders","stash_synced_assets found: " + result.size());
            } catch (Exception e) {FirebaseCrashlytics.getInstance().recordException(e); }
        });

        cleanStashSyncedAssetsFoldersThreads.start();
        try{
            cleanStashSyncedAssetsFoldersThreads.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static void cleanLibzFolders(Drive service, String userEmail, String parentFolderId){
        cleanLibzParentFolders(service, userEmail, parentFolderId);
    }

    private static void cleanLibzParentFolders(Drive service, String userEmail, String parentFolderId){
        Thread cleanLibzParentFoldersThread = new Thread(() -> {
            FileList result;
            try {
                String folderName = GoogleDriveFolders.parentFolderName;
                String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false";
                result = service.files().list()
                        .setQ(query)
                        .setOrderBy("createdTime desc")
                        .setSpaces("drive")
                        .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                        .execute();
                Log.d("cleanFolders",folderName + " found: " + result.size());

                if (!result.getFiles().isEmpty()){
                    if( hasFolderDeletePermission(result.getFiles().get(0),service,userEmail)){
                        for (File file : result.getFiles()){
                            if (!file.getId().equals(parentFolderId)){
                                if (hasFolderDeletePermission(file,service,userEmail)){
                                    System.out.println("----- moving files between parent folders: " + file.getId() + " and " + parentFolderId);
                                    moveFilesBetweenFolders(service, file.getId(), parentFolderId);
                                    System.out.println("----- deleting file: " + file.getId());
                                    service.files().delete(file.getId()).execute();
                                    System.out.println("----- deleted file: " + file.getId());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {FirebaseCrashlytics.getInstance().recordException(e); }
        });

        cleanLibzParentFoldersThread.start();
        try{

        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }
    public static void cleanAssetsFolder(String assetsFolderId, Drive service, String userEmail, String parentFolderId){
        if (assetsFolderId == null || assetsFolderId.isEmpty()){
            try{
                String query = "mimeType='application/vnd.google-apps.folder' and name='assets' and '" + parentFolderId + "' in parents and trashed=false";
                FileList result = service.files().list()
                        .setQ(query)
                        .setOrderBy("createdTime desc")
                        .setSpaces("drive")
                        .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                        .execute();
                System.out.println("----- found assets folders size : " + result.getFiles().size());
                if (!result.getFiles().isEmpty()){
                    assetsFolderId = result.getFiles().get(0).getId();
                    for (File file : result.getFiles()){
                        if (!file.getId().equals(assetsFolderId)){
                            moveFilesBetweenFolders(service, file.getId(), assetsFolderId);
                            if (hasFolderDeletePermission(file,service,userEmail)){
                                service.files().delete(file.getId()).execute();
                                System.out.println("----- deleted file: " + file.getId());
                            }
                        }
                    }
                    String finalAssetsFolderId = assetsFolderId;
                    HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                        put("assetsFolderId", finalAssetsFolderId);
                    }};
                    DBHelper.updateAccounts(userEmail,updatedValues, "backup");
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to get stash synced folder in drive : " +e.getLocalizedMessage(), true);
            }
        }

    }

    public static void cleanProfileFolder(String profileFolderId, Drive service, String userEmail, String parentFolderId){
        if (profileFolderId == null || profileFolderId.isEmpty()){
            try{
                String query = "mimeType='application/vnd.google-apps.folder' and name='stash_user_profile' and '" + parentFolderId + "' in parents and trashed=false";
                FileList result = service.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setOrderBy("createdTime desc")
                        .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                        .execute();
                if (!result.getFiles().isEmpty()){
                    profileFolderId = result.getFiles().get(0).getId();
                    for (File file : result.getFiles()){
                        if (!file.getId().equals(profileFolderId)){
                            moveFilesBetweenFolders(service, file.getId(), profileFolderId);
                            if (hasFolderDeletePermission(file,service,userEmail)){
                                service.files().delete(file.getId()).execute();
                                System.out.println("----- deleted file: " + file.getId());
                            }
                        }
                    }
                }
                String finalProfileFolderId = profileFolderId;
                HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                    put("profileFolderId", finalProfileFolderId);
                }};
                DBHelper.updateAccounts(userEmail,updatedValues, "backup");
            }catch (Exception e){
                LogHandler.saveLog("Failed to get stash synced folder in drive : " +e.getLocalizedMessage(), true);
            }
        }

    }

    public static void cleanDatabaseFolder(String databaseFolderId, Drive service, String userEmail, String parentFolderId){
        if (databaseFolderId == null || databaseFolderId.isEmpty()){
            try{
                String appName = "libz";
                String query = "mimeType='application/vnd.google-apps.folder' and name='"+appName+"_database' and '" + parentFolderId + "' in parents and trashed=false";
                FileList result = service.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setOrderBy("createdTime desc")
                        .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                        .execute();
                System.out.println("----- found database folders size : " + result.getFiles().size());
                if (!result.getFiles().isEmpty()){
                    databaseFolderId = result.getFiles().get(0).getId();
                    for (File file : result.getFiles()){
                        if (!file.getId().equals(databaseFolderId)){
                            moveFilesBetweenFolders(service, file.getId(), databaseFolderId);
                            if (hasFolderDeletePermission(file,service,userEmail)){
                                service.files().delete(file.getId()).execute();
                                System.out.println("----- deleted file: " + file.getId());
                            }
                        }
                    }
                }
                query = "mimeType='application/vnd.google-apps.folder' and name='stash_database' and '" + parentFolderId + "' in parents and trashed=false";
                result = service.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                        .execute();
                for (File file : result.getFiles()){
                    if (hasFolderDeletePermission(file,service,userEmail)){
                        service.files().delete(file.getId()).execute();
                        System.out.println("----- deleted file: " + file.getId());
                    }
                }
                String finalDatabaseFolderId = databaseFolderId;
                HashMap<String, Object> updatedValues = new HashMap<String, Object>() {{
                    put("databaseFolderId", finalDatabaseFolderId);
                }};
                DBHelper.updateAccounts(userEmail,updatedValues, "backup");
            }catch (Exception e){
                LogHandler.saveLog("Failed to get stash synced folder in drive : " +e.getLocalizedMessage(), true);
            }
        }
    }

    public static void moveFilesBetweenFolders(Drive service, String sourceFolderId, String destinationFolderId) {
        try {
            System.out.println("----- start moving files from folder " + sourceFolderId + " to folder " + destinationFolderId);
            String query = "'" + sourceFolderId + "' in parents and trashed = false";
            Drive.Files.List request = service.files().list().setQ(query).setFields("files(id, parents,mimeType, name, permissions),nextPageToken");

            do {
                FileList fileList = request.execute();

                for (File file : fileList.getFiles()) {
                    if (file.getMimeType().equals("application/vnd.google-apps.folder")) {
                        moveFilesBetweenFolders(service, file.getId(), destinationFolderId);
                    } else {
                        File fileMetadata = new File();

                        service.files().update(file.getId(), fileMetadata)
                                .setRemoveParents(String.join(",", file.getParents()))
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

    public static void deleteOldDatabaseFiles(Drive service,String userEmail) {
        try {
            String fileName = "libzDatabase.db";
            String query = "name='" + fileName + "' and trashed=false";
            List<File> result = service.files().list()
                    .setQ(query)
                    .setOrderBy("createdTime desc")
                    .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                    .execute().getFiles();

            System.out.println("----- found libzDatabase.db files: " + result.size());
            boolean isFirstFile = true;
            for (File file : result) {
                if (isFirstFile) {
                    isFirstFile = false;
                    continue; // Keep the most recent file, delete the rest
                }

                if (hasFolderDeletePermission(file,service,userEmail)) {
                    service.files().delete(file.getId()).execute();
                    System.out.println("----- deleted file: " + file.getName());
                } else {
                    LogHandler.saveLog("No permission to delete file: " + file.getName(), true);
                }
            }
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete old libzDatabase.db files from drive: " + e.getLocalizedMessage(), true);
        }

        try {
            String fileName = "stashDatabase.db";
            String query = "name='" + fileName + "' and trashed=false";
            List<File> result = service.files().list()
                    .setQ(query)
                    .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                    .execute().getFiles();

            System.out.println("----- found stashDatabase.db files: " + result.size() + "  deleting...");
            for (File file : result) {
                if (hasFolderDeletePermission(file,service, userEmail)) {
                    service.files().delete(file.getId()).execute();
                    System.out.println("----- deleted file: " + file.getName());
                } else {
                    LogHandler.saveLog("No permission to delete file: " + file.getName(), true);
                }
            }
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete old stashDatabase.db files from drive: " + e.getLocalizedMessage(), true);
        }
    }

    public static void deleteOldProfileFiles(Drive service,String userEmail) {
        try {
            String fileName = "profileMap_";
            String query = "name contains '" + fileName + "' and trashed=false";
            List<File> result = service.files().list()
                    .setQ(query)
                    .setOrderBy("createdTime desc")
                    .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                    .execute().getFiles();

            System.out.println("----- found profileMap_ files: " + result.size() + "  deleting...");
            boolean isFirstFile = true;
            for (File file : result) {
                if (isFirstFile) {
                    isFirstFile = false;
                    continue; // Keep the most recent file, delete the rest
                }

                if (hasFolderDeletePermission(file,service, userEmail)) {
                    service.files().delete(file.getId()).execute();
                    System.out.println("----- deleted file: " + file.getName());
                } else {
                    LogHandler.saveLog("No permission to delete file: " + file.getName(), true);
                }
            }

            String fileName2 = "profileMap.json";
            query = "name='" + fileName2 + "' and trashed=false";
            result = service.files().list()
                    .setQ(query)
                    .setFields("files(id, parents,mimeType, name, permissions),nextPageToken")
                    .execute().getFiles();

            System.out.println("----- found profileMap.json files: " + result.size() + "  deleting...");
            for (File file : result) {
                if (hasFolderDeletePermission(file,service, userEmail)) {
                    service.files().delete(file.getId()).execute();
                    System.out.println("----- deleted file: " + file.getName());
                } else {
                    LogHandler.saveLog("No permission to delete file: " + file.getName(), true);
                }
            }

        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete old profile files from drive: " + e.getLocalizedMessage(), true);
        }
    }

    private static boolean hasFolderDeletePermission(File file, Drive service, String userEmail) {
        try {

            if (file.getPermissions() != null) {
                for (Permission permission : file.getPermissions()) {
                    if (permission.getRole().equals("owner") || permission.getRole().equals("organizer") || permission.getRole().equals("writer")) {
                        System.out.println("file "+ file.getName() + " has owner or organizer or writer permissions. Skipping request...");
                        return true;
                    }
                }
            }

            System.out.println("file " + file.getName() + " does not have delete permissions. Requesting them...");
            Permission newPermission = new Permission();
            newPermission.setType("user");
            newPermission.setRole("writer");

            newPermission.setEmailAddress(userEmail  + "@gmail.com");
            service.permissions().create(file.getId(), newPermission)
                    .setSendNotificationEmail(false)
                    .execute();

            System.out.println("----- Granted writer permission to the file: " + file.getName());

            return true;

        } catch (Exception e) {
            LogHandler.saveLog("Failed to get or set delete permissions for file: " + file.getName() + " - " + e.getLocalizedMessage(), true);
            return false;
        }
    }

    public static boolean isBackUpAccountFull(Double totalStorage, Double usedStorage){
        try{
            Double freeStorage =  totalStorage - usedStorage;
            if(freeStorage > 50){
                return false;
            }else{
                return true;
            }
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return true;
    }
}

