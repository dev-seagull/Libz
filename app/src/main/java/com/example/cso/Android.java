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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


public class Android {
    ArrayList<MediaItem> mediaItems;

    public ArrayList<MediaItem> getGalleryMediaItems(Activity activity) {
        ArrayList<MediaItem> androidMediaItems = new ArrayList<>();
        int requestCode =1;
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        while (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(activity.getApplicationContext(),
                        android.Manifest.permission.READ_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }

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
               int columnIndexDateAdded = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
               int columnIndexDateModified = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
               int columnIndexSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
               int columnIndexMemeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);

               while (cursor.moveToNext()) {
                   String mediaItemPath = cursor.getString(columnIndexPath);
                   File mediaItemFile = new File(mediaItemPath);
                   String mediaItemName = mediaItemFile.getName();
                   String mediaItemDateAdded = cursor.getString(columnIndexDateAdded);
                   String mediaItemDateModified = cursor.getString(columnIndexDateModified);
                   Double mediaItemSize = Double.valueOf(cursor.getString(columnIndexSize))  / Double.valueOf(1073741824) ;
                   String mediaItemMemeType = cursor.getString(columnIndexMemeType);
                   MediaItem androidMediaItem = new MediaItem(mediaItemName, mediaItemPath, mediaItemDateAdded,
                           mediaItemDateModified, "", mediaItemSize, mediaItemMemeType);
                   File androidFile = new File(mediaItemPath);
                   if(androidFile.exists()){
                       androidMediaItems.add(androidMediaItem);
                       LogHandler.saveLog("File was detected in android device: " + androidFile.getName());
                   }
               }
               cursor.close();
           }
       }catch (Exception e){
            LogHandler.saveLog("Failed to get gallery files: " + e.getLocalizedMessage());
       }

        try{
            if(androidMediaItems.isEmpty()){
                LogHandler.saveLog("The Gallery was not found, " +
                        "So it's starting to get the files from the file manager");
                androidMediaItems = getFileManagerMediaItems();
            }else{
                System.out.println("it's not empty");
            }
        }catch (Exception e){
            LogHandler.saveLog("Getting device files failed: " + e.getLocalizedMessage());
        }
        LogHandler.saveLog(androidMediaItems.size() + " files were found in your device");
        return androidMediaItems;
    }


    public ArrayList<MediaItem> getFileManagerMediaItems(){
        String[] extensions = {".jpg", ".jpeg", ".png", ".webp",
                ".gif", ".mp4", ".mkv", ".webm"};
        ArrayList<MediaItem> androidMediaItems = new ArrayList<>();

        File rootDirectory = Environment.getExternalStorageDirectory();
        Queue<File> queue = new LinkedList<>();
        queue.add(rootDirectory);
        while (!queue.isEmpty()){
            File currentDirectory = queue.poll();
            File[] currentFiles = new File[0];
            if (currentDirectory != null) {
                currentFiles = currentDirectory.listFiles();
            }else{
                LogHandler.saveLog("Current directory is null");
            }
            if(currentFiles != null){
                for(File currentFile: currentFiles){
                    if(currentFile.isFile()){
                        for(String extension: extensions){
                            if(currentFile.getName().toLowerCase().endsWith(extension)){
                                String mediaItemPath = currentFile.getPath();
                                File mediaItemFile = new File(mediaItemPath);
                                String mediaItemName = currentFile.getName();
                                Double mediaItemSize = Double.valueOf(currentFile.length());
                                String mediaItemMemeType = GooglePhotos.getMemeType(mediaItemFile);
                                MediaItem androidMediaItem = new MediaItem(mediaItemName, mediaItemPath, null,
                                        null, "", mediaItemSize, mediaItemMemeType);
                                if(mediaItemFile.exists()){
                                    androidMediaItems.add(androidMediaItem);
                                    LogHandler.saveLog("File was detected in android device: " + mediaItemFile.getName());
                                }
                            }
                        }
                    } else if(currentFile.isDirectory()){
                        queue.add(currentFile);
                    }
                }
            }
        }
        return androidMediaItems;
    }

    public Android(){
        this.mediaItems = mediaItems;
    }

    public static class MediaItem{
        private String fileName;
        private String filePath;
        private String fileHash;
        private double fileSize;
        private  String date_added;
        private String date_modified;
        private String meme_type;


        public MediaItem(String fileName, String filePath, String date_added, String date_modified,
                         String fileHash, double fileSize, String meme_type) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileHash = fileHash;
            this.fileSize = fileSize;
            this.date_added = date_added;
            this.date_modified = date_modified;
            this.meme_type = meme_type;
        }

        public String getFileName() {return fileName;}

        public String getFilePath() {return filePath;}

        public String getFileHash() {return fileHash;}

        public double getFileSize() {return fileSize;}
        public String getDate_added() {return date_added;}

        public String getMeme_type() {return meme_type;}

        public String getDate_modified() {return date_modified;}

    }
}


