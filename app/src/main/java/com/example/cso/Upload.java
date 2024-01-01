package com.example.cso;

import static com.example.cso.GooglePhotos.getMemeType;
import static com.example.cso.GooglePhotos.isImage;
import static com.example.cso.GooglePhotos.isVideo;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.Environment;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Upload {

    public static void deletePhotosFromAndroid(){
        String destinationFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + "cso";
        File destinationFolder = new File(destinationFolderPath);
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

    public void uploadAndroidToDrive(){
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
                    String assetId = android_items.get(j)[8];

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

                                System.out.println("assetId for " + fileName + " is : " + assetId);
                                MainActivity.dbHelper.insertTransactionsData(String.valueOf(fileId), fileName,
                                        drive_backUp_accounts.get(0)[0], assetId, "sync" , fileHash);
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

                        System.out.println("assetId for " + fileName + " is : " + assetId);
                        if(isDuplicated == true){
                            MainActivity.dbHelper.insertTransactionsData(String.valueOf(fileId), fileName,
                                    String.valueOf(android_items.get(duplicatedFileIndex)[0])
                                    , assetId, "duplicated" , fileHash);
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

    public static void restore(Context context){
        String sqlQuery = "SELECT T.id, T.source, T.fileName, T.destination, D.assetId, " +
                "T.operation, T.hash, T.date, D.fileId , D.userEmail " +
                "FROM TRANSACTIONS T " +
                "JOIN DRIVE D ON T.hash = D.fileHash " +
                "WHERE T.operation = 'deletedInDevice' and T.destination = ?" +
                "AND D.id = (" +
                "   SELECT MIN(id) " +
                "   FROM DRIVE " +
                "   WHERE fileHash = T.hash " +
                ");";

        Cursor cursor = MainActivity.dbHelper.dbReadable.rawQuery(sqlQuery, new String[]{MainActivity.androidDeviceName});
        List<String[]> resultList = new ArrayList<>();
        if(cursor.moveToFirst() && cursor != null){
            do {
                String[] columns = {"id", "source", "fileName", "destination", "assetId"
                        , "operation", "hash", "date", "fileId", "userEmail"};
                String[] row = new String[columns.length];
                for(int i =0 ; i < columns.length ; i++){
                    int columnIndex = cursor.getColumnIndex(columns[i]);
                    if (columnIndex >= 0) {
                        row[i] = cursor.getString(columnIndex);
                    }
                }
                resultList.add(row);
            } while (cursor.moveToNext());
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        final boolean[] isFinished = {false};
        Callable<Boolean> backgroundDownloadTask = () -> {
            for (String[] row : resultList) {
                String filePath = row[1];
                String userEmail = row[9];
                String fileId = row[8];
                System.out.println("File path for restore test: " + filePath + " and user email is: " + userEmail  +  "  and file id" +
                        " is: " + fileId);
                String accessTokenSqlQuery = "SELECT accessToken from USERPROFILE WHERE USERPROFILE.userEmail = ?";
                Cursor accessTokenCursor = MainActivity.dbHelper.dbReadable.rawQuery(accessTokenSqlQuery, new String[]{userEmail});
                String accessToken = "";
                if(accessTokenCursor.moveToFirst() && accessTokenCursor != null) {
                    int accessTokenColumnIndex = accessTokenCursor.getColumnIndex("accessToken");
                    if (accessTokenColumnIndex >= 0) {
                        accessToken = accessTokenCursor.getString(accessTokenColumnIndex);
                    }
                }
                if(!accessToken.isEmpty() && accessToken != null){
                    NetHttpTransport HTTP_TRANSPORT = null;
                    try {
                        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                    } catch (GeneralSecurityException e) {
                        LogHandler.saveLog("Failed to http_transport in restore method" + e.getLocalizedMessage(), true);
                    } catch (IOException e) {
                    }
                    final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

                    String bearerToken = "Bearer " + accessToken;
                    HttpRequestInitializer requestInitializer = request -> {
                        request.getHeaders().setAuthorization(bearerToken);
                        request.getHeaders().setContentType("application/json");
                    };

                    Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                            .setApplicationName("cso")
                            .build();

                    OutputStream outputStream = null;
                    File file = new File(filePath);

                    try {
                        if(!file.exists()){
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                            try {
                                outputStream = new FileOutputStream(filePath);
                            } catch (FileNotFoundException e) {
                                LogHandler.saveLog("failed to save output stream in restore method : " + e.getLocalizedMessage(), true);
                            }
                            try {
                                service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                            } catch (IOException e) {
                                LogHandler.saveLog("failed to download in restore method : " + e.getLocalizedMessage(), true);
                            }
                        }
                    } catch (Exception e) {
                        LogHandler.saveLog("failed to create file in restore method : " + e.getLocalizedMessage(), true);
                    }

                    MediaScannerConnection.scanFile(
                            context,
                            new String[]{filePath},
                            new String[]{"image/jpeg", "image/"+getMemeType(file).toLowerCase(), "image/jpg", "video/"+getMemeType(file)},
                            null
                    );
                }
            }
            cursor.close();

           return isFinished[0];
        };
        Future<Boolean> future = executor.submit(backgroundDownloadTask);
        try{
            isFinished[0] = future.get();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get boolean finished in downloading from Photos: " + e.getLocalizedMessage());
        }
    }

    public ArrayList<String> uploadPhotosToDrive(String destinationUserEmail,String accessToken){
        System.out.println("here in photos to drive");
        String destinationFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + "cso";
        String sqlQury = "SELECT * FROM PHOTOS";
        Cursor cursor = MainActivity.dbHelper.dbReadable.rawQuery(sqlQury, null);
        ArrayList<String[]> destinationFiles = new ArrayList<>();
        if(cursor.moveToFirst() && cursor != null){
            do {
                int fileNameColumnIndex = cursor.getColumnIndex("fileName");
                int userEmailColumnIndex = cursor.getColumnIndex("userEmail");
                int fileHashColumnIndex = cursor.getColumnIndex("fileHash");
                int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                if(fileNameColumnIndex >= 0 && userEmailColumnIndex >= 0 && fileHashColumnIndex >= 0
                        && assetIdColumnIndex >= 0){
                    String fileName = cursor.getString(fileNameColumnIndex);
                    String userEmail = cursor.getString(userEmailColumnIndex);
                    String fileHash = cursor.getString(fileHashColumnIndex);
                    String assetId = cursor.getString(assetIdColumnIndex);
                    String filePath = destinationFolderPath + File.separator + fileName;
                    destinationFiles.add(new String[]{filePath,userEmail,fileName,assetId,fileHash});
                    System.out.println("File name for upload photos from android to drive: " + fileName);
                }
            }while (cursor.moveToNext());
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        ArrayList<String> finalUploadFileIds = new ArrayList<>();
        final FileContent[] mediaContent = {null};
        Callable<ArrayList<String>> backgroundTaskUpload = () -> {
            if(destinationFiles != null) {
                for (String[] destinationFile : destinationFiles) {
                    File destinationFolderFile = new File(destinationFile[0]);
                    if (!destinationFolderFile.exists()) {
                        LogHandler.saveLog("The destination file " + destinationFolderFile.getName() + " doesn't exists",false);
                        continue;
                    }
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
                                uploadFile = service.files()
                                .create(fileMetadata, mediaContent[0]).setFields("id").execute();
                        }else{
                            LogHandler.saveLog("Drive service is null");
                        }
                        String uploadFileId = uploadFile.getId();
                        while(uploadFileId.isEmpty() | uploadFileId == null){
                            wait();
                            LogHandler.saveLog("waiting for uploadFileId to be not null",false);
                        }
                        if (uploadFileId == null | uploadFileId.isEmpty()){
                            LogHandler.saveLog("UploadFileId for " + destinationFolderFile.getName() + " is null");
                        }
                        else {
                           //destinationFiles.add(new String[]{filePath,userEmail,fileName,assetId,fileHash});
                            for (String part : destinationFile){
                                System.out.println("part for upload photos from android to drive: " + part);
                            }
                            MainActivity.dbHelper.insertTransactionsData(destinationFile[1], destinationFile[2],
                                    destinationUserEmail, destinationFile[3], "syncPhotos" , destinationFile[4]);
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
            else{
                LogHandler.saveLog("Destination folder is null");
            }
            return finalUploadFileIds;
        };
        ArrayList<String> uploadFileIds = null;
        Future<ArrayList<String>> futureFileIds = executor.submit(backgroundTaskUpload);
        try{
           uploadFileIds = futureFileIds.get();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get upload file id form background task upload: " + e.getLocalizedMessage());
        }
        return uploadFileIds;
    }


    public static Boolean downloadFromPhotos(ArrayList<GooglePhotos.MediaItem> photosMediaItems,
                                             File destinationFolder, String userEmail){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final boolean[] isFinished = {false};
        Callable<Boolean> backgroundDownloadTask = () -> {
            for(GooglePhotos.MediaItem photosMediaItem: photosMediaItems){
                String sqlQuery =  "SELECT assetId FROM PHOTOS WHERE " +
                        "EXISTS (SELECT 1 FROM PHOTOS WHERE fileId = ?)";
                Cursor cursor = MainActivity.dbHelper.dbReadable.rawQuery(sqlQuery, new String[]{photosMediaItem.getId()});
                if(cursor != null && cursor.moveToFirst()){
                    int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                    if(assetIdColumnIndex >= 0){
                        String assetId = cursor.getString(assetIdColumnIndex);
                        String sqlQuery2 =  "SELECT EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ?)";
                        Cursor cursor2 = MainActivity.dbHelper.dbReadable.rawQuery(sqlQuery2, new String[]{assetId});
                        if(cursor2 != null && cursor2.moveToFirst()) {
                            int existsInDrive = cursor2.getInt(0);
                            System.out.println("exits in drive: "  + existsInDrive + " " + assetId);
                            if(existsInDrive == 0){
                                try {
                                    URL url = new URL(photosMediaItem.getBaseUrl() + "=d");
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
                                        String fileName = photosMediaItem.getFileName();
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
                                                    LogHandler.saveLog("downloaded to CSO folder : " + photosMediaItem.getFileName(), false);
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

                                            String fileHash = Hash.calculateHash(new File(filePath));
                                            System.out.println("file hash for test: "+ fileHash + " for file name: " + fileName);
                                            long last_insertedId = MainActivity.dbHelper.insertAssetData(fileHash);
                                            if(last_insertedId != -1){
                                                MainActivity.dbHelper.insertIntoPhotosTable(last_insertedId,
                                                        photosMediaItem.getId(),photosMediaItem.getFileName(),
                                                        fileHash,userEmail,photosMediaItem.getCreationTime(), photosMediaItem.getBaseUrl());
                                            }else{
                                                LogHandler.saveLog("Last inserted id -1 in inserting into asset " +
                                                        "in downloadFromPhotos",true);
                                            }
                                        }
                                        inputStream.close();
                                        connection.disconnect();
                                        isFinished[0] = true;
                                    }else {
                                        LogHandler.saveLog("Failed to download "+photosMediaItem.getFileName()+"with response code : "  + responseCode);
                                    }
                                } catch (IOException e) {
                                    LogHandler.saveLog("Downloading from Photos failed: " + e.getLocalizedMessage());
                                }
                            }
                        }
                    }
                }else{
                    try {
                        URL url = new URL(photosMediaItem.getBaseUrl() + "=d");
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
                            String fileName = photosMediaItem.getFileName();
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
                                        LogHandler.saveLog("downloaded to CSO folder : " + photosMediaItem.getFileName(), false);
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

                                String fileHash = Hash.calculateHash(new File(filePath));
                                System.out.println("file hash for test: "+ fileHash + " for file name: " + fileName);
                                long last_insertedId = MainActivity.dbHelper.insertAssetData(fileHash);
                                if(last_insertedId != -1){
                                    MainActivity.dbHelper.insertIntoPhotosTable(last_insertedId,
                                            photosMediaItem.getId(),photosMediaItem.getFileName(),
                                            fileHash,userEmail,photosMediaItem.getCreationTime(), photosMediaItem.getBaseUrl());
                                }else{
                                    LogHandler.saveLog("Last inserted id -1 in inserting into asset " +
                                            "in downloadFromPhotos",true);
                                }
                            }
                            inputStream.close();
                            connection.disconnect();
                            isFinished[0] = true;
                        }else {
                            LogHandler.saveLog("Failed to download "+photosMediaItem.getFileName()+"with response code : "  + responseCode);
                        }
                    } catch (IOException e) {
                        LogHandler.saveLog("Downloading from Photos failed: " + e.getLocalizedMessage());
                    }
                }
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

<<<<<<< HEAD

    public static byte[] readBytesFromFile(File file) throws IOException {
        int bufferSize = 1024 * 1024;
        byte[] buffer = new byte[bufferSize];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            int bytesRead;
            while ((bytesRead = in.read(buffer, 0, bufferSize)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return out.toByteArray();
    }

=======
>>>>>>> e0a6e7003bfa299c85d8a814ff4c9ef9776861c4
}


