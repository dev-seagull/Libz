package com.example.cso;


public class PrimaryAccountInfo {
    String buttonId;
    //GoogleCloud.Tokens tokens;
    //private Storage storage;
    //private AndroidFiles androidFiles;
    private String userEmail;
    private Tokens tokens;

    public static class Tokens {
        private final String refreshToken;
        private final String accessToken;

        public Tokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }

    public PrimaryAccountInfo(String buttonId, String userEmail) {
        this.buttonId = buttonId;
      //  this.token = token;
      //  this.storage = storage;
      //  this.androidFiles = androidFiles;
        this.userEmail = userEmail;
    }

    public String getButtonId() {
        return buttonId;
    }

   // public Token getToken() {
   //     return token;
   // }

    //public Storage getStorage() {
    //    return storage;
    //}

    //public AndroidFiles getAndroidFiles() {
    //    return androidFiles;
    //}

    public String getUserEmail() {
        return userEmail;
    }
}

