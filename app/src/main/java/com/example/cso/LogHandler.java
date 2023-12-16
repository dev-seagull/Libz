package com.example.cso;
import android.app.Application;
import android.os.Environment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class LogHandler extends Application {
    static String LOG_DIR_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso";

    public static String CreateLogFile() {
        String filename = "";
        try {
            File logDir = new File(LOG_DIR_PATH);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
//            filename = "cso_log_" + dateFormat.format(new Date()) + ".txt";
            filename = "cso_new_log.txt";
            File logFile = new File(LOG_DIR_PATH + File.separator + filename);

            if (!logFile.exists()){
                System.out.println("Log file is not exists");
                try{
                    logFile.createNewFile();
                    System.out.println("Log file is created ");
                }catch (SecurityException e){
                    System.out.println("error in creating log file (security)" + e.getLocalizedMessage());
                }catch (Exception e){
                    System.out.println("error in creating log file (exception)" + e.getLocalizedMessage());
                }
            }else{
                System.out.println("Log file is exists");
            }
        } catch (Exception e) {
            System.out.println("error in creating log file in existing directory" + e.getLocalizedMessage());
        }
        return filename;
    }
    public static void saveLog(String text, boolean isError) {
        File logDir = new File(LOG_DIR_PATH);
        File logFile = new File(logDir, MainActivity.logFileName);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                List<String> existingLines = Files.readAllLines(logFile.toPath());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timestamp = dateFormat.format(new Date());
                String logEntry;
                if (isError) {
                    logEntry = "err " + timestamp + " --------- " + text;
                } else {
                    logEntry = "log " + timestamp + " --------- " + text;
                }
                existingLines.add(Math.min(2,existingLines.size()), logEntry);
                Files.write(logFile.toPath(), existingLines);

            } catch (IOException e) {
                System.out.println("Error in read all lines or saving logs: " + e.getLocalizedMessage());
            }
        }
    }


    public static void saveLog(String text) {
        File logDir = new File(LOG_DIR_PATH);
        File logFile = new File(logDir, MainActivity.logFileName);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                List<String> existingLines = Files.readAllLines(logFile.toPath());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timestamp = dateFormat.format(new Date());
                String logEntry;
                logEntry = "err " + timestamp + " --------- " + text;
                existingLines.add(Math.min(2,existingLines.size()), logEntry);
                Files.write(logFile.toPath(), existingLines);

            } catch (IOException e) {
                System.out.println("Error in read all lines or saving logs: " + e.getLocalizedMessage());
            }
        }
    }


//    public static void BackupLogFile(PrimaryAccountInfo.Tokens tokens) {
//        String accessToken = tokens.getAccessToken();
//        String refreshToken = tokens.getRefreshToken();
//        try {
//            NetHttpTransport HTTP_TRANSPORT = null;
//            try {
//                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//            } catch (GeneralSecurityException e) {
//                System.out.println("error in uploading log file " + e.getLocalizedMessage());
//            } catch (IOException e) {
//                System.out.println("error in uploading 2 log file " + e.getLocalizedMessage());
//            }
//            final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//
//            HttpRequestInitializer requestInitializer = request -> {
//                request.getHeaders().setAuthorization("Bearer " + accessToken);
//                request.getHeaders().setContentType("application/json");
//            };
//            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
//                    .setApplicationName("cso")
//                    .build();
//            System.out.println("save last log into log file");
//            String filePath = LOG_DIR_PATH + File.separator + MainActivity.logFileName;
//            File file = new File(filePath);
//            com.google.api.services.drive.model.File fileMetadata =
//                    new com.google.api.services.drive.model.File();
//            fileMetadata.setName(file.getName());
//            System.out.println("fileMetadata created");
//            FileContent mediaContent = new FileContent("text/plain", file);
//            service.files().create(fileMetadata, mediaContent)
//                    .setFields("id")
//                    .execute();
//            System.out.println("log file uploaded (last of backup function)");
//        } catch (Exception e) {
//            System.out.println("error in uploading log file " + e.getLocalizedMessage());
//        }
//        System.out.println("lets delete log file");
//    }


}