package com.example.cso;

import android.Manifest;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.anychart.AnyChartView;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.gson.Gson;
import com.jaredrummler.android.device.DeviceName;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;


public class MainActivity extends AppCompatActivity {

    ArrayList<String> androidImageAndVideoPaths;
    Button mediaItemsLayoutButton;
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
    public String authcodeForGooglePhotos ="";
    public String authcodeForGoogleDrive = "";
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
    GoogleCloud googleCloud;
    AnyChartView primaryAccountsStorageAnyChartView;
    ActivityResultLauncher<Intent> signInToPrimaryLauncher;
    ActivityResultLauncher<Intent> signInToBackUpLauncher;
    GooglePhotos googlePhotos;
    HashMap<String, PrimaryAccountInfo> primaryAccountHashMap = new HashMap<>();
    HashMap<String, BackUpAccountInfo> backUpAccountHashMap = new HashMap<String, BackUpAccountInfo>();


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

    public static boolean isImageOrVideoFile(File file, ArrayList<String> imageExtensions) {
        String filePath = file.getPath().toLowerCase();
        String memeType = filePath.substring(filePath.lastIndexOf("."));
        for (String extension : imageExtensions) {
            if (memeType.equals(extension)) {
                return true;
            }
        }
        return false;
    }


    private ArrayList<String> getGalleryImagesAndVideos() {
        ArrayList<String> mediaItemPaths = new ArrayList<>();

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

    public ArrayList<String> getAndroidImagesAndVideos(){

        while (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        ArrayList<String> imagesAndVideosPaths = getGalleryImagesAndVideos();

        if(imagesAndVideosPaths.isEmpty()){
            Intent intent = new Intent(getApplicationContext(),AndroidFoldersSelectionActivity.class);
            //private static final int REQUEST_CODE_FOLDER_SELECTION = 1;
            startActivityForResult(intent,123);
        }

        System.out.println("number of image and videos path: "+ imagesAndVideosPaths.size());
        return imagesAndVideosPaths;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs",Context.MODE_PRIVATE);
        //String json = sharedPreferences.getString("AndroidImageAndVideoPaths",null);
        //if(json == null){

        androidImageAndVideoPaths = getAndroidImagesAndVideos();
        System.out.println("size of android files: " + androidImageAndVideoPaths.size());

        Button duplicatedbutton = findViewById(R.id.duplicatedbutton);
        TextView dupliacterdTextview = findViewById(R.id.duplicatedTextView);
        duplicatedbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                duplicatedbutton.setText("Wait,it may take a bit long to find duplicated files...");

                                      ArrayList<ArrayList<String>> androidduplicatedfiles = new ArrayList<ArrayList<String>>();
                                      try {
                                          ArrayList<String> newGroup = new ArrayList<String>();
                                          newGroup.add(calculateHash(androidImageAndVideoPaths.get(0)));
                                          newGroup.add(androidImageAndVideoPaths.get(0));
                                          androidduplicatedfiles.add(newGroup);
                                      } catch (IOException e) {
                                          throw new RuntimeException(e);
                                      }
                                      for (int i = 1; i < androidImageAndVideoPaths.size(); i++) {
                                          System.out.println("here");
                                          boolean wasIn = false;
                                          try {
                                              String hash = calculateHash(androidImageAndVideoPaths.get(i));
                                              for (int j = 0; j < androidduplicatedfiles.size(); j++) {
                                                  if (androidduplicatedfiles.get(j).get(0).equals(hash)) {
                                                      androidduplicatedfiles.get(j).add(androidImageAndVideoPaths.get(i));
                                                      wasIn = true;
                                                  }
                                              }
                                              if (wasIn == false) {
                                                  ArrayList<String> newGroup = new ArrayList<String>();
                                                  newGroup.add(calculateHash(androidImageAndVideoPaths.get(i)));
                                                  newGroup.add(androidImageAndVideoPaths.get(i));
                                                  androidduplicatedfiles.add(newGroup);
                                              }

                                          } catch (IOException e) {
                                              throw new RuntimeException(e);
                                          }
                                      }
                                      dupliacterdTextview.setText("dupliacted files: "+ "\n");
                                      for(ArrayList<String> androiduplicate: androidduplicatedfiles){
                                          if(androiduplicate.size() > 2){
                                              for(String s: androiduplicate){
                                                  dupliacterdTextview.append(s + " - ");
                                              }
                                              dupliacterdTextview.append("\n");
                                          }
                                      }

                duplicatedbutton.setText("find duplicated images and videos");
            }
        });

