package com.example.cso;

import android.widget.Toast;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.oauth2.AccessToken;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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


    public static ArrayList<BackUpAccountInfo.MediaItem> getMediaItems(PrimaryAccountInfo.Tokens tokens) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String refreshToken = tokens.getRefreshToken();
        String accessToken = tokens.getAccessToken();
        final ArrayList<BackUpAccountInfo.MediaItem> mediaItems = new ArrayList<>();
        Callable<ArrayList<BackUpAccountInfo.MediaItem>> backgroundTask = () -> {
            try {
                final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
                final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                HttpRequestInitializer httpRequestInitializer = request -> {
                    request.getHeaders().setAuthorization("Bearer " + accessToken);
                    request.getHeaders().setContentType("application/json");
                };
                Drive driveService = new Drive.Builder(netHttpTransport, jsonFactory, httpRequestInitializer)
                        .setApplicationName("cso").build();
                FileList result = driveService.files().list()
                        .setFields("files(id, name, sha256Checksum)")
                        .execute();
                List<File> files = result.getFiles();
                if (files != null && !files.isEmpty()) {
                    for (File file : files) {
                        if (GooglePhotos.isVideo(GoogleCloud.getMemeType(file.getName())) |
                                GooglePhotos.isImage(GoogleCloud.getMemeType(file.getName()))){
                            BackUpAccountInfo.MediaItem mediaItem = new BackUpAccountInfo.MediaItem(file.getName(),
                                    file.getSha256Checksum().toLowerCase(), file.getId());
                            mediaItems.add(mediaItem);
                        }
                    }
                    LogHandler.saveLog( mediaItems.size() + " files were found in Google Drive back up account",false);
                } else {
                    LogHandler.saveLog("No file was found in Google Drive back up account",false);
                }
                return mediaItems;
            }catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
                LogHandler.saveLog("Error when trying to get files from google drive: " + e.getLocalizedMessage());
            }
            return mediaItems;
        };
        Future<ArrayList<BackUpAccountInfo.MediaItem>> future = executor.submit(backgroundTask);
        ArrayList<BackUpAccountInfo.MediaItem> uploadFileIDs_fromFuture = null;
        try {
            uploadFileIDs_fromFuture = future.get();
        } catch (InterruptedException | ExecutionException e) {
            LogHandler.saveLog("Error when trying to get drive files from future: " + e.getLocalizedMessage());
        } finally {
            executor.shutdown();
        }
        return uploadFileIDs_fromFuture;
    }


    public static void deleteDuplicatedMediaItems (ArrayList<BackUpAccountInfo.MediaItem> mediaItems, PrimaryAccountInfo.Tokens tokens){
        HashSet<String> mediaItemsHash = new HashSet<>();
        String accessToken = tokens.getAccessToken();
        String refreshToken = tokens.getRefreshToken();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final Boolean[] isDeleted = {false};
        Callable<Boolean> backgroundTask = () -> {
            for (BackUpAccountInfo.MediaItem mediaItem : mediaItems){
                String hash = mediaItem.getHash();
                if (mediaItemsHash.contains(hash)) {
                    try{
                        String fileId = mediaItem.getId();


//                        final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
//                        final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
//                        HttpRequestInitializer httpRequestInitializer = request -> {
//                            request.getHeaders().setAuthorization("Bearer " + accessToken);
//                            request.getHeaders().setContentType("application/json");
//                        };
//                        Drive driveService = new Drive.Builder(netHttpTransport, jsonFactory, httpRequestInitializer)
//                                .setApplicationName("cso").build();
//                        driveService.files().delete(fileId).execute();
                        URL url = new URL("https://www.googleapis.com/drive/v3/files/" + fileId);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("DELETE");
                        connection.setRequestProperty("Content-type", "application/json");
                        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                        int responseCode = connection.getResponseCode();
                        System.out.println("response code of deleting: "+ responseCode);

                        InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        StringBuilder responseStringBuilder = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            responseStringBuilder.append(line);
                        }
                        bufferedReader.close();
                        String response = responseStringBuilder.toString();
                        System.out.println("response : " +response);

                        LogHandler.saveLog("Deleting Duplicate file in backup drive :" + mediaItem.getFileName(),false);
                    }catch (Exception e){
                        LogHandler.saveLog("error in deleting duplicated media items in drive: " + e.getLocalizedMessage());
                    }
                }else{
                    mediaItemsHash.add(hash);
                }
            }
            isDeleted[0] = true ;
            return isDeleted[0];
        };
        Future<Boolean> future = executor.submit(backgroundTask);
        try{
            Boolean isDeletedFuture = future.get();
        }catch (Exception e ){
            LogHandler.saveLog("Exception when trying to delete file from drive back up: " + e.getLocalizedMessage());
        }
    }
}
