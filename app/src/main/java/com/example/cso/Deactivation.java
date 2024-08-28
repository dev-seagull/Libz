package com.example.cso;

import android.util.Log;
import android.widget.Toast;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Deactivation {

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
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        downloadThread.start();
        try {
            downloadThread.join();
        } catch (InterruptedException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return fileExists[0];
    }

}
