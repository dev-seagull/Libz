package com.example.cso;


import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BackUp {
    public boolean backupAndroidToDrive(Long fileId, String fileName, String filePath,
                                        String fileHash, String mimeType, String assetId,
                                        String driveBackupAccessToken, String driveEmailAccount, String syncAssetsFolderId){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final boolean[] isUploadValid = {false};
        Callable<Boolean> uploadTask = () -> {
            try {
                isUploadValid[0] = false;
                File androidFile = new File(filePath);
                Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                com.google.api.services.drive.model.File fileMetadata =
                        new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);
                String mimeTypeToUpload = Media.getMimeType(new File(filePath));
                FileContent mediaContent = handleMediaFileContent(mimeTypeToUpload,androidFile,
                        mimeType, filePath, fileName);
                fileMetadata.setParents(Collections.singletonList(syncAssetsFolderId));
                if (mediaContent == null) {
                    LogHandler.saveLog("media content is null in syncAndroidToDrive", true);
                }

//                            if (!isVideo(memeType)) {
                LogHandler.saveLog("Starting to upload file : " + fileName, false );
                System.out.println("Starting to upload file : " + fileName);
                String uploadType = "multipart" ; //task
                HttpResponse uploadFile =
                        service.files().create(fileMetadata, mediaContent)
                                .setFields("files(id)")
                                .getMediaHttpUploader()
                                .setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE)
                                .setDirectUploadEnabled(false)
                                .upload(new GenericUrl("https://www.googleapis.com" +
                                        "/upload/drive/v3/files?uploadType="+uploadType));
                JSONObject responseJson = new JSONObject(uploadFile.parseAsString());
                int uploadStatus = uploadFile.getStatusCode();
                String uploadFileId = null;
                uploadFileId = uploadFile.getStatusMessage();
                if (uploadStatus != HttpURLConnection.HTTP_OK) {
                    LogHandler.saveLog("Failed to upload " + fileName + " from Android to backup "+
                            driveEmailAccount +" because of response : " + uploadStatus, true);
                }else{
                    if (responseJson.has("id")) {
                        uploadFileId = responseJson.getString("id");
                    }else{
                        LogHandler.saveLog("Failed to find id after back up of android file: ", true);
                    }
                }
                while (uploadFileId == null) {
                    wait();
                }
                if (uploadFileId == null | uploadFileId.isEmpty()) {
                    LogHandler.saveLog("Failed to upload " + fileName + " from Android to drive " +
                            driveEmailAccount + " with status of " + uploadStatus, true);
                } else {
                    if(isUploadHashEqual(fileHash,uploadFileId,driveBackupAccessToken)){
                        isUploadValid[0] = true;
//                        DBHelper.insertIntoDriveTable(Long.valueOf(assetId),String.valueOf(fileId)
//                                ,fileName,fileHash,driveEmailAccount);
                        MainActivity.dbHelper.insertTransactionsData(String.valueOf(fileId), fileName,
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

    private static FileContent handleMediaFileContent(String mimeTypeToUpload, File androidFile, String mimeType,String filePath, String fileName){
        FileContent mediaContent = null;
        try{
            if (Media.isImage(mimeTypeToUpload)) {
                if (androidFile.exists()) {
                    if (mimeType.toLowerCase().endsWith("jpg")) {
                        mediaContent = new FileContent("image/jpeg",
                                new File(filePath));
                    } else {
                        mediaContent = new FileContent("image/" + mimeType.toLowerCase(),
                                new File(filePath));
                    }
                } else {
                    LogHandler.saveLog("The android file " + fileName + " doesn't exists in upload method");
                }
            } else if (Media.isVideo(mimeTypeToUpload)) {
                if (new File(filePath).exists()) {
                    if (mimeType.toLowerCase().endsWith("mkv")) {
                        mediaContent = new FileContent("video/x-matroska",
                                new File(filePath));
                    } else {
                        mediaContent = new FileContent("video/" + mimeType.toLowerCase(),
                                new File(filePath));
                    }
                } else {
                    LogHandler.saveLog("The android file " + fileName + " doesn't exists in upload method");
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to handle media file content: " + e.getLocalizedMessage(), true);
        }
        return mediaContent;
    }

    public static List<String[]> sortAndroidItems(List<String[]> android_items){
        Collections.sort(android_items, new Comparator<String[]>() {
            @Override
            public int compare(String[] item1, String[] item2) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy");
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

//    public static void restore(Context context) {
//        String sqlQuery = "SELECT T.id, T.source, T.fileName, T.destination, D.assetId, " +
//                "T.operation, T.hash, T.date, D.fileId , D.userEmail " +
//                "FROM TRANSACTIONS T " +
//                "JOIN DRIVE D ON T.hash = D.fileHash " +
//                "WHERE T.operation = 'deletedInDevice' and T.destination = ?" +
//                "AND D.id = (" +
//                "   SELECT MIN(id) " +
//                "   FROM DRIVE " +
//                "   WHERE fileHash = T.hash " +
//                ");";
//
//        Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQuery, new String[]{MainActivity.androidDeviceName});
//        List<String[]> resultList = new ArrayList<>();
//        if(cursor.moveToFirst() && cursor != null){
//            do {
//                String[] columns = {"id", "source", "fileName", "destination", "assetId"
//                        , "operation", "hash", "date", "fileId", "userEmail"};
//                String[] row = new String[columns.length];
//                for(int i =0 ; i < columns.length ; i++){
//                    int columnIndex = cursor.getColumnIndex(columns[i]);
//                    if (columnIndex >= 0) {
//                        row[i] = cursor.getString(columnIndex);
//                    }
//                }
//                resultList.add(row);
//            } while (cursor.moveToNext());
//        }
//        if(cursor != null){
//            cursor.close();
//        }
//
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        final boolean[] isFinished = {false};
//        Callable<Boolean> backgroundDownloadTask = () -> {
//            for (String[] row : resultList) {
//                String filePath = row[1];
//                String userEmail = row[9];
//                String fileId = row[8];
//                System.out.println("File path for restore test: " + filePath + " and user email is: " + userEmail  +  "  and file id" +
//                        " is: " + fileId);
//                String accessTokenSqlQuery = "SELECT accessToken from ACCOUNTS WHERE ACCOUNTS.userEmail = ?";
//                Cursor accessTokenCursor = MainActivity.dbHelper.dbReadable.rawQuery(accessTokenSqlQuery, new String[]{userEmail});
//                String accessToken = "";
//                if(accessTokenCursor.moveToFirst() && accessTokenCursor != null) {
//                    int accessTokenColumnIndex = accessTokenCursor.getColumnIndex("accessToken");
//                    if (accessTokenColumnIndex >= 0) {
//                        accessToken = accessTokenCursor.getString(accessTokenColumnIndex);
//                    }
//                }
//                if(accessTokenCursor != null){
//                    accessTokenCursor.close();
//                }
//                if(!accessToken.isEmpty() && accessToken != null){
//                    Drive service = GoogleDrive.initializeDrive(accessToken);
//
//                    OutputStream outputStream = null;
//                    File file = new File(filePath);
//
//                    try {
//                        if(!file.exists()){
//                            file.getParentFile().mkdirs();
//                            file.createNewFile();
//                            try {
//                                outputStream = new FileOutputStream(filePath);
//                            } catch (FileNotFoundException e) {
//                                LogHandler.saveLog("failed to save output stream in restore method : " + e.getLocalizedMessage(), true);
//                            }
//                            try {
//                                service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
//                            } catch (IOException e) {
//                                LogHandler.saveLog("failed to download in restore method : " + e.getLocalizedMessage(), true);
//                            }
//                        }
//                    } catch (Exception e) {
//                        LogHandler.saveLog("failed to create file in restore method : " + e.getLocalizedMessage(), true);
//                    }
//
//                    MediaScannerConnection.scanFile(
//                            context,
//                            new String[]{filePath},
//                            new String[]{"image/jpeg", "image/"+Media.getMimeType(file).toLowerCase(), "image/jpg", "video/"+Media.getMimeType(file)},
//                            null
//                    );
//                }
//            }
//
//            return isFinished[0];
//        };
//        Future<Boolean> future = executor.submit(backgroundDownloadTask);
//        try{
//            isFinished[0] = future.get();
//        }catch (Exception e){
//            LogHandler.saveLog("Failed to get boolean finished in downloading from Photos: " + e.getLocalizedMessage());
//        }
//    }
//    }
}




