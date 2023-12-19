package com.example.cso;

import android.app.Activity;
import android.database.Cursor;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GooglePhotos {
    public GooglePhotos(){
    }

    public static class MediaItem{
        private String Id;
        private String baseUrl;
        private String creationTime;
        private String fileName;
        private String hash;

        public MediaItem(String Id, String baseUrl, String creationTime,
                         String fileName, String hash) {
            this.Id = Id;
            this.baseUrl = baseUrl;
            this.creationTime = creationTime;
            this.fileName = fileName;
            this.hash = hash;
        }

        public String getId() {return Id;}

        public String getBaseUrl() {return baseUrl;}

        public void setHash(String hash) {this.hash = hash;}

        public String getHash() {return hash;}

        public String getCreationTime() {return creationTime;}

        public String getFileName() {return fileName;}
    }

    public static ArrayList<MediaItem> getGooglePhotosMediaItems(String userEmail){
        String sqlQuery = "SELECT accessToken FROM USERPROFILE WHERE userEmail = ?;";
        Cursor cursor = MainActivity.dbHelper.dbReadable.rawQuery(sqlQuery, new String[]{userEmail});
        String accessToken = "";
        if(cursor.moveToFirst() && cursor != null){
            int accessTokenColumnIndex  = cursor.getColumnIndex("accessToken");
            if(accessTokenColumnIndex >= 0){
                accessToken = cursor.getString(accessTokenColumnIndex);
            }
        }else{
            LogHandler.saveLog("Failed to get access token in getGooglePhotosMediaItems");
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        String finalAccessToken = accessToken;
        Callable<ArrayList<MediaItem>> backgroundTask = () -> {
            if (finalAccessToken == null | finalAccessToken.isEmpty()) {
                return new ArrayList<>();
            }
            System.out.println("access token to uplaod to photos for test: "+ finalAccessToken);
            ArrayList<MediaItem> mediaItems = new ArrayList<>();
            String nextPageToken = null;
            JSONObject responseJson = null;
            do {
                try {
                    URL url;
                    if (nextPageToken == null) {
                        url = new URL("https://photoslibrary.googleapis.com/v1/mediaItems");
                    } else {
                        url = new URL("https://photoslibrary.googleapis.com/v1/mediaItems?pageToken=" + nextPageToken);
                    }
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("GET");
                    httpURLConnection.setRequestProperty("Content-type", "application/json");
                    httpURLConnection.setRequestProperty("Authorization", "Bearer " + finalAccessToken);
                    int responseCode = httpURLConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        StringBuilder responseStringBuilder = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            responseStringBuilder.append(line);
                        }
                        bufferedReader.close();
                        String response = responseStringBuilder.toString();
                        responseJson = new JSONObject(response);
                        JSONArray mediaItemsResponse = responseJson.getJSONArray("mediaItems");
                        for (int i = 0; i < mediaItemsResponse.length(); i++) {
                            JSONObject mediaItemJsonObject = mediaItemsResponse.getJSONObject(i);
                            String filename = mediaItemJsonObject.getString("filename");
                            String baseUrl = mediaItemJsonObject.getString("baseUrl");
                            String id = mediaItemJsonObject.getString("id");
                            JSONObject mediaMetaDataObject = mediaItemJsonObject.getJSONObject("mediaMetadata");
                            String creationTime = mediaMetaDataObject.getString("creationTime");

                            MainActivity.dbHelper.insertIntoPhotosTable(Long.valueOf(0), id, filename, "",
                                    userEmail, creationTime, baseUrl);

                            LogHandler.saveLog("File was detected in Photos account : " + filename,false);
                        }
                        nextPageToken = responseJson.optString("nextPageToken", null);
                    }
                } catch (Exception e) {
                    LogHandler.saveLog("Failed to get files from Photos account " + e.getLocalizedMessage());
                }
            } while (responseJson != null && responseJson.has("nextPageToken") && responseJson.has("mediaItems"));
            return mediaItems;
        };
        Future<ArrayList<MediaItem>> future = executor.submit(backgroundTask);
        ArrayList<MediaItem> mediaItems = new ArrayList<>();
        try{
            mediaItems = future.get();
            LogHandler.saveLog( mediaItems.size() + " files were found in Photos account",false);
        }catch (Exception e ){
            LogHandler.saveLog("Failed when trying to get Photos files from future: " + e.getLocalizedMessage());
        }finally {
            executor.shutdown();
        }
        return mediaItems;
    }

    public static String getMemeType(File file){
        int dotIndex = file.getName().lastIndexOf(".");
        String memeType="";
        if (dotIndex >= 0 && dotIndex < file.getName().length() - 1) {
            memeType = file.getName().substring(dotIndex + 1);
        }
        return memeType;
    }

    public static boolean isImage(String memeType){
        memeType = memeType.toLowerCase();
        ArrayList<String> imageExtensions = new ArrayList<String>(
                Arrays.asList("jpeg", "jpg", "png", "gif", "bmp")
        );
        if(imageExtensions.contains(memeType)){
            return true;
        }else{
            return false;
        }
    }

    public static boolean isVideo(String memeType){
        memeType = memeType.toLowerCase();
        ArrayList<String> videoExtensions = new ArrayList<String>(
                Arrays.asList("mkv", "mp4")
        );
        if(videoExtensions.contains(memeType)){
            return true;
        }else{
            return false;
        }
    }

