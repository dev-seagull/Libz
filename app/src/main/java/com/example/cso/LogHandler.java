package com.example.cso;

import android.os.Environment;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogHandler {
    private static final String LOG_FILE_NAME = "Log.txt";
    private static final String LOG_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/CSO/";

    public static void CreateLogFile() {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                if (logDir.mkdirs()) {
                    System.out.println("Log directory created");
                } else {
                    System.err.println("Failed to create the log directory");
                }
            } else {
                System.out.println("Directory for log is exists");
            }

            File logFile = new File(logDir, LOG_FILE_NAME);
            if (logFile.exists()) {
                System.out.println("can create " + logFile.getPath());
            } else {
                System.out.println("cant create " + logFile.getPath());
                File newLogFile = new File(logFile.getPath());
            }


        } catch (Exception e) {
            System.out.println("error in hereee" + e.getLocalizedMessage());
        }
    }

    public static void SaveLog(String text) {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists() && !logDir.mkdirs()) {
            System.err.println("Failed to create the log directory");
            return; // Stop if directory creation fails
        }

        File logFile = new File(logDir, LOG_FILE_NAME);

        try (FileWriter fileWriter = new FileWriter(logFile, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());

            String logEntry = timestamp + " --------- " + text;
            bufferedWriter.write(logEntry);
            bufferedWriter.newLine();
        } catch (IOException e) {
            System.err.println("Error in saving logs: " + e.getLocalizedMessage());
        }
    }

    public static void BackupLogFile(PrimaryAccountInfo.Tokens tokens) {
        String accessToken = tokens.getAccessToken();
        String refreshToken = tokens.getRefreshToken();
        try {
            NetHttpTransport HTTP_TRANSPORT = null;
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            } catch (GeneralSecurityException e) {
                //Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                //Toast.makeText(activity, "Uploading failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

            HttpRequestInitializer requestInitializer = request -> {
                request.getHeaders().setAuthorization("Bearer " + accessToken);
                request.getHeaders().setContentType("application/json");
            };

            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                    .setApplicationName("cso")
                    .build();
            File file = new File(LOG_DIR);
            com.google.api.services.drive.model.File fileMetadata =
                    new com.google.api.services.drive.model.File();
            fileMetadata.setName(file.getName());
            FileContent mediaContent = new FileContent("text/plain", file);
            service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();

        } catch (Exception e) {
            System.out.println("error in uploading log file " + e.getLocalizedMessage());
        }
    }
}
