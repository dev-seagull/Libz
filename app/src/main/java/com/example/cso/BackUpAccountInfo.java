package com.example.cso;

import java.util.ArrayList;

public class BackUpAccountInfo {
    private String userEmail;
    private com.example.cso.PrimaryAccountInfo.Tokens tokens;
    private com.example.cso.PrimaryAccountInfo.Storage storage;
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

    public BackUpAccountInfo(String userEmail, PrimaryAccountInfo.Tokens tokens,
                             PrimaryAccountInfo.Storage storage, ArrayList<MediaItem> mediaItems) {
        this.userEmail = userEmail;
        this.tokens = tokens;
        this.storage = storage;
        this.mediaItems = mediaItems;
    }

    public String getUserEmail() {
        return userEmail;
    }
    public com.example.cso.PrimaryAccountInfo.Tokens getTokens() {return tokens;}
    public com.example.cso.PrimaryAccountInfo.Storage getStorage() {return storage;}
    public ArrayList<MediaItem> getMediaItems() {return mediaItems;}
}
