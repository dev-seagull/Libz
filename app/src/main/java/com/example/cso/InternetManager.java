package com.example.cso;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InternetManager {

    public static String getInternetStatus(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo[] networksInfo = connectivityManager.getAllNetworkInfo();
            for (NetworkInfo networkInfo : networksInfo) {
                if (networkInfo.getTypeName().equalsIgnoreCase("WIFI"))
                    if (networkInfo.isConnected()) {
                        if(isInternetReachable("https://drive.google.com")){
                            return "wifi";
                        }
                    }
                if (networkInfo.getTypeName().equalsIgnoreCase("MOBILE"))
                    if (networkInfo.isConnected()) {
                        if(isInternetReachable("https://drive.google.com")){
                            return "data";
                        }
                    }
            }
        }
        return "noInternet";
    }


    public static boolean isInternetReachable(String urlString) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> callableTask = () -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(3000);
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                return (responseCode == 200);
            } catch (Exception e) {
                LogHandler.crashLog(e,"service");
                return false;
            }
        };
        Future<Boolean> future = executor.submit(callableTask);
        Boolean isReachable = false;
        try{
            isReachable = future.get();
        }catch (Exception e){
            System.out.println("Failed to check validity from future: " + e.getLocalizedMessage());
        }
        return isReachable;
    }
    public static boolean isConnectedToVPN(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities =  null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            }else{
                return true;
            }
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            }
        }
        return true;
    }
}


