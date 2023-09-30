package com.example.cso;

public class BackUpAccountInfo {
    private String userEmail;
    private com.example.cso.PrimaryAccountInfo.Tokens tokens;
    private com.example.cso.PrimaryAccountInfo.Storage storage;


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


    public static class Storage{
        private Double totalStorage;
        private Double usedStorage;

        public Storage(Double totalStorage, Double usedStorage){
            this.totalStorage = totalStorage;
            this.usedStorage = usedStorage;
        }

        public Double getTotalStorage() {return totalStorage;}
        public Double getUsedStorage() {return usedStorage;}
    }

    public BackUpAccountInfo(String userEmail, com.example.cso.PrimaryAccountInfo.Tokens tokens, com.example.cso.PrimaryAccountInfo.Storage storage) {
        this.userEmail = userEmail;
        this.tokens = tokens;
        this.storage = storage;
    }

    public String getUserEmail() {
        return userEmail;
    }
    public com.example.cso.PrimaryAccountInfo.Tokens getTokens() {return tokens;}
    public com.example.cso.PrimaryAccountInfo.Storage getStorage() {return storage;}

}
