package com.example.cso;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GooglePhotos {
    public void getGooglePhotosMediaItems(PrimaryAccountInfo.Tokens tokens){
        int pageSize = 100;
        String nextPageToken = null;
        JSONArray mediaItems = null;
        String accessToken = "";
        String refreshToken;

        try{
            URL url = new URL("https://photoslibrary.googleapis.com/v1/mediaItems");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-type", "application/json");
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = httpURLConnection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder responseStringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null){
                    responseStringBuilder.append(line);
                }
                bufferedReader.close();
                String response  = responseStringBuilder.toString();
                JSONObject responseJson = new JSONObject(response);
            }

        }catch (Exception e){
            //Toast.makeText(activity,"Login failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
