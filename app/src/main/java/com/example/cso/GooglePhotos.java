package com.example.cso;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

public class GooglePhotos {
    public static boolean is_first_task_finish = false;
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

    public static ArrayList<MediaItem> getGooglePhotosMediaItems(PrimaryAccountInfo.Tokens tokens){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<ArrayList<MediaItem>> backgroundTask = () -> {
            if (tokens == null) {
                return new ArrayList<>();
            }

            int pageSize = 100;
            String accessToken = tokens.getAccessToken();
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
                    httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

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
                            MediaItem mediaItem = new MediaItem(id, baseUrl, "2-2-2", filename, null);
                            mediaItems.add(mediaItem);
                            LogHandler.SaveLog(mediaItem.getFileName() + " detected in photos account");
                        }
                        nextPageToken = responseJson.optString("nextPageToken", null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (responseJson != null && responseJson.has("nextPageToken") && responseJson.has("mediaItems"));

            return mediaItems;
        };

        Future<ArrayList<MediaItem>> future = executor.submit(backgroundTask);
        ArrayList<MediaItem> mediaItems = new ArrayList<>();
        try{
            mediaItems = future.get();
            System.out.println("media items in primary size: " + mediaItems.size());
        }catch (Exception e ){
            System.out.println("Exception :" + e.getLocalizedMessage());
        }finally {
            executor.shutdown();
        }
        return mediaItems;
    }

