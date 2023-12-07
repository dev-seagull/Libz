package com.example.cso;

import static com.example.cso.GooglePhotos.getMemeType;
import static com.example.cso.GooglePhotos.isImage;
import static com.example.cso.GooglePhotos.isVideo;

import android.os.Environment;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Upload {
    public ArrayList<String> upload() {
//        LogHandler.saveLog("Starting to upload from Photos("+primaryUserEmail+ ") to Drive("+backupUserEmail+")",false);
        ExecutorService executor = Executors.newSingleThreadExecutor();
//        ArrayList<String> finalUploadFileIDs = uploadFileIDs;
        Callable<ArrayList<String>> uploadTask = () -> {
            try {
                uploadAndroidToDrive();

//                downloadFromPhotos(baseUrls, fileNames, destinationFolder);
//
//
//
//                LogHandler.saveLog("Started to calculate hash before uploading from Photos to Drive " ,false);
//                LogHandler.saveLog("Size of filesToUpload: " + filesToUpload.size() +
//                        " " + " media items size: " + mediaItems.size(), false);
//                for(int i=0; i < mediaItems.size(); i++){
//                    File fileToUpload = filesToUpload.get(i);
//                    String hash = calculateHash(fileToUpload).toLowerCase();
//                    mediaItems.get(i).setHash(hash);
//                    i++;
//                }
//
//                LogHandler.saveLog("Finished calculating hash before uploading from Photos to Drive " ,false);
//
//                uploadToDrive(destinationFolderFiles, backUpMediaItems, accessToken, finalUploadFileIDs);
//
//                deleteDestinationFolder(destinationFolder);
//                LogHandler.saveLog("End of uploading from Photos to Drive " ,false);

//
//            }catch (Exception e) {
//                LogHandler.saveLog("Failed to upload from Photos to Drive: " + e.getLocalizedMessage());
//            }
//        Future<ArrayList<String>> futureFileIds = executor.submit(uploadTask);
//        try{
//            uploadFileIDs = futureFileIds.get();
//        }catch (Exception e){
//            LogHandler.saveLog("Failed to get file id from upload task future: " + e.getLocalizedMessage());
//        }
                return new ArrayList<>();
            } catch (Exception e) {

            }
            return new ArrayList<>();
        };
        Future<ArrayList<String>> futureFileIds = executor.submit(uploadTask);
        try{
            ArrayList<String> uploadFileIds = futureFileIds.get();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get file id from upload task future: " + e.getLocalizedMessage());
        }
        return new ArrayList<>();
    }

    private Boolean downloadFromPhotos(ArrayList<String> baseUrls, ArrayList<String> fileNames,
                                    File destinationFolder){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final boolean[] isFinished = {false};
        Callable<Boolean> backgroundDownloadTask = () -> {
            System.out.println("number of base urls" +baseUrls.size() +"number of filenames :" +fileNames.size());
            int i = 0;
            for(String baseUrl: baseUrls){
                try {
                    URL url = new URL(baseUrl + "=d");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        int contentLength = connection.getContentLength();
                        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                        if (!destinationFolder.exists()) {
                            boolean isFolderCreated = destinationFolder.mkdirs();
                            if (!isFolderCreated) {
                                LogHandler.saveLog("The destination folder was not created");
                            }
                        }
                        String fileName = fileNames.get(i);
                        String filePath = destinationFolder + File.separator + fileName;
                        OutputStream outputStream = null;
                        try {
                            File downloadFile = new File(filePath);
                            downloadFile.createNewFile();
                            for (int k = 0; k <3; k++){
                                outputStream = new FileOutputStream(downloadFile);
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                                if (downloadFile.length() == (long) contentLength) {
                                    LogHandler.saveLog("downloaded to CSO folder : " + fileNames.get(i), false);
                                    break;
                                } else {
                                    LogHandler.saveLog("Failed to download " + downloadFile.length() + "!=" + contentLength);
                                }
                            }

                        } catch (IOException e) {
                            LogHandler.saveLog("Error in file output stream handling: " + e.getLocalizedMessage());
                        } finally {
                            try {
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                            } catch (IOException e) {
                                LogHandler.saveLog("Closing output stream failed : " + e.getLocalizedMessage());
                            }
                        }
                        inputStream.close();
                        connection.disconnect();
                        isFinished[0] = true;
                    }else {
                        LogHandler.saveLog("Failed to download "+fileNames.get(i)+"with response code : "  + responseCode);
                    }
                } catch (IOException e) {
                    LogHandler.saveLog("Downloading from Photos failed: " + e.getLocalizedMessage());
                }
                i++;
            }
            return isFinished[0];
        };
        Future<Boolean> future = executor.submit(backgroundDownloadTask);
        try{
            isFinished[0] = future.get();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get boolean finished in downloading from Photos: " + e.getLocalizedMessage());
        }
        return isFinished[0];
    }

    private void deleteDestinationFolder(File destinationFolder){
        File[] destinationFolderFiles = destinationFolder.listFiles();
        if (destinationFolderFiles != null) {
            for (File destinationFolderFile: destinationFolderFiles) {
                if (!destinationFolderFile.getName().equals(MainActivity.logFileName)){
                    boolean isDeleted = destinationFolderFile.delete();
                    if (isDeleted){
                        LogHandler.saveLog(destinationFolderFile.getName() + " deleted from CSO folder",false);
                    }else {
                        LogHandler.saveLog(destinationFolderFile.getName() + " was not deleted from CSO folder");
                    }

                }
            }
        }else{
            LogHandler.saveLog("Destination folder is null when trying to delete its content");
        }
    }


    public ArrayList<String> uploadToDrive(File[] destinationFolderFiles, ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems,
                              String accessToken, ArrayList<String> uploadFileIds){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ArrayList<String> finalUploadFileIds = uploadFileIds;
        final FileContent[] mediaContent = {null};
        Callable<ArrayList<String>> backgroundTaskUpload = () -> {
            if(destinationFolderFiles != null) {
                for (File destinationFolderFile : destinationFolderFiles) {
                    if (!Duplicate.isDuplicatedInBackup(backUpMediaItems, destinationFolderFile)) {
                        NetHttpTransport HTTP_TRANSPORT = null;
                        try {
                            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                        } catch (GeneralSecurityException | IOException e) {
                            LogHandler.saveLog("Failed to initialize http transport to upload to drive: " + e.getLocalizedMessage());
                        }
                        final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
                        HttpRequestInitializer requestInitializer = request -> {
                            request.getHeaders().setAuthorization("Bearer " + accessToken);
                            request.getHeaders().setContentType("application/json");
                        };
                        try {
                            Drive service = null;
                            if (HTTP_TRANSPORT != null) {
                                service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                                        .setApplicationName("cso")
                                        .build();
                            }else{
                                LogHandler.saveLog("Http transport is null");
                            }
                            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                            fileMetadata.setName(destinationFolderFile.getName());
                            String memeType = getMemeType(destinationFolderFile);
                            if(isImage(memeType)){
                                String destinationFolderFilePath = destinationFolderFile.getPath();
                                if(memeType.toLowerCase().endsWith("jpg")){
                                    mediaContent[0] = new FileContent("image/jpeg" ,
                                            new File(destinationFolderFilePath));
                                }else{
                                    mediaContent[0] = new FileContent("image/" + memeType.toLowerCase() ,
                                            new File(destinationFolderFilePath));
                                }
                            }else if(isVideo(memeType)){
                                String destinationFolderFilePath = destinationFolderFile.getPath();
                                if(memeType.toLowerCase().endsWith("mkv")){
                                    mediaContent[0] = new FileContent("video/x-matroska" ,
                                            new File(destinationFolderFilePath));
                                }else{
                                    mediaContent[0] = new FileContent("video/" + memeType.toLowerCase() ,
                                            new File(destinationFolderFilePath));
                                }
                            }else{
                                continue;
                            }
                            if(mediaContent[0] == null){
                                LogHandler.saveLog("You're trying to upload mediaContent of null for "
                                        + destinationFolderFile.getName());
                            }

                            //final int[] test = {2};
                            //if(test[0] > 0){
                            com.google.api.services.drive.model.File uploadFile =
                                    null;
                            if (service != null) {
                                uploadFile = service.files().create(fileMetadata, mediaContent[0]).setFields("id").execute();
                            }else{
                                LogHandler.saveLog("Drive service is null");
                            }
                            String uploadFileId = uploadFile.getId();
                            while(uploadFileId.isEmpty() | uploadFileId == null){
                                wait();
                            }
                            if (uploadFileId == null | uploadFileId.isEmpty()){
                                LogHandler.saveLog("UploadFileId for " + destinationFolderFile.getName() + " is null");
                            }
                            else {
                                finalUploadFileIds.add(uploadFileId);
                                LogHandler.saveLog("Uploading " + destinationFolderFile.getName()
                                        + " to backup account finished with uploadFileId: " + uploadFileId,false);
                            }
                            //test[0]--;
                            //}
                        }catch (Exception e) {
                            LogHandler.saveLog("Failed to upload to Drive backup account: " + e.getLocalizedMessage());
                        }
                    }
                }
            }else{
                LogHandler.saveLog("Destination folder is null");
            }
            return finalUploadFileIds;
        };
        Future<ArrayList<String>> futureFileIds = executor.submit(backgroundTaskUpload);
        try{
            uploadFileIds = futureFileIds.get();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get upload file id form background task upload: " + e.getLocalizedMessage());
        }
        return uploadFileIds;
    }

    private void uploadPhotosToDrive(){
//        ArrayList<String> baseUrls = new ArrayList<>();
//        ArrayList<String> uploadFileIDs = new ArrayList<>();
//        ArrayList<String> fileNames = new ArrayList<>();
//        for (GooglePhotos.MediaItem mediaItem : mediaItems) {
//            baseUrls.add(mediaItem.getBaseUrl());
//            fileNames.add(mediaItem.getFileName());
//        }

        String destinationFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + "cso";
        File destinationFolder = new File(destinationFolderPath);
        File[] destinationFolderFiles = destinationFolder.listFiles();
//        ArrayList<File> filesToUpload = new ArrayList<>();
        for(File destinationFolderFile: destinationFolderFiles){
//            if(fileNames.contains(destinationFolderFile.getName())){
//                filesToUpload.add(destinationFolderFile);
//            }
        }
    }


    private void uploadAndroidToDrive(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<ArrayList<String>> uploadTask = () -> {
            try {
                String[] selected_android_columns = {"id", "fileName", "filePath", "device",
                        "fileSize", "fileHash", "dateModified", "memeType","assetId"};
                List<String[]> android_items = MainActivity.dbHelper.getAndroidTable(selected_android_columns);


                String[] selected_userProfile_columns = {"userEmail" , "type"};
                List<String[]> userProfile_items = MainActivity.dbHelper.getUserProfile(selected_userProfile_columns);

                ArrayList<String> androidItemsToUpload_hash = new ArrayList<>();
                int duplicatedFileIndex = -1;
                for (int j=0 ; j < android_items.size(); j++) {
                    Long fileId = Long.valueOf(android_items.get(j)[0]);
                    String fileName = android_items.get(j)[1];
                    String filePath = android_items.get(j)[2];
                    File androidFile = new File(filePath);
                    String fileHash = android_items.get(j)[5];
                    String memeType = android_items.get(j)[7];
                    String assetId = android_items.get(j)[7];

                    Boolean isInDrive = false;
                    driveLoop:{
                        for(String[] userProfile_item : userProfile_items){
                            String userEmail = userProfile_item[0];
                            String type = userProfile_item[1];
                            if(type.equals("backup")){
                                String[] selected_drive_columns = {"id", "assetId", "fileId", "fileName",
                                        "userEmail","fileHash"};
                                List<String[]> drive_items = MainActivity.dbHelper.getDriveTable(selected_drive_columns, userEmail);
                                for(String[] drive_item : drive_items){
                                    String driveFileHash = drive_item[5];
                                    System.out.println("Drive file hash for test: " + driveFileHash);
                                    if(driveFileHash.equals(fileHash)){
                                        isInDrive = true;
                                        break driveLoop;
                                    }
                                }
                            }
                        }
                    }


                    boolean isDuplicated = false;
                    for (int i=0; i < androidItemsToUpload_hash.size(); i++){
                        if(androidItemsToUpload_hash.get(i).equals(fileHash)){
                            isDuplicated = true;
                            duplicatedFileIndex = i ;
                        }
                    }
                    if( (isDuplicated == false) && (isInDrive == false)){
                        androidItemsToUpload_hash.add(fileHash);
                        try {
                            NetHttpTransport HTTP_TRANSPORT = null;
                            try {
                                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                            } catch (GeneralSecurityException e) {
                                LogHandler.saveLog("Failed to http_transport " + e.getLocalizedMessage());
                            } catch (IOException e) {
                            }
                            final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

                            String driveBackupAccessToken = "";
                            String[] drive_backup_selected_columns = {"userEmail","type","accessToken"};
                            List<String[]> drive_backUp_accounts = MainActivity.dbHelper.getUserProfile(drive_backup_selected_columns);
                            for (String[] drive_backUp_account : drive_backUp_accounts) {
                                if (drive_backUp_account[1].equals("backup")) {
                                    driveBackupAccessToken = drive_backUp_account[2];
                                    break;
                                }

                            }
                            String bearerToken = "Bearer " + driveBackupAccessToken;
                            System.out.println("access token to upload is " + driveBackupAccessToken);
                            HttpRequestInitializer requestInitializer = request -> {
                                request.getHeaders().setAuthorization(bearerToken);
                                request.getHeaders().setContentType("application/json");
                            };

                            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                                    .setApplicationName("cso")
                                    .build();

                            com.google.api.services.drive.model.File fileMetadata =
                                    new com.google.api.services.drive.model.File();
                            fileMetadata.setName(fileName);
                            String memeTypeToUpload = getMemeType(new File(filePath));

                            FileContent mediaContent = null;
                            if (isImage(memeTypeToUpload)) {
                                if(androidFile.exists()) {
                                    if (memeType.toLowerCase().endsWith("jpg")) {
                                        mediaContent = new FileContent("image/jpeg",
                                                new File(filePath));
                                    } else {
                                        mediaContent = new FileContent("image/" + memeType.toLowerCase(),
                                                new File(filePath));
                                    }
                                }else {
                                    LogHandler.saveLog("The android file "  +fileName + " doesn't exists in upload method");
                                }
                            } else if (isVideo(memeTypeToUpload)) {
                                if(new File(filePath).exists()){
                                    if (memeType.toLowerCase().endsWith("mkv")) {
                                        mediaContent = new FileContent("video/x-matroska",
                                                new File(filePath));
                                    } else {
                                        mediaContent = new FileContent("video/" + memeType.toLowerCase(),
                                                new File(filePath));
                                    }
                                }else{
                                    LogHandler.saveLog("The android file "  +fileName + " doesn't exists in upload method");
                                }
                            }

//                                if (test[0] >0 && !isVideo(memeType)){
                            com.google.api.services.drive.model.File uploadFile =
                                    service.files().create(fileMetadata, mediaContent).setFields("id").execute();
                            String uploadFileId = uploadFile.getId();
                            while(uploadFileId == null){
                                wait();
                            }
                            if (uploadFileId == null | uploadFileId.isEmpty()){
                                LogHandler.saveLog("Failed to upload " + fileName + " from Android to backup because it's null");
                            }else{
                                LogHandler.saveLog("Uploading " + fileName +
                                        " from android into backup account uploadId : " + uploadFileId,false);
                                //date //id

                                MainActivity.dbHelper.insertTransactionsData(String.valueOf(fileId), fileName,
                                        drive_backUp_accounts.get(0)[0], "sync" , fileHash);
                            }
//                                  test[0]--;
                        } catch (Exception e) {
                            System.out.println("Uploading android error: " + e.getMessage());
                            LogHandler.saveLog("Uploading android error: " + e.getMessage());
                        }
                        //}
                    }
                    else{
                        LogHandler.saveLog("Duplicated file in android was found: " + fileName,false);
                        if(isDuplicated == true){
                            MainActivity.dbHelper.insertTransactionsData(String.valueOf(fileId), fileName,
                                    String.valueOf(android_items.get(duplicatedFileIndex)[0]),
                                    "duplicated" , fileHash);
                            System.out.println("Duplicated file " + fileName + " in android was found with another android file." );
                        }
                        if(isInDrive == true){
                            System.out.println("Duplicated file " + fileName + " in android was found with a drive file." );
                        }
                    }
                }
                //}
            } catch (Exception e){
                System.out.println("Uploading android error: " + e.getMessage());
                LogHandler.saveLog("Uploading android error: " + e.getMessage());
            }
            return new ArrayList<>();
        };
        Future<ArrayList<String>> future = executor.submit(uploadTask);
        ArrayList<String> uploadFileIdsFuture = new ArrayList<>();
        try{
            uploadFileIdsFuture = future.get();
            LogHandler.saveLog("Finished with " + uploadFileIdsFuture.size() + " uploads",false);
            //System.out.println("-----end of second ----->");
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
    }


    public static String calculateHash(File file) throws IOException {
        final int BUFFER_SIZE = 8192;
        StringBuilder hexString = new StringBuilder();
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LogHandler.saveLog("SHA-256 algorithm not available " + e.getLocalizedMessage());
        }
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(
                new FileInputStream(file))){
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, bytesRead);
            }
            bufferedInputStream.close();
            byte[] hash = digest.digest();
            for (byte b : hash) {
                hexString.append(String.format("%02X", b));
            }
        }catch (Exception e){
            LogHandler.saveLog("error in calculating hash " + e.getLocalizedMessage());
        }
        return hexString.toString().toLowerCase();
    }

    public void restore(String accessToken){
        String sqlQuery = "SELECT * FROM DRIVE ";
        MainActivity.dbHelper.dbReadable.rawQuery(sqlQuery , null);
    }
}
