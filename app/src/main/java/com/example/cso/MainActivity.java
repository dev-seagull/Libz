    package com.example.cso;

    import android.Manifest;
    import android.app.Activity;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.pm.PackageManager;
    import android.content.res.ColorStateList;
    import android.database.Cursor;
    import android.graphics.Color;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Environment;
    import android.os.Handler;
    import android.os.Looper;
    import android.provider.Settings;
    import android.text.Layout;
    import android.text.Spannable;
    import android.text.SpannableString;
    import android.text.style.AlignmentSpan;
    import android.view.Gravity;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.LinearLayout;
    import android.widget.PopupMenu;
    import android.widget.TextView;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.ActionBarDrawerToggle;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.appcompat.widget.AppCompatButton;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;
    import androidx.core.view.GravityCompat;
    import androidx.drawerlayout.widget.DrawerLayout;

    import com.google.android.material.navigation.NavigationView;
    import com.jaredrummler.android.device.DeviceName;

    import java.io.BufferedReader;
    import java.io.File;
    import java.io.IOException;
    import java.io.InputStreamReader;
    import java.net.HttpURLConnection;
    import java.net.MalformedURLException;
    import java.net.ProtocolException;
    import java.net.URL;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.concurrent.Callable;
    import java.util.concurrent.ExecutionException;
    import java.util.concurrent.Executor;
    import java.util.concurrent.Executors;
    import java.util.concurrent.FutureTask;


    public class MainActivity extends AppCompatActivity {

        private DrawerLayout drawerLayout;
        Button syncToBackUpAccountButton;
        public static Activity activity ;
        static GoogleCloud googleCloud;
        ActivityResultLauncher<Intent> signInToPrimaryLauncher;
        ActivityResultLauncher<Intent> signInToBackUpLauncher;
        GooglePhotos googlePhotos;
        HashMap<String, PrimaryAccountInfo> primaryAccountHashMap = new HashMap<>();
        HashMap<String, BackUpAccountInfo> backUpAccountHashMap = new HashMap<>();
        public static String androidDeviceName;
        Button restoreButton;
        public static String logFileName = "stash_log.txt";
        public static int errorCounter = 0;
        SharedPreferences preferences;
        public static DBHelper dbHelper;

        private Boolean isFirstTime(SharedPreferences preferences){
            Boolean isFirstTime = preferences.getBoolean("isFirstTime", true);
            if(isFirstTime == true){
                System.out.println("Welcome the app");
            }else{
                System.out.println("The app has missed you");
            }
            return isFirstTime;
        }


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            activity = this;
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            int requestCode =1;
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };

            Thread manageAccessThread = new Thread() {
                @Override
                public void run() {
                    try {
                        int buildSdkInt = Build.VERSION.SDK_INT;
                        if (buildSdkInt >= 30) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                if (!Environment.isExternalStorageManager()) {
                                    Intent getPermission = new Intent();
                                    getPermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                    startActivity(getPermission);
                                    while (!Environment.isExternalStorageManager()){
                                        System.out.println("here " + Environment.isExternalStorageManager());
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);

                                        }
                                    }
                                }
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (Environment.isExternalStorageManager()) {
                                LogHandler.saveLog("Starting to get access files from your android device",false);
                                System.out.println("Starting to get access from your android device");
                            }}
                    } catch (Exception e) {
                        LogHandler.saveLog("Failed to get manage external storage when on create thread: " + e.getLocalizedMessage());
                    }
                }
            };

            manageAccessThread.start();

            try {
                manageAccessThread.join();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


            LogHandler.CreateLogFile();
            googleCloud = new GoogleCloud(this);
//            LogHandler.saveLog("--------------------------new run----------------------------",false);
//            LogHandler.saveLog("Build.VERSION.SDK_INT and Build.VERSION_CODES.M : " + Build.VERSION.SDK_INT +
//                    Build.VERSION_CODES.M, false);

            preferences = getPreferences(Context.MODE_PRIVATE);
            dbHelper = new DBHelper(this);
//            if(dbHelper.DATABASE_VERSION < 11) {
            LogHandler.saveLog("Starting to update database from version 1 to version 2.", false);
            Boolean upgradedFrom1To2 = Upgrade.upgradeDataBaseFrom1to2();
            if(upgradedFrom1To2){
                LogHandler.saveLog("Upgraded database from version 1 to version 2", false);
            }else{
                LogHandler.saveLog("Failed to upgrade database from version 1 to version 2", true);
            }


            drawerLayout = findViewById(R.id.drawer_layout);
//            Profile.test();
            NavigationView navigationView = findViewById(R.id.navigationView);
            ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                    this, drawerLayout, R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(actionBarDrawerToggle);
            actionBarDrawerToggle.syncState();
            MenuItem menuItem1 = navigationView.getMenu().findItem(R.id.navMenuItem1);
            String appVersion = BuildConfig.VERSION_NAME;
            SpannableString centeredText = new SpannableString("Version: " + appVersion);
            centeredText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, appVersion.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            menuItem1.setTitle(centeredText);
            AppCompatButton infoButton = findViewById(R.id.infoButton);
            infoButton.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.END));

            Thread manageReadAndWritePermissonsThread = new Thread() {
                @Override
                public void run() {
                    synchronized (manageAccessThread){
                        try{
                            manageAccessThread.join();
                        }catch (Exception e){
                            LogHandler.saveLog("Failed to join delete duplicated drive " +
                                    "thread in customLoginThread : " + e.getLocalizedMessage(),true);
                        }
                    }
                    boolean isWriteAndReadPermissionGranted = (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                            (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                    while(!isWriteAndReadPermissionGranted){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED |
                                        ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                            ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
                        }
                        isWriteAndReadPermissionGranted = (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                    }
                }
            };

            initializeButtons(this,googleCloud);

            syncToBackUpAccountButton = findViewById(R.id.syncToBackUpAccountButton);

            androidDeviceName = DeviceName.getDeviceName();
            Button androidDeviceButton = findViewById(R.id.androidDeviceButton);
            androidDeviceButton.setText(androidDeviceName);

            LinearLayout primaryAccountsButtonsLayout= findViewById(R.id.primaryAccountsButtons);
            LinearLayout backupAccountsButtonsLayout= findViewById(R.id.backUpAccountsButtons);

            Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryAccountsButtonsLayout);
            newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

            Button newBackupLoginButton = googleCloud.createBackUpLoginButton(backupAccountsButtonsLayout);
            newBackupLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

            Thread customLoginThread = new Thread(() ->{
                synchronized (manageReadAndWritePermissonsThread){
                    try{
                        manageReadAndWritePermissonsThread.join();
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to join delete duplicated drive " +
                                "thread in customLoginThread : " + e.getLocalizedMessage(),true);
                    }
                }

                Callable<Void> customLoginCallable = new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivity.this, CustomLoginPage.class);
                                startActivity(intent);
                            }
                        });
                        return null;
                    }
                };

                boolean backUpAccountExists = false;
                String[] columns = {"type"};
                List<String[]> backUpAccounts = DBHelper.getAccounts(columns);
                for(String[] backUpAccount: backUpAccounts){
                    if(backUpAccount[0].equals("backup")){
                        backUpAccountExists = true;
                        break;
                    }
                }

                if (!Profile.profileMapExists() && backUpAccountExists == true){
                    {
                        FutureTask<Void> futureTask = new FutureTask<>(customLoginCallable);
                        new Thread(futureTask).start();
                        try {
                            futureTask.get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });


            runOnUiThread(() ->{
                TextView deviceStorageTextView = findViewById(R.id.deviceStorage);
                deviceStorageTextView.setText("Wait until we get an update of your assets ...");
            });

            Thread deleteRedundantAndroidThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    LogHandler.saveLog("Activity started : Starting to get files from you android device when starting the app",false);
                    dbHelper.deleteRedundantAndroid();
                    synchronized (this){
                        notify();
                    }
                }
            });

            Thread updateAndroidFilesThread = new Thread(() -> {
                synchronized (deleteRedundantAndroidThread){
                    try{
                        deleteRedundantAndroidThread.join();
                    }catch (Exception e){
                        LogHandler.saveLog("failed to join deleteRedundantAndroidThread thread: "  + e.getLocalizedMessage());
                    }
                }
                Android.getGalleryMediaItems(MainActivity.this);
                LogHandler.saveLog("End of getting files from your android device when starting the app.",false);
            });


            Thread deleteRedundantDriveThread = new Thread(() -> {
                synchronized (updateAndroidFilesThread){
                    try{
                        updateAndroidFilesThread.join();
                    }catch (Exception e){
                        LogHandler.saveLog("failed to join updateAndroidFilesThread : " + e.getLocalizedMessage());
                    }
                }

                String[] columns = {"accessToken","userEmail", "type"};
                List<String[]> account_rows = dbHelper.getAccounts(columns);

                for(String[] account_row : account_rows) {
                    String type = account_row[2];
                    if(type.equals("backup")){
                        String userEmail = account_row[1];
                        String accessToken = account_row[0];
                        ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                        ArrayList<String> driveFileIds = new ArrayList<>();

                        for (BackUpAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                            String fileId = driveMediaItem.getId();
                            driveFileIds.add(fileId);
                        }
                        dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
                    }
                }
            });

            Thread updateDriveBackUpThread = new Thread(() -> {
                synchronized (deleteRedundantDriveThread){
                    try {
                        deleteRedundantDriveThread.join();
                    } catch (Exception e) {
                        LogHandler.saveLog("failed to join deleteRedundantDrive thread: " + e.getLocalizedMessage());
                    }
                }

                String[] columns = {"accessToken", "userEmail","type"};
                List<String[]> account_rows = dbHelper.getAccounts(columns);

                for(String[] account_row : account_rows){
                    String type = account_row[2];
                    if (type.equals("backup")){
                        String accessToken = account_row[0];
                        String userEmail = account_row[1];
                        ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                        for(BackUpAccountInfo.MediaItem driveMediaItem: driveMediaItems){
                            Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
                            if (last_insertId != -1) {
                                MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
                                        driveMediaItem.getHash(), userEmail);
                            } else {
                                LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
                            }
                        }
                    }
                }
            });

            Thread deleteDuplicatedInDrive = new Thread(() -> {
                synchronized (updateDriveBackUpThread){
                    try {
                        updateDriveBackUpThread.join();
                    }catch (Exception e){
                        LogHandler.saveLog("failed to join driveBackUpThread : " + e.getLocalizedMessage());
                    }
                }

                String[] columns = {"accessToken","userEmail", "type"};
                List<String[]> account_rows = dbHelper.getAccounts(columns);

                for(String[] account_row : account_rows) {
                    String type = account_row[2];
                    if(type.equals("backup")){
                        String userEmail = account_row[1];
                        String accessToken = account_row[0];
                        GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                    }
                }
            });

            Thread updateUIThread =  new Thread(() -> {
                synchronized (deleteDuplicatedInDrive){
                    try{
                        deleteDuplicatedInDrive.join();
                    }catch (Exception e){
                        LogHandler.saveLog("failed to join androidUploadThread : "  + e.getLocalizedMessage());
                    }
                }
                runOnUiThread(() -> {
                    TextView deviceStorage = findViewById(R.id.deviceStorage);
                    ArrayList<String> storage =  Android.getAndroidDeviceStorage();
                    deviceStorage.setText("Total space: " + storage.get(0) +
                            "\n" + "free space: " + storage.get(1) + "\n");

                    TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
                    androidStatisticsTextView.setVisibility(View.VISIBLE);
                    int total_androidAssets_count = dbHelper.countAndroidAssets();
                    androidStatisticsTextView.setText("Android assets: " + total_androidAssets_count +
                            "\n" + "synced android assets: " +
                            dbHelper.countAndroidSyncedAssets());
                });
            });

            LogHandler.saveLog("--------------------------Start of app----------------------------",false);

            manageReadAndWritePermissonsThread.start();
            customLoginThread.start();
            deleteRedundantAndroidThread.start();
            updateAndroidFilesThread.start();
            deleteRedundantDriveThread.start();
            updateDriveBackUpThread.start();
            deleteDuplicatedInDrive.start();
            updateUIThread.start();
