package com.example.cso;

import android.widget.Toast;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Deactivation {

    public static boolean isDeactivationFileExists() {
        String fileName = "deActive_"+MainActivity.androidUniqueDeviceIdentifier + ".json";
        boolean[] isFileExists = {false};
        Thread downloadThread = new Thread(() -> {
            try {
                String accessToken = MainActivity.googleCloud.updateAccessToken(Support.getSupportRefreshToken()).getAccessToken();
                Drive service = GoogleDrive.initializeDrive(accessToken);

                String query = "name = '" + fileName + "'";
                FileList result = service.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id, name)")
                        .execute();

                if (!result.getFiles().isEmpty()) {
                    LogHandler.saveLog("File not found: " + fileName, true);
                    isFileExists[0] = true;
                }
            }catch (Exception e) {
                LogHandler.saveLog("Failed to check file existing : " + e.getLocalizedMessage(), true);
            }
        });
        downloadThread.start();
        try {
            downloadThread.join();
        } catch (InterruptedException e) {
            LogHandler.saveLog("Failed to join download thread: " + e.getLocalizedMessage(), true);
        }
        return isFileExists[0];
    }

    public static void checkDeActivated() {
        if (Deactivation.isDeactivationFileExists()){
            MainActivity.activity.runOnUiThread(() -> Toast.makeText(MainActivity.activity,
                    "you're deActivated, Call support", Toast.LENGTH_SHORT).show());
            MainActivity.activity.finish();
        }
    }

}