//    public ArrayList<String> uploadAndroidToGoogleDrive(ArrayList<Android.MediaItem> mediaItems,ArrayList<MediaItem> primaryMediaItems ,String accessToken,
//                                                        ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems, Activity activity) {
//        LogHandler.saveLog("Start of Syncing Android to backup");
//        ArrayList<String> uploadFileIds = new ArrayList<>();
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        final LocalTime[] currentTime = new LocalTime[1];
//        Callable<ArrayList<String>> uploadTask = () -> {
//            try {
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                    currentTime[0] = LocalTime.now();
//                } else {
//                    currentTime[0] = null;
//                }
//                ArrayList<String> mediaItemsInPhotosNames = new ArrayList<>();
//                for(MediaItem primaryMediaItem: primaryMediaItems){
//                    mediaItemsInPhotosNames.add(primaryMediaItem.getFileName());
//                }
//
//                HashSet<String> mediaItemsInAndroidNames = new HashSet<>();
////                final int[] test = {1};
//                HashSet<String> hashSet = new HashSet<>();
//
//                for (Android.MediaItem mediaItem : mediaItems) {
//                    if(hashSet.contains(mediaItem.getFileHash()) |
//                            mediaItemsInPhotosNames.contains(mediaItem.getFileName()) | mediaItemsInAndroidNames.contains(mediaItem.getFileName())){
//                        if(hashSet.contains(mediaItem.getFileHash())){
//                            LogHandler.saveLog("this file is considered duplicated in android device " + mediaItem.getFileName() , false);
//                        }else if(mediaItemsInAndroidNames.contains(mediaItem.getFileName())){
//                            LogHandler.saveLog("this file is duplicated by its name in android device" + mediaItem.getFileName(),false);
//                        }else{
//                            LogHandler.saveLog("this file is considered duplicated for its name" +
//                                    "in android and primary account " + mediaItem.getFileName(),false);
//                        }
//                    }else{
//                        mediaItemsInAndroidNames.add(mediaItem.getFileName());
//                        File file = new File(mediaItem.getFilePath());
//                        if (Duplicate.isDuplicatedInBackup(backUpMediaItems, file) == false &&
//                                Duplicate.isDuplicatedInPrimary(primaryMediaItems, file) == false) {
//                            try {
//                                NetHttpTransport HTTP_TRANSPORT = null;
//                                try {
//                                    HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//                                } catch (GeneralSecurityException e) {
//                                    LogHandler.saveLog("Failed to http_transport " + e.getLocalizedMessage());
//                                } catch (IOException e) {
//                                }
//                                final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//
//                                HttpRequestInitializer requestInitializer = request -> {
//                                    request.getHeaders().setAuthorization("Bearer " + accessToken);
//                                    request.getHeaders().setContentType("application/json");
//                                };
//
//                                Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
//                                        .setApplicationName("cso")
//                                        .build();
//
//                                com.google.api.services.drive.model.File fileMetadata =
//                                        new com.google.api.services.drive.model.File();
//                                fileMetadata.setName(mediaItem.getFileName());
//                                String memeType = getMemeType(new File(mediaItem.getFilePath()));
//
//                                FileContent mediaContent = null;
//                                if (isImage(memeType)) {
//                                    String mediaItemPath = mediaItem.getFilePath();
//                                    if(new File(mediaItemPath).exists()){
//                                    }else{
//                                        System.out.println("the android file doesn't exist in memetype condition");
//                                    }
//
//                                    if (memeType.toLowerCase().endsWith("jpg")) {
//                                        mediaContent = new FileContent("image/jpeg",
//                                                new File(mediaItemPath));
//                                    } else {
//                                        mediaContent = new FileContent("image/" + memeType.toLowerCase(),
//                                                new File(mediaItemPath));
//                                    }
//                                } else if (isVideo(memeType)) {
//                                    String mediaItemPath = mediaItem.getFilePath();
//                                    if(new File(mediaItemPath).exists()){
//                                    }else{
//                                        System.out.println("the android file doesn't exist in is video condition");
//                                    }
//
//                                    if (memeType.toLowerCase().endsWith("mkv")) {
//                                        mediaContent = new FileContent("video/x-matroska",
//                                                new File(mediaItemPath));
//                                    } else {
//                                        mediaContent = new FileContent("video/" + memeType.toLowerCase(),
//                                                new File(mediaItemPath));
//                                    }
//                                }
//
////                                if (test[0] >0 && !isVideo(memeType)){
//                                com.google.api.services.drive.model.File uploadFile =
//                                        service.files().create(fileMetadata, mediaContent).setFields("id").execute();
//                                String uploadFileId = uploadFile.getId();
//                                while(uploadFileId == null){
//                                    wait();
//                                }
//                                if (uploadFileId == null | uploadFileId.isEmpty()){
//                                    LogHandler.saveLog("Failed to upload " + file.getName() + " from Android to backup because it's null");
//                                }else{
//                                    uploadFileIds.add(uploadFileId);
//                                    LogHandler.saveLog("Uploading " + mediaItem.getFileName() +
//                                            " from android into backup account uploadId : " + uploadFileId,false);
//                                }
////                                  test[0]--;
//                            } catch (Exception e) {
//                                System.out.println("Uploading android error: " + e.getMessage());
//                                LogHandler.saveLog("Uploading android error: " + e.getMessage());
//                            }
//                        }
//                    }
//                }
//            } catch (Exception e){
//                System.out.println("Uploading android error: " + e.getMessage());
//                LogHandler.saveLog("Uploading android error: " + e.getMessage());
//            }
//            return uploadFileIds;
//        };
//        Future<ArrayList<String>> future = executor.submit(uploadTask);
//        ArrayList<String> uploadFileIdsFuture = new ArrayList<>();
//        try{
//            uploadFileIdsFuture = future.get();
//            System.out.println("Finished with " + uploadFileIdsFuture.size() + " uploads at " + currentTime[0].toString());
//            LogHandler.saveLog("Finished with " + uploadFileIdsFuture.size() + " uploads at " + currentTime[0].toString());
//            System.out.println("-----end of second ----->");
//        }catch (Exception e){
//            System.out.println(e.getLocalizedMessage());
//        }
//        return  uploadFileIdsFuture;
//    }

}
