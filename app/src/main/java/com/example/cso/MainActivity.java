package com.example.cso;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import com.jaredrummler.android.device.DeviceName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;


public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1001;

    String userLoginAccountEmail;
    Button mediaItemsLayoutButton;
    Button btnLogin;
    Button btnBackUpLogin;
    Button androidMediaItemsButton;
    Button syncAndroidButton;
    Button syncToBackUpAccountButton;
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
    ArrayList<String> selectedFolders = new ArrayList<>();
    public ArrayList<String> formats = new ArrayList<String>();
    ArrayList<String> fileNames = new ArrayList<String>();
    ArrayList<String> productUrls = new ArrayList<String>();
    ArrayList<ArrayList<String>> androidFileInfoList = new ArrayList<>();

    int Dcounter=0;
    HorizontalBarChart storage_horizontalBarChart;
    HorizontalBarChart androidStorageBarChart;
    ProgressBar androidProgressBar;


    public String calculateHash(String filePath) throws IOException {

        final int BUFFER_SIZE = 8192;

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }

        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(
                new FileInputStream(new File(filePath)))){

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = bufferedInputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, bytesRead);
            }
            bufferedInputStream.close();

            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02X", b));
            }
            return  hexString.toString();
        }
    }


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







    private List<String> getGalleryImages() {
        List<String> mediaItemPaths = new ArrayList<>();

        String[] projection = {MediaStore.Files.FileColumns.DATA};
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=? OR " +
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";
        String[] selectionArgs = {String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)};
        String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC";

        Cursor cursor = getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                sortOrder
        );

        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            while (cursor.moveToNext()) {
                String mediaItemPath = cursor.getString(columnIndex);
                mediaItemPaths.add(mediaItemPath);
            }
            cursor.close();
        }

        return mediaItemPaths;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        androidMediaItemsButton = findViewById(R.id.androidMediaItemsButton);
        btnLogin = findViewById(R.id.btnLogin);
        btnBackUpLogin = findViewById(R.id.btnLoginBackup);
        syncToBackUpAccountButton = findViewById(R.id.syncToBackUpAccountButton);
        syncAndroidButton = findViewById(R.id.syncAndroidDevice);

        mediaItemsLayoutButton = findViewById(R.id.mediaItemsLayout);
        syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
        textViewLoginState = findViewById(R.id.loginState);
        textviewGooglePhotosMediaItemsCount = findViewById(R.id.googlePhotosMediaItemsCount);
        TextView textViewAndroidDeviceName = findViewById(R.id.androidDeviceTextView);
        storage_horizontalBarChart = findViewById(R.id.StorageHorizontalBarChart);
        androidStorageBarChart = findViewById(R.id.androidStorageBarChart);
        androidProgressBar = findViewById(R.id.androidImageProgressBar);
        androidTextView = findViewById(R.id.androidTextView);

        storage_horizontalBarChart.setNoDataText("Your storage chart will be displayed here after you login");
        storage_horizontalBarChart.setNoDataTextColor(Color.RED);
        Typeface customTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        storage_horizontalBarChart.setNoDataTextTypeface(customTypeface);
        storage_horizontalBarChart.getDescription().setEnabled(false);


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
                SyncAndroid syncTask = new SyncAndroid(MainActivity.this);
                syncTask.execute();
            }
        });

        AndroidStorageTask androidStorageTask = new AndroidStorageTask();
        androidStorageTask.execute();

        androidMediaItemsButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), AndroidMediaItems.class);
                        ArrayList<String> serializedAndroidFileInfoList = new ArrayList<>();
                        for (ArrayList<String> fileInfo : androidFileInfoList) {
                            serializedAndroidFileInfoList.add(new Gson().toJson(fileInfo));
                        }
                        intent.putStringArrayListExtra("androidFileInfoList", serializedAndroidFileInfoList);
                        startActivityForResult(intent,789);
                    }
                }
        );


        if(accessTokens.size() > 1){
            syncAndroidButton.setEnabled(true);
            syncAndroidButton.setText("Sync");
        }else if(accessTokens.size() <2){
            syncAndroidButton.setEnabled(false);
            syncAndroidButton.setText("Sync (First login to your back-up account)");
        }

        mediaItemsLayoutButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(),MediaItemModel.class);
                        intent.putStringArrayListExtra("fileNames", fileNames);
                        intent.putStringArrayListExtra("productUrls", productUrls);
                        startActivityForResult(intent,456);

                    }
                }
        );


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
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
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
            if(resultCode == RESULT_OK){
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
        }



        if(accessTokens.size() > 1){
            syncAndroidButton.setEnabled(true);
            syncAndroidButton.setText("Sync");
        }else if(accessTokens.size() <2){
            syncAndroidButton.setEnabled(false);
            syncAndroidButton.setText("Sync (First login to your back-up account)");
        }

        System.out.println("ac size" + accessTokens.size());

        //private static final int REQUEST_CODE_FOLDER_SELECTION = 1;
        if (requestCode == 123 && resultCode == RESULT_OK) {

            selectedFolders = data.getStringArrayListExtra("selectedFolders");

            formats = data.getStringArrayListExtra("formats");

            for(String s: selectedFolders){
                System.out.println(s);
            }

            new SyncAndroid2().execute();
        }

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

                    String nextPageToken = null;
                    do{
                        List<com.google.api.services.drive.model.File> fis = new ArrayList<>();
                        FileList result =  service.files().list().setPageToken(nextPageToken)
                                .setFields("nextPageToken, files(id, name)").execute();
                        fis = result.getFiles();

                        if (fis != null && !fis.isEmpty()) {
                            System.out.println("length of files: " + fis.size() + "\n");
                            for (com.google.api.services.drive.model.File f: fis) {
                                System.out.println(f.toString());

                                System.out.println("File checksum for " + f.getName() + " is: " +  f.getMd5Checksum()+ "\n");
                            }
                        }else {
                            System.out.println("No files found.");
                        }

                        nextPageToken = result.getNextPageToken();

                    } while (nextPageToken != null);


                    File test =  new File(Environment.getExternalStorageDirectory() + "/Download/270px-Alan_Turing_Aged_16.jpg");
                    System.out.println("Android sha 256 is equal to " + calculateHash(test.getPath()));


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
                        productUrls = new ArrayList<String>();
                        baseUrls = new ArrayList<String>();
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
                    && accessTokens.size()-1 != 0) {
                btnBackUpLogin.setText(userLoginAccountEmail);
            }

            if(accessTokens.size() > 1){
                syncAndroidButton.setEnabled(true);
                syncAndroidButton.setText("Sync");
            }else if(accessTokens.size() <2){
                syncAndroidButton.setEnabled(false);
                syncAndroidButton.setText("Sync (First login to your back-up account)");
            }


            ArrayList<BarEntry> barEntries = new ArrayList<>();
            ArrayList<String> barLabels = new ArrayList<>();
            barEntries.add(new BarEntry(0,new float[] {(float) freeSpace, (float) usageStorage} ));
            barLabels.add("Free space: " + String.format("%.2f", freeSpace) + " GB");
            barLabels.add("Used storage " + String.format("%.2f", usageStorage)+ " GB");

            BarDataSet GCloudStorageBarChartDataSet = new BarDataSet(barEntries, "Google Cloud Storage");
            GCloudStorageBarChartDataSet.setDrawValues(false);
            GCloudStorageBarChartDataSet.setColors(ColorTemplate.COLORFUL_COLORS);

            BarData barData = new BarData(GCloudStorageBarChartDataSet);

            storage_horizontalBarChart.setData(barData);
            storage_horizontalBarChart.invalidate();
            storage_horizontalBarChart.setFitBars(true);
            storage_horizontalBarChart.getDescription().setEnabled(false);
            storage_horizontalBarChart.setDrawGridBackground(false);
            storage_horizontalBarChart.getAxisLeft().setEnabled(false);
            storage_horizontalBarChart.getXAxis().setDrawLabels(false);
            storage_horizontalBarChart.getAxisRight().setEnabled(false);
            storage_horizontalBarChart.getXAxis().setEnabled(false);
            storage_horizontalBarChart.setDrawBorders(false);
            storage_horizontalBarChart.getLegend().setEnabled(true);
            Legend barLegend = storage_horizontalBarChart.getLegend();
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
                barEntry.formColor = GCloudStorageBarChartDataSet.getColor(i);
                BarLegendEntries.add(barEntry);
            }
            barLegend.setCustom(BarLegendEntries);


            if(!androidStorageBarChart.isShown()){
                androidStorageBarChart.setNoDataText("Data for the storage of your device is not available.");
            }
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
        protected void onPreExecute() {
            super.onPreExecute();
            syncToBackUpAccountButton.setText("Wait...");
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
            ArrayList<String> uploadedCounter = new ArrayList<String>();

            NetHttpTransport HTTP_TRANSPORT = null;
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

            HttpRequestInitializer requestInitializer = request -> {
                request.getHeaders().setAuthorization("Bearer " + accessTokens.get(1));
                request.getHeaders().setContentType("application/json");

            };

            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                    .setApplicationName("cso")
                    .build();

            if (directoryFiles != null && directoryFiles.length > 0){
                for (File file : directoryFiles){
                    com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                    fileMetadata.setName(file.getName());

                    int dotIndex = file.getName().lastIndexOf(".");
                    String memeType="";
                    if (dotIndex >= 0 && dotIndex < file.getName().length() - 1) {
                        memeType = file.getName().substring(dotIndex + 1);
                    }

                    FileContent mediaContent = null;
                    if(memeType.equals("jpeg") | memeType.equals("jpg")){
                        mediaContent = new FileContent("image/jpeg" ,new File(file.getPath()));
                        uploadedCounter.add(file.getName());
                    }

                    else if(memeType.equals("png")){
                        mediaContent = new FileContent("image/png" ,new File(file.getPath()));
                        uploadedCounter.add(file.getName());
                    }

                    else if(memeType.equals("gif")){
                        mediaContent = new FileContent("image/gif" ,new File(file.getPath()));
                        uploadedCounter.add(file.getName());
                    }

                    else if(memeType.equals("bmp")){
                        mediaContent = new FileContent("image/bmp" ,new File(file.getPath()));
                        uploadedCounter.add(file.getName());
                    }

                    else if(memeType.equals("mp4")){
                        mediaContent = new FileContent("video/mp4" ,new File(file.getPath()));
                        uploadedCounter.add(file.getName());
                    }

                    else if(memeType.equals("mkv")){
                        mediaContent = new FileContent("video/x-matroska" ,new File(file.getPath()));
                        uploadedCounter.add(file.getName());
                    }

                    //System.out.println(mediaContent);

                    try {
                        com.google.api.services.drive.model.File uploadFile = service.files().create(fileMetadata, mediaContent)
                                .setFields("id")
                                .execute();

                        System.out.println("File ID: " + uploadFile.getId());
                        counter++;

                    } catch (IOException e) {
                        System.out.println("Unable to upload: " + e.getMessage());;
                    }

                }
            }

            String[] children = fileDirectory.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(fileDirectory, children[i]).delete();
            }
            if(fileDirectory.exists()){
                fileDirectory.delete();
            }

            return null;
        }
        protected void onPostExecute(String response) {
            syncToBackUpAccountTextView.setText(counter+ " items were synced");
            syncToBackUpAccountButton.setText("Sync");
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

        private Context context;

        SyncAndroid(Context context) {
            this.context = context;
        }

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


            NetHttpTransport HTTP_TRANSPORT = null;
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

            HttpRequestInitializer requestInitializer = request -> {
                request.getHeaders().setAuthorization("Bearer " + accessTokens.get(1));
                request.getHeaders().setContentType("application/json");

            };

            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                    .setApplicationName("cso")
                    .build();

            List<String> selectedMediaItemsPaths = getGalleryImages();

            int totalFiles = selectedMediaItemsPaths.size();
            int processedFiles = 0;


            int i = 0;
                for(String selectedMediaItemsPath:
                    selectedMediaItemsPaths) {

                    i++;
                    int progress = (int) ((processedFiles / (float) totalFiles) * 100);
                    publishProgress(progress);
                    processedFiles++;

                    File file  = new File(selectedMediaItemsPath);
                    String[] imageAndVideoExtensions = {".jpg", ".jpeg", ".png", ".bmp", ".webp",".heic", ".heif",
                            ".gif", ".mp4", ".mkv",".webm",".3gp",".ts"};

                    String fileName = file.getName();
                    String filePath = file.getPath();
                    String fileHash = null;
                    try {
                        fileHash = calculateHash(filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ArrayList<String> fileInfo = new ArrayList<>();
                    fileInfo.add(fileName);
                    fileInfo.add(filePath);
                    fileInfo.add(fileHash);
                    androidFileInfoList.add(fileInfo);

                    if(isImageOrVideoFile(file, imageAndVideoExtensions) && i<5){


                        String memeType = filePath.substring(filePath.lastIndexOf("."));

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

             



            return new SyncResult(androidFileInfoList , uploadedCounter);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            androidProgressBar.setProgress(progress);
        }


        protected void onPostExecute(SyncResult response) {
            //androidFolderToChooseButton.setVisibility(View.VISIBLE);

            int uploadCounter =0;
            if(androidFileInfoList.size() == 0){
                Intent intent = new Intent(context,AndroidFoldersSelectionActivity.class);
                //private static final int REQUEST_CODE_FOLDER_SELECTION = 1;
                startActivityForResult(intent,123);
            }

            else {
                androidMediaItemsButton.setVisibility(View.VISIBLE);
                ArrayList<ArrayList<String>> resultList1 = response.result1;
                ArrayList<String> resultList2 = response.result2;

                ArrayList<ArrayList<String>> hashed = new ArrayList<ArrayList<String>>();
                ArrayList<String> toAdd = new ArrayList<String>();
                toAdd.add(resultList1.get(0).get(2));
                toAdd.add(resultList1.get(0).get(1));
                hashed.add(toAdd);

                boolean wasInarrayList = false;
                for(int i=1;i<resultList1.size();i++){
                    wasInarrayList = false;
                    for(int j=0;j<hashed.size();j++){
                        if(hashed.get(j).get(0).equals(resultList1.get(i).get(2))){
                            hashed.get(j).add(resultList1.get(i).get(1));
                            wasInarrayList = true;
                            break;
                        }
                    }
                    if(wasInarrayList == false){
                        toAdd = new ArrayList<String>();
                        toAdd.add(resultList1.get(i).get(2));
                        toAdd.add(resultList1.get(i).get(1));
                        hashed.add(toAdd);
                        //System.out.println(resultList1.get(i).get(1) + " was added");
                    }
                }

                for (ArrayList<String> hash: hashed){
                    if(hash.size()> 2){
                        System.out.println("found duplicated files:"+"\n");
                        for(String h: hash){
                            System.out.println(h + " ");
                        }
                        System.out.println("\n");
                    }
                }



                androidProgressBar.setProgress(100);
                androidTextView.setText("Search of your images and videos is finished and you have this number of them: " +
                        resultList1.size() + " and " + resultList2.size() +
                        " of them were uploaded to google drive");
            }
        }

    }

    private class SyncAndroid2 extends AsyncTask<Void, Integer, SyncResult> {

        ArrayList<String> uploadedCounter = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            androidProgressBar.setVisibility(View.VISIBLE);
            androidProgressBar.setProgress(0);
        }

        @Override
        protected SyncResult doInBackground(Void... voids) {
            int uploadCounter =0;
            File rootDirectory = Environment.getExternalStorageDirectory();
            Queue<File> directoriesQueue = new LinkedList<>();
            for(String selectedFolder: selectedFolders){
                File selectedFolderToFile = new File(rootDirectory.toString() +"/" + selectedFolder);
                directoriesQueue.add(selectedFolderToFile);
            }
            while(!directoriesQueue.isEmpty()){
                //getting here
                File currentDirectory = directoriesQueue.poll();
                System.out.println(currentDirectory.getPath());
                File[] files = currentDirectory.listFiles();
                if(files!= null){
                    for(File thisFile:files){
                        if(thisFile.isDirectory() && !thisFile.isHidden()){
                            directoriesQueue.add(thisFile);
                            //totalFiles++;
                        }else if(thisFile.isFile() &&
                                !thisFile.getName().toLowerCase().endsWith("srt")) {
                            //totalFiles++;
                            String[] imageAndVideoExtensions = formats.toArray(
                                    new String[formats.size()]
                            );
                            String fileName = thisFile.getName();
                            String filePath = thisFile.getPath();
                            String fileHash = null;
                            try {
                                fileHash = calculateHash(filePath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if (isImageOrVideoFile(thisFile, imageAndVideoExtensions) &&
                               !fileName.toLowerCase().endsWith("srt")) {
                                ArrayList<String> fileInfo = new ArrayList<>();
                                fileInfo.add(fileName);
                                fileInfo.add(filePath);
                                fileInfo.add(fileHash);
                                androidFileInfoList.add(fileInfo);

                                String memeType = filePath.substring(filePath.lastIndexOf("."));

                                if(uploadCounter < 5){
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
                                        System.out.println(" " +fileName);
                                        uploadCounter++;

                                    } catch (IOException e) {
                                        System.out.println("Unable to upload: " + e.getMessage());;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new SyncResult(androidFileInfoList , uploadedCounter);
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            androidProgressBar.setProgress(progress);
        }


        protected void onPostExecute(SyncResult response) {
            //androidFolderToChooseButton.setVisibility(View.VISIBLE);

                androidMediaItemsButton.setVisibility(View.VISIBLE);
                ArrayList<ArrayList<String>> resultList1 = response.result1;
                ArrayList<String> resultList2 = response.result2;

                androidProgressBar.setProgress(100);
                androidTextView.setText("Search of your images and videos is finished and you have this number of them: " +
                        resultList1.size() + " and " + resultList2.size() +
                        " of them were uploaded to google drive");

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



