package com.example.cso;

import android.app.Activity;
import android.database.Cursor;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;


public class Android {
    ArrayList<MediaItem> mediaItems;

    public static ArrayList<MediaItem> getGalleryMediaItems(Activity activity) {
        ArrayList<MediaItem> androidMediaItems = new ArrayList<>();

        String[] projection = {
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
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
            int columnIndexName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE);
            int columnIndexSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int columnIndexDateAdded = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
            int columnIndexDateModified = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int columnIndexMimeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);

            while (cursor.moveToNext()) {
                String mediaItemPath = cursor.getString(columnIndexPath);
                String mediaItemName = cursor.getString(columnIndexName);
                double mediaItemSize = Double.valueOf(cursor.getString(columnIndexSize));
                String mediaItemDateAdded = cursor.getString(columnIndexDateAdded);
                String mediaItemDateModified = cursor.getString(columnIndexDateModified);
                try {
                    String mediaItemHash = MainActivity.calculateHash(mediaItemPath, activity);
                } catch (IOException e) {
                    Toast.makeText(activity,"Calculating hash failed: " + e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                    throw new RuntimeException(e);
                }
                String mediaItemMimeType = cursor.getString(columnIndexMimeType);
                MediaItem androidMediaItem = new MediaItem(mediaItemName, mediaItemPath,
                        mediaItemDateAdded, mediaItemDateModified, "", mediaItemSize,mediaItemMimeType);
                androidMediaItems.add(androidMediaItem);
            }
            cursor.close();
        }

        return androidMediaItems;
    }

    public Android(ArrayList<MediaItem> mediaItems){
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


