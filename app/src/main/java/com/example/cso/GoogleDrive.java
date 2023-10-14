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

}
