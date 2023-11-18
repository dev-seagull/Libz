//package com.example.cso;
//
//
//import java.util.ArrayList;
//
//public class PrimaryAccountInfo {
//    private String userEmail;
//    private Tokens tokens;
//    private Storage storage;
//    private ArrayList<GooglePhotos.MediaItem> mediaItems;
//
//    public static class Tokens {
//        private String refreshToken;
//        private String accessToken;
//
//        public Tokens(String accessToken, String refreshToken) {
//            this.accessToken = accessToken;
//            this.refreshToken = refreshToken;
//        }
//
//        public void setAccessToken(String accessToken) {
//            this.accessToken = accessToken;
//        }
//
//        public void setRefreshToken(String refreshToken) {
//            this.refreshToken= refreshToken;
//        }
//
//        public String getAccessToken() {
//            return accessToken;
//        }
//
//        public String getRefreshToken() {
//            return refreshToken;
//        }
//    }
//
//
//    public static class Storage{
//        private Double totalStorage;
//        private Double usedStorage;
//        private Double usedInDriveStorage;
//        private Double UsedInGmailAndPhotosStorage;
//
//
//        public Storage(Double totalStorage, Double usedStorage,
//                       Double usedInDriveStorage){
//            this.totalStorage = totalStorage * 1000;
//            this.usedStorage = usedStorage * 1000;
//            this.usedInDriveStorage = usedInDriveStorage * 1000;
//            this.UsedInGmailAndPhotosStorage = usedStorage - usedInDriveStorage;
//        }
//        public Double getUsedInDriveStorage() {return usedInDriveStorage;}
//        public Double getUsedInGmailAndPhotosStorage() {return UsedInGmailAndPhotosStorage;}
//        public Double getTotalStorage() {return totalStorage;}
//        public Double getUsedStorage() {return usedStorage;}
//    }
//
//    public PrimaryAccountInfo(String userEmail, Tokens tokens, Storage storage,
//                              ArrayList<GooglePhotos.MediaItem> mediaItems) {
//        this.userEmail = userEmail;
//        this.tokens = tokens;
//        this.storage = storage;
//        this.mediaItems = mediaItems;
//    }
//
//
//    public String getUserEmail() {
//        return userEmail;
//    }
//    public Tokens getTokens() {return tokens;}
//    public Storage getStorage() {return storage;}
//    public ArrayList<GooglePhotos.MediaItem> getMediaItems() {return mediaItems;}
//}
//