        androidMediaItemsButton = findViewById(R.id.androidMediaItemsButton);
        syncToBackUpAccountButton = findViewById(R.id.syncToBackUpAccountButton);
        syncAndroidButton = findViewById(R.id.syncAndroidDevice);

        mediaItemsLayoutButton = findViewById(R.id.mediaItemsLayout);
        syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
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
            //textViewLoginState.setText("You haven't logged into your google account yet");
        }


        syncAndroidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SyncAndroid syncAndroid = new SyncAndroid();
                syncAndroid.execute();
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
    }
        @Override
    protected void onStart(){
        super.onStart();
        updateButtonsListeners();
        //updatePrimaryAccountsStorageChart();

        try{
            googleCloud = new GoogleCloud(this);
            googlePhotos = new GooglePhotos(this);
        }catch (Exception e){
            Toast.makeText(this,e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
        }

        signInToPrimaryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK){
                    LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
                    PrimaryAccountInfo primaryAccountInfo = googleCloud.handleSignInToPrimaryResult(result.getData(),
                            this, primaryAccountsButtonsLinearLayout);
                    String userEmail = primaryAccountInfo.getUserEmail();
                    primaryAccountHashMap.put(primaryAccountInfo.getUserEmail(), primaryAccountInfo);

                    if(primaryAccountsButtonsLinearLayout.getChildCount() == 1){
                        Button bt = findViewById(R.id.loginButton);
                        bt.setText(userEmail);
                    }else{
                        View childview = primaryAccountsButtonsLinearLayout.getChildAt(
                                primaryAccountsButtonsLinearLayout.getChildCount() - 2);
                        if(childview instanceof Button){
                            Button bt = (Button) childview;
                            bt.setText(userEmail);
                        }
                    }

                    updateButtonsListeners();
                }
            }
        );

        signInToBackUpLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK){
                    LinearLayout backUpAccountsButtonsLinearLayout = findViewById(R.id.backUpAccountsButtons);
                    BackUpAccountInfo backUpAccountInfo = googleCloud.handleSignInToBackupResult(result.getData(),
                            this, backUpAccountsButtonsLinearLayout);
                    String userEmail = backUpAccountInfo.getUserEmail();
                    backUpAccountHashMap.put(backUpAccountInfo.getUserEmail(), backUpAccountInfo);

                    if(backUpAccountsButtonsLinearLayout.getChildCount() == 1){
                        Button bt = findViewById(R.id.backUpLoginButton);
                        bt.setText(userEmail);
                    }else{
                        View childview = backUpAccountsButtonsLinearLayout.getChildAt(
                                backUpAccountsButtonsLinearLayout.getChildCount() - 2);
                        if(childview instanceof Button){
                            Button bt = (Button) childview;
                            bt.setText(userEmail);
                        }
                    }

                    updateButtonsListeners();
                }
            });

        syncToBackUpAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TextView syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
                syncToBackUpAccountTextView.setText("Wait untill the uploading process is finished");

                BackUpAccountInfo firstBackUpAccountInfo = backUpAccountHashMap.values().iterator().next();
                String backUpAccessToken = firstBackUpAccountInfo.getTokens().getAccessToken();
                for (PrimaryAccountInfo primaryAccountInfo : primaryAccountHashMap.values()) {
                    ArrayList<GooglePhotos.MediaItem> mediaItems = primaryAccountInfo.getMediaItems();
                    googlePhotos.uploadToGoogleDrive(mediaItems, backUpAccessToken);
                }

                syncToBackUpAccountTextView.setText("Uploading process is finished");
            }
        });

    }

    private void updateButtonsListeners() {
        LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);

        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        googleCloud.signInToGoogleCloud(signInToPrimaryLauncher);
                        //wait
                    }
                }
        );

        for (int i = 0; i < primaryAccountsButtonsLinearLayout.getChildCount(); i++) {
            View childView = primaryAccountsButtonsLinearLayout.getChildAt(i);

            if (childView instanceof Button) {
                Button button = (Button) childView;
                button.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                googleCloud.signInToGoogleCloud(signInToPrimaryLauncher);
                            }
                        }
                );
            }
        }


        LinearLayout backUpAccountsButtonsLinearLayout = findViewById(R.id.backUpAccountsButtons);

        Button backUpLoginButton = findViewById(R.id.backUpLoginButton);
        backUpLoginButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        googleCloud.signInToGoogleCloud(signInToBackUpLauncher);
                    }
                }
        );

        for (int i = 0; i < backUpAccountsButtonsLinearLayout.getChildCount(); i++) {
            View childView = backUpAccountsButtonsLinearLayout.getChildAt(i);

            if (childView instanceof Button) {
                Button button = (Button) childView;
                button.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                googleCloud.signInToGoogleCloud(signInToBackUpLauncher);
                            }
                        }
                );
            }
        }
    }



    private void updatePrimaryAccountsStorageChart() {
        try {
            HorizontalBarChart horizontalBarChart = findViewById(R.id.StorageHorizontalBarChart);
            horizontalBarChart.getDescription().setEnabled(false);
            //horizontalBarChart.setDrawBarShadow(false);
            horizontalBarChart.setDrawValueAboveBar(true);
            horizontalBarChart.setDrawGridBackground(false);
            horizontalBarChart.getLegend().setEnabled(false); // Hide legends

            XAxis xAxis = horizontalBarChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(false);
            xAxis.setDrawLabels(false);

            YAxis yAxis = horizontalBarChart.getAxisLeft();
            yAxis.setDrawGridLines(false);
            yAxis.setDrawAxisLine(false);
            yAxis.setDrawLabels(false);

            horizontalBarChart.getAxisRight().setEnabled(false);

            // Use a list of Float to store usedStorage values
           // List<Float> chartfloatvalues = new ArrayList<>();

            //List<BarEntry> entries = new ArrayList<>();
            //BarData barData = new BarData();

            Double freeStorage = 0.0;
            int m = 1;

            int[] barColors = {Color.RED, Color.GRAY, Color.GREEN, Color.BLACK, Color.BLUE};

            List<Float> chartfloatvalues = new ArrayList<>();


            ArrayList<BarEntry> barEntries = new ArrayList<>();
            ArrayList<String> barLabels = new ArrayList<>();
            //barEntries.add(new BarEntry(0,new float[] {availableSpace,usedSpace} ));
           // barLabels.add("Free Space: " + availableSpace + " GB");
           // barLabels.add("Used space: " + usedSpace + " GB");

            for (String userEmail : primaryAccountHashMap.keySet()) {
                PrimaryAccountInfo primaryAccountInfo = primaryAccountHashMap.get(userEmail);
                Double totalStorage = primaryAccountInfo.getStorage().getTotalStorage();
                Double usedStorage = primaryAccountInfo.getStorage().getUsedStorage();
                freeStorage += totalStorage - usedStorage;

                // Store usedStorage values in the list
                chartfloatvalues.add(usedStorage.floatValue());
                m++;
            }

            float[] usedStorageArray = new float[chartfloatvalues.size()];
            for (int i = 0; i < chartfloatvalues.size(); i++) {
                usedStorageArray[i] = chartfloatvalues.get(i);
            }

            BarEntry barEntry = new BarEntry(0, usedStorageArray);
            barEntries.add(barEntry);
            //androidStorageBarChartDataSet.(PieDataSet.ValuePosition.OUTSIDE_SLICE);

            // Convert the list of usedStorage values to an array
            barEntries.add(new BarEntry(0,new float[] {freeStorage.floatValue()}));

            for(BarEntry be: barEntries){
                //
            }

            BarDataSet barDataSet = new BarDataSet(barEntries, "");
            barDataSet.setDrawValues(true);// Adjust the width as needed
            barDataSet.setColors(barColors);
            barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);

            BarData barData = new BarData(barDataSet);
            androidStorageBarChart.setData(barData);
            androidStorageBarChart.invalidate();

            //BarEntry barEntry = new BarEntry(m, usedStorageArray);
            //String freeSpaceLabel = "used Space: " + freeStorage;
            //freeSpaceLabel.setD
            //entries.add(barEntry);

           // BarEntry freeSpaceEntry = new BarEntry(m , new float[]{freeStorage.floatValue()});
           // String freeSpaceLabel = "Free Space: " + freeStorage;
           // freeSpaceEntry.setData(freeSpaceLabel);
           // entries.add(freeSpaceEntry);

            // Set the bar data to the horizontalBarChart
            horizontalBarChart.setData(barData);
            horizontalBarChart.invalidate();

            // Configure other properties as needed

            System.out.println("successful2");
        } catch (Exception e) {
            System.out.println("error " + e.getLocalizedMessage());
        }

    }


    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


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

            File rootDirectory = Environment.getExternalStorageDirectory();
            Queue<File> directoriesQueue = new LinkedList<>();
            for(String selectedFolder: selectedFolders){
                File selectedFolderFile = new File(rootDirectory.toString() +"/" + selectedFolder);

                if(selectedFolderFile.exists() && selectedFolderFile.isDirectory()) {
                    directoriesQueue.add(selectedFolderFile);
                }else{
                    System.out.println(selectedFolder + " folder was not found!");
                }
            }

            while (!directoriesQueue.isEmpty()){
                File folder = directoriesQueue.poll();
                File[] files = folder.listFiles();

                if(files!=null){
                    for(File file: files){
                        if(file.isFile()){
                            if(isImageOrVideoFile(file,formats) && !file.getPath().toLowerCase().endsWith(".srt")){
                                androidImageAndVideoPaths.add(file.getPath());
                            }
                        }else if(file.isDirectory()){
                            directoriesQueue.add(file);
                        }
                    }
                }
            }
            System.out.println(androidImageAndVideoPaths.size());
        }
    }


    private static  class TokenRequestResult{
        ArrayList<String> fileNames;
        ArrayList<String> baseurls;

        TokenRequestResult(ArrayList<String> fileNames, ArrayList<String> baseurls){
            this.fileNames = fileNames;
            this.baseurls = baseurls;
        }

    }


    private class GooglePhotosUpload extends AsyncTask<Void, Void, String> {

        int counter = 0;


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


            int totalFiles = androidImageAndVideoPaths.size();
            int processedFiles = 0;


            int i = 0;
                for(String androidImageAndVideoPath:
                    androidImageAndVideoPaths) {

                    i++;
                    int progress = (int) ((processedFiles / (float) totalFiles) * 100);
                    publishProgress(progress);
                    processedFiles++;

                    File file  = new File(androidImageAndVideoPath);
                    ArrayList<String> imageAndVideoExtensions = new ArrayList<String>(){{
                        add(".jpg");
                        add(".jpeg");
                        add(".png");
                        add(".bmp");
                        add(".webp");
                        add(".gif");
                        add(".mp4");
                        add(".mkv");
                        add(".webm");
                    }};

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



