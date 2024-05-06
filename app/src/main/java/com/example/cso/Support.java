package com.example.cso;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Support {

    public static String supportEmail = "Alireza.Hoseini20031382@gmail.com";
    public static boolean sendEmail(String accessToken, String message, File attachment) {
        final String[] accessTokens = {accessToken};
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> uploadTask = () -> {
            boolean isSent = false;
            try {
                URL url = new URL("https://gmail.googleapis.com/gmail/v1/users/me/messages/send");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer "+accessTokens[0]);
                connection.setDoOutput(true);
                StringBuilder emailContentBuilder = new StringBuilder();
                emailContentBuilder.append("To: ").append(supportEmail).append("\r\n");
                emailContentBuilder.append("Subject: ").append("LogHandler").append("\r\n");
                emailContentBuilder.append("Content-Type: multipart/mixed; boundary=boundary123\r\n");
                emailContentBuilder.append("\r\n");
                emailContentBuilder.append("--boundary123\r\n");
                emailContentBuilder.append("Content-Type: text/plain; charset=utf-8\r\n");
                emailContentBuilder.append("\r\n");
                emailContentBuilder.append(message).append("\r\n");
                if (attachment != null) {
                    emailContentBuilder.append("--boundary123\r\n");
                    emailContentBuilder.append("Content-Type: application/octet-stream\r\n");
                    emailContentBuilder.append("Content-Disposition: attachment; filename=").append(attachment.getName()).append("\r\n");
                    emailContentBuilder.append("\r\n");
                    try (BufferedReader reader = new BufferedReader(new FileReader(attachment))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            emailContentBuilder.append(line).append("\r\n");
                        }
                    }
                }
                emailContentBuilder.append("--boundary123--\r\n");
                String emailContent = emailContentBuilder.toString();
                String payload = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    payload = "{\"raw\": \"" + Base64.getUrlEncoder().encodeToString(emailContent.getBytes()) + "\"}";
                }
                try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                    outputStream.writeBytes(payload);
                }
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    isSent = true;
                }
                System.out.println("Response Code of sending email: " + responseCode);
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    System.out.println("Response of sending Email: " + response);
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return isSent;
        };
        Future<Boolean> future = executor.submit(uploadTask);
        boolean is_ok = false;
        try{
            is_ok = future.get();
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return is_ok;
    }


    public static String getUserEmailForSupport(){
        List<String []> accountRows = DBHelper.getAccounts(new String[]{"type","refreshToken"});
        //should be inside an executor service
        for (String[] row : accountRows) {
            if (row[0].equals("backup")) {
                return row[1];
            }
        }
        return null;
    }
}

