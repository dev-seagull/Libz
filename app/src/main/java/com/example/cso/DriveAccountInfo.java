package com.example.cso;

import java.util.ArrayList;

public class DriveAccountInfo {
    private String userEmail;
    private GoogleCloud.Tokens tokens;
    private PhotosAccountInfo.Storage storage;
    private ArrayList<MediaItem> mediaItems ;

    public static class MediaItem{
        private String fileName;
        private String hash;
        private String fileId;

        public MediaItem(String fileName, String hash, String fileId) {
            this.fileName = fileName;
            this.hash = hash;
            this.fileId = fileId;
        }
        public String getFileName() {return fileName;}
        public String getHash() {return hash;}
        public String getId() {return fileId;}
    }

    public DriveAccountInfo(String userEmail,  GoogleCloud.Tokens tokens,
                            PhotosAccountInfo.Storage storage, ArrayList<MediaItem> mediaItems) {
        this.userEmail = userEmail;
        this.tokens = tokens;
        this.storage = storage;
        this.mediaItems = mediaItems;
    }

    public String getUserEmail() {
        return userEmail;
    }
    public GoogleCloud.Tokens getTokens() {return tokens;}
    public PhotosAccountInfo.Storage getStorage() {return storage;}
    public ArrayList<MediaItem> getMediaItems() {return mediaItems;}
}
