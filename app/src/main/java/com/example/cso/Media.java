package com.example.cso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class Media {
    public static boolean isImage(String mimeType) {
        String extension = getExtensionFromMimeType(mimeType);
        ArrayList<String> imageExtensions = new ArrayList<>(
                Arrays.asList("jpeg", "jpg", "png", "gif", "bmp", "webp")
        );
        return imageExtensions.contains(extension);
    }

    public static boolean isVideo(String mimeType) {
        String extension = getExtensionFromMimeType(mimeType);
        ArrayList<String> videoExtensions = new ArrayList<>(
                Arrays.asList("mkv", "mp4", "mov", "avi")
        );
        return videoExtensions.contains(extension);
    }

    private static String getExtensionFromMimeType(String mimeType) {
        // Extract the part after the last dot
        int lastDotIndex = mimeType.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < mimeType.length() - 1) {
            return mimeType.substring(lastDotIndex + 1).toLowerCase();
        } else {
            return "";
        }
    }

    public static String getMimeType(File file){
        int dotIndex = file.getName().lastIndexOf(".");
        String mimeType="";
        try{
            if (dotIndex >= 0 && dotIndex < file.getName().length() - 1) {
                mimeType = file.getName().substring(dotIndex + 1);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get mime type: " + e.getLocalizedMessage(), true);
        }

        return mimeType;
    }
}
