package com.example.cso;

import static com.example.cso.Sync.getSortedAndroidFiles;

import android.app.Activity;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Android {
    static int[] galleryItems = {0};
    static String[] forbiddenFolders = {"/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Private",
            "/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers",
            "/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Private",
            "/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Animated Gifs/Private",
            "/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents/Private",
            "/Telegram/Telegram Documents"};
    public static int getGalleryMediaItems(Activity activity) {
        galleryItems[0] = 0;
        LogHandler.saveLog("Started to get android files from your device.", false);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Callable<Integer> backgroundTask = () -> {
            try (Cursor cursor = createAndroidCursor(activity)) {
                if (cursor == null) {
                    getFileManagerMediaItems();
                } else {
                    int columnIndexPath = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    int columnIndexSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int columnIndexMimeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);

                    while (cursor.moveToNext()) {
                        processGalleryItem(cursor, columnIndexPath, columnIndexSize, columnIndexMimeType);
                    }
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to get gallery files : " + e.getLocalizedMessage(), true);
            }
//            try{
//                if(galleryItems[0] == 0){
//                    getFileManagerMediaItems();
//                }
//            }catch (Exception e){
//                LogHandler.saveLog("Getting device files failed: " + e.getLocalizedMessage(), true);
//            }
            return galleryItems[0];
        };

        Future<Integer> future = null;
        try{
            future = executor.submit(backgroundTask);
        }catch (Exception e){
            LogHandler.saveLog("Failed to submit executor: " + e.getLocalizedMessage(), true);
        }
        int result = 0;
        try {
            if (future != null) {
                result = future.get();
            }
        } catch (Exception e) {
            LogHandler.saveLog("error when downloading user profile : " + e.getLocalizedMessage(), true);
        }
        return result;
    }


    public static void getFileManagerMediaItems(){
        LogHandler.saveLog("Did not found any files in your gallery, so " +
                "it started to get files from file manager." , false);
        String[] extensions = {".jpg", ".jpeg", ".png", ".webp",
                ".gif", ".mp4", ".mkv", ".webm"};
        galleryItems[0] = 0;
        File rootDirectory = Environment.getExternalStorageDirectory();
        Queue<File> queue = new LinkedList<>();
        try{
            queue.add(rootDirectory);
            while (!queue.isEmpty()){
                File currentDirectory = queue.poll();
                File[] currentFiles = new File[0];
                if (currentDirectory != null && !currentDirectory.isHidden()) {
                    currentFiles = currentDirectory.listFiles();
                }
                if(currentFiles != null){
                    for(File currentFile: currentFiles){
                        processFileManagerFile(currentFile, extensions, queue);
                    }
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get files from file manager: " + e.getLocalizedMessage(), true);
        }
    }


    private static boolean hasValidExtension(File file, String[] extensions) {
        for (String extension : extensions) {
            if (file.getName().toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }


    private static void processFileManagerFile(File currentFile, String[] extensions, Queue<File> queue){
        if(currentFile.isFile() && hasValidExtension(currentFile, extensions)){
            String mediaItemPath = currentFile.getPath();
            File mediaItemFile = new File(mediaItemPath);
            String mediaItemName = currentFile.getName();
            Double mediaItemSize = currentFile.length() / (Math.pow(1024, 2));
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
            String mediaItemDateModified = dateFormat.format(new Date(mediaItemFile.lastModified()));
            String mediaItemMimeType = Media.getMimeType(mediaItemName);
            if(mediaItemFile.exists()){
                galleryItems[0]++;
                if(!DBHelper.existsInAndroidWithoutHash(mediaItemPath, MainActivity.androidUniqueDeviceIdentifier,
                        mediaItemDateModified, mediaItemSize)){
                    processFileManagerItem(mediaItemFile, mediaItemName, mediaItemPath, mediaItemSize,
                            mediaItemDateModified, mediaItemMimeType);
                }
            }
        }else if(currentFile.isDirectory() && !currentFile.isHidden()){
            boolean isForbidden = false;
            String directoryPath = currentFile.getPath();
            for (String forbiddenFolder: forbiddenFolders){
                if(directoryPath.equals(Environment.getExternalStorageDirectory() + forbiddenFolder)){
                    isForbidden = true;
                    break;
                }
            }
            if(!isForbidden){
                queue.add(currentFile);
            }
        }
    }

    private static void processFileManagerItem(File mediaItemFile, String mediaItemName, String mediaItemPath,
                                               Double mediaItemSize, String mediaItemDateModified, String mediaItemMimeType){
        String mediaItemHash = "";
        try {
            mediaItemHash = Hash.calculateHash(mediaItemFile);
        } catch (Exception e) {
            LogHandler.saveLog("Failed to calculate hash in file manager: " + e.getLocalizedMessage(), true);
        }
        long lastInsertedId =
                DBHelper.insertAssetData(mediaItemHash);
        if(lastInsertedId != -1){
            DBHelper.insertIntoAndroidTable(lastInsertedId,mediaItemName, mediaItemPath, MainActivity.androidUniqueDeviceIdentifier,
                    mediaItemHash,mediaItemSize, mediaItemDateModified,mediaItemMimeType);
        }else{
            LogHandler.saveLog("Failed to insert file into android table in file manager : " + mediaItemFile.getName(), true);
        }
    }

    private static void processGalleryItem(Cursor cursor,int columnIndexPath, int columnIndexSize
            ,int columnIndexMimeType){
        String mediaItemPath = cursor.getString(columnIndexPath);
        File mediaItemFile = new File(mediaItemPath);
        String mediaItemName = mediaItemFile.getName();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        String mediaItemDateModified = dateFormat.format(new Date(mediaItemFile.lastModified()));
        Double mediaItemSize = Double.parseDouble(cursor.getString(columnIndexSize)) / (Math.pow(10, 6));
        String mediaItemMemeType = cursor.getString(columnIndexMimeType);
        File androidFile = new File(mediaItemPath);

        if(androidFile.exists()){
            galleryItems[0]++;
            if(!DBHelper.existsInAndroidWithoutHash(mediaItemPath, MainActivity.androidUniqueDeviceIdentifier,
                    mediaItemDateModified, mediaItemSize)){
                System.out.println("here inserting android : " + mediaItemName + " " + mediaItemDateModified);
                String fileHash = "";
                try {
                    fileHash = Hash.calculateHash(androidFile);

                } catch (Exception e) {
                    LogHandler.saveLog("Failed to calculate hash: " + e.getLocalizedMessage(), true);
                }
                long lastInsertedId =
                        DBHelper.insertAssetData(fileHash);
                if(lastInsertedId != -1){
                    DBHelper.insertIntoAndroidTable(lastInsertedId,
                            mediaItemName, mediaItemPath, MainActivity.androidUniqueDeviceIdentifier,
                            fileHash,mediaItemSize, mediaItemDateModified,mediaItemMemeType);
                }else{
                    LogHandler.saveLog("Failed to insert file into android table: " + mediaItemFile.getName(), true);
                }
            }
        }
    }

    private static Cursor createAndroidCursor(Activity activity){
        try{
            String[] projection = {
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.MIME_TYPE
            };

            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=? OR " +
                    MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";
            String[] selectionArgs = {String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)};
            String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " ASC";

            return activity.getContentResolver().query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            );
        } catch (Exception e) {
            LogHandler.saveLog("Failed to create cursor: " + e.getLocalizedMessage(), true);
            return null;
        }
    }

    public static boolean deleteAndroidFile(String device, String filePath, String assetId, String fileHash, String fileSize,
                                            String fileName, Activity activity){
        final boolean[] isDeleted = {false};
        Thread deleteAndroidFileThread = new Thread(() -> {
            try{
                File androidFile = new File(filePath);
                androidFile.delete();
                if(!androidFile.exists()) {
                    isDeleted[0] = DBHelper.deleteFromAndroidTable(assetId, fileSize, filePath, fileName, fileHash);
                    MediaScannerConnection.scanFile(activity.getApplicationContext(),
                            new String[]{filePath}, null, (path, uri) -> {});
                    if(isDeleted[0]){
                        DBHelper.insertTransactionsData(filePath,fileName,device,assetId,"deletedInDevice",fileHash);
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to delete android file : " + e.getLocalizedMessage(), true);
            }
        });

        deleteAndroidFileThread.start();
        try{
            deleteAndroidFileThread.join();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e);}

        return isDeleted[0];
    }

    private static void startDeleteRedundantAndroidThread(){
        Log.d("Threads","DeleteRedundantAndroidThread started");
        Thread deleteRedundantAndroidThread = new Thread(DBHelper::deleteRedundantAndroidFromDB);
        deleteRedundantAndroidThread.start();
        Log.d("Threads","DeleteRedundantAndroidThread finished");
    }

    private static void startUpdateAndroidThread(Activity activity){
        Log.d("Threads","startUpdateAndroidThread started");
        Thread updateAndroidThread = new Thread( () -> {
            int galleryItems = Android.getGalleryMediaItems(activity);
            LogHandler.saveLog("End of getting files from your android " +
                    "device and found : " + galleryItems + " gallery items",false);
        });
        updateAndroidThread.start();
        try{
            updateAndroidThread.join();
        }catch (Exception e){
            LogHandler.saveLog("Failed to join update android thread: " + e.getLocalizedMessage(), true );
        }
        MainActivity.isAndroidTimerRunning = false; // end of android timer
        Log.d("Threads","startUpdateAndroidThread finished");
    }

    public static void startThreads(Activity activity){
        startDeleteRedundantAndroidThread();
        startUpdateAndroidThread(activity);
    }

    public static long getMinimumFileSize() {
        List<String[]> files = getSortedAndroidFiles();
        if (files.isEmpty()) return -1;

        long minSize = Long.MAX_VALUE;

        for (String[] file : files) {
            try {
                long fileSize = Long.parseLong(file[4]);
                if (fileSize < minSize) {
                    minSize = fileSize;
                }
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }

        return minSize == Long.MAX_VALUE ? -1 : minSize;
    }

}


