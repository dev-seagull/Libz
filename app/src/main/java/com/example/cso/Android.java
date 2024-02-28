package com.example.cso;

import android.app.Activity;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.os.StatFs;

import com.google.gson.JsonObject;

public class Android {

    public static int getGalleryMediaItems(Activity activity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final int[] galleryItems = {0};
        Callable<Integer> backgroundTask = () -> {
            String[] projection = {
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.MIME_TYPE
            };

            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=? OR " +
                    MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";
            String[] selectionArgs = {String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)};
            String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
            Cursor cursor = null;
            int columnIndexPath = 0;
            int columnIndexSize = 0;
            int columnIndexMemeType = 0;
            try{
                cursor = activity.getContentResolver().query(
                        MediaStore.Files.getContentUri("external"),
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                );
                if (cursor != null) {
                    columnIndexPath = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    columnIndexSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    columnIndexMemeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to create cursor: " + e.getLocalizedMessage());
            }

            try{
                while (cursor.moveToNext() && cursor!=null) {
                    String mediaItemPath = cursor.getString(columnIndexPath);
                    File mediaItemFile = new File(mediaItemPath);
                    String mediaItemName = mediaItemFile.getName();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy");
                    String mediaItemDateModified = dateFormat.format(new Date(mediaItemFile.lastModified()));
                    Double mediaItemSize = Double.valueOf(cursor.getString(columnIndexSize)) / (Math.pow(10, 6));
                    String mediaItemMemeType = cursor.getString(columnIndexMemeType);
                    File androidFile = new File(mediaItemPath);
                    if(androidFile.exists()){
                        galleryItems[0]++;
                        if(!MainActivity.dbHelper.existsInAndroidWithoutHash(mediaItemPath, MainActivity.androidDeviceName,
                                mediaItemDateModified, mediaItemSize)){
                            String fileHash = "";
                            try {
                                fileHash = Hash.calculateHash(androidFile);

                            } catch (Exception e) {
                                LogHandler.saveLog("Failed to calculate hash: " + e.getLocalizedMessage());
                            }
                            long lastInsertedId =
                                    MainActivity.dbHelper.insertAssetData(fileHash);
                            if(lastInsertedId != -1){
                                MainActivity.dbHelper.insertIntoAndroidTable(lastInsertedId,
                                        mediaItemName, mediaItemPath, MainActivity.androidDeviceName,
                                        fileHash,mediaItemSize, mediaItemDateModified,mediaItemMemeType);
                                LogHandler.saveLog("File was detected in android device: " + mediaItemFile.getName(),false);
                            }else{
                                LogHandler.saveLog("Failed to insert file into android table: " + mediaItemFile.getName());
                            }
                        }
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to get gallery files: " + e.getLocalizedMessage());
            }finally {
                if(cursor != null){
                    cursor.close();
                }
            }
            try{
                if(galleryItems[0] == 0){
                    LogHandler.saveLog("The Gallery was not found, " +
                            "So it's starting to get the files from the file manager",false);
                    galleryItems[0] = getFileManagerMediaItems();
                }
            }catch (Exception e){
                LogHandler.saveLog("Getting device files failed: " + e.getLocalizedMessage());
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
            result = future.get();
        } catch (Exception e) {
            LogHandler.saveLog("error when downloading user profile : " + e.getLocalizedMessage());
        }


        LogHandler.saveLog(String.valueOf(result)
                + " files were found in your device",false);
        return result;
    }


    public static int getFileManagerMediaItems(){
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
                        if(currentFile.isFile()){
                            for(String extension: extensions){
                                if(currentFile.getName().toLowerCase().endsWith(extension)){
                                    String mediaItemPath = currentFile.getPath();
                                    File mediaItemFile = new File(mediaItemPath);
                                    String mediaItemName = currentFile.getName();
                                    Double mediaItemSize = Double.valueOf(currentFile.length() / (Math.pow(10,6)));
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy");
                                    String mediaItemDateModified = dateFormat.format(new Date(mediaItemFile.lastModified()));
                                    String mediaItemMemeType = GooglePhotos.getMemeType(mediaItemFile);
                                    if(mediaItemFile.exists()){
                                        fileManagerItems++;
                                        if(!MainActivity.dbHelper.existsInAndroidWithoutHash(mediaItemPath, MainActivity.androidDeviceName,
                                                mediaItemDateModified, mediaItemSize)){
                                            String mediaItemHash = "";
                                            try {
                                                mediaItemHash = Hash.calculateHash(mediaItemFile);
                                            } catch (Exception e) {
                                                LogHandler.saveLog("Failed to calculate hash in file manager: " + e.getLocalizedMessage());
                                            }
                                            long lastInsertedId =
                                                    MainActivity.dbHelper.insertAssetData(mediaItemHash);
                                            if(lastInsertedId != -1){

                                                MainActivity.dbHelper.insertIntoAndroidTable(lastInsertedId,mediaItemName, mediaItemPath, MainActivity.androidDeviceName,
                                                        mediaItemHash,mediaItemSize, mediaItemDateModified,mediaItemMemeType);
                                                LogHandler.saveLog("File was detected in file manager: " + mediaItemFile.getName(),false);
                                            }else{
                                                LogHandler.saveLog("Failed to insert file into android table in file manager : " + mediaItemFile.getName());
                                            }
                                        }
                                    }
                                }
                            }
                        }else if(currentFile.isDirectory()){
                            queue.add(currentFile);
                        }
                    }
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("failed to get files from file manager: " + e.getLocalizedMessage());
        }
        return fileManagerItems;
    }

}


