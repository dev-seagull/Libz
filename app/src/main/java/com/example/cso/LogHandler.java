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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LogHandler extends Application {
    static String LOG_DIR_PATH = Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "stash";
    public static String logFileName = "stash_log.txt";

    public static boolean createLogFile() {
        for(int i=0 ; i < 2;i++){
            try {
                System.out.println("try to create file " + i);
                File logDir = new File(LOG_DIR_PATH);
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }
                File logFile = new File(LOG_DIR_PATH + File.separator + logFileName);
                if (!logFile.exists()){
                    try{
                        logFile.createNewFile();
                        if(logFile.exists()){
                            return true;
                        }
                    }catch (SecurityException e){
                        System.out.println("error in creating log file (security)" + e.getLocalizedMessage());
                    }catch (Exception e){
                        System.out.println("error in creating log file (exception)" + e.getLocalizedMessage());
                    }
                }else{
                    System.out.println("Log file exists");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("error in creating log file in existing directory" + e.getLocalizedMessage());
            }
        }
        return false;
    }

    private static boolean logFileContainsError(File logFile){
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
            if (logFile.exists()) {
                try{
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Err")) {
                            return true;
                        }
                    }
                }catch (Exception e){
                    System.out.println("Error in buffer reader log handler" + e.getLocalizedMessage());
                }
            }
        }catch (Exception e){
            System.out.println("Error in log file contains error" + e.getLocalizedMessage());
        }
        return false;
    }

    public static void actionOnLogFile() {
        File logFile = new File(LOG_DIR_PATH + File.separator + logFileName);
        if (logFileContainsError(logFile)) {
            String accessToken = Support.getUserEmailForSupport();
            if (accessToken != null) {
                boolean isSent = Support.sendEmail(accessToken, "Log file has errors", logFile);
                if (isSent){
                    cleanLogFile(logFile);
                }
            }
        }else{
            cleanLogFile(logFile);
        }
    }

    public static void cleanLogFile(File logFile){
        try{
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)))) {
                writer.write("");
                System.out.println("All existing lines removed from the log file: " + logFile.getAbsolutePath());
            }catch (Exception e){
                System.out.println("Error when cleaning log file: " + e.getLocalizedMessage());
            }
        }catch (Exception e){
            System.out.println("Error when cleaning log file: " + e.getLocalizedMessage());
        }
    }


    public static void saveLog(String text, boolean isError) {
        File logDir = new File(LOG_DIR_PATH);
        File logFile = new File(logDir, logFileName);
        List<String> existingLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
            if (logFile.exists()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("line is : " + line);
                    existingLines.add(line);
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timestamp = dateFormat.format(new Date());
                String logEntry;
                if (isError) {
                    logEntry = "Err " + timestamp + " --------- " + text;
                    System.out.println("Err IS SAVED: " + text);
                } else {
                    logEntry = "Log " + timestamp + " --------- " + text;
                    System.out.println("LOG IS SAVED: " + text);
                }
                existingLines.add(Math.min(2, existingLines.size()), logEntry);
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)))) {
                    for (String existingLine : existingLines) {
                        System.out.println("existing line is : " + existingLine);
                        System.out.println("writer is " + writer);
                        writer.write(existingLine);
                        writer.newLine();
                    }
                }catch (Exception e){
                    System.out.println("Failed to write log: " + e.getLocalizedMessage());
                }
            }
        }catch (Exception e){
            System.out.println("Error when saving log: " + e.getLocalizedMessage());
        }
    }

    public static void saveLog(String text) {
        File logDir = new File(LOG_DIR_PATH);
        File logFile = new File(logDir, logFileName);
        List<String> existingLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
            if (logFile.exists()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(" line is : " + line);
                    existingLines.add(line);
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timestamp = dateFormat.format(new Date());
                String logEntry;

                logEntry = "Err " + timestamp + " --------- " + text;
                System.out.println("Err IS SAVED: " + text);
                existingLines.add(logEntry);

                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)))) {
                    for (String existingLine : existingLines) {
                        System.out.println("existing line is : " + existingLine);
                        System.out.println("writer is " + writer);
                        writer.write(existingLine);
                        writer.newLine();
                    }
                }catch (Exception e){
                    System.out.println("Failed to write log: " + e.getLocalizedMessage());
                }
            }
        }catch (Exception e){
            System.out.println("Error when saving log: " + e.getLocalizedMessage());
        }
    }
}