package com.example.cso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class Media {
    public static boolean isImage(String mimeType) {
        ArrayList<String> imageExtensions = new ArrayList<>(
                Arrays.asList("jpeg", "jpg", "png", "gif", "bmp", "webp")
        );
        return imageExtensions.contains(mimeType.toLowerCase());
    }

    public static boolean isVideo(String mimeType) {
        ArrayList<String> videoExtensions = new ArrayList<>(
                Arrays.asList("mkv", "mp4", "mov", "avi")
        );
        return videoExtensions.contains(mimeType.toLowerCase());
    }

    private static String getExtensionFromMimeType(String mimeType) {
        int lastDotIndex = mimeType.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < mimeType.length() - 1) {
            return mimeType.substring(lastDotIndex + 1).toLowerCase();
        } else {
            return "";
        }
    }

    public static String getMimeType(String fileName){
        int dotIndex = fileName.lastIndexOf(".");
        String mimeType="";
        try{
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                mimeType = fileName.substring(dotIndex + 1);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get mime type: " + e.getLocalizedMessage(), true);
        }

        return mimeType;
    }
}
