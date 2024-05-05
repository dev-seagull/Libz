package com.example.cso;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class Sync {
    public static void syncAndroidFiles(Context context){
        try{
            MainActivity.storageHandler.freeStorageUpdater();
            double amountSpaceToFreeUp = MainActivity.storageHandler.getAmountSpaceToFreeUp();

            String[] selected_accounts_columns = {"userEmail","type", "totalStorage","usedStorage", "refreshToken"};
            List<String[]> account_rows = DBHelper.getAccounts(selected_accounts_columns);

            for(String[] account_row: account_rows){
                String[] type = {account_row[1]};
                if(type[0].equals("backup")){
                    syncAndroidToBackupAccount(account_row,amountSpaceToFreeUp,context);
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to sync files: " + e.getLocalizedMessage(), true);
        }
    }

    private static void syncAndroidToBackupAccount(String[] accountRow, double amountSpaceToFreeUp, Context context){
        try{
            String userEmail = accountRow[0];
            String refreshToken = accountRow[4];
            String syncedAssetsFolderId = GoogleDrive.createStashSyncedAssetsFolderInDrive(userEmail);

            double driveFreeSpace = calculateDriveFreeSpace(accountRow);

            if (driveFreeSpace <= 0) return;

            List<String[]> sortedAndroidFiles = getSortedAndroidFiles();

            for (String[] androidRow : sortedAndroidFiles) {
                MainActivity.storageHandler.freeStorageUpdater();
                syncAndroidFile(androidRow, userEmail, refreshToken, syncedAssetsFolderId,
                        driveFreeSpace, amountSpaceToFreeUp, context);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to sync android to backup accounts: "  + e.getLocalizedMessage(), true);
        }
    }


    private static void syncAndroidFile(String[] androidRow, String userEmail, String refreshToken, String syncedAssetsFolderId,
                                        double driveFreeSpace, double amountSpaceToFreeUp, Context context){
        try{
            String fileName = androidRow[1];
            String fileHash = androidRow[5];
            String fileSize = androidRow[4];
            String filePath = androidRow[2];
            Long assetId = Long.valueOf(androidRow[8]);

            boolean isWifiOnlySwitchOn = SharedPreferencesHandler.getWifiOnlySwitchState(MainActivity.preferences);
            if ((isWifiOnlySwitchOn && InternetManager.getInternetStatus(context).equals("wifi")) || (!isWifiOnlySwitchOn)){
                if (!DBHelper.androidFileExistsInDrive(assetId, fileHash)){
                    String accessToken = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                    if(driveFreeSpace > Double.parseDouble(fileSize)) {
                        boolean isBackedUp = uploadAndroidToDrive(androidRow, userEmail, accessToken, syncedAssetsFolderId);
                        if (isBackedUp) {
                            driveFreeSpace -= Double.parseDouble(fileSize);
                            if(amountSpaceToFreeUp > 0){
                                deleteRedundantDriveFilesFromAccount(userEmail, accessToken);
                                if (DBHelper.androidFileExistsInDrive(Long.valueOf(androidRow[8]), fileHash)) {
                                    boolean isDeleted = Android.deleteAndroidFile(filePath, String.valueOf(assetId), fileHash
                                            , fileSize, fileName);
                                    if (isDeleted) {
                                        amountSpaceToFreeUp -= Double.parseDouble(fileSize);
                                    }
                                }
                            }

                        }
                    }
                }else {
                    if (amountSpaceToFreeUp > 0) {
                        String accessToken = MainActivity.googleCloud.updateAccessToken(refreshToken).getAccessToken();
                        deleteRedundantDriveFilesFromAccount(userEmail, accessToken);
                        if (DBHelper.androidFileExistsInDrive(Long.valueOf(androidRow[8]), fileHash)) {
                            boolean isDeleted = Android.deleteAndroidFile(filePath, androidRow[8], fileHash, fileSize, androidRow[1]);
                            if (isDeleted) {
                                amountSpaceToFreeUp -= Double.parseDouble(fileSize);
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to sync android file: " + e.getLocalizedMessage());
        }
    }

    private static boolean uploadAndroidToDrive(String[] androidRow, String userEmail
            , String accessToken, String syncedAssetsFolderId){
        boolean[] isBackedUp = {false};
        try{
            Thread backupThread = new Thread(() -> {
                Long fileId = Long.valueOf(androidRow[0]);
                String fileName = androidRow[1];
                String filePath = androidRow[2];
                String fileHash = androidRow[5];
                String mimeType = androidRow[7];
                String assetId = androidRow[8];
                BackUp backUp = new BackUp();
                isBackedUp[0] = backUp.backupAndroidToDrive(fileId,fileName, filePath,fileHash,mimeType,assetId,
                        accessToken,userEmail,syncedAssetsFolderId);
            });
            backupThread.start();
            try {
                backupThread.join();
            } catch (Exception e) {
                LogHandler.saveLog("Failed to join backup thread when syncing android file.", true);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to upload android to derive: " + e.getLocalizedMessage());
        }

        return isBackedUp[0];
    }

    private static void deleteRedundantDriveFilesFromAccount(String userEmail, String accessToken) {
        try {
            Thread deleteRedundantDrive = new Thread(() -> {
                ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken, userEmail);
                ArrayList<String> driveFileIds = new ArrayList<>();

                for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                    String fileId = driveMediaItem.getId();
                    driveFileIds.add(fileId);
                }
                MainActivity.dbHelper.deleteRedundantDriveFromDB(driveFileIds, userEmail);
            });
            deleteRedundantDrive.start();
            try {
                deleteRedundantDrive.join();
            } catch (Exception e) {
                LogHandler.saveLog("Failed to join delete " +
                        " redundant drive thread when syncing android file.", true);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to run delete redundant drive files from account : " +e.getLocalizedMessage(), true);
        }
    }

    private static List<String[]> getSortedAndroidFiles(){
        String[] selected_android_columns = {"id", "fileName", "filePath", "device",
                "fileSize", "fileHash", "dateModified", "memeType","assetId"};

        List<String[]> unique_android_rows = new ArrayList<>();
        ArrayList<String> file_hashes = new ArrayList<>() ;

        try{
            List<String[]> android_rows =  MainActivity.dbHelper.getAndroidTable(selected_android_columns);
            BackUp.sortAndroidItems(android_rows);
            for(String[] android_row: android_rows){
                String fileHash = android_row[5];
                if(!file_hashes.contains(fileHash)){
                    file_hashes.add(fileHash);
                    unique_android_rows.add(android_row);
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("get sorted android files failed: " + e.getLocalizedMessage(), true );
        }

        return unique_android_rows;
    }

    private static double calculateDriveFreeSpace(String[] accountRow){
        try{
            String[] totalStorage = {accountRow[2]};
            String[] usedStorage = {accountRow[3]};
            return Double.parseDouble(totalStorage[0]) - Double.parseDouble(usedStorage[0]);
        }catch (Exception e) {
            LogHandler.saveLog("Failed to calculate drive free space: " + e.getLocalizedMessage(), true);
            return 0;
        }
    }
}

//--------------------------lets go for next comment code (google photos is here)--------------------------------

//
//            syncToBackUpAccountButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        if (!isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
//                            TimerService.shouldCancel = false;
//                            startForegroundService(serviceIntent);
//                        }
//                    }
//                    System.out.println("startService(serviceIntent); " + serviceIntent);
//
//                }
//            });


//                    int buildSdkInt = Build.VERSION.SDK_INT;
//                    if (!dbHelper.backupAccountExists()){
//                        runOnUiThread(() ->{
//                            TextView restoreTextView = findViewById(R.id.restoreTextView);
//                            restoreTextView.setText("First Login to a backup account");
//                        });
//                        return;
//                    }
//                    runOnUiThread(() ->{
//                        syncToBackUpAccountButton.setClickable(false);
//                        restoreButton.setClickable(false);
//                        TextView restoreTextView = findViewById(R.id.restoreTextView);
//                        restoreTextView.setText("Wait until the restoring process is finished");
//                    });
//                    Thread deleteRedundantAndroidThread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            LogHandler.saveLog("Starting to get files from you android device",false);
//                            dbHelper.deleteRedundantAndroid();
//                            synchronized (this){
//                                notify();
//                            }
//                        }
//                    });
//
//                    Thread updateAndroidFilesThread = new Thread(() -> {
//                        synchronized (deleteRedundantAndroidThread){
//                            try{
//                                deleteRedundantAndroidThread.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join deleteRedundantAndroidThread thread: "  + e.getLocalizedMessage());
//                            }
//                        }
//                        Android.getGalleryMediaItems(MainActivity.this);
//                        LogHandler.saveLog("End of getting files from your android device",false);
//                    });
//
//
//                    deleteRedundantDriveThread = new Thread(() -> {
//                        synchronized (updateAndroidFilesThread){
//                            try{
//                                updateAndroidFilesThread.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join updateAndroidFilesThread : " + e.getLocalizedMessage());
//                            }
//                        }
//
//                        String[] columns = {"accessToken","userEmail", "type"};
//                        List<String[]> account_rows = dbHelper.getAccounts(columns);
//
//                        for(String[] account_row : account_rows) {
//                            String type = account_row[2];
//                            if(type.equals("backup")){
//                                String userEmail = account_row[1];
//                                String accessToken = account_row[0];
//                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
//                                ArrayList<String> driveFileIds = new ArrayList<>();
//
//                                for (BackUpAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
//                                    String fileId = driveMediaItem.getId();
//                                    driveFileIds.add(fileId);
//                                }
//                                dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
//                            }
//                        }
//                    });
//
//                    updateDriveBackUpThread = new Thread(() -> {
//                        synchronized (deleteRedundantDriveThread){
//                            try {
//                                deleteRedundantDriveThread.join();
//                            } catch (Exception e) {
//                                LogHandler.saveLog("failed to join deleteRedundantDrive thread: " + e.getLocalizedMessage());
//                            }
//                        }
//
//                        String[] columns = {"accessToken", "userEmail","type"};
//                        List<String[]> account_rows = dbHelper.getAccounts(columns);
//
//                        for(String[] account_row : account_rows){
//                            String accessToken = account_row[0];
//                            String userEmail = account_row[1];
//                            String type = account_row[2];
//                            if (type.equals("backup")){
//                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
//                                for(BackUpAccountInfo.MediaItem driveMediaItem: driveMediaItems){
//                                    Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
//                                    if (last_insertId != -1) {
//                                        MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
//                                                driveMediaItem.getHash(), userEmail);
//                                    } else {
//                                        LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
//                                    }
//                                }
//                            }
//                        }
//                    });
//
//                    deleteDuplicatedInDrive = new Thread(() -> {
//                        synchronized (updateDriveBackUpThread){
//                            try {
//                                updateDriveBackUpThread.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join driveBackUpThread : " + e.getLocalizedMessage());
//                            }
//                        }
//
//
//                        String[] columns = {"accessToken","userEmail", "type"};
//                        List<String[]> account_rows = dbHelper.getAccounts(columns);
//
//                        for(String[] account_row : account_rows) {
//                            String type = account_row[2];
//                            if(type.equals("backup")){
//                                String userEmail = account_row[1];
//                                String accessToken = account_row[0];
//                                GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
//                            }
//                        }
//                    });
//
//
//                    Thread restoreThread = new Thread() {
//                        @Override
//                        public void run() {
//                            synchronized (deleteDuplicatedInDrive){
//                                try{
//                                    deleteDuplicatedInDrive.join();
//                                }catch (Exception e){
//                                    LogHandler.saveLog("failed to join androidUploadThread : "  + e.getLocalizedMessage());
//                                }
//                            }
//                            try {
//                                if (buildSdkInt >= 30) {
//                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                                        if (!Environment.isExternalStorageManager()) {
//                                            Intent getPermission = new Intent();
//                                            getPermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                                            startActivity(getPermission);
//                                            while (!Environment.isExternalStorageManager()){
//                                                System.out.println("here " + Environment.isExternalStorageManager());
//                                                try {
//                                                    Thread.sleep(500);
//                                                    System.out.println("... " + Environment.isExternalStorageManager());
//                                                } catch (InterruptedException e) {
//                                                    System.out.println("here2" + Environment.isExternalStorageManager());
//                                                    LogHandler.saveLog("Failed to sleep the thread : " + e.getLocalizedMessage(), true);
//
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                                    if (Environment.isExternalStorageManager()) {
//                                        LogHandler.saveLog("Starting to restore files from your android device",false);
//                                        System.out.println("Starting to restore files from your android device");
//                                        Upload.restore(getApplicationContext());
//                                    }}
//                            } catch (Exception e) {
//                                LogHandler.saveLog("kFailed to get manage external storage in restore thread: " + e.getLocalizedMessage());
//                            }
//                        }
//                    };
//
//
//                    Thread deleteRedundantAndroidThread2 = new Thread(() -> {
//                        synchronized (restoreThread){
//                            try{
//                                restoreThread.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join restore thread: "  + e.getLocalizedMessage());
//                            }
//                        }
//                        dbHelper.deleteRedundantAndroid();
//                    });
//
//                    Thread updateAndroidFilesThread2 = new Thread(() -> {
//                        synchronized (deleteRedundantAndroidThread2){
//                            try{
//                                deleteRedundantAndroidThread2.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join deleteRedundantAndroidThread thread: "  + e.getLocalizedMessage());
//                            }
//                        }
//                        Android.getGalleryMediaItems(MainActivity.this);
//                        LogHandler.saveLog("End of getting files from your android device",false);
//                    });
//
//
//                    Thread updateUIThread =  new Thread(() -> {
//                        synchronized (updateAndroidFilesThread2){
//                            try{
//                                updateAndroidFilesThread2.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join updateAndroidFilesThread2 in restoring : "  + e.getLocalizedMessage());
//                            }
//                        }
//                        runOnUiThread(() -> {
//                            syncToBackUpAccountButton.setClickable(true);
//                            restoreButton.setClickable(true);
//
//                            NotificationHandler.sendNotification("1","restoringAlert", MainActivity.this,
//                                    "Restoring is finished","You're files are restored!");
//                            TextView restoreTextView = findViewById(R.id.restoreTextView);
//                            restoreTextView.setText("restoring process is finished");
//                        });
//                    });
//
//                    deleteRedundantAndroidThread.start();
//                    updateAndroidFilesThread.start();
//                    deleteRedundantDriveThread.start();
//                    updateDriveBackUpThread.start();
//                    deleteDuplicatedInDrive.start();
//                    restoreThread.start();
//                    deleteRedundantAndroidThread2.start();
//                    updateAndroidFilesThread2.start();
//                    updateUIThread.start();
//                    stopService(serviceIntent);
//                    serviceIntent = new Intent(activity,TimerService.class);

//            restoreButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    TimerService.shouldCancel = true;
//                    while (!isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("off")) {
//                        stopService(serviceIntent);
//                    }
//                }
//            });


//--------------------------lets go for next comment code (google photos is here)--------------------------------




//            syncToBackUpAccountButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {

//                    if (!dbHelper.backupAccountExists()){
//                        runOnUiThread(() ->{
//                            TextView syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
//                            syncToBackUpAccountTextView.setText("First Login to a backup account");
//                        });
//                        return;
//                    }
//                    runOnUiThread(() ->{
//                        restoreButton.setClickable(false);
//                        syncToBackUpAccountButton.setClickable(false);
//                        TextView syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
//                        syncToBackUpAccountTextView.setText("Wait until the uploading process is finished");
//                    });
//
//                    Thread deleteRedundantAndUpdatePhotos = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            String[] columns = {"accessToken","userEmail", "type"};
//                            List<String[]> account_rows = dbHelper.getAccounts(columns);
//                            File destinationFolder = new File(Environment
//                                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator
//                                    + "stash");
//                            for(String[] account_row : account_rows) {
//                                String type = account_row[2];
//                                if(type.equals("primary")){
//                                    String userEmail = account_row[1];
//                                    String accessToken = account_row[0];
//                                    ArrayList<GooglePhotos.MediaItem> photosMediaItems =
//                                            GooglePhotos.getGooglePhotosMediaItems(accessToken);
//                                    System.out.println("media itemsssss size : " + photosMediaItems.size());
//                                    ArrayList<String> fileIds = new ArrayList<>();
//                                    for(GooglePhotos.MediaItem photosMediaItem: photosMediaItems){
//                                        fileIds.add(photosMediaItem.getId());
//                                    }
//                                    MainActivity.dbHelper.deleteRedundantPhotos(fileIds,userEmail);
//                                    Upload.downloadFromPhotos(photosMediaItems,destinationFolder,userEmail);
//                                }
//                            }
//                            synchronized (this){
//                                notify();
//                            }
//                        }
//                    });
//
//                    Thread uploadPhotosToDriveThread = new Thread(() -> {
//                        int buildSdkInt = Build.VERSION.SDK_INT;
//                        synchronized (deleteRedundantAndUpdatePhotos){
//                            try{
//                                deleteRedundantAndUpdatePhotos.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join deleteRedundantAndUpdatePhotos : "  + e.getLocalizedMessage());
//                            }
//                        }
//                        try {
//                            if (buildSdkInt >= 30) {
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                                    if (!Environment.isExternalStorageManager()) {
//                                        Intent getPermission = new Intent();
//                                        getPermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                                        startActivity(getPermission);
//                                        while (!Environment.isExternalStorageManager()){
//                                            System.out.println("here " + Environment.isExternalStorageManager());
//                                            try {
//                                                Thread.sleep(500);
//                                                System.out.println("... " + Environment.isExternalStorageManager());
//                                            } catch (Exception e) {
//                                                System.out.println("here2" + Environment.isExternalStorageManager());
//                                                LogHandler.saveLog("Failed to sleep the thread: " + e.getLocalizedMessage());
//
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            System.out.println("here Build.VERSION.SDK_INT >= Build.VERSION_CODES.R" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R));
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                                if (Environment.isExternalStorageManager()) {
//                                    String[] columns = {"userEmail", "type" ,"accessToken"};
//                                    List<String[]> selectedRows = dbHelper.getAccounts(columns);
//                                    for(String[] selectedRow: selectedRows){
//                                        String destinationUserEmail = selectedRow[0];
//                                        String type = selectedRow[1];
//                                        String accessToken = selectedRow[2];
//                                        if(type.equals("backup")){
//                                            System.out.println("here ready to upload to drive ");
//                                            Upload upload = new Upload();
//                                            upload.uploadPhotosToDrive(destinationUserEmail,accessToken);
//                                            break;
//                                        }
//                                    }
//                                }
//                            }
//                        } catch (Exception e) {
//                            LogHandler.saveLog("Failed to get manage external storage in photos thread: " + e.getLocalizedMessage());
//                        }
//                    });
//
//
//                    Thread deletePhotosFromAndroidThread = new Thread(() -> {
//                        synchronized (uploadPhotosToDriveThread){
//                            try{
//                                uploadPhotosToDriveThread.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join uploadPhotosToDriveThread : "  + e.getLocalizedMessage());
//                            }
//                        }
//                        Upload.deletePhotosFromAndroid();
//                    });
//
//
//                    Thread deleteRedundantAndroidThread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            synchronized (deletePhotosFromAndroidThread){
//                                try {
//                                    deletePhotosFromAndroidThread.join();
//                                } catch (InterruptedException e) {
//                                    LogHandler.saveLog("Failed to join deleteRedundantAndUpdatePhotos" +
//                                            " in deleteRedundantAndroidThread: "+ e.getLocalizedMessage());
//                                }
//                            }
//                            LogHandler.saveLog("Starting to get files from you android device",false);
//                            dbHelper.deleteRedundantAndroid();
//                        }
//                    });
//
//                    Thread updateAndroidFilesThread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            synchronized (deleteRedundantAndroidThread){
//                                try{
//                                    deleteRedundantAndroidThread.join();
//                                }catch (Exception e){
//                                    LogHandler.saveLog("failed to join deleteRedundantAndroidThread thread: "  + e.getLocalizedMessage());
//                                }
//                            }
//                            Android.getGalleryMediaItems(MainActivity.this);
//                            LogHandler.saveLog("End of getting files from your android device",false);
//                        }
//                    });
//
//
//                    deleteRedundantDriveThread = new Thread(() -> {
//                        synchronized (updateAndroidFilesThread){
//                            try{
//                                updateAndroidFilesThread.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join updateAndroidFilesThread : " + e.getLocalizedMessage());
//                            }
//                        }
//
//                        String[] columns = {"accessToken","userEmail", "type"};
//                        List<String[]> account_rows = dbHelper.getAccounts(columns);
//
//                        for(String[] account_row : account_rows) {
//                            String type = account_row[2];
//                            if(type.equals("backup")){
//                                String userEmail = account_row[1];
//                                String accessToken = account_row[0];
//                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
//                                ArrayList<String> driveFileIds = new ArrayList<>();
//
//                                for (BackUpAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
//                                    String fileId = driveMediaItem.getId();
//                                    driveFileIds.add(fileId);
//                                }
//                                dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
//                            }
//                        }
//                    });
//
//                    Thread driveBackUpThread = new Thread(() -> {
//                        synchronized (deleteRedundantDriveThread){
//                            try {
//                                deleteRedundantDriveThread.join();
//                            } catch (Exception e) {
//                                LogHandler.saveLog("failed to join deleteRedundantDrive thread: " + e.getLocalizedMessage());
//                            }
//                        }
//
//                        String[] columns = {"accessToken", "userEmail","type"};
//                        List<String[]> account_rows = dbHelper.getAccounts(columns);
//
//                        for(String[] account_row : account_rows){
//                            String type = account_row[2];
//                            if (type.equals("backup")){
//                                String accessToken = account_row[0];
//                                String userEmail = account_row[1];
//                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
//                                for(BackUpAccountInfo.MediaItem driveMediaItem: driveMediaItems){
//                                    Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
//                                    if (last_insertId != -1) {
//                                        MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
//                                                driveMediaItem.getHash(), userEmail);
//                                    } else {
//                                        LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
//                                    }
//                                }
//                            }
//                        }
//                    });
//
//                    deleteDuplicatedInDrive = new Thread(() -> {
//                        synchronized (driveBackUpThread){
//                            try {
//                                driveBackUpThread.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join driveBackUpThread : " + e.getLocalizedMessage());
//                            }
//                        }
//
//
//                        String[] columns = {"accessToken","userEmail", "type"};
//                        List<String[]> account_rows = dbHelper.getAccounts(columns);
//
//                        for(String[] account_row : account_rows) {
//                            String type = account_row[2];
//                            if(type.equals("backup")){
//                                String userEmail = account_row[1];
//                                String accessToken = account_row[0];
//                                GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
//                            }
//                        }
//                    });
//
//
//                    Thread androidUploadThread = new Thread(() -> {
//                        synchronized (deleteDuplicatedInDrive){
//                            try{
//                                deleteDuplicatedInDrive.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join deleteDuplicatedInDrive : "  + e.getLocalizedMessage());
//                            }
//                        }
//                        Upload upload = new Upload();
//                        upload.uploadAndroidToDrive();
//                    });
//
//                    Thread deleteRedundantDriveThread2 = new Thread(() -> {
//                        synchronized (androidUploadThread){
//                            try{
//                                androidUploadThread.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join updateAndroidFilesThread : " + e.getLocalizedMessage());
//                            }
//                        }
//
//                        String[] columns = {"accessToken","userEmail", "type"};
//                        List<String[]> account_rows = dbHelper.getAccounts(columns);
//
//                        for(String[] account_row : account_rows) {
//                            String type = account_row[2];
//                            if(type.equals("backup")){
//                                String userEmail = account_row[1];
//                                String accessToken = account_row[0];
//                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
//                                ArrayList<String> driveFileIds = new ArrayList<>();
//
//                                for (BackUpAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
//                                    String fileId = driveMediaItem.getId();
//                                    driveFileIds.add(fileId);
//                                }
//                                dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
//                            }
//                        }
//                    });
//
//                    Thread driveBackUpThread2 = new Thread(() -> {
//                        synchronized (deleteRedundantDriveThread2){
//                            try {
//                                deleteRedundantDriveThread2.join();
//                            } catch (Exception e) {
//                                LogHandler.saveLog("failed to join deleteRedundantDrive thread: " + e.getLocalizedMessage());
//                            }
//                        }
//
//                        String[] columns = {"accessToken", "userEmail","type"};
//                        List<String[]> account_rows = dbHelper.getAccounts(columns);
//
//                        for(String[] account_row : account_rows){
//                            String type = account_row[2];
//                            if (type.equals("backup")){
//                                String accessToken = account_row[0];
//                                String userEmail = account_row[1];
//                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
//                                for(BackUpAccountInfo.MediaItem driveMediaItem: driveMediaItems){
//                                    Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
//                                    if (last_insertId != -1) {
//                                        MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
//                                                driveMediaItem.getHash(), userEmail);
//                                    } else {
//                                        LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
//                                    }
//                                }
//                            }
//                        }
//                    });
//
//                    Thread deleteDuplicatedInDrive2 = new Thread(() -> {
//                        synchronized (driveBackUpThread2){
//                            try {
//                                driveBackUpThread2.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join driveBackUpThread : " + e.getLocalizedMessage());
//                            }
//                        }
//
//
//                        String[] columns = {"accessToken","userEmail", "type"};
//                        List<String[]> account_rows = dbHelper.getAccounts(columns);
//
//                        for(String[] account_row : account_rows) {
//                            String type = account_row[2];
//                            if(type.equals("backup")){
//                                String userEmail = account_row[1];
//                                String accessToken = account_row[0];
//                                GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
//                            }
//                        }
//
//                        String DBSqlQuery = "SELECT * FROM BACKUPDB";
//                        Cursor cursor = dbHelper.dbReadable.rawQuery(DBSqlQuery, null);
//                        if(cursor != null && cursor.moveToFirst()){
//                            System.out.println("cur no null");
//                            int fileIdColumnIndex = cursor.getColumnIndex("fileId");
//                            int userEmailColumnIndex = cursor.getColumnIndex("userEmail");
//
//                            if(fileIdColumnIndex >= 0 && userEmailColumnIndex >= 0){
//                                String fileId = cursor.getString(fileIdColumnIndex);
//                                String userEmail = cursor.getString(userEmailColumnIndex);
//                                String driveBackupAccessToken = "";
//                                String[] drive_backup_selected_columns = {"userEmail", "type", "accessToken"};
//                                List<String[]> drive_backUp_accounts = MainActivity.dbHelper.getAccounts(drive_backup_selected_columns);
//                                for (String[] drive_backUp_account : drive_backUp_accounts) {
//                                    if (drive_backUp_account[1].equals("backup") && drive_backUp_account[0].equals(userEmail)) {
//                                        driveBackupAccessToken = drive_backUp_account[2];
//                                        break;
//                                    }
//                                }
//
//                                System.out.println("drive token to delete database: "+ driveBackupAccessToken);
//                                URL url = null;
//                                try {
//                                    url = new URL("https://www.googleapis.com/drive/v3/files/" + fileId);
//                                } catch (MalformedURLException e) {
//                                    LogHandler.saveLog("failed to set url to delete backup database");
//                                }
//                                for(int i=0; i<3; i++){
//                                    HttpURLConnection connection = null;
//                                    try {
//                                        connection = (HttpURLConnection) url.openConnection();
//                                    } catch (IOException e) {
//                                        LogHandler.saveLog("failed to set connection to delete backup database");
//                                    }
//                                    try {
//                                        connection.setRequestMethod("DELETE");
//                                    } catch (ProtocolException e) {
//                                        LogHandler.saveLog("failed to set delete request method to delete backup database");
//                                    }
//                                    connection.setRequestProperty("Content-type", "application/json");
//                                    connection.setRequestProperty("Authorization", "Bearer " + driveBackupAccessToken);
//                                    int responseCode = 0;
//                                    try {
//                                        responseCode = connection.getResponseCode();
//                                    } catch (Exception e) {
//                                        LogHandler.saveLog("failed to get response code of deleting backup database : " + e.getLocalizedMessage());
//                                    }
//                                    LogHandler.saveLog("responseCode of deleting duplicate drive : " + responseCode,false);
//                                    if(responseCode == HttpURLConnection.HTTP_NO_CONTENT){
//                                        String deleteQuery = "DELETE FROM BACKUPDB WHERE UserEmail = ?  and fileId = ? ";
//                                        dbHelper.dbWritable.execSQL(deleteQuery, new String[]{userEmail, fileId});
//                                        break;
//                                    }else{
//                                        BufferedReader bufferedReader = null;
//                                        try {
//                                            bufferedReader = new BufferedReader(
//                                                    new InputStreamReader(connection.getInputStream() != null ? connection.getErrorStream() : connection.getInputStream())
//                                            );
//                                        } catch (Exception e) {
//                                            LogHandler.saveLog("Failed to work with bufferReader in deleting backup database : " + e.getLocalizedMessage());
//                                        }
//                                        StringBuilder responseBuilder = new StringBuilder();
//                                        String line = null;
//                                        try {
//                                            if (!((line = bufferedReader.readLine()) != null))
//                                                responseBuilder.append(line);
//                                        } catch (Exception e) {
//                                            LogHandler.saveLog("Failed to work with bufferReader to line in deleting backup database : " + e.getLocalizedMessage());
//                                        }
//                                        String response = responseBuilder.toString();
//                                        System.out.println(response);
//                                        LogHandler.saveLog("Retrying to delete backup database " +
//                                            "from Drive back up account " + userEmail +
//                                            " with response code of " + responseCode);
//                                    }
//                                }
//                            }
//                        }
//                        if(cursor != null){
//                            cursor.close();
//                        }
//
//                        List<String> result = dbHelper.backUpDataBase(getApplicationContext());
//                        String userEmailDatabase = result.get(0);
//                        String databaseFileId = result.get(1);
//                        String sqlQuery = "INSERT INTO BACKUPDB(userEmail, fileId) VALUES (?,?)";
//                        dbHelper.dbWritable.execSQL(sqlQuery,new String[]{userEmailDatabase,databaseFileId});
//                    });
//
//                    Thread updateUIThread =  new Thread(() -> {
//                        synchronized (deleteDuplicatedInDrive2){
//                            try{
//                                deleteDuplicatedInDrive2.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join androidUploadThread : "  + e.getLocalizedMessage());
//                            }
//                        }
//                        runOnUiThread(() -> {
//                            restoreButton.setClickable(true);
//                            syncToBackUpAccountButton.setClickable(true);
//                            NotificationHandler.sendNotification("1","syncingAlert", MainActivity.this,
//                                            "Syncing is finished","You're files are backed-up!");
//                            TextView syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
//                            syncToBackUpAccountTextView.setText("Uploading process is finished");
//                        });
//                    });
//
//
//
//                    deleteRedundantAndUpdatePhotos.start();
//                    uploadPhotosToDriveThread.start();
//                    deletePhotosFromAndroidThread.start();
//
//
//
//                    deleteRedundantAndroidThread.start();
//                    updateAndroidFilesThread.start();
//                    deleteRedundantDriveThread.start();
//                    driveBackUpThread.start();
//                    deleteDuplicatedInDrive.start();
//                    androidUploadThread.start();
//                    deleteRedundantDriveThread2.start();
//                    driveBackUpThread2.start();
//                    deleteDuplicatedInDrive2.start();
//                    updateUIThread.start();




// ---------------------------------------------END of Commented codes -----------