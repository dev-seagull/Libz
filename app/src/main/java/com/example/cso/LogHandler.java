package com.example.cso;

import android.app.Activity;
import android.app.Application;
import android.os.Environment;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LogHandler extends Application {
    static String LOG_DIR_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso";

    public static void CreateLogFile(Activity activity) {
        try {
            File logDir = new File(LOG_DIR_PATH);
            if (!logDir.exists()) {
                if (logDir.mkdirs()) {
                    System.out.println("Log directory created");
                } else {
                    System.err.println("Failed to create the log directory");
                }
            } else {
                System.out.println("Directory for log is exists");
            }
            File logFile = new File(logDir + "log.txt");
            if (logFile.exists()) {
                System.out.println("Log directory was created with the path of " + logFile.getPath());
            } else {
                File newLogFile = new File(logFile.getPath());
                System.out.println("Created log file with the path of: " + newLogFile.getPath());;
            }
        } catch (Exception e) {
            System.out.println("error in creating log file in existing directory" + e.getLocalizedMessage());
        }
    }

    public static void saveLog(String text) {
        File logDir = new File(LOG_DIR_PATH);
        File logFile = new File(logDir, "LOG_FILE_NAME.TXT");
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
                System.out.println("error in uploading log file " + e.getLocalizedMessage());
            } catch (IOException e) {
                System.out.println("error in uploading 2 log file " + e.getLocalizedMessage());
            }
            final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

            HttpRequestInitializer requestInitializer = request -> {
                request.getHeaders().setAuthorization("Bearer " + accessToken);
                request.getHeaders().setContentType("application/json");
            };
            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                    .setApplicationName("cso")
                    .build();
//            SaveLog("Uploading log file to backup drive");
            System.out.println("save last log into log file");
            String filePath = LOG_DIR_PATH + "/" +"LOG_FILE_NAME";
            File file = new File(filePath);
            com.google.api.services.drive.model.File fileMetadata =
                    new com.google.api.services.drive.model.File();
            fileMetadata.setName(file.getName());
            System.out.println("fileMetadata created");
            FileContent mediaContent = new FileContent("text/plain", file);
            service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("log file uploaded (last of backup function)");
        } catch (Exception e) {
            System.out.println("error in uploading log file " + e.getLocalizedMessage());
        }
        System.out.println("lets delete log file");
    }
}