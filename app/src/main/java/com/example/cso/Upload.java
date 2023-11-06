package com.example.cso;

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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Upload {
    public ArrayList<String> uploadPhotosToGoogleDrive(ArrayList<GooglePhotos.MediaItem> mediaItems, String accessToken,
                                                       ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems) {
        LogHandler.saveLog("Starting to upload from Photos to Drive");
        LocalTime currentTime;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            currentTime = LocalTime.now();
        } else {
            currentTime = null;
        }
        if (currentTime != null) {
            LogHandler.saveLog("Uploading from Photos to Drive started in " + currentTime);
        }else{
            LogHandler.saveLog("Current time is null");
        }

        final File[] file = new File[1];
        ArrayList<String> baseUrls = new ArrayList<>();
        ArrayList<String> uploadFileIDs = new ArrayList<>();
        ArrayList<String> fileNames = new ArrayList<>();
        for (GooglePhotos.MediaItem mediaItem : mediaItems) {
            baseUrls.add(mediaItem.getBaseUrl());
            fileNames.add(mediaItem.getFileName());
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        ArrayList<String> finalUploadFileIDs = uploadFileIDs;
        Callable<ArrayList<String>> uploadTask = () -> {
            try {
                String destinationFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                        + File.separator + "cso";
                File destinationFolder = new File(destinationFolderPath);

                downloadFromPhotos(baseUrls, fileNames, destinationFolder, file);

                File[] destinationFolderFiles = destinationFolder.listFiles();

                int i = 0;
                LogHandler.saveLog("Stated to calculate hash before uploading from Photos to Drive at " + currentTime);
                for(File destinationFolderFile: destinationFolderFiles) {
                    String hash = calculateHash(destinationFolderFile).toLowerCase();
                    mediaItems.get(i).setHash(hash);
                    i++;
                }
                LogHandler.saveLog("Finished calculating hash before uploading from Photos to Drive at " + currentTime);

                uploadToDrive(destinationFolderFiles, backUpMediaItems, accessToken, finalUploadFileIDs);

                deleteDestinationFolder(destinationFolder);
                LogHandler.saveLog("End of uploading from Photos to Drive: " + currentTime);
            }catch (Exception e) {
                LogHandler.saveLog("Failed to upload from Photos to Drive: " + e.getLocalizedMessage());
            }
            return finalUploadFileIDs;
        };
        Future<ArrayList<String>> futureFileIds = executor.submit(uploadTask);
        try{
            uploadFileIDs = futureFileIds.get();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get file id from upload task future: " + e.getLocalizedMessage());
        }
        return uploadFileIDs;
    }

    private Boolean downloadFromPhotos(ArrayList<String> baseUrls, ArrayList<String> fileNames,
                                    File destinationFolder, File[] file){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final boolean[] isFinished = {false};
        Callable<Boolean> backgroundDownloadTask = () -> {
            int i = 0;
            for(String baseUrl: baseUrls){
                try {
                    URL url = new URL(baseUrl + "=d");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
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
                            file[0] = new File(filePath);
                            file[0].createNewFile();
                            outputStream = new FileOutputStream(file[0]);
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
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
                    }
                } catch (IOException e) {
                    LogHandler.saveLog("Downloading from Photos failed: " + e.getLocalizedMessage());
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

    private void deleteDestinationFolder(File destinationFolder){
        File[] destinationFolderFiles = destinationFolder.listFiles();
        if (destinationFolderFiles != null) {
            for (File destinationFolderFile: destinationFolderFiles) {
                destinationFolderFile.delete();
            }
        }else{
            LogHandler.saveLog("Destination folder is null when trying to delete its content");
        }
        if(destinationFolder.exists()){
            destinationFolder.delete();
            LogHandler.saveLog("Destination folder was deleted after the uploading process is finished");
        }else{
            LogHandler.saveLog("The destination folder doesn't exist to delete");
        }
    }

    public ArrayList<String> uploadToDrive(File[] destinationFolderFiles, ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems,
                              String accessToken, ArrayList<String> uploadFileIds){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ArrayList<String> finalUploadFileIds = uploadFileIds;
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
                            String memeType = GooglePhotos.getMemeType(destinationFolderFile);
                            FileContent mediaContent = null;
                            if(GooglePhotos.isImage(memeType)){
                                String destinationFolderFilePath = destinationFolderFile.getPath();
                                if(memeType.toLowerCase().endsWith("jpg")){
                                    mediaContent = new FileContent("image/jpeg" ,
                                            new File(destinationFolderFilePath));
                                }else{
                                    mediaContent = new FileContent("image/" + memeType.toLowerCase() ,
                                            new File(destinationFolderFilePath));
                                }
                            }else if(GooglePhotos.isVideo(memeType)){
                                String destinationFolderFilePath = destinationFolderFile.getPath();
                                if(memeType.toLowerCase().endsWith("mkv")){
                                    mediaContent = new FileContent("video/x-matroska" ,
                                            new File(destinationFolderFilePath));
                                }else{
                                    mediaContent = new FileContent("video/" + memeType.toLowerCase() ,
                                            new File(destinationFolderFilePath));
                                }
                            }else if(destinationFolderFile.getName().equals("log.txt")){
                                String destinationFolderFilePath = destinationFolderFile.getPath();
                                mediaContent = new FileContent("text/plain" ,
                                        new File(destinationFolderFilePath));
                            }
                            if(mediaContent == null){
                                LogHandler.saveLog("You're trying to upload mediaContent of null for "
                                        + destinationFolderFile.getName());
                            }

                            //final int[] test = {2};
                            //if(test[0] > 0){
                            com.google.api.services.drive.model.File uploadFile =
                                    null;
                            if (service != null) {
                                uploadFile = service.files().create(fileMetadata, mediaContent).setFields("id").execute();
                            }else{
                                LogHandler.saveLog("Drive service is null");
                            }
                            String uploadFileId = uploadFile.getId();
                            while(uploadFileId.isEmpty() | uploadFileId == null){
                                wait();
                            }
                            finalUploadFileIds.add(uploadFileId);
                            LogHandler.saveLog("Uploading " + destinationFolderFile.getName()
                                    + " to Drive backup account uploadId finished with FileId: " + uploadFileId);
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
        return hexString.toString();
    }
}