//            if(errorCounter == 0){
//                LogHandler.deleteLogFile();
//            }

            System.out.println(" here 1 : " +  errorCounter );
            LogHandler.saveLog("--------------------------first threads were finished----------------------------",false);


            restoreButton = findViewById(R.id.restoreButton);
            restoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int buildSdkInt = Build.VERSION.SDK_INT;
                    if (!dbHelper.backupAccountExists()){
                        runOnUiThread(() ->{
                            TextView restoreTextView = findViewById(R.id.restoreTextView);
                            restoreTextView.setText("First Login to a backup account");
                        });
                        return;
                    }
                    runOnUiThread(() ->{
                        syncToBackUpAccountButton.setClickable(false);
                        restoreButton.setClickable(false);
                        TextView restoreTextView = findViewById(R.id.restoreTextView);
                        restoreTextView.setText("Wait until the restoring process is finished");
                    });
                    Thread deleteRedundantAndroidThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            LogHandler.saveLog("Starting to get files from you android device",false);
                            dbHelper.deleteRedundantAndroid();
                            synchronized (this){
                                notify();
                            }
                        }
                    });

                    Thread updateAndroidFilesThread = new Thread(() -> {
                        synchronized (deleteRedundantAndroidThread){
                            try{
                                deleteRedundantAndroidThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join deleteRedundantAndroidThread thread: "  + e.getLocalizedMessage());
                            }
                        }
                        Android.getGalleryMediaItems(MainActivity.this);
                        LogHandler.saveLog("End of getting files from your android device",false);
                    });


                    Thread deleteRedundantDriveThread = new Thread(() -> {
                        synchronized (updateAndroidFilesThread){
                            try{
                                updateAndroidFilesThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join updateAndroidFilesThread : " + e.getLocalizedMessage());
                            }
                        }

                        String[] columns = {"accessToken","userEmail", "type"};
                        List<String[]> account_rows = dbHelper.getAccounts(columns);

                        for(String[] account_row : account_rows) {
                            String type = account_row[2];
                            if(type.equals("backup")){
                                String userEmail = account_row[1];
                                String accessToken = account_row[0];
                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                ArrayList<String> driveFileIds = new ArrayList<>();

                                for (BackUpAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                                    String fileId = driveMediaItem.getId();
                                    driveFileIds.add(fileId);
                                }
                                dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
                            }
                        }
                    });

                    Thread updateDriveBackUpThread = new Thread(() -> {
                        synchronized (deleteRedundantDriveThread){
                            try {
                                deleteRedundantDriveThread.join();
                            } catch (Exception e) {
                                LogHandler.saveLog("failed to join deleteRedundantDrive thread: " + e.getLocalizedMessage());
                            }
                        }

                        String[] columns = {"accessToken", "userEmail","type"};
                        List<String[]> account_rows = dbHelper.getAccounts(columns);

                        for(String[] account_row : account_rows){
                            String accessToken = account_row[0];
                            String userEmail = account_row[1];
                            String type = account_row[2];
                            if (type.equals("backup")){
                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                for(BackUpAccountInfo.MediaItem driveMediaItem: driveMediaItems){
                                    Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
                                    if (last_insertId != -1) {
                                        MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
                                                driveMediaItem.getHash(), userEmail);
                                    } else {
                                        LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
                                    }
                                }
                            }
                        }
                    });

                    Thread deleteDuplicatedInDrive = new Thread(() -> {
                        synchronized (updateDriveBackUpThread){
                            try {
                                updateDriveBackUpThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join driveBackUpThread : " + e.getLocalizedMessage());
                            }
                        }


                        String[] columns = {"accessToken","userEmail", "type"};
                        List<String[]> account_rows = dbHelper.getAccounts(columns);

                        for(String[] account_row : account_rows) {
                            String type = account_row[2];
                            if(type.equals("backup")){
                                String userEmail = account_row[1];
                                String accessToken = account_row[0];
                                GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                            }
                        }
                    });


                    Thread restoreThread = new Thread() {
                        @Override
                        public void run() {
                            synchronized (deleteDuplicatedInDrive){
                                try{
                                    deleteDuplicatedInDrive.join();
                                }catch (Exception e){
                                    LogHandler.saveLog("failed to join androidUploadThread : "  + e.getLocalizedMessage());
                                }
                            }
                            try {
                                if (buildSdkInt >= 30) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        if (!Environment.isExternalStorageManager()) {
                                            Intent getPermission = new Intent();
                                            getPermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                            startActivity(getPermission);
                                            while (!Environment.isExternalStorageManager()){
                                                System.out.println("here " + Environment.isExternalStorageManager());
                                                try {
                                                    Thread.sleep(500);
                                                    System.out.println("... " + Environment.isExternalStorageManager());
                                                } catch (InterruptedException e) {
                                                    System.out.println("here2" + Environment.isExternalStorageManager());
                                                    throw new RuntimeException(e);

                                                }
                                            }
                                        }
                                    }
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    if (Environment.isExternalStorageManager()) {
                                        LogHandler.saveLog("Starting to restore files from your android device",false);
                                        System.out.println("Starting to restore files from your android device");
                                        Upload.restore(getApplicationContext());
                                    }}
                            } catch (Exception e) {
                                LogHandler.saveLog("kFailed to get manage external storage in restore thread: " + e.getLocalizedMessage());
                            }
                        }
                    };


                    Thread deleteRedundantAndroidThread2 = new Thread(() -> {
                        synchronized (restoreThread){
                            try{
                                restoreThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join restore thread: "  + e.getLocalizedMessage());
                            }
                        }
                        dbHelper.deleteRedundantAndroid();
                    });

                    Thread updateAndroidFilesThread2 = new Thread(() -> {
                        synchronized (deleteRedundantAndroidThread2){
                            try{
                                deleteRedundantAndroidThread2.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join deleteRedundantAndroidThread thread: "  + e.getLocalizedMessage());
                            }
                        }
                        Android.getGalleryMediaItems(MainActivity.this);
                        LogHandler.saveLog("End of getting files from your android device",false);
                    });


                    Thread updateUIThread =  new Thread(() -> {
                        synchronized (updateAndroidFilesThread2){
                            try{
                                updateAndroidFilesThread2.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join updateAndroidFilesThread2 in restoring : "  + e.getLocalizedMessage());
                            }
                        }
                        runOnUiThread(() -> {
                            syncToBackUpAccountButton.setClickable(true);
                            restoreButton.setClickable(true);
                            TextView deviceStorage = findViewById(R.id.deviceStorage);
                            ArrayList<String> storage =  Android.getAndroidDeviceStorage();
                            deviceStorage.setText("Total space: " + storage.get(0) +
                                    "\n" + "free space: " + storage.get(1) + "\n");

                            TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
                            androidStatisticsTextView.setVisibility(View.VISIBLE);
                            int total_androidAssets_count = dbHelper.countAndroidAssets();
                            androidStatisticsTextView.setText("Android assets: " + total_androidAssets_count +
                                    "\n" + "synced android assets: " +
                                    dbHelper.countAndroidSyncedAssets());
                            NotificationHandler.sendNotification("1","restoringAlert", MainActivity.this,
                                    "Restoring is finished","You're files are restored!");
                            TextView restoreTextView = findViewById(R.id.restoreTextView);
                            restoreTextView.setText("restoring process is finished");
                        });
                    });

                    deleteRedundantAndroidThread.start();
                    updateAndroidFilesThread.start();
                    deleteRedundantDriveThread.start();
                    updateDriveBackUpThread.start();
                    deleteDuplicatedInDrive.start();
                    restoreThread.start();
                    deleteRedundantAndroidThread2.start();
                    updateAndroidFilesThread2.start();
                    updateUIThread.start();
                }
            });
        }

        @Override
        protected void onStart(){
            super.onStart();
            runOnUiThread(this::updateButtonsListeners);
            try{
                googleCloud = new GoogleCloud(this);
                googlePhotos = new GooglePhotos();
            }catch (Exception e){
                LogHandler.saveLog("failed to initialize the classes: " + e.getLocalizedMessage());
            }

            signInToPrimaryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == RESULT_OK){
                        LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
                        View[] childview = {primaryAccountsButtonsLinearLayout.getChildAt(
                                primaryAccountsButtonsLinearLayout.getChildCount() - 1)};
                        runOnUiThread(() -> childview[0].setClickable(false));
                        Executor signInExecutor = Executors.newSingleThreadExecutor();
                        try {
                            Runnable backgroundTask = () -> {
                                String userEmail = googleCloud.handleSignInToPrimaryResult(result.getData());
                                runOnUiThread(() -> {
                                    if (userEmail != "f") {
                                        childview[0] = primaryAccountsButtonsLinearLayout.getChildAt(
                                                primaryAccountsButtonsLinearLayout.getChildCount() - 2);
                                        LogHandler.saveLog(userEmail + " has logged in to the primary account", false);
                                        if (childview[0] instanceof Button) {
                                            Button bt = (Button) childview[0];
                                            bt.setText(userEmail);
                                            bt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D47A1")));
                                        }
                                    }else{
                                            LogHandler.saveLog("login with launcher failed");
                                            LinearLayout primaryAccountsButtonsLinearLayout2 = findViewById(R.id.primaryAccountsButtons);
                                            View childview2 = primaryAccountsButtonsLinearLayout2.getChildAt(
                                                    primaryAccountsButtonsLinearLayout2.getChildCount() - 1);
                                            if (childview2 instanceof Button) {
                                                Button bt = (Button) childview2;
                                                bt.setText("ADD A PRIMARY ACCOUNT");
                                            }
                                    }
                                    updateButtonsListeners();
                                });
                            };
                            signInExecutor.execute(backgroundTask);
                            runOnUiThread(() -> childview[0].setClickable(true));
                        }catch (Exception e){
                            LogHandler.saveLog("Failed to sign in to primary : "  + e.getLocalizedMessage());
                        }
                   }else{
                        runOnUiThread(() -> {
                            LogHandler.saveLog("login with launcher failed");
                            LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
                            View childview = primaryAccountsButtonsLinearLayout.getChildAt(
                                    primaryAccountsButtonsLinearLayout.getChildCount() - 1);
                            if(childview instanceof Button){
                                Button bt = (Button) childview;
                                bt.setText("ADD A PRIMARY ACCOUNT");
                            }
                            updateButtonsListeners();
                        });


                    }
                }
            );
            signInToBackUpLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == RESULT_OK){
                        LinearLayout backupAccountsButtonsLinearLayout = findViewById(R.id.backUpAccountsButtons);
                        final View[] childview = {backupAccountsButtonsLinearLayout.getChildAt(
                                backupAccountsButtonsLinearLayout.getChildCount() - 1)};
                        runOnUiThread(() -> childview[0].setClickable(false));


                        Executor signInToBackupExecutor = Executors.newSingleThreadExecutor();
                        try{
                            Runnable backgroundTask = () -> {
                                String userEmail = googleCloud.handleSignInToBackupResult(result.getData());
                                runOnUiThread(() -> {
                                    if (userEmail != "f") {
                                        LogHandler.saveLog(userEmail + " has logged in to the backup account", false);
                                        childview[0] = backupAccountsButtonsLinearLayout.getChildAt(
                                                backupAccountsButtonsLinearLayout.getChildCount() - 2);
                                        if (childview[0] instanceof Button) {
                                            Button bt = (Button) childview[0];
                                            bt.setText(userEmail);
                                            bt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#42A5F5")));
                                        }
                                    } else {
                                        LogHandler.saveLog(userEmail + " has logged in to the backup account", false);
                                        childview[0] = backupAccountsButtonsLinearLayout.getChildAt(
                                                backupAccountsButtonsLinearLayout.getChildCount() - 2);
                                        if (childview[0] instanceof Button) {
                                            Button bt = (Button) childview[0];
                                            bt.setText(userEmail);
                                            bt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#42A5F5")));
                                        }
                                    }
                                    updateButtonsListeners();
                                });
                            };
                            signInToBackupExecutor.execute(backgroundTask);

                            Thread deleteRedundantDriveThread = new Thread(() -> {
                                synchronized (this){
                                    notify();
                                }

                                try{
                                    String[] columns = {"accessToken","userEmail", "type"};
                                    List<String[]> accounts_rows = dbHelper.getAccounts(columns);

                                    for(String[] account_row : accounts_rows) {
                                        String type = account_row[2];
                                        if(type.equals("backup")){
                                            String userEmail = account_row[1];
                                            String accessToken = account_row[0];
                                            ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                            ArrayList<String> driveFileIds = new ArrayList<>();

                                            for (BackUpAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                                                String fileId = driveMediaItem.getId();
                                                driveFileIds.add(fileId);
                                            }
                                            dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
                                        }
                                    }
                                }catch (Exception e){
                                    LogHandler.saveLog("failed in deleteRedundantDriveThread : " + e.getLocalizedMessage());
                                }
                            });

                            Thread updateDriveBackUpThread = new Thread(() -> {
                                synchronized (deleteRedundantDriveThread){
                                    try {
                                        deleteRedundantDriveThread.join();
                                    } catch (Exception e) {
                                        LogHandler.saveLog("failed to join deleteRedundantDrive thread: " + e.getLocalizedMessage());
                                    }
                                }

                                String[] columns = {"accessToken", "userEmail","type"};
                                List<String[]> account_rows = dbHelper.getAccounts(columns);

                                for(String[] account_row : account_rows){
                                    String type = account_row[2];
                                    if (type.equals("backup")){
                                        String accessToken = account_row[0];
                                        String userEmail = account_row[1];
                                        ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                        for(BackUpAccountInfo.MediaItem driveMediaItem: driveMediaItems){
                                            Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
                                            if (last_insertId != -1) {
                                                MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
                                                        driveMediaItem.getHash(), userEmail);
                                            } else {
                                                LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
                                            }
                                        }
                                    }
                                }
                            });

                            Thread deleteDuplicatedInDrive = new Thread(() -> {
                                synchronized (updateDriveBackUpThread){
                                    try {
                                        updateDriveBackUpThread.join();
                                    }catch (Exception e){
                                        LogHandler.saveLog("failed to join driveBackUpThread : " + e.getLocalizedMessage());
                                    }
                                }

                                String[] columns = {"accessToken","userEmail", "type"};
                                List<String[]> account_rows = dbHelper.getAccounts(columns);

                                for(String[] account_row : account_rows) {
                                    String type = account_row[2];
                                    if(type.equals("backup")){
                                        String userEmail = account_row[1];
                                        String accessToken = account_row[0];
                                        GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                                    }
                                }
                            });

                            Thread customLoginThread = new Thread(() ->{
                                synchronized (deleteDuplicatedInDrive){
                                    try{
                                        deleteDuplicatedInDrive.join();
                                    }catch (Exception e){
                                        LogHandler.saveLog("Failed to join delete duplicated drive " +
                                                "thread in customLoginThread : " + e.getLocalizedMessage(),true);
                                    }
                                }

                                Callable<Void> customLoginCallable = new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        Handler handler = new Handler(Looper.getMainLooper());
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent intent = new Intent(MainActivity.this, CustomLoginPage.class);
                                                startActivity(intent);
                                            }
                                        });
                                        return null;
                                    }
                                };
                                if (!Profile.profileMapExists()){
                                    {
                                        FutureTask<Void> futureTask = new FutureTask<>(customLoginCallable);
                                        new Thread(futureTask).start();
                                        try {
                                            futureTask.get();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        } catch (ExecutionException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });

                            deleteRedundantDriveThread.start();
                            updateDriveBackUpThread.start();
                            deleteDuplicatedInDrive.start();
                            customLoginThread.start();

                            runOnUiThread(() -> {
                                childview[0].setClickable(true);
                                TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
                                androidStatisticsTextView.setVisibility(View.VISIBLE);
                                int total_androidAssets_count = dbHelper.countAndroidAssets();
                                androidStatisticsTextView.setText("Android assets: " + total_androidAssets_count +
                                        "\n" + "synced android assets: " +
                                        dbHelper.countAndroidSyncedAssets());

                            });
                        }catch (Exception e){
                            LogHandler.saveLog("Failed to sign in to backup : "  + e.getLocalizedMessage());
                        }
                    }else{
                        runOnUiThread(() -> {
                            LogHandler.saveLog("login with back up launcher failed with response code :" + result.getResultCode());
                            LinearLayout backupAccountsButtonsLinearLayout = findViewById(R.id.backUpAccountsButtons);
                            View childview = backupAccountsButtonsLinearLayout.getChildAt(
                                    backupAccountsButtonsLinearLayout.getChildCount() - 1);
                            if(childview instanceof Button){
                                Button bt = (Button) childview;
                                bt.setText("ADD A BACK UP ACCOUNT");
                            }
                            updateButtonsListeners();
                        });
                }
            });

            syncToBackUpAccountButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!dbHelper.backupAccountExists()){
                        runOnUiThread(() ->{
                            TextView syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
                            syncToBackUpAccountTextView.setText("First Login to a backup account");
                        });
                        return;
                    }
                    runOnUiThread(() ->{
                        restoreButton.setClickable(false);
                        syncToBackUpAccountButton.setClickable(false);
                        TextView syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
                        syncToBackUpAccountTextView.setText("Wait until the uploading process is finished");
                    });

                    Thread deleteRedundantAndUpdatePhotos = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String[] columns = {"accessToken","userEmail", "type"};
                            List<String[]> account_rows = dbHelper.getAccounts(columns);
                            File destinationFolder = new File(Environment
                                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator
                                    + "cso");
                            for(String[] account_row : account_rows) {
                                String type = account_row[2];
                                if(type.equals("primary")){
                                    String userEmail = account_row[1];
                                    String accessToken = account_row[0];
                                    ArrayList<GooglePhotos.MediaItem> photosMediaItems =
                                            GooglePhotos.getGooglePhotosMediaItems(accessToken);
                                    ArrayList<String> fileIds = new ArrayList<>();
                                    for(GooglePhotos.MediaItem photosMediaItem: photosMediaItems){
                                        fileIds.add(photosMediaItem.getId());
                                    }
                                    MainActivity.dbHelper.deleteRedundantPhotos(fileIds,userEmail);
                                    Upload.downloadFromPhotos(photosMediaItems,destinationFolder,userEmail);
                                }
                            }
                            synchronized (this){
                                notify();
                            }
                        }
                    });

                    Thread uploadPhotosToDriveThread = new Thread(() -> {
                        int buildSdkInt = Build.VERSION.SDK_INT;
                        synchronized (deleteRedundantAndUpdatePhotos){
                            try{
                                deleteRedundantAndUpdatePhotos.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join deleteRedundantAndUpdatePhotos : "  + e.getLocalizedMessage());
                            }
                        }
                        try {
                            if (buildSdkInt >= 30) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    if (!Environment.isExternalStorageManager()) {
                                        Intent getPermission = new Intent();
                                        getPermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                        startActivity(getPermission);
                                        while (!Environment.isExternalStorageManager()){
                                            System.out.println("here " + Environment.isExternalStorageManager());
                                            try {
                                                Thread.sleep(500);
                                                System.out.println("... " + Environment.isExternalStorageManager());
                                            } catch (InterruptedException e) {
                                                System.out.println("here2" + Environment.isExternalStorageManager());
                                                throw new RuntimeException(e);

                                            }
                                        }
                                    }
                                }
                            }
                            System.out.println("here Build.VERSION.SDK_INT >= Build.VERSION_CODES.R" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                if (Environment.isExternalStorageManager()) {
                                    String[] columns = {"userEmail", "type" ,"accessToken"};
                                    List<String[]> selectedRows = dbHelper.getAccounts(columns);
                                    for(String[] selectedRow: selectedRows){
                                        String destinationUserEmail = selectedRow[0];
                                        String type = selectedRow[1];
                                        String accessToken = selectedRow[2];
                                        if(type.equals("backup")){
                                            Upload upload = new Upload();
                                            upload.uploadPhotosToDrive(destinationUserEmail,accessToken);
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LogHandler.saveLog("Failed to get manage external storage in photos thread: " + e.getLocalizedMessage());
                        }
                    });


                    Thread deletePhotosFromAndroidThread = new Thread(() -> {
                        synchronized (uploadPhotosToDriveThread){
                            try{
                                uploadPhotosToDriveThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join uploadPhotosToDriveThread : "  + e.getLocalizedMessage());
                            }
                        }
                        Upload.deletePhotosFromAndroid();
                    });


                    Thread deleteRedundantAndroidThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (deletePhotosFromAndroidThread){
                                try {
                                    deletePhotosFromAndroidThread.join();
                                } catch (InterruptedException e) {
                                    LogHandler.saveLog("Failed to join deleteRedundantAndUpdatePhotos" +
                                            " in deleteRedundantAndroidThread: "+ e.getLocalizedMessage());
                                }
                            }
                            LogHandler.saveLog("Starting to get files from you android device",false);
                            dbHelper.deleteRedundantAndroid();
                        }
                    });

                    Thread updateAndroidFilesThread = new Thread(() -> {
                        synchronized (deleteRedundantAndroidThread){
                            try{
                                deleteRedundantAndroidThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join deleteRedundantAndroidThread thread: "  + e.getLocalizedMessage());
                            }
                        }
                        Android.getGalleryMediaItems(MainActivity.this);
                        LogHandler.saveLog("End of getting files from your android device",false);
                    });


                    Thread deleteRedundantDriveThread = new Thread(() -> {
                        synchronized (updateAndroidFilesThread){
                            try{
                                updateAndroidFilesThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join updateAndroidFilesThread : " + e.getLocalizedMessage());
                            }
                        }

                        String[] columns = {"accessToken","userEmail", "type"};
                        List<String[]> account_rows = dbHelper.getAccounts(columns);

                        for(String[] account_row : account_rows) {
                            String type = account_row[2];
                            if(type.equals("backup")){
                                String userEmail = account_row[1];
                                String accessToken = account_row[0];
                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                ArrayList<String> driveFileIds = new ArrayList<>();

                                for (BackUpAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                                    String fileId = driveMediaItem.getId();
                                    driveFileIds.add(fileId);
                                }
                                dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
                            }
                        }
                    });

                    Thread driveBackUpThread = new Thread(() -> {
                        synchronized (deleteRedundantDriveThread){
                            try {
                                deleteRedundantDriveThread.join();
                            } catch (Exception e) {
                                LogHandler.saveLog("failed to join deleteRedundantDrive thread: " + e.getLocalizedMessage());
                            }
                        }

                        String[] columns = {"accessToken", "userEmail","type"};
                        List<String[]> account_rows = dbHelper.getAccounts(columns);

                        for(String[] account_row : account_rows){
                            String type = account_row[2];
                            if (type.equals("backup")){
                                String accessToken = account_row[0];
                                String userEmail = account_row[1];
                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                for(BackUpAccountInfo.MediaItem driveMediaItem: driveMediaItems){
                                    Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
                                    if (last_insertId != -1) {
                                        MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
                                                driveMediaItem.getHash(), userEmail);
                                    } else {
                                        LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
                                    }
                                }
                            }
                        }
                    });

                    Thread deleteDuplicatedInDrive = new Thread(() -> {
                        synchronized (driveBackUpThread){
                            try {
                                driveBackUpThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join driveBackUpThread : " + e.getLocalizedMessage());
                            }
                        }


                        String[] columns = {"accessToken","userEmail", "type"};
                        List<String[]> account_rows = dbHelper.getAccounts(columns);

                        for(String[] account_row : account_rows) {
                            String type = account_row[2];
                            if(type.equals("backup")){
                                String userEmail = account_row[1];
                                String accessToken = account_row[0];
                                GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                            }
                        }
                    });


                    Thread androidUploadThread = new Thread(() -> {
                        synchronized (deleteDuplicatedInDrive){
                            try{
                                deleteDuplicatedInDrive.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join deleteDuplicatedInDrive : "  + e.getLocalizedMessage());
                            }
                        }
                        Upload upload = new Upload();
                        upload.uploadAndroidToDrive();
                    });

                    Thread deleteRedundantDriveThread2 = new Thread(() -> {
                        synchronized (androidUploadThread){
                            try{
                                androidUploadThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join updateAndroidFilesThread : " + e.getLocalizedMessage());
                            }
                        }

                        String[] columns = {"accessToken","userEmail", "type"};
                        List<String[]> account_rows = dbHelper.getAccounts(columns);

                        for(String[] account_row : account_rows) {
                            String type = account_row[2];
                            if(type.equals("backup")){
                                String userEmail = account_row[1];
                                String accessToken = account_row[0];
                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                ArrayList<String> driveFileIds = new ArrayList<>();

                                for (BackUpAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                                    String fileId = driveMediaItem.getId();
                                    driveFileIds.add(fileId);
                                }
                                dbHelper.deleteRedundantDrive(driveFileIds, userEmail);
                            }
                        }
                    });

                    Thread driveBackUpThread2 = new Thread(() -> {
                        synchronized (deleteRedundantDriveThread2){
                            try {
                                deleteRedundantDriveThread2.join();
                            } catch (Exception e) {
                                LogHandler.saveLog("failed to join deleteRedundantDrive thread: " + e.getLocalizedMessage());
                            }
                        }

                        String[] columns = {"accessToken", "userEmail","type"};
                        List<String[]> account_rows = dbHelper.getAccounts(columns);

                        for(String[] account_row : account_rows){
                            String type = account_row[2];
                            if (type.equals("backup")){
                                String accessToken = account_row[0];
                                String userEmail = account_row[1];
                                ArrayList<BackUpAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken);
                                for(BackUpAccountInfo.MediaItem driveMediaItem: driveMediaItems){
                                    Long last_insertId = MainActivity.dbHelper.insertAssetData(driveMediaItem.getHash());
                                    if (last_insertId != -1) {
                                        MainActivity.dbHelper.insertIntoDriveTable(last_insertId, driveMediaItem.getId(), driveMediaItem.getFileName(),
                                                driveMediaItem.getHash(), userEmail);
                                    } else {
                                        LogHandler.saveLog("Failed to insert file into drive table: " + driveMediaItem.getFileName());
                                    }
                                }
                            }
                        }
                    });

                    Thread deleteDuplicatedInDrive2 = new Thread(() -> {
                        synchronized (driveBackUpThread2){
                            try {
                                driveBackUpThread2.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join driveBackUpThread : " + e.getLocalizedMessage());
                            }
                        }


                        String[] columns = {"accessToken","userEmail", "type"};
                        List<String[]> account_rows = dbHelper.getAccounts(columns);

                        for(String[] account_row : account_rows) {
                            String type = account_row[2];
                            if(type.equals("backup")){
                                String userEmail = account_row[1];
                                String accessToken = account_row[0];
                                GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                            }
                        }

                        String DBSqlQuery = "SELECT * FROM BACKUPDB";
                        Cursor cursor = dbHelper.dbReadable.rawQuery(DBSqlQuery, null);
                        if(cursor != null && cursor.moveToFirst()){
                            System.out.println("cur no null");
                            int fileIdColumnIndex = cursor.getColumnIndex("fileId");
                            int userEmailColumnIndex = cursor.getColumnIndex("userEmail");

                            if(fileIdColumnIndex >= 0 && userEmailColumnIndex >= 0){
                                String fileId = cursor.getString(fileIdColumnIndex);
                                String userEmail = cursor.getString(userEmailColumnIndex);
                                String driveBackupAccessToken = "";
                                String[] drive_backup_selected_columns = {"userEmail", "type", "accessToken"};
                                List<String[]> drive_backUp_accounts = MainActivity.dbHelper.getAccounts(drive_backup_selected_columns);
                                for (String[] drive_backUp_account : drive_backUp_accounts) {
                                    if (drive_backUp_account[1].equals("backup") && drive_backUp_account[0].equals(userEmail)) {
                                        driveBackupAccessToken = drive_backUp_account[2];
                                        break;
                                    }
                                }

                                System.out.println("drive token to delete database: "+ driveBackupAccessToken);
                                URL url = null;
                                try {
                                    url = new URL("https://www.googleapis.com/drive/v3/files/" + fileId);
                                } catch (MalformedURLException e) {
                                    LogHandler.saveLog("failed to set url to delete backup database");
                                }
                                for(int i=0; i<3; i++){
                                    HttpURLConnection connection = null;
                                    try {
                                        connection = (HttpURLConnection) url.openConnection();
                                    } catch (IOException e) {
                                        LogHandler.saveLog("failed to set connection to delete backup database");
                                    }
                                    try {
                                        connection.setRequestMethod("DELETE");
                                    } catch (ProtocolException e) {
                                        LogHandler.saveLog("failed to set delete request method to delete backup database");
                                    }
                                    connection.setRequestProperty("Content-type", "application/json");
                                    connection.setRequestProperty("Authorization", "Bearer " + driveBackupAccessToken);
                                    int responseCode = 0;
                                    try {
                                        responseCode = connection.getResponseCode();
                                    } catch (IOException e) {
                                        LogHandler.saveLog("failed to get response code of deleting backup database");
                                    }
                                    LogHandler.saveLog("responseCode of deleting duplicate drive : " + responseCode,false);
                                    if(responseCode == HttpURLConnection.HTTP_NO_CONTENT){
                                        String deleteQuery = "DELETE FROM BACKUPDB WHERE UserEmail = ?  and fileId = ? ";
                                        dbHelper.dbWritable.execSQL(deleteQuery, new String[]{userEmail, fileId});
                                        break;
                                    }else{
                                        BufferedReader bufferedReader = null;
                                        try {
                                            bufferedReader = new BufferedReader(
                                                    new InputStreamReader(connection.getInputStream() != null ? connection.getErrorStream() : connection.getInputStream())
                                            );
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        StringBuilder responseBuilder = new StringBuilder();
                                        String line;
                                        while (true) {
                                            try {
                                                if (!((line = bufferedReader.readLine()) != null))
                                                    break;
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                            responseBuilder.append(line);
                                        }
                                        String response = responseBuilder.toString();
                                        System.out.println(response);
                                        LogHandler.saveLog("Retrying to delete backup database " +
                                            "from Drive back up account " + userEmail +
                                            " with response code of " + responseCode);
                                    }
                                }
                            }
                        }

                        List<String> result = dbHelper.backUpDataBase(getApplicationContext());
                        String userEmailDatabase = result.get(0);
                        String databaseFileId = result.get(1);
                        String sqlQuery = "INSERT INTO BACKUPDB(userEmail, fileId) VALUES (?,?)";
                        dbHelper.dbWritable.execSQL(sqlQuery,new String[]{userEmailDatabase,databaseFileId});
                    });

                    Thread updateUIThread =  new Thread(() -> {
                        synchronized (deleteDuplicatedInDrive2){
                            try{
                                deleteDuplicatedInDrive2.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join androidUploadThread : "  + e.getLocalizedMessage());
                            }
                        }
                        runOnUiThread(() -> {
                            restoreButton.setClickable(true);
                            syncToBackUpAccountButton.setClickable(true);
                            TextView deviceStorage = findViewById(R.id.deviceStorage);
                            ArrayList<String> storage =  Android.getAndroidDeviceStorage();
                            deviceStorage.setText("Total space: " + storage.get(0) +
                                    "\n" + "free space: " + storage.get(1) + "\n");

                            TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
                            androidStatisticsTextView.setVisibility(View.VISIBLE);
                            int total_androidAssets_count = dbHelper.countAndroidAssets();
                            androidStatisticsTextView.setText("Android assets: " + total_androidAssets_count +
                                    "\n" + "synced android assets: " +
                                    dbHelper.countAndroidSyncedAssets());
                            NotificationHandler.sendNotification("1","syncingAlert", MainActivity.this,
                                            "Syncing is finished","You're files are backed-up!");
                            TextView syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
                            syncToBackUpAccountTextView.setText("Uploading process is finished");
                        });
                    });

                    deleteRedundantAndUpdatePhotos.start();
                    uploadPhotosToDriveThread.start();
                    deletePhotosFromAndroidThread.start();
                    deleteRedundantAndroidThread.start();
                    updateAndroidFilesThread.start();
                    deleteRedundantDriveThread.start();
                    driveBackUpThread.start();
                    deleteDuplicatedInDrive.start();
                    androidUploadThread.start();
                    deleteRedundantDriveThread2.start();
                    driveBackUpThread2.start();
                    deleteDuplicatedInDrive2.start();
                    updateUIThread.start();

                }
            });
        }




        private void updateButtonsListeners() {
            updatePrimaryButtonsListener();
            updateBackupButtonsListener();
        }
        private void updatePrimaryButtonsListener(){
            LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
            for (int i = 0; i < primaryAccountsButtonsLinearLayout.getChildCount(); i++) {
                View childView = primaryAccountsButtonsLinearLayout.getChildAt(i);
                if (childView instanceof Button) {
                    Button button = (Button) childView;
                    button.setOnClickListener(
                            view -> {
                                syncToBackUpAccountButton.setClickable(false);
                                restoreButton.setClickable(false);
                                String buttonText = button.getText().toString().toLowerCase();
                                if (buttonText.equals("add a primary account")){
                                    button.setText("Wait");
                                    button.setClickable(false);
                                    googleCloud.signInToGoogleCloud(signInToPrimaryLauncher);
                                    button.setClickable(true);
                                }else if (buttonText.equals("wait")){
                                    button.setText("Add a primary account");
                                }
                                else {
                                    PopupMenu popupMenu = new PopupMenu(MainActivity.this,button, Gravity.CENTER);
                                    popupMenu.getMenuInflater().inflate(R.menu.account_button_menu,popupMenu.getMenu());
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        popupMenu.setGravity(Gravity.CENTER);
                                    }
                                    popupMenu.setOnMenuItemClickListener(item -> {
                                        if (item.getItemId() == R.id.sign_out) {
                                            googleCloud.signOut();
                                            dbHelper.deleteAccounts(buttonText,"primary");
                                            String sqlQuery = "DELETE FROM photos WHERE userEmail = ?";
                                            dbHelper.dbWritable.execSQL(sqlQuery, new String[] {buttonText});
                                            dbHelper.deleteRedundantAsset();

                                            ViewGroup parentView = (ViewGroup) button.getParent();
                                            for (Map.Entry<String, PrimaryAccountInfo> primaryAccountEntrySet :
                                                    primaryAccountHashMap.entrySet()) {
                                                if (primaryAccountEntrySet.getKey().equals(buttonText)) {
                                                    String entry = primaryAccountEntrySet.getKey();
                                                    primaryAccountHashMap.remove(
                                                            primaryAccountEntrySet.getKey());
                                                    LogHandler.saveLog("successfully logged out from "+
                                                            entry + " in primary accounts",false);
                                                    LogHandler.saveLog("Number of primary accounts : " + primaryAccountHashMap.size(),false);
                                                    break;
                                                }
                                            }

                                            parentView.removeView(button);
                                        }
                                        return true;
                                    });
                                    popupMenu.show();
                                }
                                syncToBackUpAccountButton.setClickable(true);
                                restoreButton.setClickable(true);
                            }
                        );

                }
            }
        }
        private void updateBackupButtonsListener(){
            LinearLayout backUpAccountsButtonsLinearLayout = findViewById(R.id.backUpAccountsButtons);
            for (int i = 0; i < backUpAccountsButtonsLinearLayout.getChildCount(); i++) {
                View childView = backUpAccountsButtonsLinearLayout.getChildAt(i);
                if (childView instanceof Button) {
                    Button button = (Button) childView;
                    button.setOnClickListener(
                            view -> {
                                syncToBackUpAccountButton.setClickable(false);
                                restoreButton.setClickable(false);
                                String buttonText = button.getText().toString().toLowerCase();
                                if (buttonText.equals("add a back up account")) {
                                    button.setText("Wait");
                                    button.setClickable(false);
                                    googleCloud.signInToGoogleCloud(signInToBackUpLauncher);
                                    button.setClickable(true);
                                } else if (buttonText.equals("wait")){
                                    button.setText("add a back up account");
                                }else {
                                    PopupMenu popupMenu = new PopupMenu(MainActivity.this, button, Gravity.CENTER);
                                    popupMenu.getMenuInflater().inflate(R.menu.account_button_menu, popupMenu.getMenu());
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        popupMenu.setGravity(Gravity.CENTER);
                                    }
                                    popupMenu.setOnMenuItemClickListener(item -> {
                                        if (item.getItemId() == R.id.sign_out) {
                                            googleCloud.signOut();
                                            dbHelper.deleteAccounts(buttonText,"backup");
                                            String sqlQuery = "DELETE FROM drive WHERE userEmail = ?";
                                            dbHelper.dbWritable.execSQL(sqlQuery, new String[] {buttonText});
                                            dbHelper.deleteRedundantAsset();

                                            ViewGroup parentView = (ViewGroup) button.getParent();
                                            for (Map.Entry<String, BackUpAccountInfo> backUpAccountEntrySet :
                                                    backUpAccountHashMap.entrySet()) {
                                                if (backUpAccountEntrySet.getKey().equals(buttonText)) {
                                                    String entry = backUpAccountEntrySet.getKey();
                                                    backUpAccountHashMap.remove(
                                                            backUpAccountEntrySet.getKey());
                                                    LogHandler.saveLog("successfully logged out from "+
                                                            entry + " in backup accounts",false);
                                                    LogHandler.saveLog("Number of backup accounts : " + backUpAccountHashMap.size(),false);
                                                    break;
                                                }
                                            }
                                            parentView.removeView(button);
                                        }
                                        return true;
                                    });
                                    popupMenu.show();
                                }
                                syncToBackUpAccountButton.setClickable(true);
                                restoreButton.setClickable(true);
                            }
                    );
                }
            }
        }

        public static void initializeButtons(Activity activity,GoogleCloud googleCloud){
            String[] columnsList = {"userEmail", "type", "refreshToken"};
            List<String[]> account_rows = dbHelper.getAccounts(columnsList);
            for (String[] account_row : account_rows) {
                String userEmail = account_row[0];
                String type = account_row[1];
                String refreshToken = account_row[2];
                PrimaryAccountInfo.Tokens tokens = googleCloud.requestAccessToken(refreshToken);
                Map<String, Object> updatedValues = new HashMap<String, Object>(){{
                    put("accessToken", tokens.getAccessToken());
                }};

                dbHelper.updateAccounts(userEmail, updatedValues, type);

                if (type.equals("primary")){
                    LinearLayout primaryLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
                    Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryLinearLayout);
                    newGoogleLoginButton.setText(userEmail);
                }else if (type.equals("backup")){
                    LinearLayout backupLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                    Button newGoogleLoginButton = googleCloud.createBackUpLoginButton(backupLinearLayout);
                    newGoogleLoginButton.setText(userEmail);
                }
            }
        }

    }



