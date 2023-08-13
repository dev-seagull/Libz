package com.example.cso;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.jaredrummler.android.device.DeviceName;

import org.checkerframework.checker.units.qual.A;
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
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;


public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1001;

    String userLoginAccountEmail;
    Button mediaItemsLayoutButton;
    Button btnLogin;
    Button btnBackUpLogin;
    private GoogleSignInClient googleSignInClient;
    public double totalStorage;
    public double usageStorage;
    public double driveUsageStorage;
    public double gmail_plus_googlePhotos_storage;
    public  double freeSpace;
    TextView syncToBackUpAccountTextView;
    TextView androidTextView;
    TextView textViewLoginState;
    TextView textviewGooglePhotosMediaItemsCount;
    TextView galleryTextView;
    ArrayList<String> accessTokens = new ArrayList<String>();
    String authcodeForPhotos ="";
    String authcodeForDrive = "";
    Drive service;

    ArrayList<String> baseUrls = new ArrayList<String>();
    ArrayList<String> fileNames = new ArrayList<String>();
    ArrayList<String> productUrls = new ArrayList<String>();

    int Dcounter=0;
    PieChart storage_pieChart;
    HorizontalBarChart androidStorageBarChart;
    ProgressBar androidProgressBar;


    String formatSize(long sizeBytes) {
        float sizeGB = sizeBytes / (1024f * 1024f * 1024f);
        return String.format(Locale.getDefault(), "%.2f", sizeGB);
    }

    public static boolean isImageOrVideoFile(File file, String[] imageExtensions) {
        String filePath = file.getPath().toLowerCase();
        String memeType = filePath.substring(filePath.lastIndexOf("."));
        for (String extension : imageExtensions) {
            if (memeType.equals(extension)) {
                return true;
            }
        }
        return false;
    }


    public void createMediaItemsModel(String name,String productUrl,LinearLayout parentLayout){
        String mediaItemName = name;
        TextView textViewName = new TextView(parentLayout.getContext());
        textViewName.setText(mediaItemName);

        TextView textViewBackUpStatus = new TextView(parentLayout.getContext());
        textViewBackUpStatus.setText("no back-up");

        Button mediaItemDisplayButton = new Button(parentLayout.getContext());
        mediaItemDisplayButton.setVisibility(View.VISIBLE);
        mediaItemDisplayButton.setText("Click here to see the image");


        mediaItemDisplayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(productUrl));
                parentLayout.getContext().startActivity(intent);
            }
        });

        parentLayout.addView(textViewName);
        parentLayout.addView(textViewBackUpStatus);
        parentLayout.addView(mediaItemDisplayButton);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnLogin = findViewById(R.id.btnLogin);
        btnBackUpLogin = findViewById(R.id.btnLoginBackup);
        Button syncToBackUpAccountButton = findViewById(R.id.syncToBackUpAccountButton);
        Button syncAndroidButton = findViewById(R.id.syncAndroidDevice);
        mediaItemsLayoutButton = findViewById(R.id.mediaItemsLayout);
        syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
        textViewLoginState = findViewById(R.id.loginState);
        textviewGooglePhotosMediaItemsCount = findViewById(R.id.googlePhotosMediaItemsCount);
        TextView textViewAndroidDeviceName = findViewById(R.id.androidDeviceTextView);
        storage_pieChart = findViewById(R.id.StoragepieChart);
        androidStorageBarChart = findViewById(R.id.androidStorageBarChart);
        androidProgressBar = findViewById(R.id.androidImageProgressBar);
        androidTextView = findViewById(R.id.androidTextView);

        storage_pieChart.setNoDataText("Your storage chart will be displayed here after you login");
        storage_pieChart.setNoDataTextColor(Color.RED);
        Typeface customTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        storage_pieChart.setNoDataTextTypeface(customTypeface);
        storage_pieChart.getDescription().setEnabled(false);


        String androidDeviceName = DeviceName.getDeviceName();
        textViewAndroidDeviceName.setText(androidDeviceName);

        if(accessTokens.size() == 0){
            textViewLoginState.setText("You haven't logged into your google account yet");
        }

        btnLogin.setOnClickListener(v -> signIn());
        btnBackUpLogin.setOnClickListener(v -> signInBackupAccount());

        syncToBackUpAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GooglePhotosUpload().execute();
            }
        });


        syncAndroidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SyncAndroid().execute();
            }
        });

        AndroidStorageTask androidStorageTask = new AndroidStorageTask();
        androidStorageTask.execute();


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary.readonly")
                        ,new Scope("https://www.googleapis.com/auth/drive.readonly" ),
                        new Scope("https://www.googleapis.com/auth/photoslibrary.appendonly"),
                        new Scope("https://www.googleapis.com/auth/drive.file"))
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
                            new Scope("https://www.googleapis.com/auth/photoslibrary.appendonly"),
                            new Scope("https://www.googleapis.com/auth/drive.file"))
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

                userLoginAccountEmail = account.getEmail();

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



        mediaItemsLayoutButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setContentView(R.layout.activity_mediaitems);
                        LinearLayout mediaItemsLinearLayout = findViewById(R.id.mediaItemsLinearLayout);
                        LinearLayout.LayoutParams mediaItemsLinearLayoutParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        mediaItemsLinearLayoutParams.setMargins(0,5,0, 0);

                        for (int i=0;i<fileNames.size();i++) {
                            createMediaItemsModel(fileNames.get(i)
                                    , productUrls.get(i)
                                    ,mediaItemsLinearLayout);
                        }

                    }
                }
        );
    }

    private  static  class TokenRequestResult{
        ArrayList<String> fileNames;
        ArrayList<String> baseurls;

        TokenRequestResult(ArrayList<String> fileNames, ArrayList<String> baseurls){
            this.fileNames = fileNames;
            this.baseurls = baseurls;
        }

    }

    private class TokenRequestTask extends AsyncTask<String, Void, TokenRequestResult> {

        @Override
        protected  void  onPreExecute(){
            super.onPreExecute();
            if(accessTokens.size()==0){
                btnLogin.setText("Please wait, it may take some minutes to get things done here...");
            }

            if(accessTokens.size() > 0){
                btnBackUpLogin.setText("Please wait, it may take some minutes to get things done here...");
            }
        }

        public double convertToGigaByte(float storage){
            double Divider = (Math.pow(1024,3));
            return storage/Divider;
        }


        @Override
        protected TokenRequestResult doInBackground(String... params) {

            String authCode = params[0];
            int responseCode = 0;
            String response = "";

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

                String client_id = getString(R.string.client_id);
                String client_secret = getString(R.string.client_secret);

                String requestBody = "code=" + authCode +
                        "&client_id=" + client_id +
                        "&client_secret=" + client_secret +
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
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    response = responseBuilder.toString();
                    JSONObject jsonResponse = null;
                    try {
                        jsonResponse = new JSONObject(response);
                         accessTokens.add(jsonResponse.getString("access_token"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                    final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

                    HttpRequestInitializer requestInitializer = request -> {
                        request.getHeaders().setAuthorization("Bearer " + accessTokens.get(0));
                        request.getHeaders().setContentType("application/json");

                    };

                    service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                            .setApplicationName("cso")
                            .build();


                    totalStorage = convertToGigaByte(service.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getLimit());

                    usageStorage = convertToGigaByte(service.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getUsage());

                    try {
                        driveUsageStorage = convertToGigaByte(service.about().get()
                                .setFields("user, storageQuota")
                                .execute().getStorageQuota().getUsageInDrive());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    gmail_plus_googlePhotos_storage = usageStorage - driveUsageStorage;

                    freeSpace = totalStorage - usageStorage;
                }

                for (String at :  accessTokens){
                    System.out.println(at);
                }

                int pageSize = 100;
                String nextPageToken = null;
                JSONArray mediaItems = null;

                try {
                    url = new URL("https://photoslibrary.googleapis.com/v1/mediaItems");

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Content-type", "application/json");
                    connection.setRequestProperty("Authorization", "Bearer " + accessTokens.get(0));
                    System.out.println("m with" + accessTokens.get(0));

                    responseCode = connection.getResponseCode();
                    System.out.println("r is " + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder mediaItemsresponse = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            mediaItemsresponse.append(line);
                        }
                        reader.close();

                        String mediaItemResponseString = mediaItemsresponse.toString();

                        JSONObject mediaItemResponseJson = new JSONObject(mediaItemResponseString);
                        mediaItems = mediaItemResponseJson.getJSONArray("mediaItems");
                        fileNames = new ArrayList<String>();
                        for (int i = 0; i < mediaItems.length(); i++) {
                            JSONObject mediaItem = mediaItems.getJSONObject(i);
                            String filename = mediaItem.getString("filename");
                            String baseUrl = mediaItem.getString("baseUrl");
                            String productUrl = mediaItem.getString("productUrl");
                            productUrls.add(productUrl);
                            baseUrls.add(baseUrl);
                            fileNames.add(filename);
                        }
                        nextPageToken = mediaItemResponseJson.optString("nextPageToken", null);
                        while (mediaItemResponseJson.has("nextPageToken") && nextPageToken != null && mediaItemResponseJson.has("mediaItems")) {
                            nextPageToken = mediaItemResponseJson.optString("nextPageToken", null);
                            String nextPageUrl = "https://photoslibrary.googleapis.com/v1/mediaItems?pageToken=" + nextPageToken;
                            URL nextUrl = new URL(nextPageUrl);
                            connection = (HttpURLConnection) nextUrl.openConnection();
                            connection.setRequestMethod("GET");
                            connection.setRequestProperty("Content-type", "application/json");
                            connection.setRequestProperty("Authorization", "Bearer " + accessTokens.get(0));
                            responseCode = connection.getResponseCode();

                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                mediaItemsresponse = new StringBuilder();
                                while ((line = reader.readLine()) != null) {
                                    mediaItemsresponse.append(line);
                                }
                                reader.close();

                                mediaItemResponseString = mediaItemsresponse.toString();

                                mediaItemResponseJson = new JSONObject(mediaItemResponseString);

                                if (mediaItemResponseJson.has("mediaItems")) {
                                    mediaItems = mediaItemResponseJson.getJSONArray("mediaItems");
                                } else {
                                    mediaItems = new JSONArray();
                                }


                                for (int i = 0; i < mediaItems.length(); i++) {
                                    JSONObject mediaItem = mediaItems.getJSONObject(i);
                                    String filename = mediaItem.getString("filename");
                                    String baseUrl = mediaItem.getString("baseUrl");
                                    String productUrl = mediaItem.getString("productUrl");
                                    productUrls.add(productUrl);
                                    baseUrls.add(baseUrl);
                                    fileNames.add(filename);
                                }

                            }
                        }
                        connection.disconnect();
                    }

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }


            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new TokenRequestResult(fileNames,baseUrls);
        }
        @Override
        protected void onPostExecute(TokenRequestResult tokenRequestResult) {

            if(accessTokens.get(0) != null && userLoginAccountEmail != null){
                textViewLoginState.setText("You have successfully logged into your account");
                if(fileNames != null){
                    textviewGooglePhotosMediaItemsCount.setText(fileNames.size() + " items were found in your Google Photos account");
                    mediaItemsLayoutButton.setVisibility(View.VISIBLE);
                }else{
                    textviewGooglePhotosMediaItemsCount.setText("No items were found in your Google Photos account");
                }
            }else {
                textViewLoginState.setText("The login wasn't successful");
            }
            if(accessTokens.get(accessTokens.size()-1) != null && userLoginAccountEmail != null
            && accessTokens.size()-1 == 0){
                btnLogin.setText(userLoginAccountEmail);
            }

            if(accessTokens.get(accessTokens.size()-1) != null && userLoginAccountEmail != null
                    && accessTokens.size()-1 != 0){
                btnBackUpLogin.setText(userLoginAccountEmail);
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

            ArrayList<String > fileNames = tokenRequestResult.fileNames;
            ArrayList<String> baseUrls = tokenRequestResult.baseurls;

        }
    }

    private class GooglePhotosUpload extends AsyncTask<Void, Void, String> {

        int counter = 0;

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
            try {
                int i = 0;
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
                                syncToBackUpAccountTextView.setText("couldn't create directory");
                            }

                        }
                        String fileName = fileNames.get(i);
                        //int dotIndex = fileName.lastIndexOf(".");
                        //String fileFormat="";
                        //if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                        //    fileFormat = fileName.substring(dotIndex + 1);
                        //}

                        i++;
                        String filePath = destinationFolder + File.separator + fileName;

                        OutputStream outputStream = null;
                        try {
                            File file = new File(filePath);
                            file.createNewFile();
                            outputStream = new FileOutputStream(file);

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                Log.d("Write", "Bytes written: " + bytesRead);
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            Dcounter = Dcounter + 1;
                        } catch (IOException e) {
                            e.printStackTrace();
                            syncToBackUpAccountTextView.setText("Failed to download the itmes");
                        } finally {
                            try {
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        inputStream.close();

                        connection.disconnect();

                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            String destinationFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso";

            File fileDirectory = new File(destinationFolder);
            File[] directoryFiles = fileDirectory.listFiles();

            if (directoryFiles != null && directoryFiles.length > 0){
                for (File file : directoryFiles){
                    try{
                        URL url = new URL("https://photoslibrary.googleapis.com/v1/uploads");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setUseCaches(false);

                        String authorizationHeader = "Bearer " + accessTokens.get(1);
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
                            syncToBackUpAccountTextView.setText("Item format not found");
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
                        System.out.println("r for upload is " + responseCode);

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
                            connection.setRequestProperty("Authorization", "Bearer " + accessTokens.get(1));
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
                            }
                        }


                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    if(fileDirectory.exists()) {
                        fileDirectory.delete();
                    }
                }
            }

            return null;
        }
        protected void onPostExecute(String response) {
            syncToBackUpAccountTextView.setText(counter+ " items were synced");
        }
    }

    private static class SyncResult {
        ArrayList<ArrayList<String>> result1;
        ArrayList<String> result2;

        SyncResult(ArrayList<ArrayList<String>> result1,ArrayList<String> result2) {
            this.result1 = result1;
            this.result2 = result2;
        }
    }

    private class SyncAndroid extends AsyncTask<Void, Integer, SyncResult> {

        ArrayList<String> uploadedCounter  = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            androidProgressBar.setVisibility(View.VISIBLE);
            androidProgressBar.setProgress(0);
        }

        @Override
        protected SyncResult doInBackground(Void... voids) {

            while (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.READ_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }


            File rootDirectory = Environment.getExternalStorageDirectory();
            ArrayList<ArrayList<String>> fileInfoList = new ArrayList<>();
            Queue<File> directoriesQueue = new LinkedList<>();
            directoriesQueue.add(rootDirectory);

            String[] imageAndVideoExtensions = {".jpg", ".jpeg", ".png", ".bmp", ".webp",".heic", ".heif",
                    ".gif", ".mp4", ".mkv",".webm",".3gp",".ts"};

            int totalFiles = 0;
            int processedFiles = 0;


            while (!directoriesQueue.isEmpty()){
                File currentDirectory = directoriesQueue.poll();
                if(currentDirectory.getPath().equals(rootDirectory + "/Android")){
                    continue;
                }else{
                    File[] files = currentDirectory.listFiles();

                    if(files != null){
                        for(File file: files){
                            if(file.isDirectory() && !file.isHidden()){
                                directoriesQueue.add(file);
                                totalFiles++;

                            }else if(file.isFile()){
                                totalFiles++;

                                if(isImageOrVideoFile(file, imageAndVideoExtensions)){

                                    String fileName = file.getName();
                                    String filePath = file.getPath();
                                    Integer fileHash = file.hashCode();

                                    ArrayList<String> fileInfo = new ArrayList<>();
                                    fileInfo.add(fileName);
                                    fileInfo.add(filePath);
                                    fileInfo.add(String.valueOf(fileHash));
                                    fileInfoList.add(fileInfo);
                                    String memeType = filePath.substring(filePath.lastIndexOf("."));

                                    processedFiles++;




                                        //System.out.println("upload file name:" + fileName);
                                        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                                        fileMetadata.setName(fileName);

                                        FileContent mediaContent = null;
                                        if(memeType.equals(".jpeg") | memeType.equals(".jpg")){
                                            mediaContent = new FileContent("image/jpeg" ,new File(filePath));
                                            uploadedCounter.add(fileName);
                                        }

                                        else if(memeType.equals(".png")){
                                            mediaContent = new FileContent("image/png" ,new File(filePath));
                                            uploadedCounter.add(fileName);
                                        }

                                        else if(memeType.equals(".gif")){
                                            mediaContent = new FileContent("image/gif" ,new File(filePath));
                                            uploadedCounter.add(fileName);
                                        }

                                        else if(memeType.equals(".bmp")){
                                            mediaContent = new FileContent("image/bmp" ,new File(filePath));
                                            uploadedCounter.add(fileName);
                                        }

                                        else if(memeType.equals(".mp4")){
                                            mediaContent = new FileContent("video/mp4" ,new File(filePath));
                                            uploadedCounter.add(fileName);
                                        }

                                        else if(memeType.equals(".mkv")){
                                            mediaContent = new FileContent("video/x-matroska" ,new File(filePath));
                                            uploadedCounter.add(fileName);
                                        }



                                        try {
                                            com.google.api.services.drive.model.File uploadFile = service.files().create(fileMetadata, mediaContent)
                                                    .setFields("id")
                                                    .execute();

                                            System.out.println("File ID: " + uploadFile.getId());

                                        } catch (IOException e) {
                                            System.out.println("Unable to upload: " + e.getMessage());;
                                        }

                                }

                            }
                            int progress = (int) ((processedFiles / (float) totalFiles) * 100);
                            publishProgress(progress);
                        }
                    }
                }
            }


            return new SyncResult(fileInfoList , uploadedCounter);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            androidProgressBar.setProgress(progress);
        }



        protected void onPostExecute(SyncResult response) {
            ArrayList<ArrayList<String>> resultList1 = response.result1;
            ArrayList<String> resultList2 = response.result2;
            StringBuilder uploadedFilenames = null;
            for(String rs2: resultList2){
                uploadedFilenames.append( rs2+ " ");
            }
            androidProgressBar.setProgress(100);
            androidTextView.setText("Search of your images and videos is finished and you have this number of them: " +
               resultList1.size() + " and "+ resultList2.size()  +
                    " of them were uploaded to google drive"
            + "\n" + "filenames: " + uploadedFilenames);
        }

    }

    private class AndroidStorageTask extends AsyncTask<Void, Void,ArrayList<String>> {

        ArrayList<String> responses = new ArrayList<>();

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {

            File externalDir  = Environment.getExternalStorageDirectory();

            long availableSpaceBytes, totalSpaceBytes;

            StatFs stat = new StatFs(externalDir.getPath());

            if (android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                availableSpaceBytes = stat.getAvailableBytes();
                totalSpaceBytes = stat.getTotalBytes();


            }else{
                long blockSize = stat.getBlockSizeLong();
                long availableBlocks = stat.getAvailableBlocksLong();
                long totalBlocks = stat.getBlockCountLong();
                availableSpaceBytes = blockSize * availableBlocks;
                totalSpaceBytes = blockSize * totalBlocks;


            }
            String availableSpaceString = formatSize(availableSpaceBytes);
            String totalSpaceString = formatSize(totalSpaceBytes);

            responses.add(totalSpaceString);
            responses.add(availableSpaceString);



            //if(accessToken != null){
            //    textViewLoginState.setText("You have successfully logged into your account");
            //}

            return responses;
        }

        protected void onPostExecute(ArrayList<String> response) {

            Float totalSpace = Float.valueOf(responses.get(0));
            Float availableSpace = Float.valueOf(responses.get(1));
            Float usedSpace = totalSpace - availableSpace;

            ArrayList<BarEntry> barEntries = new ArrayList<>();
            ArrayList<String> barLabels = new ArrayList<>();
            barEntries.add(new BarEntry(0,new float[] {availableSpace,usedSpace} ));
            barLabels.add("Free Space: " + availableSpace + " GB");
            barLabels.add("Used space: " + usedSpace + " GB");

            for(BarEntry barent: barEntries){
                System.out.println("bar entry: " + String.valueOf(barent));
            }

            BarDataSet androidStorageBarChartDataSet = new BarDataSet(barEntries, "Android device Storage");
            androidStorageBarChartDataSet.setDrawValues(false);
            //androidStorageBarChartDataSet.(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            androidStorageBarChartDataSet.setColors(ColorTemplate.COLORFUL_COLORS);




            BarData barData = new BarData(androidStorageBarChartDataSet);
            androidStorageBarChart.setData(barData);
            androidStorageBarChart.invalidate();
            androidStorageBarChart.setFitBars(true);
            androidStorageBarChart.getDescription().setEnabled(false);
            androidStorageBarChart.setDrawGridBackground(false);
            androidStorageBarChart.getAxisLeft().setEnabled(false);
            androidStorageBarChart.getAxisRight().setEnabled(false);
            androidStorageBarChart.getXAxis().setEnabled(false);
            androidStorageBarChart.setDrawBorders(false);
            androidStorageBarChart.getLegend().setEnabled(true);
            Legend barLegend = androidStorageBarChart.getLegend();
            barLegend.setForm(Legend.LegendForm.CIRCLE);
            barLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            barLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            barLegend.setOrientation(Legend.LegendOrientation.VERTICAL);
            barLegend.setDrawInside(false);
            barLegend.setXEntrySpace(2f);
            barLegend.setYEntrySpace(2f);
            barLegend.setTextColor( Color.parseColor("#808080"));


            ArrayList<LegendEntry> BarLegendEntries = new ArrayList<>();
            for (int i = 0; i < barLabels.size(); i++) {
                LegendEntry barEntry = new LegendEntry();
                barEntry.label = barLabels.get(i);
                barEntry.formColor = androidStorageBarChartDataSet.getColor(i);
                BarLegendEntries.add(barEntry);
            }
            barLegend.setCustom(BarLegendEntries);

            if(!androidStorageBarChart.isShown()){
                androidStorageBarChart.setNoDataText("Data for the storage of your device is not available.");
            }
        }
    }



}



