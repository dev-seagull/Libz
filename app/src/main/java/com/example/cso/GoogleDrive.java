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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
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
                        final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
                        final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                        HttpRequestInitializer httpRequestInitializer = request -> {
                            request.getHeaders().setAuthorization("Bearer " + accessToken);
                            request.getHeaders().setContentType("application/json");
                        };
                        Drive driveService = new Drive.Builder(netHttpTransport, jsonFactory, httpRequestInitializer)
                                .setApplicationName("cso").build();
                        driveService.files().delete(fileId).execute();
                        LogHandler.SaveLog("Deleting Duplicate file in backup drive :" + mediaItem.getFileName());
                    }catch (Exception e){
                        System.out.println("error in deleting duplicated media items in drive " + e.getLocalizedMessage());
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
            System.out.println("isDeletedFuture" + isDeletedFuture);
        }catch (Exception e ){
            System.out.println("EXception :" + e.getLocalizedMessage());
        }
    }
}
