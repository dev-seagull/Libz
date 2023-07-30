package com.example.cso;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {




    private static final int RC_SIGN_IN = 1001;

    private GoogleSignInClient googleSignInClient;
    public double totalStorage;
    public double usageStorage;
    public double driveUsageStorage;
    public double gmail_plus_googlePhotos_storage;
    public  double freeSpace;
    TextView textViewAccessToken;
    TextView textViewLoginState;
    String accessToken;
    String authcodeForPhotos ="";
    String authcodeForDrive = "";

    ArrayList<String> baseUrls = new ArrayList<String>();;
    int Dcounter=0;
    PieChart storage_pieChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnBackUpLogin = findViewById(R.id.btnLoginBackup);
        Button googlephoto = findViewById(R.id.buttonphoto);
        Button downloadbtn = findViewById(R.id.buttonDownload);
        Button uploadButton = findViewById(R.id.uploadButton);
        textViewAccessToken = findViewById(R.id.accesstoken);
        textViewLoginState = findViewById(R.id.loginState);
        storage_pieChart = findViewById(R.id.StoragepieChart);

        storage_pieChart.setNoDataText("Your storage chart will be displayed here after you login");
        storage_pieChart.setNoDataTextColor(Color.RED);
        Typeface customTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        storage_pieChart.setNoDataTextTypeface(customTypeface);
        storage_pieChart.getDescription().setEnabled(false);



        if(accessToken == null){
            textViewLoginState.setText("You haven't logged into your google account yet");
        }

        btnLogin.setOnClickListener(v -> signIn());
        btnBackUpLogin.setOnClickListener(v -> signInBackupAccount());


        googlephoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GooglePhotosRequestTask().execute();
            }
        });


        downloadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GooglePhotosDownlaod().execute();
            }
        });


        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GooglePhotosUpload().execute();
            }
        });




        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary.readonly")
                        ,new Scope("https://www.googleapis.com/auth/drive.readonly" ),
                        new Scope("https://www.googleapis.com/auth/photoslibrary.appendonly"))
                .requestServerAuthCode(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signInBackupAccount() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary.readonly"),
                            new Scope("https://www.googleapis.com/auth/drive.readonly"),
                            new Scope("https://www.googleapis.com/auth/photoslibrary.appendonly"))
                    .requestServerAuthCode(getString(R.string.web_client_id))
                    .requestEmail()
                    .build();

            GoogleSignInClient googleSignInClientBackup = GoogleSignIn.getClient(this, gso);

            Intent signInIntent = googleSignInClientBackup.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);

                });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {

                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);

                String authCode = account.getServerAuthCode();
                authcodeForPhotos = authCode;
                authcodeForDrive = authCode;
                new TokenRequestTask().execute(authcodeForPhotos);

            } catch (ApiException e) {
                Toast.makeText(this, "Sign-in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }




    private class TokenRequestTask extends AsyncTask<String, Void, String> {
        public double convertToGigaByte(float storage){
            double Divider = (Math.pow(1024,3));
            return storage/Divider;
        }


        @Override
        protected String doInBackground(String... params) {
            String authCode = params[0];
            int responseCode = 0;
            String  response= "";

            try {
                URL url = null;
                try {
                    url = new URL("https://oauth2.googleapis.com/token");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) url.openConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    connection.setRequestMethod("POST");
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }

                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Host", "oauth2.googleapis.com");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                String client_id= getString(R.string.client_id);
                String client_secret = getString(R.string.client_secret);

                String requestBody = "code=" + authCode +
                        "&client_id="+ client_id+
                        "&client_secret=" + client_secret+
                        "&grant_type=authorization_code";

                byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);
                connection.setRequestProperty("Content-Length", String.valueOf(postData.length));

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(postData);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    responseCode = connection.getResponseCode();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder responseBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    }
                    response = responseBuilder.toString();
                    JSONObject jsonResponse = null;
                    try {
                        jsonResponse = new JSONObject(response);
                        accessToken = jsonResponse.getString("access_token");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }



                    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                    final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

                    HttpRequestInitializer requestInitializer = request -> {
                        request.getHeaders().setAuthorization("Bearer " + accessToken);
                        request.getHeaders().setContentType("application/json");

                    };

                    Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                            .setApplicationName("cso")
                            .build();


                    totalStorage  = convertToGigaByte(service.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getLimit());

                    usageStorage = convertToGigaByte(service.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getUsage());

                    driveUsageStorage = convertToGigaByte(service.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getUsageInDrive());

                    gmail_plus_googlePhotos_storage =  usageStorage - driveUsageStorage;

                    freeSpace = totalStorage - usageStorage;


                }

            } catch (IOException | GeneralSecurityException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {

            if(accessToken != null){
                textViewLoginState.setText("You have successfully logged into your account");
            }

            ArrayList<PieEntry> entries = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();
            entries.add(new PieEntry((float) totalStorage, 0));
            labels.add("Total Storage");

            entries.add(new PieEntry((float) driveUsageStorage,1));
            labels.add("Google Drive");

            entries.add(new PieEntry((float)  freeSpace, 2));
            labels.add("Free Space");

            entries.add(new PieEntry((float) gmail_plus_googlePhotos_storage, 3));
            labels.add("Gmail and Google Photos");

            PieDataSet dataSet = new PieDataSet(entries, "GStorage");
            dataSet.setDrawValues(true);
            dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    String formattedValue = String.valueOf(value);
                    if (formattedValue.length() > 4) {
                        formattedValue = formattedValue.substring(0, 4);
                    }
                    return formattedValue + " GB";
                }
            });

            PieData data = new PieData(dataSet);
            storage_pieChart.setData(data);
            storage_pieChart.invalidate();

            Legend legend = storage_pieChart.getLegend();
            legend.setForm(Legend.LegendForm.CIRCLE);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            legend.setOrientation(Legend.LegendOrientation.VERTICAL);
            legend.setDrawInside(false);
            legend.setXEntrySpace(10f);
            legend.setYEntrySpace(10f);

            ArrayList<LegendEntry> legendEntries = new ArrayList<>();
            for (int i = 0; i < labels.size(); i++) {
                LegendEntry entry = new LegendEntry();
                entry.label = labels.get(i);
                entry.formColor = dataSet.getColor(i);
                legendEntries.add(entry);
            }
            legend.setCustom(legendEntries);
        }

    }


    private class GooglePhotosRequestTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {

            int pageSize = 100;
            String nextPageToken = null;
            JSONArray mediaItems = null;
            StringBuilder filenames = null;

            try {

                URL url = new URL("https://photoslibrary.googleapis.com/v1/mediaItems");


                HttpURLConnection connection = (HttpURLConnection) url.openConnection();


                connection.setRequestMethod("GET");

                connection.setRequestProperty("Content-type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);


                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }


                    reader.close();

                    String jsonResponse = response.toString();

                    JSONObject responseJson = new JSONObject(jsonResponse);
                    mediaItems = responseJson.getJSONArray("mediaItems");
                    filenames = new StringBuilder();
                    for (int i = 0; i < mediaItems.length(); i++) {
                        JSONObject mediaItem = mediaItems.getJSONObject(i);
                        String filename = mediaItem.getString("filename");
                        String baseUrl = mediaItem.getString("baseUrl");
                        baseUrls.add(baseUrl);
                        filenames.append(filename).append("\n");
                    }
                    nextPageToken = responseJson.optString("nextPageToken", null);
                    while (responseJson.has("nextPageToken") && nextPageToken!=null && responseJson.has("mediaItems")) {
                        nextPageToken = responseJson.optString("nextPageToken",null);
                        //System.out.println(nextPageToken);

                        // connection = (HttpURLConnection) new URL(url.toString() + params).openConnection();
                        String nextPageUrl = "https://photoslibrary.googleapis.com/v1/mediaItems?pageToken=" + nextPageToken;
                        URL nextUrl = new URL(nextPageUrl);
                        connection = (HttpURLConnection) nextUrl.openConnection();

                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("Content-type", "application/json");
                        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

                        responseCode = connection.getResponseCode();

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            response = new StringBuilder();

                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }


                            reader.close();

                            jsonResponse = response.toString();

                            responseJson = new JSONObject(jsonResponse);

                            if(responseJson.has("mediaItems")){
                                mediaItems = responseJson.getJSONArray("mediaItems");
                            }else{
                                mediaItems = new JSONArray();
                            }


                            filenames = new StringBuilder();


                            for (int i = 0; i < mediaItems.length(); i++) {
                                    JSONObject mediaItem = mediaItems.getJSONObject(i);
                                    String filename = mediaItem.getString("filename");
                                    String baseUrl = mediaItem.getString("baseUrl");
                                    baseUrls.add(baseUrl);
                                    filenames.append(filename).append("\n");
                            }

                        }
                    }

                    connection.disconnect();
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return String.valueOf(baseUrls.size());
        }

            protected void onPostExecute(String response) {
            if (response != null) {
                textViewAccessToken.setText("Number of media items in Google Photos: "+response);
                Toast.makeText(MainActivity.this, response, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Failed to retrieve data", Toast.LENGTH_LONG).show();
            }
        }
    }


    private class GooglePhotosDownlaod extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {

            try {
                for(String baseUrl: baseUrls){
                    URL url = new URL(baseUrl+"=d");

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                        String destinationFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso";
                        File folder = new File(destinationFolder);
                        if (!folder.exists()) {
                            boolean folderCreated = folder.mkdirs();
                            if (!folderCreated) {
                                textViewAccessToken.setText("couldn't create directory");
                            }

                        }
                        String fileName = ("filename"+Dcounter+".jpg").substring(("filename"+Dcounter+".jpg").lastIndexOf("/") + 1);
                        String filePath = destinationFolder + File.separator + fileName;

                        OutputStream outputStream = null;
                        try {
                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso"+File.separator + "picture"+Dcounter+".jpg");
                            file.createNewFile();
                            outputStream = new FileOutputStream(file);

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                Log.d("Write", "Bytes written: " + bytesRead);
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            Dcounter = Dcounter + 1;
                            textViewAccessToken.setText(Dcounter+ " files were downloaded");
                        } catch (IOException e) {
                            e.printStackTrace();
                            textViewAccessToken.setText("Failed to save the file");
                        } finally {
                            try {
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        outputStream.close();

                        inputStream.close();

                        connection.disconnect();

                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String response) {
            textViewAccessToken.setText(Dcounter + " files were downloaded");
        }
    }

    private class GooglePhotosUpload extends AsyncTask<Void, Void, String> {

        byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String destinationFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso";

            File fileDirectory = new File(destinationFolder);
            File[] directoryFiles = fileDirectory.listFiles();


            if (directoryFiles != null && directoryFiles.length > 0){
                int counter = 0;
                for (File file : directoryFiles){
                    try{
                        URL url = new URL("https://photoslibrary.googleapis.com/v1/uploads");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setUseCaches(false);

                        String authorizationHeader = "Bearer " + accessToken;
                        String filename = file.getName();
                        int dotIndex = filename.lastIndexOf(".");
                        String fileFormat="";
                        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
                            fileFormat = filename.substring(dotIndex + 1);
                        }
                        String uploadContentTypeHeader="";
                        if(fileFormat != null){
                            uploadContentTypeHeader = fileFormat;
                        }else{
                            textViewAccessToken.setText("file format not found");
                        }

                        connection.setRequestProperty("Authorization", authorizationHeader);
                        connection.setRequestProperty("Content-type", "application/octet-stream");
                        connection.setRequestProperty("X-Goog-Upload-Content-Type", uploadContentTypeHeader);
                        connection.setRequestProperty("X-Goog-Upload-Protocol",  "raw");

                        BufferedOutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
                        InputStream fileInputStream = new FileInputStream(file);
                        byte[] data = inputStreamToByteArray(fileInputStream);
                        outputStream.write(data);
                        outputStream.flush();
                        outputStream.close();

                        int responseCode = connection.getResponseCode();

                        if(responseCode == HttpURLConnection.HTTP_OK){
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;

                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }

                            reader.close();

                            String uploadToken = response.toString();
                            JSONObject newUploadedMediaItem = new JSONObject();
                            newUploadedMediaItem.put("description", "uploaded by CSO");
                            newUploadedMediaItem.put("simpleMediaItem", new JSONObject()
                                    .put("fileName", filename)
                                    .put("uploadToken", uploadToken));


                            JSONArray newUploadedMediaItemsArray = new JSONArray();
                            newUploadedMediaItemsArray.put(newUploadedMediaItem);

                            JSONObject requestBody = new JSONObject();
                            requestBody.put("newMediaItems", newUploadedMediaItemsArray);

                            url = new URL("https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate");
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("POST");
                            connection.setRequestProperty("Content-type", "application/json");
                            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                            connection.setDoOutput(true);

                            outputStream = new BufferedOutputStream(connection.getOutputStream());
                            outputStream.write(requestBody.toString().getBytes("UTF-8"));
                            outputStream.flush();
                            outputStream.close();

                            responseCode = connection.getResponseCode();
                            if(responseCode == HttpURLConnection.HTTP_OK){
                                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                response = new StringBuilder();

                                while ((line = reader.readLine()) != null) {
                                    response.append(line);
                                }
                                reader.close();

                                String jsonResponse = response.toString();
                                counter = counter + 1;
                                textViewAccessToken.setText(counter+ " files were uploaded");
                            }
                        }


                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }


            }

            return null;
        }

        protected void onPostExecute(String response) {

        }

    }



}