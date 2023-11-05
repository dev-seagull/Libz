package com.example.cso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Duplicate {
    public static boolean isDuplicatedInBackup(ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems
            , File file){
        boolean isDuplicatedInBackup = false;
        String fileHash = "";
        try {
            fileHash = Upload.calculateHash(file).toLowerCase();
        } catch (IOException e) {
            LogHandler.saveLog("Failed to get file hash in isDuplicatedInBackup: " + e.getLocalizedMessage());
        }
        for(BackUpAccountInfo.MediaItem backUpMediaItem: backUpMediaItems){
            String backUpHash = backUpMediaItem.getHash();
            if(fileHash.equals(backUpHash)){
                LogHandler.saveLog("duplicate of " + file.getName() + " was found in back up drive account.");
                isDuplicatedInBackup = true;
                break;
            }
        }
        return isDuplicatedInBackup;
    }

    public static boolean isDuplicatedInPrimary(ArrayList<GooglePhotos.MediaItem> primaryMediaItems
            ,File file){
        boolean isDuplicatedInPrimary = false;
        String fileHash = "";
        try {
            fileHash = Upload.calculateHash(file).toLowerCase();
            LogHandler.saveLog("File hash for " + file.getName() +
                    ": " + fileHash);
        } catch (IOException e) {
            LogHandler.saveLog("Failed to calculate hash in isDuplicatedInPrimary");
        }
        for(GooglePhotos.MediaItem primaryMediaItem: primaryMediaItems){
            String primaryHash = primaryMediaItem.getHash();
            if(fileHash.equals(primaryHash)){
                LogHandler.saveLog(file.getName() + "detected as duplicate in photos and device");
                isDuplicatedInPrimary = true;
                break;
            }
        }
        return isDuplicatedInPrimary;
    }
}
