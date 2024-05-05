package com.example.cso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class Media {
    public static boolean isImage(String mimeType){
        mimeType = mimeType.toLowerCase();
        ArrayList<String> imageExtensions = new ArrayList<>(
                Arrays.asList("jpeg", "jpg", "png", "gif", "bmp")
        );
        return imageExtensions.contains(mimeType);
    }

    public static boolean isVideo(String mimeType){
        mimeType = mimeType.toLowerCase();
        ArrayList<String> videoExtensions = new ArrayList<>(
                Arrays.asList("mkv", "mp4")
        );
        return videoExtensions.contains(mimeType);
    }

    public static String getMimeType(File file){
        int dotIndex = file.getName().lastIndexOf(".");
        String memeType="";
        try{
            if (dotIndex >= 0 && dotIndex < file.getName().length() - 1) {
                memeType = file.getName().substring(dotIndex + 1);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get mime type: " + e.getLocalizedMessage(), true);
        }

        return memeType;
    }
}
