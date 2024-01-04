package com.example.cso;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

    public static String calculateHash(File file) throws IOException {
        final int BUFFER_SIZE = 8192;
        StringBuilder hexString = new StringBuilder();
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LogHandler.saveLog("SHA-256 algorithm not available " + e.getLocalizedMessage());
        }
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(
                new FileInputStream(file))){
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) > 0) {
                if (digest != null) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            bufferedInputStream.close();
            byte[] hash = new byte[0];
            if (digest != null) {
                hash = digest.digest();
            }
            for (byte b : hash) {
                hexString.append(String.format("%02X", b));
            }
        }catch (Exception e){
            LogHandler.saveLog("error in calculating hash " + e.getLocalizedMessage());
        }
        return hexString.toString().toLowerCase();
    }

    public static String calculateSHA256(String input, Context context){
        StringBuilder hexString = null;
        String salt = context.getResources().getString(R.string.salt);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(input.getBytes());

            hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
            LogHandler.saveLog("Failed with error of :  " + e.getLocalizedMessage());
        }
        return hexString.toString();
    }

}
