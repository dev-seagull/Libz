package com.example.cso;


public class PrimaryAccountInfo {
    //private AndroidFiles androidFiles;
    private String userEmail;
    private Tokens tokens;
    private Storage storage;


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

    public PrimaryAccountInfo(String userEmail, Tokens tokens, Storage storage) {
        this.userEmail = userEmail;
        this.tokens = tokens;
        this.storage = storage;
    }

    public String getUserEmail() {
        return userEmail;
    }
    public Tokens getTokens() {return tokens;}
    public Storage getStorage() {return storage;}
}

