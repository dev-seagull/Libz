package com.example.cso;

import android.app.Activity;
import android.os.Environment;
import android.widget.Toast;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import org.json.JSONArray;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.time.LocalTime;

public class GooglePhotos {
    private final Activity activity;
    public static boolean is_first_task_finish = false;
    public GooglePhotos(Activity activity){
        this.activity = activity;
    }


    public class MediaItem{
        private String Id;
        private String baseUrl;
        private String creationTime;
        public  String fileName;

        public MediaItem(String Id, String baseUrl, String creationTime, String fileName) {
            this.Id = Id;
            this.baseUrl = baseUrl;
            this.creationTime = creationTime;
            this.fileName = fileName;
        }

        public String getId() {return Id;}

        public String getBaseUrl() {return baseUrl;}

        public String getCreationTime() {return creationTime;}

        public String getFileName() {return fileName;}
    }

    public ArrayList<MediaItem> getGooglePhotosMediaItems(PrimaryAccountInfo.Tokens tokens){
        int pageSize = 100;
        final String[] nextPageToken = {null};
        String accessToken = tokens.getAccessToken();
        String refreshToken = tokens.getRefreshToken();
        ArrayList<MediaItem> MediaItems = new ArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        ArrayList<MediaItem> finalMediaItems = MediaItems;
        Callable<ArrayList<MediaItem>> backgroundTask = () -> {
            try {
                URL url = new URL("https://photoslibrary.googleapis.com/v1/mediaItems");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Content-type", "application/json");
                System.out.println("Access token is: " + accessToken);
                httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = httpURLConnection.getResponseCode();
                System.out.println(responseCode);
                if(responseCode == HttpURLConnection.HTTP_OK){
                    InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    StringBuilder responseStringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null){
                        responseStringBuilder.append(line);
                    }
                    bufferedReader.close();
                    String response  = responseStringBuilder.toString();
                    JSONObject responseJson = new JSONObject(response);
                    JSONArray MediaItemsResponse = responseJson.getJSONArray("mediaItems");
                    for (int i = 0; i < MediaItemsResponse.length(); i++) {
                        JSONObject MediaItemJsonObject = MediaItemsResponse.getJSONObject(i);
                        String filename = MediaItemJsonObject.getString("filename");
                        String baseUrl = MediaItemJsonObject.getString("baseUrl");
                        String id = MediaItemJsonObject.getString("id");
                        MediaItem MediaItem = new MediaItem(id, baseUrl, "2-2-2", filename);
                        finalMediaItems.add(MediaItem);
                        //MediaItem.getString()
                    }
                    nextPageToken[0] = responseJson.optString("nextPageToken", null);
                    while (responseJson.has("nextPageToken") && nextPageToken != null
                        && responseJson.has("mediaItems")) {
                        nextPageToken[0] = responseJson.optString("nextPageToken", null);
                        System.out.println("next page token:" + nextPageToken[0]);
                        String nextPageUrlString = "https://photoslibrary.googleapis.com/v1/mediaItems?pageToken=" +
                                nextPageToken[0];
                        URL nextPageUrl = new URL(nextPageUrlString);
                        HttpURLConnection nextHttpURLConnection = (HttpURLConnection) nextPageUrl.openConnection();
                        nextHttpURLConnection.setRequestMethod("GET");
                        nextHttpURLConnection.setRequestProperty("Content-type", "application/json");
                        nextHttpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
                        responseCode = nextHttpURLConnection.getResponseCode();
                        System.out.println("r2: " + responseCode);
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            InputStreamReader nextInputStreamReader = new InputStreamReader(nextHttpURLConnection.getInputStream());
                            BufferedReader nextBufferedReader = new BufferedReader(nextInputStreamReader);
                            responseStringBuilder = new StringBuilder();
                            while ((line = nextBufferedReader.readLine()) != null) {
                                responseStringBuilder.append(line);
                            }
                            nextBufferedReader.close();
                            response = responseStringBuilder.toString();
                            responseJson = new JSONObject(response);
                            if (responseJson.has("mediaItems")) {
                                MediaItemsResponse = responseJson.getJSONArray("mediaItems");
                            } else {
                                MediaItemsResponse = new JSONArray();
                            }
                            for (int i = 0; i < MediaItemsResponse.length(); i++) {
                                JSONObject MediaItemJsonObject = MediaItemsResponse.getJSONObject(i);
                                String filename = MediaItemJsonObject.getString("filename");
                                String baseUrl = MediaItemJsonObject.getString("baseUrl");
                                String id = MediaItemJsonObject.getString("id");
                                String creationTime = "";
                                if (MediaItemJsonObject.has("mediaMetadata")) {
                                    JSONObject mediaMetadata = MediaItemJsonObject.getJSONObject("mediaMetadata");
                                    if (mediaMetadata.has("creationTime")) {
                                        creationTime = mediaMetadata.getString("creationTime");
                                    }
                                }
                                MediaItem MediaItem = new MediaItem(id, baseUrl, creationTime, filename);
                                finalMediaItems.add(MediaItem);
                            }

                        }
                        nextHttpURLConnection.disconnect();
                    }
                    httpURLConnection.disconnect();
                }
            }catch (Exception e) {
                System.out.println("catch error" + e.getMessage());
                Toast.makeText(activity,"Login failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }finally {
                executor.shutdown();
            }
            System.out.println("Number of your media items in photos equals to: " + finalMediaItems.size());
            //Future<ArrayList<MediaItem>> future = executor.submit(backgroundTask);
            //storage[0] = future.get();
            return finalMediaItems;
        };
        Future<ArrayList<MediaItem>> future = executor.submit(backgroundTask);
        try {
            MediaItems = future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return MediaItems;
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
            Toast.makeText(activity,"Uploading failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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
            ,File file, Activity activity){
        boolean isDuplicatedInBackup = false;
        String fileHash;

        try {
            fileHash = MainActivity.calculateHash(file,activity).toLowerCase();
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        System.out.println("file hash is :" + fileHash);
        for(BackUpAccountInfo.MediaItem backUpMediaItem: backUpMediaItems){
            String backUpHash = backUpMediaItem.getHash();
            System.out.println("backup hash :" + backUpHash);
            if(fileHash.equals(backUpHash)){
                System.out.println(file.getName() + " is duplicated");
                isDuplicatedInBackup = true;
                break;
            }
        }

        return isDuplicatedInBackup;
    }

    public void uploadPhotosToGoogleDrive(ArrayList<MediaItem> MediaItems, String accessToken,
                                          ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems, Activity activity) {
        System.out.println("-------first----->");
        final int[] test = {5};
        final File[] file = new File[1];

        ArrayList<String> baseUrls = new ArrayList<>();
        ArrayList<String> fileNames = new ArrayList<>();
        for (MediaItem MediaItem : MediaItems) {
            baseUrls.add(MediaItem.getBaseUrl());
            fileNames.add(MediaItem.getFileName());
        }

        ArrayList<String> uploadFileIDs  = new ArrayList();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<ArrayList<String>>  uploadTask = () -> {
            try{
                String destinationFolderPath = Environment.getExternalStoragePublicDirectory
                        (Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso";
                File destinationFolder = new File(destinationFolderPath);


                int i =0;
                for(String baseUrl: baseUrls) {
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
                                    Toast.makeText(activity, "Uploading failed", Toast.LENGTH_LONG).show();
                                }
                            }

                            String fileName = fileNames.get(i);
                            i++;
                            String filePath = destinationFolder + File.separator + fileName;
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
                                if (!isVideo(getMemeType(file[0]))){
                                    String this_hash = MainActivity.calculateHash(file[0],activity);
                                    }
                            } catch (IOException e) {
                                Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            } finally {
                                try {
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                } catch (IOException e) {
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
                }

                if(isDuplicatedInBackup(backUpMediaItems,file[0],activity) == false){
                    File[] destinationFolderFiles = destinationFolder.listFiles();

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

                    try{
                        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                                .setApplicationName("cso")
                                .build();
                        if(destinationFolderFiles != null && destinationFolderFiles.length> 0){
                            for(File destinationFolderFile : destinationFolderFiles){
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

//                            if(mediaContent == null){
//                                System.out.println("media content of photos null ");
//                            }else {
//                                System.out.println("media content of photos not null ");
//                            }

                                if(test[0] > 0){
                                    com.google.api.services.drive.model.File uploadFile =
                                            service.files().create(fileMetadata, mediaContent).setFields("id").execute();
                                    String uploadFileId = uploadFile.getId();
                                    while(uploadFileId.isEmpty() | uploadFileId == null){
                                        wait();
                                    }
                                    uploadFileIDs.add(uploadFileId);
                                    System.out.println("upload finished with this uploadfile id :" + uploadFileId.toString());
                                }
                                test[0]--;
                            }
                        }else{
                            //Toast.makeText(activity,"Uploading failed", Toast.LENGTH_LONG).show();
                        }
                    }catch (Exception e){
                        System.out.println("this is the error: " + e.getLocalizedMessage());
                    }

                    for (File destinationFolderFile: destinationFolderFiles) {
                        destinationFolderFile.delete();
                    }
                    if(destinationFolder.exists()){
                        destinationFolder.delete();
                    }
                }


            }catch (Exception e){
                Toast.makeText(activity,"Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                return null;
            }
            return uploadFileIDs;
        };
        Future<ArrayList<String>> future = executor.submit(uploadTask);

        try {
            // Wait for the task to complete and get the result
            ArrayList<String> uploadFileIDs_fromFuture = future.get();
            uploadFileIDs_fromFuture.size();
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
        is_first_task_finish = true;
    }


    public void uploadAndroidToGoogleDrive(ArrayList<Android.MediaItem> mediaItems, String accessToken) {
        while(is_first_task_finish == false){

        }
        System.out.println("-------second----->");
        final int[] test = {3};
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runnable uploadTask = () -> {
            try {
                int i = 0;
                for (Android.MediaItem mediaItem : mediaItems) {
                    System.out.println("mediaItem android upload: " + mediaItem.getFileName());
                    try {
                        NetHttpTransport HTTP_TRANSPORT = null;
                        try {
                            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                        } catch (GeneralSecurityException e) {
                            Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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
                        if(isImage(memeType)) {
                            String mediaItemPath = mediaItem.getFilePath();
//                            if(new File(mediaItemPath).exists()){
//                                System.out.println("the android file exists");
//                            }else{
//                                System.out.println("the android file doesn't exist");
//                            }

                            if(memeType.toLowerCase().endsWith("jpg")){
                                mediaContent = new FileContent("image/jpeg" ,
                                        new File(mediaItemPath));
                            }else{
                                mediaContent = new FileContent("image/" + memeType.toLowerCase() ,
                                        new File(mediaItemPath));
                            }}else if(isVideo(memeType)){
                            String mediaItemPath = mediaItem.getFilePath();

                            if(memeType.toLowerCase().endsWith("mkv")){
                                mediaContent = new FileContent("video/x-matroska" ,
                                        new File(mediaItemPath));
                            }else{
                                mediaContent = new FileContent("video/" + memeType.toLowerCase() ,
                                        new File(mediaItemPath));
                            }
                        }
                        if (isVideo(getMemeType(new File(mediaItem.getFilePath())))){
                            String this_hash = MainActivity.calculateHash(new File(mediaItem.getFilePath()),activity);
                        }

//
//                        if (test[0] >0 && !isVideo(memeType)){
                            com.google.api.services.drive.model.File uploadFile =
                                    service.files().create(fileMetadata, mediaContent).setFields("id").execute();
                            String uploadFileId = uploadFile.getId();

//                            test[0]--;
//                        }
                    } catch (Exception e) {
                        System.out.println("Uploading android error: " + e.getMessage());
                        Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e){
                System.out.println("Uploading android error: " + e.getMessage());
                Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        };
        executor.submit(uploadTask);
    }

}
