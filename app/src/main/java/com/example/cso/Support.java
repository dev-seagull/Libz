package com.example.cso;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Support {
    public static String supportEmail = MainActivity.activity.getResources().getString(R.string.supportEmail);
    public static boolean sendEmail(String message, File attachment) {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Callable<Boolean> uploadTask = () -> {
//            try {
//                String refreshToken = getSupportRefreshToken();
//                String accessToken = requestAccessToken(refreshToken).getAccessToken();
//                System.out.println("accessTOken"  +accessToken);
//
//                String emailContent = createEmailContent(message,attachment);
//                if(emailContent != null){
//                    return sendEmailRequest(emailContent, accessToken);
//                }else{
//                    System.out.println("Support email content is null");
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return false;
//        };
//        Future<Boolean> future = executor.submit(uploadTask);
//        boolean isSentFuture = false;
//        try{
//            isSentFuture = future.get();
//        }catch (Exception e){
//            System.out.println(e.getLocalizedMessage());
//        }
//        return isSentFuture;
        return true;
    }

    public static boolean sendEmail(String message) {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Callable<Boolean> uploadTask = () -> {
//            try {
//                String refreshToken = getSupportRefreshToken();
//                String accessToken = requestAccessToken(refreshToken).getAccessToken();
//                String emailContent = createEmailContent(message,null);
//                if(emailContent != null){
//                    return sendEmailRequest(emailContent, accessToken);
//                }else{
//                    System.out.println("Support email content is null");
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return false;
//        };
//        Future<Boolean> future = executor.submit(uploadTask);
//        boolean isSentFuture = false;
//        try{
//            isSentFuture = future.get();
//        }catch (Exception e){
//            System.out.println(e.getLocalizedMessage());
//        }
//        return isSentFuture;
        return true;
    }

    private static String getSupportRefreshToken(){
        return MainActivity.activity.getResources().getString(R.string.supportRefreshToken);
    }

    private static String createEmailContent(String message, File attachment){
        StringBuilder emailContentBuilder = new StringBuilder();
        try{
            emailContentBuilder.append("To: ").append(supportEmail).append("\r\n");
            emailContentBuilder.append("Subject: ").append("LogHandler from " + MainActivity.androidUniqueDeviceIdentifier).append("\r\n");
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
                emailContentBuilder.append(readFileContent(attachment)).append("\r\n");
            }
            emailContentBuilder.append("--boundary123--\r\n");
        }catch (Exception e){
            System.out.println("Failed to create email content: " + e.getLocalizedMessage());
        }
        return emailContentBuilder.toString();
    }

    public void sendQueryChange(String sqlQuery, Object[] objects) {
//        StringBuilder completeQuery = new StringBuilder();
//        int paramIndex = 0;
//
//        for (int i = 0; i < sqlQuery.length(); i++) {
//            char c = sqlQuery.charAt(i);
//            if (c == '?') {
//                if (paramIndex < objects.length) {
//                    Object value = objects[paramIndex++];
//                    if (value instanceof String) {
//                        completeQuery.append("'").append(value).append("'");
//                    } else if (value == null) {
//                        completeQuery.append("NULL");
//                    } else {
//                        completeQuery.append(value);
//                    }
//                }
//            } else {
//                completeQuery.append(c);
//            }
//        }
//        Support.sendEmail(completeQuery.toString());
    }

    private static String readFileContent(File attachment){
//        StringBuilder fileContent = new StringBuilder();
//        try (BufferedReader reader = new BufferedReader(new FileReader(attachment))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                fileContent.append(line).append("\r\n");
//            }
//        }catch (Exception e) {
//            System.out.println("Failed to read file content: "  +e.getLocalizedMessage());
//        }
//        return fileContent.toString();
        return "";
    }

    private static boolean sendEmailRequest(String emailContent, String accessToken){
        boolean isSent = false;
        try{
            URL url = new URL("https://gmail.googleapis.com/gmail/v1/users/me/messages/send");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer "+accessToken);
            connection.setDoOutput(true);
            String payload = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                payload = "{\"raw\": \"" + Base64.getUrlEncoder().encodeToString(emailContent.getBytes()) + "\"}";
            }
            int responseCode = 0;
            if(payload != null){
                try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                    outputStream.writeBytes(payload);
                }
//                try (BufferedReader in = new BufferedReader(new InputStreamReader(
//                        responseCode >= 200 && responseCode < 300
//                                ? connection.getInputStream()
//                                : connection.getErrorStream()))) {
//                    String inputLine;
//                    StringBuilder response = new StringBuilder();
//                    while ((inputLine = in.readLine()) != null) {
//                        response.append(inputLine);
//                    }
//                    System.out.println("Response Body: " + response.toString());
//                }
                if(responseCode != 200){
                    System.out.println("Response of sending email: " + connection.getResponseMessage());
                }
            }else{
                System.out.println("Payload is null");
            }
            System.out.println("response "  + connection.getResponseMessage());
            connection.disconnect();
            if(responseCode == 200){
                System.out.println("Response code of sending email is 200");
                return true;
            }
        }catch (Exception e){
            System.out.println("Failed to send email request: " + e.getLocalizedMessage());
        }finally {
            return isSent;
        }
    }

    private static GoogleCloud.Tokens requestAccessToken(String refreshToken){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<GoogleCloud.Tokens> backgroundTokensTask = () -> {
            String accessToken = null;
            try {
                URL googleAPITokenUrl = new URL("https://www.googleapis.com/oauth2/v4/token");
                HttpURLConnection httpURLConnection = (HttpURLConnection) googleAPITokenUrl.openConnection();
                String clientId = MainActivity.activity.getResources().getString(R.string.client_id);
                String clientSecret = MainActivity.activity.getString(R.string.client_secret);
                String requestBody = "&client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&refresh_token= " + refreshToken +
                        "&grant_type=refresh_token";
                byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postData);
                outputStream.flush();

                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    LogHandler.saveLog("Updating access token with response code of " + responseCode,false);
                    StringBuilder responseBuilder = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(httpURLConnection.getInputStream())
                    );
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    String response = responseBuilder.toString();
                    JSONObject responseJSONObject = new JSONObject(response);
                    accessToken = responseJSONObject.getString("access_token");
                    return new GoogleCloud.Tokens(accessToken, refreshToken);
                }else {
                    LogHandler.saveLog("Getting access token failed with response code of " + responseCode, true);
                }
            } catch (Exception e) {
                LogHandler.saveLog("Getting access token failed: " + e.getLocalizedMessage(), true);
            }
            return new GoogleCloud.Tokens(accessToken, refreshToken);
        };
        Future<GoogleCloud.Tokens> future = executor.submit(backgroundTokensTask);
        GoogleCloud.Tokens tokens_fromFuture = null;
        try {
            tokens_fromFuture = future.get();
            DBHelper.updateAccessTokenInDB(refreshToken,tokens_fromFuture.getAccessToken());
        }catch (Exception e){
            LogHandler.saveLog("failed to get access token from the future: " + e.getLocalizedMessage(), true);
        }finally {
            executor.shutdown();
        }
        return tokens_fromFuture;
    }
}

