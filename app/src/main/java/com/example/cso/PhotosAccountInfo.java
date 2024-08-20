package com.example.cso;


import java.util.ArrayList;

public class PhotosAccountInfo {
    private String userEmail;
    private GoogleCloud.Tokens tokens;
    private GoogleCloud.Storage storage;
    private ArrayList<GooglePhotos.MediaItem> mediaItems;


    public PhotosAccountInfo(String userEmail,  GoogleCloud.Tokens tokens, GoogleCloud.Storage storage,
                             ArrayList<GooglePhotos.MediaItem> mediaItems) {
        this.userEmail = userEmail;
        this.tokens = tokens;
        this.storage = storage;
        this.mediaItems = mediaItems;
    }


    public String getUserEmail() {
        return userEmail;
    }
    public GoogleCloud.Tokens getTokens() {return tokens;}
    public GoogleCloud.Storage getStorage() {return storage;}
    public ArrayList<GooglePhotos.MediaItem> getMediaItems() {return mediaItems;}
}