    byte[] inputStreamToByteArray(InputStream inputStream) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while (true) {
            try {
                if (!((nRead = inputStream.read(data, 0, data.length)) != -1)) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            buffer.write(data, 0, nRead);
        }
        try {
            buffer.flush();
        } catch (IOException e) {
            //Toast.makeText(activity,"Uploading failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        return buffer.toByteArray();
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
    public boolean isDuplicatedInBackup(ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems
            ,File file){
        boolean isDuplicatedInBackup = false;
        String fileHash;

        try {
            fileHash = MainActivity.calculateHash(file).toLowerCase();
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        for(BackUpAccountInfo.MediaItem backUpMediaItem: backUpMediaItems){
            String backUpHash = backUpMediaItem.getHash();
            if(fileHash.equals(backUpHash)){
                System.out.println(file.getName() + " is duplicated");
                LogHandler.SaveLog(file.getName() + " detected as duplicate in backup and photos");
                isDuplicatedInBackup = true;
                break;
            }
        }

        return isDuplicatedInBackup;
    }


    public boolean isDuplicatedInPrimary(ArrayList<GooglePhotos.MediaItem> primaryMediaItems
            ,File file){
        boolean isDuplicatedInPrimary = false;
        String fileHash;

        try {
            fileHash = MainActivity.calculateHash(file).toLowerCase();
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        for(GooglePhotos.MediaItem primaryMediaItem: primaryMediaItems){
            String primaryHash = primaryMediaItem.getHash();
            if(fileHash.equals(primaryHash)){
                System.out.println(file.getName() + " is duplicated");
                LogHandler.SaveLog(file.getName() + "detected as duplicate in photos and device");
                isDuplicatedInPrimary = true;
                break;
            }
        }

        return isDuplicatedInPrimary;
    }

    public ArrayList<String> uploadPhotosToGoogleDrive(ArrayList<MediaItem> mediaItems, String accessToken,
                                          ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems) {
        System.out.println("-------first----->");
        LocalTime currentTime;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            currentTime = LocalTime.now();
        } else {
            currentTime = null;
        }
        System.out.println("start of photos: " + currentTime.toString());
        //final int[] test = {2};
        final File[] file = new File[1];

        ArrayList<String> baseUrls = new ArrayList<>();
        ArrayList<String> fileNames = new ArrayList<>();
        for (MediaItem mediaItem : mediaItems) {
            baseUrls.add(mediaItem.getBaseUrl());
            fileNames.add(mediaItem.getFileName());
        }

        ArrayList<String> uploadFileIDs = new ArrayList();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<ArrayList<String>> uploadTask = () -> {
            try {
                String destinationFolderPath = Environment.getExternalStoragePublicDirectory
                        (Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso";
                File destinationFolder = new File(destinationFolderPath);

                int i =0;
                for(String baseUrl: baseUrls) {
                    System.out.println("base url : " + baseUrl);
                    try {
                        URL url = new URL(baseUrl + "=d");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");

                        int responseCode = connection.getResponseCode();
                        System.out.println("photos download respones code: " + responseCode);
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                            if (!destinationFolder.exists()) {
                                boolean isFolderCreated = destinationFolder.mkdirs();
                                if (!isFolderCreated) {
                                    System.out.println("uploading failed -- > cant create folder");
//                                    Toast.makeText(activity, "Uploading failed", Toast.LENGTH_LONG).show();
                                }
                            }

                            String fileName = fileNames.get(i);
                            String filePath = destinationFolder + File.separator + fileName;
                            System.out.println("file name : "+ fileName + " file path :" + filePath);
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
                                System.out.println("error in file output stream handling " + e.getLocalizedMessage() );
//                                Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            } finally {
                                try {
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                } catch (IOException e) {
                                    System.out.println("error in closing outputstream" + e.getLocalizedMessage());
                                    //Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                            inputStream.close();
                            connection.disconnect();
                        }
                    } catch (MalformedURLException e) {
                        //Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    } catch (ProtocolException e) {
                        //Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        //Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                    String hash = MainActivity.calculateHash(file[0]).toLowerCase();
                    System.out.println("try to set hash : " + hash);
                    mediaItems.get(i).setHash(hash);
                    i++;
                }

                System.out.println("start of checking duplicated photos: " + currentTime.toString());
                File[] destinationFolderFiles = destinationFolder.listFiles();
                if(destinationFolderFiles != null && destinationFolderFiles.length> 0) {
                    for (File destinationFolderFile : destinationFolderFiles) {
                        if (isDuplicatedInBackup(backUpMediaItems, destinationFolderFile) == false) {
                            NetHttpTransport HTTP_TRANSPORT = null;
                            try {
                                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                            } catch (GeneralSecurityException e) {
                                //Toast.makeText(activity,"Uploading failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                                //Toast.makeText(activity,"Uploading failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                            final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

                            HttpRequestInitializer requestInitializer = request -> {
                                request.getHeaders().setAuthorization("Bearer " + accessToken);
                                request.getHeaders().setContentType("application/json");
                            };

                            try {
                                Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                                        .setApplicationName("cso")
                                        .build();

                                System.out.println("hell upload " + destinationFolderFile.getName());
                                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                                fileMetadata.setName(destinationFolderFile.getName());
                                String memeType = getMemeType(destinationFolderFile);

                                FileContent mediaContent = null;
                                if(isImage(memeType)){
                                    String destinationFolderFilePath = destinationFolderFile.getPath();

                                    if(memeType.toLowerCase().endsWith("jpg")){
                                        mediaContent = new FileContent("image/jpeg" ,
                                                new File(destinationFolderFilePath));

                                    }else{
                                        mediaContent = new FileContent("image/" + memeType.toLowerCase() ,
                                                new File(destinationFolderFilePath));
                                    }
                                }else if(isVideo(memeType)){
                                    String destinationFolderFilePath = destinationFolderFile.getPath();

                                    if(memeType.toLowerCase().endsWith("mkv")){
                                        mediaContent = new FileContent("video/x-matroska" ,
                                                new File(destinationFolderFilePath));
                                    }else{
                                        mediaContent = new FileContent("video/" + memeType.toLowerCase() ,
                                                new File(destinationFolderFilePath));
                                    }
                                }

                                if(mediaContent == null){
                                    System.out.println("media content of photos null ");
                                }else {
                                    System.out.println("media content of photos not null ");
                                }

                                //if(test[0] > 0){
                                com.google.api.services.drive.model.File uploadFile =
                                        service.files().create(fileMetadata, mediaContent).setFields("id").execute();
                                String uploadFileId = uploadFile.getId();
                                while(uploadFileId.isEmpty() | uploadFileId == null){
                                    wait();
                                }
                                uploadFileIDs.add(uploadFileId);
                                LogHandler.SaveLog("Uploading " + uploadFile.getName() + " from photos into backup account uploadId" + uploadFileId);
                                System.out.println("upload finished with this uploadfile id :" + uploadFileId.toString());
                                    //test[0]--;
                                //}

                                //here
                            }catch (Exception e) {
                                System.out.println(e.getLocalizedMessage());
                            }

                        }
                    }
                }
                for (File destinationFolderFile: destinationFolderFiles) {
                    destinationFolderFile.delete();
                }

                if(destinationFolder.exists()){
                    destinationFolder.delete();
                }

                System.out.println("end of photos: " + currentTime.toString());
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return uploadFileIDs;
        };
        Future<ArrayList<String>> future = executor.submit(uploadTask);
        ArrayList<String> uploadFileIDs_fromFuture = new ArrayList<>();
        try {
            // Wait for the task to complete and get the result
            uploadFileIDs_fromFuture = future.get();
            System.out.println("future is completed ");
            for(String str : uploadFileIDs_fromFuture){
                System.out.println("ids :" + str);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        System.out.println("-----end of first ----->");
        return uploadFileIDs_fromFuture;
    }


    public ArrayList<String> uploadAndroidToGoogleDrive(ArrayList<Android.MediaItem> mediaItems,ArrayList<MediaItem> primaryMediaItems ,String accessToken,
                                                        ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems, Activity activity) {
        System.out.println("-------second----->");
        ArrayList<String> uploadFileIds = new ArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final LocalTime[] currentTime = new LocalTime[1];
        Callable<ArrayList<String>> uploadTask = () -> {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    currentTime[0] = LocalTime.now();
                } else {
                    currentTime[0] = null;
                }
                ArrayList<String> mediaItemsInPhotosNames = new ArrayList<>();
                for(MediaItem primaryMediaItem: primaryMediaItems){
                    mediaItemsInPhotosNames.add(primaryMediaItem.getFileName());
                }

                HashSet<String> mediaItemsInAndroidNames = new HashSet<>();

                System.out.println("start of android: " + currentTime[0].toString());

                final int[] test = {1};
                HashSet<String> hashSet = new HashSet<>();

                int i = 0;
                for (Android.MediaItem mediaItem : mediaItems) {
                    if(hashSet.contains(mediaItem.getFileHash()) |
                            mediaItemsInPhotosNames.contains(mediaItem.getFileName()) | mediaItemsInAndroidNames.contains(mediaItem.getFileName())){
                        if(hashSet.contains(mediaItem.getFileHash())){
                            System.out.println("this file is considered duplicated in android device " + mediaItem.getFileName());
                        }else if(mediaItemsInAndroidNames.contains(mediaItem.getFileName())){
                            System.out.println("this file is duplicated by its name in android device");
                        }else{
                            System.out.println("this file is considered duplicated for its name" +
                                    "in android and primary account " + mediaItem.getFileName());
                        }
                        continue;
                    }else{
                        mediaItemsInAndroidNames.add(mediaItem.getFileName());
                        File file = new File(mediaItem.getFilePath());
                        if (isDuplicatedInBackup(backUpMediaItems, file) == false &&
                                isDuplicatedInPrimary(primaryMediaItems, file) == false) {

                            System.out.println("start to upload at: " + currentTime[0].toString());
                            try {
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
                                        .setApplicationName("cso")
                                        .build();

                                com.google.api.services.drive.model.File fileMetadata =
                                        new com.google.api.services.drive.model.File();
                                fileMetadata.setName(mediaItem.getFileName());
                                String memeType = getMemeType(new File(mediaItem.getFilePath()));

                                FileContent mediaContent = null;
                                if (isImage(memeType)) {
                                    String mediaItemPath = mediaItem.getFilePath();
                                    if(new File(mediaItemPath).exists()){
                                        //System.out.println("the android file exists");
                                    }else{
                                        System.out.println("the android file doesn't exist");
                                    }

                                    if (memeType.toLowerCase().endsWith("jpg")) {
                                        mediaContent = new FileContent("image/jpeg",
                                                new File(mediaItemPath));
                                    } else {
                                        mediaContent = new FileContent("image/" + memeType.toLowerCase(),
                                                new File(mediaItemPath));
                                    }
                                    //String hash = MainActivity.calculateHash(new File(mediaItem.getFilePath()), activity);
                                } else if (isVideo(memeType)) {
                                    String mediaItemPath = mediaItem.getFilePath();
                                    if(new File(mediaItemPath).exists()){
                                        //System.out.println("the android file exists");
                                    }else{
                                        System.out.println("the android file doesn't exist");
                                    }

                                    if (memeType.toLowerCase().endsWith("mkv")) {
                                        mediaContent = new FileContent("video/x-matroska",
                                                new File(mediaItemPath));
                                    } else {
                                        mediaContent = new FileContent("video/" + memeType.toLowerCase(),
                                                new File(mediaItemPath));
                                    }
                                }

                                if (test[0] >0 && !isVideo(memeType)){
                                com.google.api.services.drive.model.File uploadFile =
                                        service.files().create(fileMetadata, mediaContent).setFields("id").execute();
                                String uploadFileId = uploadFile.getId();
                                while(uploadFileId == null){
                                    wait();
                                }
                                uploadFileIds.add(uploadFileId);
                                LogHandler.SaveLog("Uploading " + uploadFile.getName() + " from photos into backup account uploadId" + uploadFileId);
                                  test[0]--;
                                }
                            } catch (Exception e) {
                                System.out.println("Uploading android error: " + e.getMessage());
                                //Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            } catch (Exception e){
                System.out.println("Uploading android error: " + e.getMessage());
               // Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            return uploadFileIds;
        };
        Future<ArrayList<String>> future = executor.submit(uploadTask);
        ArrayList<String> uploadFileIdsFuture = new ArrayList<>();
        try{
            uploadFileIdsFuture = future.get();
            System.out.println("Finished with " + uploadFileIdsFuture.size() + " uploads at " + currentTime[0].toString());
            System.out.println("-----end of second ----->");
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return  uploadFileIdsFuture;
    }

}
