package com.example.cso;

import android.util.Log;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

public class Deactivation {
    private static String TAG = "Deactivation";

    public static boolean isDeactivationFileExists() {
        String fileName = "deActive_" + MainActivity.androidUniqueDeviceIdentifier + ".json";
        boolean[] fileExists = {false};

        Thread downloadThread = new Thread(() -> {
            try {
                String accessToken = GoogleCloud.updateAccessToken(Support.getSupportRefreshToken()).getAccessToken();
                Drive service = GoogleDrive.initializeDrive(accessToken);

                String query = "name = '" + fileName + "'";
                FileList result = service.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id, name)")
                        .execute();

                if (!result.getFiles().isEmpty()) {
                    Log.d("Deactivate","Deactivation file found: " +fileName );
                    fileExists[0] = true;
                }else{
                    Log.d("Deactivate","Deactivation file not found: " +fileName );
                }
            }catch (Exception e) {
                LogHandler.recordException(e,TAG);
            }
        });
        downloadThread.start();
        try {
            downloadThread.join();
        } catch (InterruptedException e) {
            LogHandler.recordException(e,TAG);
        }
        return fileExists[0];
    }

}
