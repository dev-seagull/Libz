package com.example.cso;

import android.widget.Toast;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

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

    public ArrayList<MediaItem> getMediaItems(PrimaryAccountInfo.Tokens backUpAccountTokens){
        ArrayList<MediaItem> mediaItems = new ArrayList<>();
        String accessToken = backUpAccountTokens.getAccessToken();
        String refreshToken = backUpAccountTokens.getRefreshToken();

        NetHttpTransport HTTP_TRANSPORT = null;
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException e) {
            //Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            //Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

        HttpRequestInitializer requestInitializer = request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken);
            request.getHeaders().setContentType("application/json");
        };

        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                .setApplicationName("cso").build();

        List<File> driveFiles = null;
        try{
            driveFiles = service.files().list()
                    .setFields("files(id, name, md5Checksum, videoMediaMetadata)").execute().getFiles();
            System.out.println("len: " + driveFiles.size());
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        if(driveFiles != null){
            System.out.println("it's not null");
            for(File driveFile: driveFiles){
                String mediaItemId = driveFile.getId();
                String mediaItemName = driveFile.getName();
                String mediaItemHash = driveFile.getMd5Checksum();
                System.out.println(mediaItemId + " name and hash" + mediaItemName + " " +  mediaItemHash);

                Long videoDuration = null;
                if(driveFile.getVideoMediaMetadata() != null){
                    videoDuration = driveFile.getVideoMediaMetadata().getDurationMillis();
                }

                MediaItem mediaItem = new MediaItem(mediaItemId, mediaItemName,
                        mediaItemHash, videoDuration);
                mediaItems.add(mediaItem);
            }
        }else {
            System.out.println("it's null");
        }

        return mediaItems;
    }
}
