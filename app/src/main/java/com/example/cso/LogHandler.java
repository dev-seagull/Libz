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
import java.util.Locale;


public class LogHandler extends Application {
    public static String LOG_DIR_PATH = Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "stash";
    public static String logFileName = "stash_log.txt";
    public static File logDir = new File(LOG_DIR_PATH);
    public  static File logFile = new File(logDir, logFileName);

    public static boolean createLogFile() {
        for(int i=0 ; i < 2;i++){
            try {
                System.out.println("Trying to create file for " + i + " time");
                if(ensureLogDirectoryExists()){
                    if (createNewLogFile()) {
                        return true;
                    }
                }else{
                    System.out.println("Log directory doesn't exist.");
                }
            } catch (Exception e) {
                System.out.println("error in creating log file in existing directory" + e.getLocalizedMessage());
            }
        }
        return false;
    }

    private static boolean ensureLogDirectoryExists() {
        if (!logDir.exists()) {
            return logDir.mkdirs();
        }
        return true;
    }

    private static boolean createNewLogFile(){
        if (!logFile.exists()){
            try{
                boolean resultOfCreation = logFile.createNewFile();
                if (resultOfCreation) {
                    return logFile.exists();
                }else {
                    boolean resultOfDeletion = logFile.delete();
                    return createNewLogFile();
                }
            }catch (SecurityException e){
                System.out.println("error in creating log file (security)" + e.getLocalizedMessage());
            }catch (Exception e){
                System.out.println("error in creating log file (exception)" + e.getLocalizedMessage());
            }
        }else{
            try{
                performActionOnLogFile();
                boolean resultOfCreation = logFile.createNewFile();
                if (resultOfCreation) {
                    return logFile.exists();
                }else {
                    boolean resultOfDeletion = logFile.delete();
                    return createNewLogFile();
                }
            }catch (SecurityException e){
                System.out.println("error in creating log file (security)" + e.getLocalizedMessage());
            }catch (Exception e){
                System.out.println("error in creating log file (exception)" + e.getLocalizedMessage());
            }
        }
        return false;
    }

    private static boolean logFileContainsError(){
        if(!logFile.exists()){
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
            return checkForErrorsInLogFile(reader);
        }catch (Exception e){
            System.out.println("Error in log file contains error" + e.getLocalizedMessage());
        }
        return false;
    }

    private static boolean checkForErrorsInLogFile(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Err")) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("Error in buffer reader log handler: " + e.getLocalizedMessage());
        }
        return false;
    }

    public static void performActionOnLogFile() {
        if (logFileContainsError()) {
//            boolean isSent = Support.sendEmail("Log file has errors", logFile);
//            if (isSent){
                logFile.delete();
//            }
        }else{
            logFile.delete();
        }
    }

    public static void saveLog(String text, boolean isError) {
        new Thread(() -> {
            try{
                if (!logFile.exists()) {
                    System.out.println("Log file does not exist.");
                    return;
                }
                List<String> existingLines = readExistingLogLines();
                String logEntry = createLogEntry(text,isError);
                if(logEntry != null){
                    assert existingLines != null;
                    existingLines.add(logEntry);
                    writeLogLines(existingLines);
                }else{
                    System.out.println("Log entry is null.");
                }
            }catch (Exception e){
                System.out.println("Error: " + e.getLocalizedMessage());
            }
        }).start();
    }

    public static void saveLog(String text) {
        saveLog(text, true);
    }

    private static List<String> readExistingLogLines() {
        List<String> existingLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                existingLines.add(line);
            }
        } catch (Exception e) {
            System.out.println("Error when reading log file: " + e.getLocalizedMessage());
            return null;
        }
        return existingLines;
    }

    private static String createLogEntry(String text, boolean isError){
        String logEntry = null;
        try{
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            String timestamp = dateFormat.format(new Date());
            String logType;
            if (isError) {
                logType = "Err";
            } else {
                logType = "Log";
            }
            logEntry = logType + " " + timestamp + " --------- " + text;
        }catch (Exception e){
            System.out.println("Failed to create log entry.");
        }
        if (isError) {
            System.out.println("Err IS SAVED: " + text);
        } else {
            System.out.println("LOG IS SAVED: " + text);
        }
        return logEntry;
    }

    private static void writeLogLines(List<String> existingLines){
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)))) {
            for (String existingLine : existingLines) {
                writer.write(existingLine);
                writer.newLine();
            }
        }catch (Exception e){
            System.out.println("Failed to write log: " + e.getLocalizedMessage());
        }
    }
}