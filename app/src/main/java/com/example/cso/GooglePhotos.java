package com.example.cso;

import android.database.Cursor;
import android.os.Environment;

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
import java.util.ArrayList;
import java.util.Arrays;
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

    public static ArrayList<MediaItem> getGooglePhotosMediaItems(String accessToken){
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
                    System.out.println("photos response code : " + responseCode);
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
                        System.out.println("photos response result : " + response);
                        responseJson = new JSONObject(response);
                        JSONArray mediaItemsResponse = responseJson.getJSONArray("mediaItems");
                        System.out.println("photos response json : " + mediaItemsResponse.toString());
                        for (int i = 0; i < mediaItemsResponse.length(); i++) {
                            JSONObject mediaItemJsonObject = mediaItemsResponse.getJSONObject(i);
                            String filename = mediaItemJsonObject.getString("filename");
                            String baseUrl = mediaItemJsonObject.getString("baseUrl");
                            String id = mediaItemJsonObject.getString("id");
                            JSONObject mediaMetaDataObject = mediaItemJsonObject.getJSONObject("mediaMetadata");
                            String creationTime = mediaMetaDataObject.getString("creationTime");
                            MediaItem mediaItem = new MediaItem(id,baseUrl,creationTime,filename,"");
                            mediaItems.add(mediaItem);

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

//    public static void deletePhotosFromAndroid(){
//        String destinationFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
//                + File.separator + "cso";
//        File destinationFolder = new File(destinationFolderPath);
//        File[] destinationFolderFiles = destinationFolder.listFiles();
//        if (destinationFolderFiles != null) {
//            for (File destinationFolderFile: destinationFolderFiles) {
//                if (!destinationFolderFile.getName().equals(LogHandler.logFileName)){
//                    boolean isDeleted = destinationFolderFile.delete();
//                    if (isDeleted){
//                        LogHandler.saveLog(destinationFolderFile.getName() + " deleted from CSO folder",false);
//                    }else {
//                        LogHandler.saveLog(destinationFolderFile.getName() + " was not deleted from CSO folder");
//                    }
//                }
//            }
//        }else{
//            LogHandler.saveLog("Destination folder is null when trying to delete its content");
//        }
//    }

    public ArrayList<String> uploadPhotosToDrive(String destinationUserEmail,String accessToken){
        System.out.println("here in photos to drive");
        String destinationFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + "stash";
        String sqlQury = "SELECT * FROM PHOTOS";
        Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQury, null);
        ArrayList<String[]> destinationFiles = new ArrayList<>();
        System.out.println("here2 in photos to drive");
        if(cursor.moveToFirst() && cursor != null){
            do {
                int fileNameColumnIndex = cursor.getColumnIndex("fileName");
                int userEmailColumnIndex = cursor.getColumnIndex("userEmail");
                int fileHashColumnIndex = cursor.getColumnIndex("fileHash");
                int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                if(fileNameColumnIndex >= 0 && userEmailColumnIndex >= 0 && fileHashColumnIndex >= 0
                        && assetIdColumnIndex >= 0){
                    String fileName = cursor.getString(fileNameColumnIndex);
                    String userEmail = cursor.getString(userEmailColumnIndex);
                    String fileHash = cursor.getString(fileHashColumnIndex);
                    String assetId = cursor.getString(assetIdColumnIndex);
                    String filePath = destinationFolderPath + File.separator + fileName;
                    destinationFiles.add(new String[]{filePath,userEmail,fileName,assetId,fileHash});
                    System.out.println("File name for upload photos from android to drive: " + fileName);
                }
            }while (cursor.moveToNext());
        }
        if(cursor != null){
            cursor.close();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        ArrayList<String> finalUploadFileIds = new ArrayList<>();
        FileContent[] mediaContent = {null};
        Callable<ArrayList<String>> backgroundTaskUpload = () -> {
            System.out.println("here3 in photos to drive");
            if(destinationFiles != null) {
                System.out.println("here4 in photos to drive "+ destinationFiles.size());
                for (String[] destinationFile : destinationFiles) {
                    File destinationFolderFile = new File(destinationFile[0]);
                    if (!destinationFolderFile.exists()) {
                        LogHandler.saveLog("The destination file " + destinationFolderFile.getName() + " doesn't exists",false);
                        continue;
                    }
                    NetHttpTransport HTTP_TRANSPORT = null;
                    try {
                        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                    } catch (GeneralSecurityException | IOException e) {
                        LogHandler.saveLog("Failed to initialize http transport to upload to drive: " + e.getLocalizedMessage());
                    }
                    JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
                    HttpRequestInitializer requestInitializer = request -> {
                        request.getHeaders().setAuthorization("Bearer " + accessToken);
                        request.getHeaders().setContentType("application/json");
                    };
                    try {
                        Drive service = GoogleDrive.initializeDrive(accessToken);

                        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                        fileMetadata.setName(destinationFolderFile.getName());
                        String memeType = Media.getMimeType(destinationFolderFile);
                        if(Media.isImage(memeType)){
                            System.out.println("here6 in photos to drive");
                            String destinationFolderFilePath = destinationFolderFile.getPath();
                            if(memeType.toLowerCase().endsWith("jpg")){
                                mediaContent[0] = new FileContent("image/jpeg" ,
                                        new File(destinationFolderFilePath));
                            }else{
                                mediaContent[0] = new FileContent("image/" + memeType.toLowerCase() ,
                                        new File(destinationFolderFilePath));
                            }
                        }else if(Media.isVideo(memeType)){
                            String destinationFolderFilePath = destinationFolderFile.getPath();
                            if(memeType.toLowerCase().endsWith("mkv")){
                                mediaContent[0] = new FileContent("video/x-matroska" ,
                                        new File(destinationFolderFilePath));
                            }else{
                                mediaContent[0] = new FileContent("video/" + memeType.toLowerCase() ,
                                        new File(destinationFolderFilePath));
                            }
                        }else{
                            continue;
                        }
                        if(mediaContent[0] == null){
                            LogHandler.saveLog("You're trying to upload mediaContent of null for "
                                    + destinationFolderFile.getName());
                        }

                        //final int[] test = {2};
                        //if(test[0] > 0){
                        com.google.api.services.drive.model.File uploadFile =
                                null;
                        if (service != null) {
                            uploadFile = service.files()
                                    .create(fileMetadata, mediaContent[0]).setFields("id").execute();
                        }else{
                            LogHandler.saveLog("Drive service is null");
                        }
                        String uploadFileId = uploadFile.getId();
                        while(uploadFileId.isEmpty() | uploadFileId == null){
                            wait();
                            LogHandler.saveLog("waiting for uploadFileId to be not null",false);
                        }
                        if (uploadFileId == null | uploadFileId.isEmpty()){
                            LogHandler.saveLog("UploadFileId for " + destinationFolderFile.getName() + " is null");
                        }
                        else {
                            System.out.println("here7 in photos to drive");
//                           destinationFiles.add(new String[]{filePath,userEmail,fileName,assetId,fileHash});
                            for (String part : destinationFile){
                                System.out.println("part for upload photos from android to drive: " + part);
                            }
                            DBHelper.insertTransactionsData(destinationFile[1], destinationFile[2],
                                    destinationUserEmail, destinationFile[3], "syncPhotos" , destinationFile[4]);
                            finalUploadFileIds.add(uploadFileId);
                            LogHandler.saveLog("Uploading " + destinationFolderFile.getName()
                                    + " to backup account finished with uploadFileId: " + uploadFileId,false);
                        }
                        //test[0]--;
                        //}
                    }catch (Exception e) {
                        LogHandler.saveLog("Failed to upload to Drive backup account: " + e.getLocalizedMessage());
                    }
                    System.out.println("here8 in photos to drive");
                }
            }
            else{
                LogHandler.saveLog("Destination folder is null");
            }
            System.out.println("here9 in photos to drive " + finalUploadFileIds.toString()) ;
            return finalUploadFileIds;
        };
        ArrayList<String> uploadFileIds = null;
        Future<ArrayList<String>> futureFileIds = executor.submit(backgroundTaskUpload);
        try{
            uploadFileIds = futureFileIds.get();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get upload file id form background task upload: " + e.getLocalizedMessage());
        }
        return uploadFileIds;
    }

//    public static Boolean downloadFromPhotos(ArrayList<GooglePhotos.MediaItem> photosMediaItems,
//                                             File destinationFolder, String userEmail) {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        try {
//            return executor.submit(() -> {
//                if (!destinationFolder.exists() && !destinationFolder.mkdirs()) {
//                    LogHandler.saveLog("The destination folder was not created");
//                    return false;
//                }
//
//                for (GooglePhotos.MediaItem photosMediaItem : photosMediaItems) {
//                    if (shouldDownloadAsset(photosMediaItem.getId())) {
//                        if (!downloadMediaItem(photosMediaItem, destinationFolder, userEmail)) {
//                            LogHandler.saveLog("Failed to download " + photosMediaItem.getFileName());
//                            return false;
//                        }
//                    }
//                }
//                return true;
//            }).get();
//        } catch (Exception e) {
//            LogHandler.saveLog("Error in downloading from Photos: " + e.getLocalizedMessage());
//            return false;
//        } finally {
//            executor.shutdown();
//        }
//    }

    private static boolean shouldDownloadAsset(String mediaItemId) {
        String sqlQuery = "SELECT assetId FROM PHOTOS WHERE " +
                "EXISTS (SELECT 1 FROM PHOTOS WHERE fileId = ?)";
        Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQuery, new String[]{mediaItemId});
        boolean existsInAssets =   cursor != null && cursor.moveToFirst();
        if (!existsInAssets) {
            return true;
        }else{
            int assetIdColumnIndex = cursor.getColumnIndex("assetId");
            if (assetIdColumnIndex >= 0) {
                String assetId = cursor.getString(assetIdColumnIndex);
                String sqlQuery2 = "SELECT EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ?)";
                Cursor cursor2 = DBHelper.dbReadable.rawQuery(sqlQuery2, new String[]{assetId});
                if (cursor2 != null && cursor2.moveToFirst()) {
                    int existsInDrive = cursor2.getInt(0);
                    System.out.println("exits in drive: " + existsInDrive + " " + assetId);
                    if (existsInDrive == 0) {
                        return true;
                    }
                }
                if(cursor2 != null){
                    cursor2.close();
                }
            }
        }
        if(cursor != null){
            cursor.close();
        }
        return false;
    }

    //            displayDialogForRestoreAccountsDecision(preferences);
//            signInToPrimaryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if(result.getResultCode() == RESULT_OK){
//                        LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
//                        View[] childview = {primaryAccountsButtonsLinearLayout.getChildAt(
//                                primaryAccountsButtonsLinearLayout.getChildCount() - 1)};
//                        runOnUiThread(() -> childview[0].setClickable(false));
//                        Executor signInExecutor = Executors.newSingleThreadExecutor();
//                        try {
//                            Runnable backgroundTask = () -> {
//                                GoogleCloud.signInResult signInResult = googleCloud.handleSignInToPrimaryResult(result.getData());
//
//                                if (signInResult.getHandleStatus() == true) {
//                                    boolean[] isBackedUp = {false};
//                                    backUpJsonThread = new Thread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            try {
//                                                dbHelper.insertIntoAccounts(signInResult.getUserEmail(), "primary"
//                                                        , signInResult.getTokens().getRefreshToken(), signInResult.getTokens().getAccessToken(),
//                                                        signInResult.getStorage().getTotalStorage(), signInResult.getStorage().getUsedStorage(),
//                                                        signInResult.getStorage().getUsedInDriveStorage(), signInResult.getStorage().getUsedInGmailAndPhotosStorage());
//
//                                                isBackedUp[0] = MainActivity.dbHelper.backUpProfileMap(false,"");
//                                                System.out.println("isBackedUp "+isBackedUp[0]);
//                                            }catch (Exception e){
//                                                LogHandler.saveLog("failed to back up profile map in primary account: " + e.getLocalizedMessage());
//                                            }
//                                            synchronized (this){
//                                                notify();
//                                            }
//                                        }
//                                    });
//
//                                    Thread UIThread = new Thread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            synchronized (backUpJsonThread){
//                                                try{
//                                                    backUpJsonThread.join();
//                                                }catch (Exception e){
//                                                    LogHandler.saveLog("failed to join backUpJsonThread in primary account: " + e.getLocalizedMessage());
//                                                }
//                                            }
//                                            if (isBackedUp[0] == true) {
//                                                runOnUiThread(() -> {
//                                                    Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryAccountsButtonsLinearLayout);
//                                                    newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
//                                                    childview[0] = primaryAccountsButtonsLinearLayout.getChildAt(
//                                                            primaryAccountsButtonsLinearLayout.getChildCount() - 2);
//                                                    LogHandler.saveLog(signInResult.getUserEmail()
//                                                            + " has logged in to the primary account", false);
//
//                                                    if (childview[0] instanceof Button) {
//                                                        Button bt = (Button) childview[0];
//                                                        bt.setText(signInResult.getUserEmail());
//                                                        bt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D47A1")));
//                                                    }
//                                                    updateButtonsListeners();
//                                                });
//                                            }
//                                        }
//                                    });
//                                    backUpJsonThread.start();
//                                    UIThread.start();
//                                }else{
//                                    runOnUiThread(() -> {
//                                            LogHandler.saveLog("login with launcher failed");
//                                            LinearLayout primaryAccountsButtonsLinearLayout2 =
//                                                    findViewById(R.id.primaryAccountsButtons);
//                                            View childview2 = primaryAccountsButtonsLinearLayout2.getChildAt(
//                                                    primaryAccountsButtonsLinearLayout2.getChildCount() - 1);
//                                            if (childview2 instanceof Button) {
//                                                Button bt = (Button) childview2;
//                                                bt.setText("ADD A PRIMARY ACCOUNT");
//                                            }
//                                    updateButtonsListeners();
//                                    });
//                                }
//                            };
//                            signInExecutor.execute(backgroundTask);
//                            runOnUiThread(() -> childview[0].setClickable(true));
//                        }catch (Exception e){
//                            LogHandler.saveLog("Failed to sign in to primary : "  + e.getLocalizedMessage());
//                        }
//                   }else{
//                        runOnUiThread(() -> {
//                            LogHandler.saveLog("login with primary launcher failed with response code :" + result.getResultCode());
//                            LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
//                            View childview = primaryAccountsButtonsLinearLayout.getChildAt(
//                                    primaryAccountsButtonsLinearLayout.getChildCount() - 1);
//                            if(childview instanceof Button){
//                                Button bt = (Button) childview;
//                                bt.setText("ADD A PRIMARY ACCOUNT");
//                            }
//                            updateButtonsListeners();
//                        });
//                    }
//                }
//            );


    //        private void updatePrimaryButtonsListener(){
//            LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
//            for (int i = 0; i < primaryAccountsButtonsLinearLayout.getChildCount(); i++) {
//                View childView = primaryAccountsButtonsLinearLayout.getChildAt(i);
//                if (childView instanceof Button) {
//                    Button button = (Button) childView;
//                    button.setOnClickListener(
//                            view -> {
////                                syncToBackUpAccountButton.setClickable(false);
////                                restoreButton.setClickable(false);
//                                String buttonText = button.getText().toString().toLowerCase();
//                                if (buttonText.equals("add a primary account")){
//                                    button.setText("Wait");
//                                    button.setClickable(false);
//                                    googleCloud.signInToGoogleCloud(signInToPrimaryLauncher);
//                                    button.setClickable(true);
//                                }else if (buttonText.equals("wait")){
//                                    button.setText("Add a primary account");
//                                }
//                                else {
//                                    PopupMenu popupMenu = new PopupMenu(MainActivity.this,button, Gravity.CENTER);
//                                    popupMenu.getMenuInflater().inflate(R.menu.account_button_menu,popupMenu.getMenu());
//                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                        popupMenu.setGravity(Gravity.CENTER);
//                                    }
//                                    popupMenu.setOnMenuItemClickListener(item -> {
//                                        if (item.getItemId() == R.id.sign_out) {
//                                            try {
//                                                button.setText("Wait...");
//                                            } catch (Exception e) {
//                                                LogHandler.saveLog("Failed to set text to wait : " +
//                                                e.getLocalizedMessage(), true);
//                                            }
//
//                                            final boolean[] isSignedout = {false};
//                                            final boolean[] isBackedUp = {false};
//
//                                            Thread signOutThread = new Thread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    isSignedout[0] = googleCloud.signOut(buttonText);
//                                                    System.out.println("isSignedOut " + isSignedout[0]);
//                                                    synchronized (this){
//                                                        notify();
//                                                    }
//                                                }
//                                            });
//
//                                            backUpJsonThread = new Thread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    synchronized (signOutThread){
//                                                        try {
//                                                            MainActivity.dbHelper.deleteFromAccountsTable(buttonText, "primary");
//                                                            MainActivity.dbHelper.deleteAccountFromPhotosTable(buttonText);
//                                                            dbHelper.deleteRedundantAsset();
//                                                            signOutThread.join();
//                                                        } catch (InterruptedException e) {
//                                                            LogHandler.saveLog("Failed to join the signout thread" +
//                                                                    " + " + e.getLocalizedMessage(), true);
//                                                        }
//                                                    }
//                                                    if (isSignedout[0]) {
//                                                        isBackedUp[0] = MainActivity.dbHelper.backUpProfileMap(true,buttonText);
//                                                    }
//                                                    System.out.println("isBackedUp " + isBackedUp[0]);
//                                                }
//                                            });
//
//                                            Thread uiThread = new Thread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    synchronized (backUpJsonThread){
//                                                        try {
//                                                            backUpJsonThread.join();
//                                                        } catch (InterruptedException e) {
//                                                            LogHandler.saveLog("Failed to join " +
//                                                                    " back up json thread : " + e.getLocalizedMessage(), true);
//                                                        }
//                                                    }
//                                                    runOnUiThread(() -> {
//                                                        System.out.println("isSigned " + isSignedout[0]);
//                                                        if (isBackedUp[0]) {
//                                                            try {
//                                                                item.setEnabled(false);
//                                                                ViewGroup parentView = (ViewGroup) button.getParent();
//                                                                parentView.removeView(button);
//                                                            } catch (Exception e) {
//                                                                LogHandler.saveLog(
//                                                                        "Failed to handle ui after signout : "
//                                                                        + e.getLocalizedMessage(), true
//                                                                );
//                                                            }
//                                                        } else {
//                                                            try {
//                                                                button.setText(buttonText);
//                                                            } catch (Exception e) {
//                                                                LogHandler.saveLog(" Failed to set text " +
//                                                                        " to button text  : " + e.getLocalizedMessage()
//                                                                , true);
//                                                            }
//                                                        }
//                                                    });
//                                                }
//                                            });
//
//                                            signOutThread.start();
//                                            backUpJsonThread.start();
//                                            uiThread.start();
//                                        }
//                                        return true;
//                                    });
//                                    popupMenu.show();
//                                }
////                                syncToBackUpAccountButton.setClickable(true);
////                                restoreButton.setClickable(true);
//                            }
//                        );
//                }
//            }
//        }
}
