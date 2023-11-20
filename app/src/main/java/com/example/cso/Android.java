package com.example.cso;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class Android {

    public static void getGalleryMediaItems(Activity activity) {
        int galleryItems = 0;

        try{
           String[] projection = {
                   MediaStore.Files.FileColumns.DATA,
                   MediaStore.Files.FileColumns.DATE_ADDED,
                   MediaStore.Files.FileColumns.DATE_MODIFIED,
                   MediaStore.Files.FileColumns.SIZE,
                   MediaStore.Files.FileColumns.MIME_TYPE
           };

           String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=? OR " +
                   MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";
           String[] selectionArgs = {String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                   String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)};
           String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
           Cursor cursor = activity.getContentResolver().query(
                   MediaStore.Files.getContentUri("external"),
                   projection,
                   selection,
                   selectionArgs,
                   sortOrder
           );

           if (cursor != null) {
               int columnIndexPath = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
               int columnIndexSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
               int columnIndexMemeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);

               while (cursor.moveToNext()) {
                   String mediaItemPath = cursor.getString(columnIndexPath);
                   File mediaItemFile = new File(mediaItemPath);
                   String mediaItemName = mediaItemFile.getName();
                   SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy");
                   String mediaItemDateModified = dateFormat.format(new Date(mediaItemFile.lastModified()));
                   Double mediaItemSize = Double.valueOf(cursor.getString(columnIndexSize)) / (Math.pow(10, 6));
                   String mediaItemMemeType = cursor.getString(columnIndexMemeType);
                   File androidFile = new File(mediaItemPath);
                   if(androidFile.exists()){
                       galleryItems++;
                       long lastInsertedId =
                               MainActivity.dbHelper.insertAssetData(mediaItemName,"ANDROID" ,"");
                       MainActivity.dbHelper.insertIntoAndroidTable(lastInsertedId,mediaItemName, mediaItemPath, MainActivity.androidDeviceName,
                                mediaItemSize, "",mediaItemDateModified,mediaItemMemeType);
                       LogHandler.saveLog("File was detected in android device: " + androidFile.getName(),false);
                   }
               }
               cursor.close();
           }
       }catch (Exception e){
            LogHandler.saveLog("Failed to get gallery files: " + e.getLocalizedMessage());
       }

        try{
            if(galleryItems == 0){
                LogHandler.saveLog("The Gallery was not found, " +
                        "So it's starting to get the files from the file manager",false);
                galleryItems = getFileManagerMediaItems();
            }
        }catch (Exception e){
            LogHandler.saveLog("Getting device files failed: " + e.getLocalizedMessage());
        }
        LogHandler.saveLog(String.valueOf(galleryItems)
                + " files were found in your device",false);
    }


    public static int getFileManagerMediaItems(){
        String[] extensions = {".jpg", ".jpeg", ".png", ".webp",
                ".gif", ".mp4", ".mkv", ".webm"};
        int fileManagerItems = 0;

        File rootDirectory = Environment.getExternalStorageDirectory();
        Queue<File> queue = new LinkedList<>();
        queue.add(rootDirectory);
        while (!queue.isEmpty()){
            File currentDirectory = queue.poll();
            File[] currentFiles = new File[0];
            if (currentDirectory != null) {
                currentFiles = currentDirectory.listFiles();
            }else{
                LogHandler.saveLog("Current directory is null",false);
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
                                    long lastInsertedId =
                                            MainActivity.dbHelper.insertAssetData(mediaItemName,"ANDROID" ,"");
                                    MainActivity.dbHelper.insertIntoAndroidTable(lastInsertedId,mediaItemName, mediaItemPath, MainActivity.androidDeviceName,
                                            mediaItemSize, "",mediaItemDateModified,mediaItemMemeType);
                                    LogHandler.saveLog("File was detected in android device: " + mediaItemFile.getName(),false);
                                }
                            }
                        }
                    } else if(currentFile.isDirectory()){
                        queue.add(currentFile);
                    }
                }
            }
        }
        return fileManagerItems;
    }
    public Android(){
    }


}


