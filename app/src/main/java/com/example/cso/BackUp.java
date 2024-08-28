package com.example.cso;


import android.app.Activity;
import android.view.View;

import com.bumptech.glide.Glide;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BackUp {
    public boolean backupAndroidToDrive(Long fileId, String fileName, String filePath,
                                        String fileHash, String mimeType, String assetId,
                                        String driveBackupAccessToken, String driveEmailAccount, String syncAssetsFolderId){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        boolean[] isUploadValid = {false};
        Callable<Boolean> uploadTask = () -> {
            try {
                isUploadValid[0] = false;
                File androidFile = new File(filePath);
                Double androidFileSize = androidFile.length() / (Math.pow(10, 6));
                System.out.println("file size for android file is: " + androidFileSize);

                Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                com.google.api.services.drive.model.File fileMetadata =
                        new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);
                String mimeTypeToUpload = Media.getMimeType(new File(filePath));
                FileContent mediaContent = handleMediaFileContent(mimeTypeToUpload,androidFile, fileName);
                fileMetadata.setParents(Collections.singletonList(syncAssetsFolderId));
                if (mediaContent == null) {
                    LogHandler.saveLog("media content is null in syncAndroidToDrive", true);
                }

//                            if (!isVideo(memeType)) {
                LogHandler.saveLog("Starting to upload file : " + fileName, false );
                com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();
                String uploadFileId = uploadedFile.getId();
                while (uploadedFile == null){
                    wait();
                }
                if (uploadedFile == null | uploadFileId.isEmpty()) {
                    LogHandler.saveLog("Failed to upload " + fileName + " from Android to backup "+
                            driveEmailAccount, true);
                }else {
                    if(isUploadHashEqual(fileHash,uploadFileId,driveBackupAccessToken)){
                        isUploadValid[0] = true;
//                        DBHelper.insertIntoDriveTable(Long.valueOf(assetId),String.valueOf(fileId)
//                                ,fileName,fileHash,driveEmailAccount);
                        DBHelper.insertTransactionsData(String.valueOf(fileId), fileName,
                                driveEmailAccount, assetId, "sync", fileHash);
                        LogHandler.saveLog("Uploading " + fileName +
                                " is finished with upload file id of : " + uploadFileId, false);
                        System.out.println("Uploading " + fileName +
                                " is finished with upload file id of : " + uploadFileId);
                    }
                }
//                            }
//                            }
//                                  test[0]--;

            }catch (Exception e){
                LogHandler.saveLog("Failed to back up android file to drive: "  + e.getLocalizedMessage(), true);
            }
            return isUploadValid[0];
        };
        Boolean isUploadValidFuture = false;
        Future<Boolean> future = executor.submit(uploadTask);
        try{
            isUploadValidFuture = future.get();
        }catch (Exception e){
            LogHandler.saveLog("Failed to back up android file to drive (future) : "  + e.getLocalizedMessage(), true);
        }
        LogHandler.saveLog("Upload validation : " + isUploadValidFuture, false);
        return isUploadValidFuture;
    }

    private static class FileUploadProgressListener implements MediaHttpUploaderProgressListener {
        @Override
        public void progressChanged(MediaHttpUploader uploader) {
            try{
                switch (uploader.getUploadState()) {
                    case INITIATION_STARTED:
                        LogHandler.saveLog("Initiation Started", false);
                        break;
                    case INITIATION_COMPLETE:
                        LogHandler.saveLog("Initiation Completed", false);
                        break;
                    case MEDIA_IN_PROGRESS:
                        LogHandler.saveLog("Upload in progress", false);
                        break;
                    case MEDIA_COMPLETE:
                        LogHandler.saveLog("Upload Completed", false);
                        break;
                    default:
                        break;
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed in http progress listener: " + e.getLocalizedMessage(), true);
            }
        }
    }

    private static FileContent handleMediaFileContent(String mimeTypeToUpload, java.io.File androidFile, String fileName) {
        FileContent mediaContent = null;
        try {
            if (androidFile.exists()) {
                String lowerMimeType = mimeTypeToUpload.toLowerCase();
                if (Media.isImage(lowerMimeType)) {
                    if (lowerMimeType.equals("jpg") || lowerMimeType.equals("jpeg")) {
                        mediaContent = new FileContent("image/jpeg", androidFile);
                    } else if (lowerMimeType.equals("png")) {
                        mediaContent = new FileContent("image/png", androidFile);
                    } else if (lowerMimeType.equals("gif")) {
                        mediaContent = new FileContent("image/gif", androidFile);
                    } else if (lowerMimeType.equals("bmp")) {
                        mediaContent = new FileContent("image/bmp", androidFile);
                    } else if (lowerMimeType.equals("webp")) {
                        mediaContent = new FileContent("image/webp", androidFile);
                    } else {
                        mediaContent = new FileContent("image/" + lowerMimeType, androidFile);
                    }

                }  else if (Media.isVideo(lowerMimeType)) {
                    if (lowerMimeType.equals("mp4")) {
                        mediaContent = new FileContent("video/mp4", androidFile);
                    } else if (lowerMimeType.equals("mkv")) {
                        mediaContent = new FileContent("video/x-matroska", androidFile);
                    } else if (lowerMimeType.equals("mov")) {
                        mediaContent = new FileContent("video/quicktime", androidFile);
                    } else if (lowerMimeType.equals("avi")) {
                        mediaContent = new FileContent("video/x-msvideo", androidFile);
                    } else {
                        mediaContent = new FileContent("video/" + lowerMimeType, androidFile);
                    }
                } else {
                    LogHandler.saveLog("Unsupported MIME type for file: " + mimeTypeToUpload, true);
                }
            } else {
                LogHandler.saveLog("The android file " + fileName + " doesn't exist in upload method", true);
            }
        } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return mediaContent;
    }

    public static List<String[]> sortAndroidItems(List<String[]> android_items){
        Collections.sort(android_items, new Comparator<String[]>() {
            @Override
            public int compare(String[] item1, String[] item2) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                try {
                    Date date1 = dateFormat.parse(item1[6]);
                    Date date2 = dateFormat.parse(item2[6]);
                    return date1.compareTo(date2);
                } catch (Exception e) {
                    LogHandler.saveLog("Failed to sort android media items: " + e.getLocalizedMessage(),true);
                    return 0;
                }
            }
        });
        return android_items;
    }

    public static boolean isUploadHashEqual(String fileHash, String driveFileId, String accessToken){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            return executor.submit(() -> {
                URL url = new URL("https://www.googleapis.com/drive/v3/files/" + driveFileId +
                        "?fields=sha256Checksum,+originalFilename");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                int responseCode = connection.getResponseCode();
                System.out.println("response code for hash equal: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    JSONObject responseJson = new JSONObject(response.toString());
                    String sha256Checksum = responseJson.getString("sha256Checksum").toLowerCase();
                    System.out.println("drive sha256Checksum: " + sha256Checksum + "\nfileHash: " + fileHash);
                    return sha256Checksum.equals(fileHash);
                } else {
                    return false;
                }
            }).get();
        } catch (Exception e) {
            LogHandler.saveLog("Error in get file from google drive api: " + e.getLocalizedMessage());
            return false;
        } finally {
            executor.shutdown();
        }
    }

}




