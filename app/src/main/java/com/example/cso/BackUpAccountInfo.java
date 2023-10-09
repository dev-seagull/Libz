package com.example.cso;

import java.util.ArrayList;

public class BackUpAccountInfo {
    private String userEmail;
    private com.example.cso.PrimaryAccountInfo.Tokens tokens;
    private com.example.cso.PrimaryAccountInfo.Storage storage;



    public static class Storage{
        private Double totalStorage;
        private Double usedStorage;
        //private Double usedInDriveStorage;

        public Storage(Double totalStorage, Double usedStorage){
            this.totalStorage = totalStorage;
            this.usedStorage = usedStorage;
        }

        public Double getTotalStorage() {return totalStorage;}
        public Double getUsedStorage() {return usedStorage;}
    }

    public BackUpAccountInfo(String userEmail, com.example.cso.PrimaryAccountInfo.Tokens tokens,
                             com.example.cso.PrimaryAccountInfo.Storage storage) {
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
