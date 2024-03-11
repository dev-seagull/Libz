package com.example.cso;

import android.app.Activity;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Android {
    static int[] galleryItems = {0};
    public static int getGalleryMediaItems(Activity activity) {
        galleryItems[0] = 0;

        LogHandler.saveLog("Started to get android files from your device.", false);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Callable<Integer> backgroundTask = () -> {
            try (Cursor cursor = createAndroidCursor(activity)) {
                if (cursor != null) {
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

            try{
                if(galleryItems[0] == 0){
                    galleryItems[0] = getFileManagerMediaItems();
                }
            }catch (Exception e){
                LogHandler.saveLog("Getting device files failed: " + e.getLocalizedMessage(), true);
            }
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
        LogHandler.saveLog(result
                + " files were found in your device",false);

        return result;
    }


    public static int getFileManagerMediaItems(){
        LogHandler.saveLog("Did not found any files in your gallery, so " +
                "it started to get files from file manager." , false);
        String[] extensions = {".jpg", ".jpeg", ".png", ".webp",
                ".gif", ".mp4", ".mkv", ".webm"};
        int fileManagerItems = 0;
        File rootDirectory = Environment.getExternalStorageDirectory();
           Queue<File> queue = new LinkedList<>();
        try{
            queue.add(rootDirectory);
            while (!queue.isEmpty()){
                File currentDirectory = queue.poll();
                File[] currentFiles = new File[0];
                if (currentDirectory != null) {
                    currentFiles = currentDirectory.listFiles();
                }
                if(currentFiles != null){
                    for(File currentFile: currentFiles){
                        fileManagerItems = processFileManagerFile(currentFile, extensions, queue);
                    }
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get files from file manager: " + e.getLocalizedMessage(), true);
        }

        return fileManagerItems;
    }


    private static boolean hasValidExtension(File file, String[] extensions) {
        for (String extension : extensions) {
            if (file.getName().toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }


    private static int processFileManagerFile(File currentFile, String[] extensions, Queue<File> queue){
        int fileManagerItems = 0;
        if(currentFile.isFile() && hasValidExtension(currentFile, extensions)){
            String mediaItemPath = currentFile.getPath();
            File mediaItemFile = new File(mediaItemPath);
            String mediaItemName = currentFile.getName();
            Double mediaItemSize = currentFile.length() / (Math.pow(10, 6));
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
            String mediaItemDateModified = dateFormat.format(new Date(mediaItemFile.lastModified()));
            String mediaItemMimeType = GooglePhotos.getMemeType(mediaItemFile);
            if(mediaItemFile.exists()){
                fileManagerItems++;
                if(!MainActivity.dbHelper.existsInAndroidWithoutHash(mediaItemPath, MainActivity.androidDeviceName,
                        mediaItemDateModified, mediaItemSize)){
                    processFileManagerItem(mediaItemFile, mediaItemName, mediaItemPath, mediaItemSize,
                            mediaItemDateModified, mediaItemMimeType);
                }
            }
        }else if(currentFile.isDirectory()){
            queue.add(currentFile);
        }
        return fileManagerItems;
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
                MainActivity.dbHelper.insertAssetData(mediaItemHash);
        if(lastInsertedId != -1){

            MainActivity.dbHelper.insertIntoAndroidTable(lastInsertedId,mediaItemName, mediaItemPath, MainActivity.androidDeviceName,
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
            if(!MainActivity.dbHelper.existsInAndroidWithoutHash(mediaItemPath, MainActivity.androidDeviceName,
                    mediaItemDateModified, mediaItemSize)){
                String fileHash = "";
                try {
                    fileHash = Hash.calculateHash(androidFile);

                } catch (Exception e) {
                    LogHandler.saveLog("Failed to calculate hash: " + e.getLocalizedMessage(), true);
                }
                long lastInsertedId =
                        MainActivity.dbHelper.insertAssetData(fileHash);
                if(lastInsertedId != -1){
                    MainActivity.dbHelper.insertIntoAndroidTable(lastInsertedId,
                            mediaItemName, mediaItemPath, MainActivity.androidDeviceName,
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

}


