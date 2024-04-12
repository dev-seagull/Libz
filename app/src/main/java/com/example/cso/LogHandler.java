package com.example.cso;

import android.app.Application;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LogHandler extends Application {
    static String LOG_DIR_PATH = Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "stash";

    public static boolean CreateLogFile() {
        boolean hasError = false;
        try {
            File logDir = new File(LOG_DIR_PATH);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            //filename = "stash_log_" + dateFormat.format(new Date()) + ".txt";
            File logFile = new File(LOG_DIR_PATH + File.separator + MainActivity.logFileName);
            if (!logFile.exists()){
                try{
                    logFile.createNewFile();
                }catch (SecurityException e){
                    System.out.println("error in creating log file (security)" + e.getLocalizedMessage());
                }catch (Exception e){
                    System.out.println("error in creating log file (exception)" + e.getLocalizedMessage());
                }
            }else{
                System.out.println("Log file exists");
            }

            File oldLogFile = new File(logDir, MainActivity.logFileName);
            if (logFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(oldLogFile)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("err")) {
                            hasError = true;
                            break;
                        }
                    }
                }catch (Exception e){
                    System.out.println("Error in buffer reader log handler" + e.getLocalizedMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("error in creating log file in existing directory" + e.getLocalizedMessage());
        }
        return hasError;
    }

    public static void actionOnLogFile(boolean hasError, File oldLogFile) {
        if (hasError // || true
        ) {
            String accessToken = Support.getUserEmailForSupport();
            if (accessToken == null) {
                System.out.println("No email found for support");
                return;
            }
            Support.sendEmail(accessToken, "Log file has errors", oldLogFile);
        } else {
            //truncate the file
        }
    }


    public static void saveLog(String text, boolean isError) {
        File logDir = new File(LOG_DIR_PATH);
        File logFile = new File(logDir, MainActivity.logFileName);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                // Read existing lines
                List<String> existingLines = new ArrayList<>();
                if (logFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            existingLines.add(line);
                        }
                    }catch (Exception e){
                        System.out.println("Error in buffer reader log handler" + e.getLocalizedMessage());
                    }
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timestamp = dateFormat.format(new Date());
                String logEntry;
                if (isError) {
                    MainActivity.errorCounter++;
                    logEntry = "err " + timestamp + " --------- " + text;
                    System.out.println("err IS SAVED: " + text);
                } else {
                    logEntry = "log " + timestamp + " --------- " + text;
                    System.out.println("LOG IS SAVED: " + text);
                }
                existingLines.add(Math.min(2, existingLines.size()), logEntry);

                // Write back to the file
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)))) {
                    for (String line : existingLines) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
                catch (Exception e){
                    System.out.println("Error in buffer writer log handler" + e.getLocalizedMessage());
                }
            } catch (Exception e) {
                System.out.println("Error in reading all lines or saving logs: " + e.getMessage());
            }
        }
    }

    public static void saveLog(String text) {
        File logDir = new File(LOG_DIR_PATH);
        File logFile = new File(logDir, MainActivity.logFileName);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                // Read existing lines
                List<String> existingLines = new ArrayList<>();
                if (logFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            existingLines.add(line);
                        }
                    }catch (Exception e){
                        System.out.println("Error in buffer reader log handler" + e.getLocalizedMessage());
                    }
                }

                // Append new log entry
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timestamp = dateFormat.format(new Date());
                String logEntry = "err " + timestamp + " --------- " + text;
                existingLines.add(Math.min(2, existingLines.size()), logEntry);

                // Write back to the file
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)))) {
                    for (String line : existingLines) {
                        writer.write(line);
                        writer.newLine();
                    }
                }catch (Exception e){
                    System.out.println("Error in buffer writer log handler" + e.getLocalizedMessage() );
                }

                MainActivity.errorCounter++;
                System.out.println("Err IS SAVED: " + text);
            } catch (Exception e) {
                System.out.println("Error in reading all lines or saving logs: " + e.getMessage());
            }
        }
    }

    public static void deleteLogFile(){
        File logFile = new File(LOG_DIR_PATH + File.separator + MainActivity.logFileName);
        if(logFile.exists()){
            logFile.delete();
        }
    }
}