package com.example.cso;


import android.os.FileUtils;
import android.util.Log;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackUp {
    public static boolean backupAndroidToDrive(Long fileId, String fileName, String filePath,
                                        String fileHash, String mimeType, String assetId,
                                        String driveBackupAccessToken, String driveEmailAccount, String syncAssetsFolderId){
        boolean[] isUploadValid = {false};
        Thread backupAndroidToDriveThread = new Thread( () -> {
            try {
                Log.d("Threads","backupAndroidToDriveThread started");
                File androidFile = new File(filePath);
                Double androidFileSize = androidFile.length() / (Math.pow(10, 6));
                Log.d("service","file size for android file is: " + androidFileSize);

                Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                com.google.api.services.drive.model.File fileMetadata =
                        new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList(syncAssetsFolderId));

                String mimeTypeToUpload = Media.getMimeType(fileName);
                FileContent mediaContent = handleMediaFileContent(mimeTypeToUpload,androidFile, fileName);
                if (mediaContent == null) {
                    LogHandler.crashLog(new Exception("media content is null in syncAndroidToDrive"), "backup");
                }

//                if(!Media.isVideoBackUp(mimeTypeToUpload)) {
                    Log.d("service", "Uploading file " + fileName + " to " +driveEmailAccount +" started");
                    com.google.api.services.drive.model.File uploadedFile;
                    if(androidFileSize > 10.0){
                        Drive.Files.Create createRequest = service.files().create(fileMetadata, mediaContent)
                                .setFields("id, name");
                        MediaHttpUploader uploader = createRequest.getMediaHttpUploader();
                        uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE);

                        uploadedFile = createRequest.execute();
                        Log.d("service", "Uploaded File ID: " + uploadedFile.getId());
                    }else{
                        uploadedFile = service.files().create(fileMetadata, mediaContent)
                                .setFields("id, name")
                                .execute();
                    }

                    String uploadFileId = uploadedFile.getId();
                    if (uploadedFile == null | uploadFileId.isEmpty()) {
                        LogHandler.crashLog(new Exception("Failed to upload: " + fileName), "backup");
                    } else {
                        if (isUploadHashEqual(fileHash, uploadFileId, driveBackupAccessToken)) {
                            Log.d("service", "Uploading file " + fileName + " to " +driveEmailAccount +" finished : " + uploadFileId);
                            isUploadValid[0] = true;
                            try{
                                DBHelper.insertTransactionsData(String.valueOf(fileId), fileName,
                                        driveEmailAccount, assetId, "sync", fileHash);
                            }catch (Exception e) { }
                        }else{
                            LogHandler.crashLog(new Exception("Failed to detect same file hash: " + fileName + " " + fileHash), "backup");
                        }
                    }
//                }

            }catch (Exception e) { LogHandler.crashLog(e,"backup"); }
        });

        backupAndroidToDriveThread.start();
        try{
            backupAndroidToDriveThread.join();
        }catch (Exception e) { LogHandler.crashLog(e,"backup"); }

        Log.d("Threads","backupAndroidToDriveThread finished");
        return isUploadValid[0];
    }
    private static FileContent handleMediaFileContent(String mimeTypeToUpload, java.io.File androidFile, String fileName) {
        FileContent mediaContent = null;
        try {
            if (androidFile.exists()) {
                String lowerMimeType = mimeTypeToUpload.toLowerCase();
                Log.d("AreaSquareChart","media is image: "  + Media.isImage(lowerMimeType) + " " + fileName );
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
        Log.d("service", "sortAndroidItems started");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        Collections.sort(android_items, (item1, item2) -> {
            try {
                Date date1 = dateFormat.parse(item1[6]);
                Date date2 = dateFormat.parse(item2[6]);
                return date1.compareTo(date2);
            } catch (Exception e) {
                LogHandler.saveLog("Failed to sort android media items: " + e.getLocalizedMessage(),true);
                return 0;
            }
        });
        Log.d("service", "sortAndroidItems finished");
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




