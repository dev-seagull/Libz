package com.example.cso;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

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
import java.util.ArrayList;
import java.util.Arrays;

public class GooglePhotos {
    private final Activity activity;
    public GooglePhotos(FragmentActivity activity){
        this.activity = activity;
    }


    public void getGooglePhotosMediaItems(PrimaryAccountInfo.Tokens tokens){
        int pageSize = 100;
        String nextPageToken = null;
        JSONArray mediaItems = null;
        String accessToken = tokens.getAccessToken();
        String refreshToken = tokens.getRefreshToken();


        Runnable backgroundTask = () -> {
            try {
                URL url = new URL("https://photoslibrary.googleapis.com/v1/mediaItems");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Content-type", "application/json");
                System.out.println("Access token is: " + accessToken);
                httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = httpURLConnection.getResponseCode();
                System.out.println("response code is " + responseCode);
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
                    System.out.println(response);
                    System.out.println("response is: " + response);
                    JSONObject responseJson = new JSONObject(response);
                }
            }catch (Exception e) {
                Toast.makeText(activity,"Login failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        };

        backgroundTask.run();
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

    public String getMemeType(File file){
        int dotIndex = file.getName().lastIndexOf(".");
        String memeType="";
        if (dotIndex >= 0 && dotIndex < file.getName().length() - 1) {
            memeType = file.getName().substring(dotIndex + 1);
        }
        return memeType;
    }

    public boolean isImage(String memeType){
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

    public boolean isVideo(String memeType){
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

    public void uploadToGoogleDrive(ArrayList<String> baseUrls, ArrayList<String> filePaths,
                                    String accessToken, TextView textView){
        try{
            String destinationFolderPath = Environment.getExternalStoragePublicDirectory
                    (Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso";
            File destinationFolder = new File(destinationFolderPath);


            for(String baseUrl: baseUrls) {
                try {
                    URL url = new URL(baseUrl + "=d");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                        if (!destinationFolder.exists()) {
                            boolean isFolderCreated = destinationFolder.mkdirs();
                            if (!isFolderCreated) {
                                Toast.makeText(activity, "Uploading failed", Toast.LENGTH_LONG).show();
                            }
                        }

                        String fileName = filePaths.get(0);
                        String filePath = destinationFolder + File.separator + fileName;
                        OutputStream outputStream = null;
                        try {
                            File file = new File(filePath);
                            file.createNewFile();
                            outputStream = new FileOutputStream(file);

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        } catch (IOException e) {
                            Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        } finally {
                            try {
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                            } catch (IOException e) {
                                Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                        inputStream.close();
                        connection.disconnect();
                    }
                } catch (MalformedURLException e) {
                    Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                } catch (ProtocolException e) {
                    Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }

            File[] destinationFolderFiles = destinationFolder.listFiles();

            NetHttpTransport HTTP_TRANSPORT = null;
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            } catch (GeneralSecurityException e) {
                Toast.makeText(activity,"Uploading failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(activity,"Uploading failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

            HttpRequestInitializer requestInitializer = request -> {
                request.getHeaders().setAuthorization("Bearer " + accessToken);
                request.getHeaders().setContentType("application/json");
            };

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
                        mediaContent = new FileContent("image/" + memeType.toLowerCase() ,
                                new File(destinationFolderFilePath));
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

                    com.google.api.services.drive.model.File uploadFile =
                                service.files().create(fileMetadata, mediaContent).setFields("id").execute();
                    }
                }else{
                Toast.makeText(activity,"Uploading failed", Toast.LENGTH_LONG).show();
                }

                for (File destinationFolderFile: destinationFolderFiles) {
                    destinationFolderFile.delete();
                }
                if(destinationFolder.exists()){
                    destinationFolder.delete();
                }
        }catch (Exception e){
            Toast.makeText(activity,"Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
