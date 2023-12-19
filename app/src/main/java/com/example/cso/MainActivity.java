    package com.example.cso;

    import android.Manifest;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.pm.PackageManager;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Environment;
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

    import java.io.File;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.concurrent.Executor;
    import java.util.concurrent.Executors;


    public class MainActivity extends AppCompatActivity {

        private DrawerLayout drawerLayout;
        Button syncToBackUpAccountButton;
        GoogleCloud googleCloud;
        ActivityResultLauncher<Intent> signInToPrimaryLauncher;
        ActivityResultLauncher<Intent> signInToBackUpLauncher;
        GooglePhotos googlePhotos;
        HashMap<String, PrimaryAccountInfo> primaryAccountHashMap = new HashMap<>();
        HashMap<String, BackUpAccountInfo> backUpAccountHashMap = new HashMap<>();
        public static String androidDeviceName;
        public static String logFileName;
        SharedPreferences preferences;
        public  static  ArrayList<String> comparator1;

        public  static  ArrayList<String> comparator2;
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
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            int requestCode =1;
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };

            boolean isWriteAndReadPermissionGranted = (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            while(!isWriteAndReadPermissionGranted){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED |
                                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                    ActivityCompat.requestPermissions(this, permissions, requestCode);
                }
                isWriteAndReadPermissionGranted = (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            }
            
            googleCloud = new GoogleCloud(this);
            logFileName = LogHandler.CreateLogFile();
            LogHandler.saveLog("Attention : Don't remove this file - this file makes sure that CSO app is working well.",false);
            LogHandler.saveLog("if you have any questions or problems, please contact us by : ",false);
            LogHandler.saveLog("--------------------------new run----------------------------",false);


            preferences = getPreferences(Context.MODE_PRIVATE);
            dbHelper = new DBHelper(this);
            drawerLayout = findViewById(R.id.drawer_layout);
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

            initializeButtons();

            syncToBackUpAccountButton = findViewById(R.id.syncToBackUpAccountButton);
            TextView androidDeviceNameTextView = findViewById(R.id.androidDeviceTextView);
            androidDeviceName = DeviceName.getDeviceName();
            androidDeviceNameTextView.setText(androidDeviceName);

            LinearLayout primaryAccountsButtonsLayout= findViewById(R.id.primaryAccountsButtons);
            LinearLayout backupAccountsButtonsLayout= findViewById(R.id.backUpAccountsButtons);

            googleCloud.createPrimaryLoginButton(primaryAccountsButtonsLayout);
            googleCloud.createBackUpLoginButton(backupAccountsButtonsLayout);

            runOnUiThread(() ->{
                TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
                androidStatisticsTextView.setText("Wait until we get an update of your assets ...");
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
                List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                for(String[] userProfile_row : userProfile_rows) {
                    String type = userProfile_row[2];
                    if(type.equals("backup")){
                        String userEmail = userProfile_row[1];
                        String accessToken = userProfile_row[0];
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

                String[] columns = {"accessToken", "userEmail"};
                List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                for(String[] userProfile_row : userProfile_rows){
                    String accessToken = userProfile_row[0];
                    String userEmail = userProfile_row[1];
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
                List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                for(String[] userProfile_row : userProfile_rows) {
                    String type = userProfile_row[2];
                    if(type.equals("backup")){
                        String userEmail = userProfile_row[1];
                        String accessToken = userProfile_row[0];
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
                    int total_androidAssets_count = dbHelper.countAndroidAssets();
                    androidStatisticsTextView.setText("Android assets: " + total_androidAssets_count +
                            "\n" + "synced android assets: " +
                            dbHelper.countAndroidSyncedAssets());
                });
            });

            deleteRedundantAndroidThread.start();
            updateAndroidFilesThread.start();
            deleteRedundantDriveThread.start();
            updateDriveBackUpThread.start();
            deleteDuplicatedInDrive.start();
            updateUIThread.start();


            Button restoreButton = findViewById(R.id.restoreButton);
            restoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int buildSdkInt = Build.VERSION.SDK_INT;
                    runOnUiThread(() ->{
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
                        List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                        for(String[] userProfile_row : userProfile_rows) {
                            String type = userProfile_row[2];
                            if(type.equals("backup")){
                                String userEmail = userProfile_row[1];
                                String accessToken = userProfile_row[0];
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

                        String[] columns = {"accessToken", "userEmail"};
                        List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                        for(String[] userProfile_row : userProfile_rows){
                            String accessToken = userProfile_row[0];
                            String userEmail = userProfile_row[1];
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
                        List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                        for(String[] userProfile_row : userProfile_rows) {
                            String type = userProfile_row[2];
                            if(type.equals("backup")){
                                String userEmail = userProfile_row[1];
                                String accessToken = userProfile_row[0];
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
                                LogHandler.saveLog("Failed to get manage external storage in restore thread: " + e.getLocalizedMessage());
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
                            TextView deviceStorage = findViewById(R.id.deviceStorage);
                            ArrayList<String> storage =  Android.getAndroidDeviceStorage();
                            deviceStorage.setText("Total space: " + storage.get(0) +
                                    "\n" + "free space: " + storage.get(1) + "\n");

                            TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
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
                        final View[] childview = {primaryAccountsButtonsLinearLayout.getChildAt(
                                primaryAccountsButtonsLinearLayout.getChildCount() - 1)};
                        runOnUiThread(() -> childview[0].setClickable(false));
                        Executor signInExecutor = Executors.newSingleThreadExecutor();
                        try {
                            Runnable backgroundTask = () -> {
                                String userEmail = googleCloud.handleSignInToPrimaryResult(result.getData());

                                runOnUiThread(() -> {
                                    childview[0] = primaryAccountsButtonsLinearLayout.getChildAt(
                                            primaryAccountsButtonsLinearLayout.getChildCount() - 2);
                                    LogHandler.saveLog(userEmail +  " has logged in to the primary account",false);
                                     if(childview[0] instanceof Button){
                                            Button bt = (Button) childview[0];
                                            bt.setText(userEmail);
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
                                BackUpAccountInfo backUpAccountInfo = googleCloud.handleSignInToBackupResult(result.getData());
                                String userEmail = backUpAccountInfo.getUserEmail();
                                backUpAccountHashMap.put(backUpAccountInfo.getUserEmail(),backUpAccountInfo);
                                LogHandler.saveLog("Number of backup accounts : " + backUpAccountHashMap.size(),false);
                                runOnUiThread(() -> {
                                    LogHandler.saveLog(userEmail +  " has logged in to the backup account",false);
                                    childview[0] = backupAccountsButtonsLinearLayout.getChildAt(
                                                backupAccountsButtonsLinearLayout.getChildCount() - 2);
                                    if(childview[0] instanceof Button){
                                        Button bt = (Button) childview[0];
                                        bt.setText(userEmail);
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
                                    List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                                    for(String[] userProfile_row : userProfile_rows) {
                                        String type = userProfile_row[2];
                                        if(type.equals("backup")){
                                            String userEmail = userProfile_row[1];
                                            String accessToken = userProfile_row[0];
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

                                String[] columns = {"accessToken", "userEmail"};
                                List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                                for(String[] userProfile_row : userProfile_rows){
                                    String accessToken = userProfile_row[0];
                                    String userEmail = userProfile_row[1];
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
                                List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                                for(String[] userProfile_row : userProfile_rows) {
                                    String type = userProfile_row[2];
                                    if(type.equals("backup")){
                                        String userEmail = userProfile_row[1];
                                        String accessToken = userProfile_row[0];
                                        GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                                    }
                                }
                            });

                            deleteRedundantDriveThread.start();
                            updateDriveBackUpThread.start();
                            deleteDuplicatedInDrive.start();

                            runOnUiThread(() -> {
                                childview[0].setClickable(true);
                                TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
                                int total_androidAssets_count = dbHelper.countAndroidAssets();
                                androidStatisticsTextView.setText("Android assets: " + total_androidAssets_count +
                                        "\n" + "synced android assets: " +
                                        dbHelper.countAndroidSyncedAssets());

                            });
                        }catch (Exception e){
                            LogHandler.saveLog("Failed to sign in to backup : "  + e.getLocalizedMessage());
                        }
                    }
                });

            syncToBackUpAccountButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    runOnUiThread(() ->{
                        TextView syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
                        syncToBackUpAccountTextView.setText("Wait until the uploading process is finished");
                    });
//
//                    BackUpAccountInfo firstBackUpAccountInfo = backUpAccountHashMap.values().iterator().next();
//                    PrimaryAccountInfo.Tokens backupTokens = firstBackUpAccountInfo.getTokens();
//                    String backUpAccessToken = firstBackUpAccountInfo.getTokens().getAccessToken();
//                    ArrayList<BackUpAccountInfo.MediaItem> backupMediaItems = firstBackUpAccountInfo.getMediaItems();
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
                        List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                        for(String[] userProfile_row : userProfile_rows) {
                            String type = userProfile_row[2];
                            if(type.equals("backup")){
                                String userEmail = userProfile_row[1];
                                String accessToken = userProfile_row[0];
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

                        String[] columns = {"accessToken", "userEmail"};
                        List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                        for(String[] userProfile_row : userProfile_rows){
                            String accessToken = userProfile_row[0];
                            String userEmail = userProfile_row[1];
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
                        List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                        for(String[] userProfile_row : userProfile_rows) {
                            String type = userProfile_row[2];
                            if(type.equals("backup")){
                                String userEmail = userProfile_row[1];
                                String accessToken = userProfile_row[0];
                                GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                            }
                        }
                    });


//                    Thread photosUploadThread = new Thread(() -> {
//                        synchronized (driveBackUpThread){
//                            try{
//                                driveBackUpThread.join();
//                            }catch (Exception e){
//                                LogHandler.saveLog("failed to join drive back up thread: "  + e.getLocalizedMessage());
//                            }
//                        }
//                        String[] columns = {"userEmail", "type" ,"accessToken"};
//                        List<String[]> selectedRows = dbHelper.getUserProfile(columns);
//
//                        ArrayList
//                        for(String[] selectedRow: selectedRows){
//                            if(type.eq)
//                        }
//                        for(String[] selectedRow: selectedRows){
//                        String userEmail = selectedRow[0];
//                        String type = selectedRow[1];
//                        String accessToken = selectedRow[3];
//                        if(type.equals("primary")){
//                            ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems = firstBackUpAccountInfo.getMediaItems();
//                            ArrayList<GooglePhotos.MediaItem> mediaItems = primaryAccountInfo.getMediaItems();
//                            Upload upload = new Upload();
//
//                            upload.uploadPhotosToGoogleDrive(mediaItems, backUpAccessToken
//                                    ,backUpMediaItems,primaryAccountInfo.getUserEmail(),firstBackUpAccountInfo.getUserEmail());
//                        }
//                    }
//                        for (PrimaryAccountInfo primaryAccountInfo : primaryAccountHashMap.values()) {
//                             }
//                    });
//
                    Thread androidUploadThread = new Thread(() -> {
                        synchronized (deleteDuplicatedInDrive){
                            try{
                                deleteDuplicatedInDrive.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join deleteDuplicatedInDrive : "  + e.getLocalizedMessage());
                            }
                        }
                        Upload upload = new Upload();
                        upload.upload();
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
                        List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                        for(String[] userProfile_row : userProfile_rows) {
                            String type = userProfile_row[2];
                            if(type.equals("backup")){
                                String userEmail = userProfile_row[1];
                                String accessToken = userProfile_row[0];
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

                        String[] columns = {"accessToken", "userEmail"};
                        List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                        for(String[] userProfile_row : userProfile_rows){
                            String accessToken = userProfile_row[0];
                            String userEmail = userProfile_row[1];
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
                        List<String[]> userProfile_rows = dbHelper.getUserProfile(columns);

                        for(String[] userProfile_row : userProfile_rows) {
                            String type = userProfile_row[2];
                            if(type.equals("backup")){
                                String userEmail = userProfile_row[1];
                                String accessToken = userProfile_row[0];
                                GoogleDrive.deleteDuplicatedMediaItems(accessToken, userEmail);
                            }
                        }
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
                            TextView deviceStorage = findViewById(R.id.deviceStorage);
                            ArrayList<String> storage =  Android.getAndroidDeviceStorage();
                            deviceStorage.setText("Total space: " + storage.get(0) +
                                    "\n" + "free space: " + storage.get(1) + "\n");

                            TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
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

//        void uploadAndroidToDriveAccounts(String backUpAccessToken){
//            try{
//                Executor uploadExecutor = Executors.newSingleThreadExecutor();
//                Runnable backgroundTask = () -> {
//                    BackUpAccountInfo firstBackUpAccountInfo = backUpAccountHashMap.values().iterator().next();
//                    ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems = firstBackUpAccountInfo.getMediaItems();
//                    for(PrimaryAccountInfo primaryAccountInfo: primaryAccountHashMap.values()){
//                        ArrayList<GooglePhotos.MediaItem> primaryMediaItems = primaryAccountInfo.getMediaItems();
//                        System.out.println("here to sync android!");
//                        googlePhotos.uploadAndroidToGoogleDrive(androidMediaItems, primaryMediaItems ,backUpAccessToken,
//                                backUpMediaItems, this);
//                    }
//                };
//                uploadExecutor.execute(backgroundTask);
//            }catch (Exception e){
//                LogHandler.saveLog("Failed to upload from android to drive: " + e.getLocalizedMessage());
//            }
//        }

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
                                String buttonText = button.getText().toString().toLowerCase();
                                if (buttonText.equals("add a primary account")){
                                    button.setText("Wait");
                                    googleCloud.signInToGoogleCloud(signInToPrimaryLauncher);
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
                                            dbHelper.deleteUserProfileData(buttonText);
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
                                String buttonText = button.getText().toString().toLowerCase();
                                if (buttonText.equals("add a back up account")) {
                                    button.setText("Wait");
                                    googleCloud.signInToGoogleCloud(signInToBackUpLauncher);
                                } else if (buttonText.equals("wait")){
                                    button.setText("Add a primary account");
                                }else {
                                    PopupMenu popupMenu = new PopupMenu(MainActivity.this, button, Gravity.CENTER);
                                    popupMenu.getMenuInflater().inflate(R.menu.account_button_menu, popupMenu.getMenu());
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        popupMenu.setGravity(Gravity.CENTER);
                                    }
                                    popupMenu.setOnMenuItemClickListener(item -> {
                                        if (item.getItemId() == R.id.sign_out) {
                                            googleCloud.signOut();
                                            dbHelper.deleteUserProfileData(buttonText);
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
                            }
                    );
                }
            }
        }


        private void initializeButtons(){
            String[] columnsList = {"userEmail", "type", "refreshToken"};
            List<String[]> userProfiles = dbHelper.getUserProfile(columnsList);
            for (String[] userProfile : userProfiles) {
                String userEmail = userProfile[0];
                String type = userProfile[1];
                String refreshToken = userProfile[2];

                PrimaryAccountInfo.Tokens tokens = googleCloud.requestAccessToken(refreshToken);
                Map<String, Object> updatedValues = new HashMap<String, Object>(){{
                    put("accessToken", tokens.getAccessToken());
                }};

                dbHelper.updateUserProfileData(userEmail, updatedValues);

                if (type.equals("primary")){
                    LinearLayout primaryLinearLayout = findViewById(R.id.primaryAccountsButtons);
                    Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryLinearLayout);
                    newGoogleLoginButton.setText(userEmail);
                }else if (type.equals("backup")){
                    LinearLayout backupLinearLayout = findViewById(R.id.backUpAccountsButtons);
                    Button newGoogleLoginButton = googleCloud.createBackUpLoginButton(backupLinearLayout);
                    newGoogleLoginButton.setText(userEmail);
                }
            }
        }
    }



