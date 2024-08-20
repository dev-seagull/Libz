package com.example.cso;

import android.annotation.SuppressLint;
import android.util.Base64;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class JsonEncryptor {

    private static String ALGORITHM = "AES";
    private static String TRANSFORMATION = "AES";

    public static String encryptJsonContent(String jsonContent) {
        try {
            String encryptionKey = MainActivity.activity.getResources().getString(R.string.ENCRYPTION_KEY);
            Key key = generateKey(encryptionKey);
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(jsonContent.getBytes());
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            LogHandler.saveLog("Failed to encrypt JSON content: " + e.getLocalizedMessage());
            return null;
        }
    }

    public static String decryptJsonContent(String encryptedJsonContent) {
        try {
            String encryptionKey = MainActivity.activity.getResources().getString(R.string.ENCRYPTION_KEY);
            Key key = generateKey(encryptionKey);
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.decode(encryptedJsonContent, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            LogHandler.saveLog("Failed to decrypt JSON content: " + e.getLocalizedMessage());
            return null;
        }
    }

    private static Key generateKey(String encryptionKey) {
        return new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
    }
}
