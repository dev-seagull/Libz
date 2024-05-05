    package com.example.cso;

    import android.Manifest;
    import android.app.Activity;
    import android.app.ActivityManager;
    import android.app.AlertDialog;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.pm.PackageManager;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.graphics.drawable.Drawable;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Environment;
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
    import android.widget.ImageButton;
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
    import com.google.android.material.switchmaterial.SwitchMaterial;
    import com.google.gson.JsonArray;
    import com.google.gson.JsonObject;
    import com.jaredrummler.android.device.DeviceName;

    import net.sqlcipher.database.SQLiteDatabase;

    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.Timer;
    import java.util.TimerTask;
    import java.util.concurrent.CountDownLatch;


    public class MainActivity extends AppCompatActivity {
        private DrawerLayout drawerLayout;

        public static Activity activity ;
        static GoogleCloud googleCloud;
//        ActivityResultLauncher<Intent> signInToPrimaryLauncher;
        public ActivityResultLauncher<Intent> signInToBackUpLauncher;
//        GooglePhotos googlePhotos;
//        HashMap<String, PhotosAccountInfo> primaryAccountHashMap = new HashMap<>();
        public static String androidDeviceName;
        static SharedPreferences preferences;
        public static DBHelper dbHelper;

        public static StorageHandler storageHandler;
        public Thread firstUiThread;
        public Thread secondUiThread;
        public static Thread backUpJsonThread;
        public static Intent serviceIntent;
        public Thread insertMediaItemsThread;
        public Thread deleteRedundantDriveThread;
        public Thread updateDriveBackUpThread;
        public Thread deleteDuplicatedInDrive;
        SwitchMaterial syncSwitchMaterialButton;
        SwitchMaterial mobileDataSwitchMaterial;
        SwitchMaterial wifiOnlySwitchMaterial;
        int buildSdkInt = Build.VERSION.SDK_INT;
        List<Thread> threads = new ArrayList<>(Arrays.asList(firstUiThread, secondUiThread,
                backUpJsonThread, insertMediaItemsThread, deleteRedundantDriveThread, updateDriveBackUpThread, deleteDuplicatedInDrive));

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

            PermissionManager permissionManager = new PermissionManager();
            boolean isAccessGranted = permissionManager.requestStorageAccess(activity);

            boolean hasCreated = LogHandler.createLogFile();
            System.out.println("Log file is created :"  + hasCreated);

            googleCloud = new GoogleCloud(this);
//            LogHandler.saveLog("--------------------------new run----------------------------",false);
//            LogHandler.saveLog("Build.VERSION.SDK_INT and Build.VERSION_CODES.M : " + Build.VERSION.SDK_INT +
//                    Build.VERSION_CODES.M, false);

            preferences = getPreferences(Context.MODE_PRIVATE);
            dbHelper = new DBHelper(this,"StashDatabase");

            LogHandler.saveLog("salam",true);
            LogHandler.actionOnLogFile();
            LogHandler.saveLog("salam2",true);
            androidDeviceName = DeviceName.getDeviceName();
            Upgrade.versionHandler(preferences);
            storageHandler = new StorageHandler();
            if(dbHelper.DATABASE_VERSION < 11) {
                LogHandler.saveLog("Starting to update database from version 1 to version 2.", false);
            }
            dbHelper.insertSupportCredential();

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
//                    synchronized (manageAccessThread){
//                        try{
//                            manageAccessThread.join();
//                        }catch (Exception e){
//                            LogHandler.saveLog("Failed to join delete duplicated drive " +
//                                    "thread in customLoginThread : " + e.getLocalizedMessage(),true);
//                        }
//                    }
                    boolean isWriteAndReadPermissionGranted = (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                            (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                    while(!isWriteAndReadPermissionGranted){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED |
                                        ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                            ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
                        }
                        isWriteAndReadPermissionGranted = (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                    }
                }
            };

            initializeButtons(this,googleCloud);


            Button androidDeviceButton = findViewById(R.id.androidDeviceButton);
            androidDeviceButton.setText(androidDeviceName);

//            LinearLayout primaryAccountsButtonsLayout= findViewById(R.id.primaryAccountsButtons);
            LinearLayout backupAccountsButtonsLayout= findViewById(R.id.backUpAccountsButtons);

//            Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryAccountsButtonsLayout);
//            newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

            Button newBackupLoginButton = googleCloud.createBackUpLoginButton(backupAccountsButtonsLayout);
            newBackupLoginButton.setBackgroundTintList(UIHelper.addBackupAccountButtonColor);

            TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
            TextView deviceStorage = findViewById(R.id.deviceStorage);
            ImageButton displayDirectoriesUsagesButton = findViewById(R.id.directoriesButton);

            serviceIntent = new Intent(this.getApplicationContext(), TimerService.class);
            new TimerService();//.onStartCommand(serviceIntent, Service.START_FLAG_REDELIVERY,1505);
            HashMap<String,String> dirHashMap = storageHandler.directoryUIDisplay();

            runOnUiThread(() ->{
                TextView deviceStorageTextView = findViewById(R.id.deviceStorage);
                TextView directoryUsages = findViewById(R.id.directoryUsages);
                deviceStorageTextView.setText("Wait until we get an update of your assets ...");
                syncSwitchMaterialButton = findViewById(R.id.syncSwitchMaterial);

                for (Map.Entry<String, String> entry : dirHashMap.entrySet()) {
                    directoryUsages.append(entry.getKey() + ": " + entry.getValue() + " GB\n");
                }

                UIHandler.handleSwitchMaterials();
            });

            Thread deleteRedundantAndroidThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    dbHelper.deleteRedundantAndroidFromDB();
                    synchronized (this){
                        notify();
                    }
                }
            });

            Thread updateAndroidFilesThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (deleteRedundantAndroidThread){
                        try{
                            deleteRedundantAndroidThread.join();
                        }catch (Exception e){
                            LogHandler.saveLog("failed to join deleteRedundantAndroidThread thread: "  + e.getLocalizedMessage());
                        }
                    }
                    System.out.println(" here running updateAndroidFilesThread" );
                    int galleryItems = Android.getGalleryMediaItems(MainActivity.this);
                    LogHandler.saveLog("End of getting files from your android device when starting the app.",false);
                }
            });


            deleteRedundantDriveThread = new Thread(new Runnable() {
                @Override
                public void run() {
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
                            ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken, userEmail);
                            ArrayList<String> driveFileIds = new ArrayList<>();

                            for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                                String fileId = driveMediaItem.getId();
                                driveFileIds.add(fileId);
                            }
                            dbHelper.deleteRedundantDriveFromDB(driveFileIds, userEmail);
                        }
                    }
                }
            });

            updateDriveBackUpThread = new Thread(() -> {
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
                        ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken, userEmail);
                        System.out.println("dv nums: " + driveMediaItems.size());
                        for(DriveAccountInfo.MediaItem driveMediaItem: driveMediaItems){
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

            deleteDuplicatedInDrive = new Thread(() -> {
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
                try{
                    runOnUiThread(() -> {
                        deviceStorage.setText("Storage : " + storageHandler.getFreeSpace() +
                                " /Out Of" + storageHandler.getTotalStorage()+ " GB\n"+
                                "Media : "  + dbHelper.getPhotosAndVideosStorage() + "\n");
                        displayDirectoriesUsagesButton.setVisibility(View.VISIBLE);
                        TextView directoriesUsages = findViewById(R.id.directoryUsages);
                        displayDirectoriesUsagesButton.setOnClickListener(view -> {
                            if (directoriesUsages.getVisibility() == View.VISIBLE) {
                                Drawable newBackground = getResources().getDrawable(R.drawable.down_vector_icon);
                                displayDirectoriesUsagesButton.setBackground(newBackground);
                                directoriesUsages.setVisibility(View.GONE);
                            } else {
                                Drawable newBackground = getResources().getDrawable(R.drawable.up_vector_icon);
                                displayDirectoriesUsagesButton.setBackground(newBackground);
                                directoriesUsages.setVisibility(View.VISIBLE);
                            }
                        });
                        androidStatisticsTextView.setVisibility(View.VISIBLE);
                        int total_androidAssets_count = dbHelper.countAndroidAssets();
                        androidStatisticsTextView.setText("Sync Status : " + dbHelper.countAndroidSyncedAssets() +
                                " Of " + total_androidAssets_count);
                    });
                }catch (Exception e){
                    LogHandler.saveLog("Failed to run on ui thread : " + e.getLocalizedMessage() , true);
                }
            });

            LogHandler.saveLog("---------------Start of app--------------",false);

            manageReadAndWritePermissonsThread.start();
            deleteRedundantAndroidThread.start();
            updateAndroidFilesThread.start();
            deleteRedundantDriveThread.start();
            updateDriveBackUpThread.start();
            deleteDuplicatedInDrive.start();
            updateUIThread.start();

            final Thread[] updateAndroidFilesThread2 = {new Thread(updateAndroidFilesThread)};
            final Thread[] deleteRedundantAndroidThread2 = {new Thread(deleteRedundantDriveThread)};
            new Timer().scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    System.out.println("inner timer");
                    boolean anyThreadAlive = false;
                    if (!isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
                        runOnUiThread(() -> {
                            syncSwitchMaterialButton.setChecked(false);
                            syncSwitchMaterialButton.setThumbTintList(UIHelper.offSwitchMaterialThumb);
                            syncSwitchMaterialButton.setTrackTintList(UIHelper.offSwitchMaterialTrack);
                        });
                    }else{
                        runOnUiThread(() -> {
                            syncSwitchMaterialButton.setChecked(true);
                            syncSwitchMaterialButton.setThumbTintList(UIHelper.onSwitchMaterialThumb);
                            syncSwitchMaterialButton.setTrackTintList(UIHelper.onSwitchMaterialTrack);
                        });
                    }

                    try{
                        for(Thread thread : threads ){
                            if(thread != null){
                                System.out.println(thread.getId());
                                System.out.println("Name : " + thread.getName() + " is " + thread.isAlive());
                            }
                            if (thread != null && thread.isAlive()) {
                                anyThreadAlive = true;
                                break;
                            }
                        }
                        System.out.println("should run : " + !anyThreadAlive);
                        if(!updateAndroidFilesThread.isAlive() && !updateAndroidFilesThread2[0].isAlive()){
                            if(!anyThreadAlive){

//                                if (!storageUpdaterThread[0].isAlive()) {
//                                    Thread storageUpdaterThreadTemp = new Thread(storageUpdaterThread[0]);
//                                    storageUpdaterThreadTemp.start();
//                                }
                                runOnUiThread(() -> {
                                    TextView deviceStorage = findViewById(R.id.deviceStorage);
                                    deviceStorage.setText("Storage : " + storageHandler.getFreeSpace() +
                                             " Out Of " + storageHandler.getTotalStorage()+ " GB\n"+
                                            "Media : "  + dbHelper.getPhotosAndVideosStorage() + "\n");

                                    TextView androidStatisticsTextView = findViewById(R.id.androidStatistics);
                                    androidStatisticsTextView.setVisibility(View.VISIBLE);
                                    int total_androidAssets_count = dbHelper.countAndroidAssets();
                                    androidStatisticsTextView.setText("Sync Status : " + dbHelper.countAndroidSyncedAssets() +
                                            " Of " + total_androidAssets_count);
                                });
                            }
                        }

                        System.out.println("is alive: " + updateAndroidFilesThread.isAlive() + updateAndroidFilesThread2[0].isAlive());
                        if(!deleteRedundantAndroidThread.isAlive() && !anyThreadAlive){
                            deleteRedundantAndroidThread2[0] = new Thread(deleteRedundantAndroidThread);
                            System.out.println("here running 1.1");
                            deleteRedundantAndroidThread2[0].start();
                        }
                        if(!updateAndroidFilesThread.isAlive() && !updateAndroidFilesThread2[0].isAlive() && !anyThreadAlive){
                            updateAndroidFilesThread2[0] = new Thread(updateAndroidFilesThread);
                            System.out.println("here running 2.1");
                            updateAndroidFilesThread2[0].start();
                        }
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to run on ui thread : " + e.getLocalizedMessage() , true);
                    }
                }
            }, 10000, 3000);

            LogHandler.saveLog("--------------------------first threads were finished----------------------------",false);
        }


        @Override
        protected void onStart(){
            super.onStart();
            dbHelper.backUpDataBase(getApplicationContext());
//            System.out.println("before database decryption");
//            String exportPath = getDatabasePath(DBHelper.NEW_DATABASE_NAME + "_decrypted.db").getPath();
//            dbHelper.exportDecryptedDatabase(exportPath);
//            System.out.println("after database decryption");

            signInToBackUpLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if(result.getResultCode() == RESULT_OK){
                            LinearLayout backupButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                            View[] child = {backupButtonsLinearLayout.getChildAt(
                                    backupButtonsLinearLayout.getChildCount() - 1)};
                            UIHandler.setLastBackupAccountButtonClickableFalse(activity);
                            try{
                                Thread mainmThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final GoogleCloud.signInResult signInResult =
                                                googleCloud.startSignInToBackUpThread(result.getData());

                                        boolean isBackedUp = Profile.backUpJsonFile(signInResult, signInToBackUpLauncher);

                                        firstUiThread = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isBackedUp) {
                                                    runOnUiThread(() -> {
                                                        Button newBackupLoginButton = googleCloud.createBackUpLoginButton(backupButtonsLinearLayout);
                                                        newBackupLoginButton.setBackgroundTintList(UIHelper.backupAccountButtonColor);
                                                        child[0] = backupButtonsLinearLayout.getChildAt(
                                                                backupButtonsLinearLayout.getChildCount() - 2);
                                                        LogHandler.saveLog(signInResult.getUserEmail()
                                                                + " has logged in to the backup account", false);

                                                        if (child[0] instanceof Button) {
                                                            Button bt = (Button) child[0];
                                                            bt.setText(signInResult.getUserEmail());
                                                            bt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D47A1")));
                                                        }
                                                        updateButtonsListeners(signInToBackUpLauncher);
                                                    });
                                                }
                                            }
                                        });

                                        insertMediaItemsThread = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                synchronized (firstUiThread){
                                                    try {
                                                        firstUiThread.join();
                                                    } catch (Exception e) {
                                                        LogHandler.saveLog("failed to join backUpJsonThread in backup account: " + e.getLocalizedMessage());
                                                    }
                                                }
                                                try{
                                                    for(DriveAccountInfo.MediaItem mediaItem : signInResult.getMediaItems()){
                                                        Long last_insertId = MainActivity.dbHelper.insertAssetData(mediaItem.getHash());
                                                        if (last_insertId != -1) {
                                                            MainActivity.dbHelper.insertIntoDriveTable(last_insertId, mediaItem.getId(), mediaItem.getFileName(),
                                                                    mediaItem.getHash(), signInResult.getUserEmail());
                                                        } else {
                                                            LogHandler.saveLog("Failed to insert file into drive table: " + mediaItem.getFileName());
                                                        }
                                                    }
                                                }catch (Exception e){
                                                    LogHandler.saveLog("failed to insert media items into drive table : " + e.getLocalizedMessage());
                                                }
                                            }
                                        });

                                        deleteRedundantDriveThread = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                synchronized (insertMediaItemsThread){
                                                    try {
                                                        insertMediaItemsThread.join();
                                                    } catch (Exception e) {
                                                        LogHandler.saveLog("failed to join insertMediaItemsThread in backup account: " + e.getLocalizedMessage());
                                                    }
                                                }
                                                try{
                                                    String[] columns = {"accessToken","userEmail", "type"};
                                                    List<String[]> accounts_rows = dbHelper.getAccounts(columns);

                                                    for(String[] account_row : accounts_rows) {
                                                        String type = account_row[2];
                                                        if(type.equals("backup")){
                                                            String userEmail = account_row[1];
                                                            String accessToken = account_row[0];
                                                            ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken, userEmail);
                                                            ArrayList<String> driveFileIds = new ArrayList<>();

                                                            for (DriveAccountInfo.MediaItem driveMediaItem : driveMediaItems) {
                                                                String fileId = driveMediaItem.getId();
                                                                driveFileIds.add(fileId);
                                                            }
                                                            dbHelper.deleteRedundantDriveFromDB(driveFileIds, userEmail);
                                                        }
                                                    }
                                                }catch (Exception e){
                                                    LogHandler.saveLog("failed in deleteRedundantDriveThread : " + e.getLocalizedMessage());
                                                }
                                            }
                                        });

                                        updateDriveBackUpThread = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
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
                                                        ArrayList<DriveAccountInfo.MediaItem> driveMediaItems = GoogleDrive.getMediaItems(accessToken, userEmail);
                                                        for(DriveAccountInfo.MediaItem driveMediaItem: driveMediaItems){
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
                                            }
                                        });

                                        deleteDuplicatedInDrive = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
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
                                            }
                                        });

                                        secondUiThread = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                synchronized (deleteDuplicatedInDrive){
                                                    try {
                                                        deleteDuplicatedInDrive.join();
                                                    }catch (Exception e){
                                                        LogHandler.saveLog("failed to join androidUploadThread : "  + e.getLocalizedMessage());
                                                    }
                                                }

                                                runOnUiThread(() -> {
                                                    child[0].setClickable(true);
                                                });
                                            }
                                        });

                                        firstUiThread.start();
                                        insertMediaItemsThread.start();
                                        deleteRedundantDriveThread.start();
                                        updateDriveBackUpThread.start();
                                        deleteDuplicatedInDrive.start();
                                        secondUiThread.start();
                                    }
                                });
                                mainmThread.start();
                            }catch (Exception e){
                                LogHandler.saveLog("Failed to sign in to backup : "  + e.getLocalizedMessage());
                            }
                        }
                        else{
                            runOnUiThread(() -> {
                                LogHandler.saveLog("login with back up launcher failed with response code :" + result.getResultCode());
                                LinearLayout backupAccountsButtonsLinearLayout = findViewById(R.id.backUpAccountsButtons);
                                View childview = backupAccountsButtonsLinearLayout.getChildAt(
                                        backupAccountsButtonsLinearLayout.getChildCount() - 1);
                                if(childview instanceof Button){
                                    Button bt = (Button) childview;
                                    bt.setText("ADD A BACK UP ACCOUNT");
                                }
                                updateButtonsListeners(signInToBackUpLauncher);
                            });
                        }
                    });

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateButtonsListeners(signInToBackUpLauncher);
                }
            });
            System.out.println("startService(serviceIntent); " + serviceIntent);
//            serviceIntent.getData();
//            Intent.getIntentOld();
            try{
                googleCloud = new GoogleCloud(this);
//                googlePhotos = new GooglePhotos();
            }catch (Exception e){
                LogHandler.saveLog("failed to initialize the classes: " + e.getLocalizedMessage());
            }
//            displayDialogForRestoreAccountsDecision(preferences);
//            signInToPrimaryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if(result.getResultCode() == RESULT_OK){
//                        LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
//                        View[] childview = {primaryAccountsButtonsLinearLayout.getChildAt(
//                                primaryAccountsButtonsLinearLayout.getChildCount() - 1)};
//                        runOnUiThread(() -> childview[0].setClickable(false));
//                        Executor signInExecutor = Executors.newSingleThreadExecutor();
//                        try {
//                            Runnable backgroundTask = () -> {
//                                GoogleCloud.signInResult signInResult = googleCloud.handleSignInToPrimaryResult(result.getData());
//
//                                if (signInResult.getHandleStatus() == true) {
//                                    boolean[] isBackedUp = {false};
//                                    backUpJsonThread = new Thread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            try {
//                                                dbHelper.insertIntoAccounts(signInResult.getUserEmail(), "primary"
//                                                        , signInResult.getTokens().getRefreshToken(), signInResult.getTokens().getAccessToken(),
//                                                        signInResult.getStorage().getTotalStorage(), signInResult.getStorage().getUsedStorage(),
//                                                        signInResult.getStorage().getUsedInDriveStorage(), signInResult.getStorage().getUsedInGmailAndPhotosStorage());
//
//                                                isBackedUp[0] = MainActivity.dbHelper.backUpProfileMap(false,"");
//                                                System.out.println("isBackedUp "+isBackedUp[0]);
//                                            }catch (Exception e){
//                                                LogHandler.saveLog("failed to back up profile map in primary account: " + e.getLocalizedMessage());
//                                            }
//                                            synchronized (this){
//                                                notify();
//                                            }
//                                        }
//                                    });
//
//                                    Thread UIThread = new Thread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            synchronized (backUpJsonThread){
//                                                try{
//                                                    backUpJsonThread.join();
//                                                }catch (Exception e){
//                                                    LogHandler.saveLog("failed to join backUpJsonThread in primary account: " + e.getLocalizedMessage());
//                                                }
//                                            }
//                                            if (isBackedUp[0] == true) {
//                                                runOnUiThread(() -> {
//                                                    Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryAccountsButtonsLinearLayout);
//                                                    newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
//                                                    childview[0] = primaryAccountsButtonsLinearLayout.getChildAt(
//                                                            primaryAccountsButtonsLinearLayout.getChildCount() - 2);
//                                                    LogHandler.saveLog(signInResult.getUserEmail()
//                                                            + " has logged in to the primary account", false);
//
//                                                    if (childview[0] instanceof Button) {
//                                                        Button bt = (Button) childview[0];
//                                                        bt.setText(signInResult.getUserEmail());
//                                                        bt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D47A1")));
//                                                    }
//                                                    updateButtonsListeners();
//                                                });
//                                            }
//                                        }
//                                    });
//                                    backUpJsonThread.start();
//                                    UIThread.start();
//                                }else{
//                                    runOnUiThread(() -> {
//                                            LogHandler.saveLog("login with launcher failed");
//                                            LinearLayout primaryAccountsButtonsLinearLayout2 =
//                                                    findViewById(R.id.primaryAccountsButtons);
//                                            View childview2 = primaryAccountsButtonsLinearLayout2.getChildAt(
//                                                    primaryAccountsButtonsLinearLayout2.getChildCount() - 1);
//                                            if (childview2 instanceof Button) {
//                                                Button bt = (Button) childview2;
//                                                bt.setText("ADD A PRIMARY ACCOUNT");
//                                            }
//                                    updateButtonsListeners();
//                                    });
//                                }
//                            };
//                            signInExecutor.execute(backgroundTask);
//                            runOnUiThread(() -> childview[0].setClickable(true));
//                        }catch (Exception e){
//                            LogHandler.saveLog("Failed to sign in to primary : "  + e.getLocalizedMessage());
//                        }
//                   }else{
//                        runOnUiThread(() -> {
//                            LogHandler.saveLog("login with primary launcher failed with response code :" + result.getResultCode());
//                            LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
//                            View childview = primaryAccountsButtonsLinearLayout.getChildAt(
//                                    primaryAccountsButtonsLinearLayout.getChildCount() - 1);
//                            if(childview instanceof Button){
//                                Button bt = (Button) childview;
//                                bt.setText("ADD A PRIMARY ACCOUNT");
//                            }
//                            updateButtonsListeners();
//                        });
//                    }
//                }
//            );

            wifiOnlySwitchMaterial = findViewById(R.id.wifiOnlySwitchMaterial);
            wifiOnlySwitchMaterial.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(wifiOnlySwitchMaterial.isChecked()){
                        runOnUiThread( () -> {
                            wifiOnlySwitchMaterial.setThumbTintList(UIHelper.onSwitchMaterialThumb);
                            wifiOnlySwitchMaterial.setTrackTintList(UIHelper.onSwitchMaterialTrack);
                            wifiOnlySwitchMaterial.setAlpha(1.0f);
                        });
                        SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",true,preferences);
                    }else{
                        runOnUiThread( () -> {
                            wifiOnlySwitchMaterial.setThumbTintList(UIHelper.offSwitchMaterialThumb);
                            wifiOnlySwitchMaterial.setTrackTintList(UIHelper.offSwitchMaterialTrack);
                        });
                        SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",false,preferences);
                    }
                }
            });

            syncSwitchMaterialButton = findViewById(R.id.syncSwitchMaterial);
            syncSwitchMaterialButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(syncSwitchMaterialButton.isChecked()){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (!isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
                                TimerService.shouldCancel = false;
                                startService(serviceIntent);
                            }
                        }
                    }else{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
                                TimerService.shouldCancel = true;
                                stopService(serviceIntent);
                            }
                        }
                    }
                    UIHandler.handleSwitchMaterials();
                }
            });

        }

        public static String isMyServiceRunning(Context context,Class<?> serviceClass) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    System.out.println("isMyServiceRunning : true");
                    return "on";
                }
            }
            System.out.println("isMyServiceRunning : false");
            return "off";
        }

        public static void updateButtonsListeners(ActivityResultLauncher<Intent> signInToBackUpLauncher) {
//            updatePrimaryButtonsListener();
            updateBackupButtonsListener(activity, signInToBackUpLauncher);
        }
//        private void updatePrimaryButtonsListener(){
//            LinearLayout primaryAccountsButtonsLinearLayout = findViewById(R.id.primaryAccountsButtons);
//            for (int i = 0; i < primaryAccountsButtonsLinearLayout.getChildCount(); i++) {
//                View childView = primaryAccountsButtonsLinearLayout.getChildAt(i);
//                if (childView instanceof Button) {
//                    Button button = (Button) childView;
//                    button.setOnClickListener(
//                            view -> {
////                                syncToBackUpAccountButton.setClickable(false);
////                                restoreButton.setClickable(false);
//                                String buttonText = button.getText().toString().toLowerCase();
//                                if (buttonText.equals("add a primary account")){
//                                    button.setText("Wait");
//                                    button.setClickable(false);
//                                    googleCloud.signInToGoogleCloud(signInToPrimaryLauncher);
//                                    button.setClickable(true);
//                                }else if (buttonText.equals("wait")){
//                                    button.setText("Add a primary account");
//                                }
//                                else {
//                                    PopupMenu popupMenu = new PopupMenu(MainActivity.this,button, Gravity.CENTER);
//                                    popupMenu.getMenuInflater().inflate(R.menu.account_button_menu,popupMenu.getMenu());
//                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                        popupMenu.setGravity(Gravity.CENTER);
//                                    }
//                                    popupMenu.setOnMenuItemClickListener(item -> {
//                                        if (item.getItemId() == R.id.sign_out) {
//                                            try {
//                                                button.setText("Wait...");
//                                            } catch (Exception e) {
//                                                LogHandler.saveLog("Failed to set text to wait : " +
//                                                e.getLocalizedMessage(), true);
//                                            }
//
//                                            final boolean[] isSignedout = {false};
//                                            final boolean[] isBackedUp = {false};
//
//                                            Thread signOutThread = new Thread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    isSignedout[0] = googleCloud.signOut(buttonText);
//                                                    System.out.println("isSignedOut " + isSignedout[0]);
//                                                    synchronized (this){
//                                                        notify();
//                                                    }
//                                                }
//                                            });
//
//                                            backUpJsonThread = new Thread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    synchronized (signOutThread){
//                                                        try {
//                                                            MainActivity.dbHelper.deleteFromAccountsTable(buttonText, "primary");
//                                                            MainActivity.dbHelper.deleteAccountFromPhotosTable(buttonText);
//                                                            dbHelper.deleteRedundantAsset();
//                                                            signOutThread.join();
//                                                        } catch (InterruptedException e) {
//                                                            LogHandler.saveLog("Failed to join the signout thread" +
//                                                                    " + " + e.getLocalizedMessage(), true);
//                                                        }
//                                                    }
//                                                    if (isSignedout[0]) {
//                                                        isBackedUp[0] = MainActivity.dbHelper.backUpProfileMap(true,buttonText);
//                                                    }
//                                                    System.out.println("isBackedUp " + isBackedUp[0]);
//                                                }
//                                            });
//
//                                            Thread uiThread = new Thread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    synchronized (backUpJsonThread){
//                                                        try {
//                                                            backUpJsonThread.join();
//                                                        } catch (InterruptedException e) {
//                                                            LogHandler.saveLog("Failed to join " +
//                                                                    " back up json thread : " + e.getLocalizedMessage(), true);
//                                                        }
//                                                    }
//                                                    runOnUiThread(() -> {
//                                                        System.out.println("isSigned " + isSignedout[0]);
//                                                        if (isBackedUp[0]) {
//                                                            try {
//                                                                item.setEnabled(false);
//                                                                ViewGroup parentView = (ViewGroup) button.getParent();
//                                                                parentView.removeView(button);
//                                                            } catch (Exception e) {
//                                                                LogHandler.saveLog(
//                                                                        "Failed to handle ui after signout : "
//                                                                        + e.getLocalizedMessage(), true
//                                                                );
//                                                            }
//                                                        } else {
//                                                            try {
//                                                                button.setText(buttonText);
//                                                            } catch (Exception e) {
//                                                                LogHandler.saveLog(" Failed to set text " +
//                                                                        " to button text  : " + e.getLocalizedMessage()
//                                                                , true);
//                                                            }
//                                                        }
//                                                    });
//                                                }
//                                            });
//
//                                            signOutThread.start();
//                                            backUpJsonThread.start();
//                                            uiThread.start();
//                                        }
//                                        return true;
//                                    });
//                                    popupMenu.show();
//                                }
////                                syncToBackUpAccountButton.setClickable(true);
////                                restoreButton.setClickable(true);
//                            }
//                        );
//                }
//            }
//        }
        public static void updateBackupButtonsListener(Activity activity, ActivityResultLauncher<Intent> signInToBackUpLauncher){
            LinearLayout backUpAccountsButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
            for (int i = 0; i < backUpAccountsButtonsLinearLayout.getChildCount(); i++) {
                View childView = backUpAccountsButtonsLinearLayout.getChildAt(i);
                if (childView instanceof Button) {
                    Button button = (Button) childView;
                    button.setOnClickListener(
                            view -> {
//                                syncToBackUpAccountButton.setClickable(false);
//                                restoreButton.setClickable(false);
                                String buttonText = button.getText().toString().toLowerCase();
                                if (buttonText.equals("add a back up account")) {
                                    button.setText("Wait");
                                    button.setClickable(false);
                                    googleCloud.signInToGoogleCloud(signInToBackUpLauncher);
                                    button.setClickable(true);
                                } else if (buttonText.equals("wait")){
                                    button.setText("add a back up account");
                                }
                                else {
                                    PopupMenu popupMenu = new PopupMenu(activity.getApplicationContext(), button, Gravity.CENTER);
                                    popupMenu.getMenuInflater().inflate(R.menu.account_button_menu, popupMenu.getMenu());
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        popupMenu.setGravity(Gravity.CENTER);
                                    }
                                    popupMenu.setOnMenuItemClickListener(item -> {
                                        if (item.getItemId() == R.id.sign_out) {
                                            try {
                                                button.setText("Wait...");
                                            }catch (Exception e){}

                                            final boolean[] isSignedout = {false};
                                            final boolean[] isDeleted = {false};
                                            final boolean[] isBackedUp = {false};


                                            Thread deleteProfileJsonThread = new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    isDeleted[0] = Profile.deleteProfileJson(buttonText);
                                                    System.out.println("isDeleted " + isDeleted[0]);
                                                    synchronized (this){
                                                        notify();
                                                    }
                                                }
                                            });

                                            Thread signOutThread = new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        deleteProfileJsonThread.join();
                                                    } catch (Exception e) {
                                                        LogHandler.saveLog("Failed to join " +
                                                                " delete json thread : "  +e.getLocalizedMessage(), true);
                                                    }
                                                    if(isDeleted[0]){
                                                        isSignedout[0] = googleCloud.signOut(buttonText);
                                                        System.out.println("isSignedOut " + isSignedout[0]);
                                                    }
                                                }
                                            });


                                            backUpJsonThread = new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    synchronized (signOutThread){
                                                        try {
                                                            signOutThread.join();
                                                        } catch (Exception e) {
                                                            LogHandler.saveLog("Failed to join the" +
                                                                    " signout thread : " +
                                                                     e.getLocalizedMessage(), true);
                                                        }
                                                    }
                                                    if (isSignedout[0]) {
                                                        MainActivity.dbHelper.deleteFromAccountsTable(buttonText, "backup");
                                                        MainActivity.dbHelper.deleteAccountFromDriveTable(buttonText);
                                                        dbHelper.deleteRedundantAsset();
                                                        isBackedUp[0] = MainActivity.dbHelper.backUpProfileMap(true,buttonText);
                                                    }
                                                    System.out.println("isBackedUp " + isBackedUp[0]);
                                                }
                                            });

                                            Thread uiThread = new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    synchronized (backUpJsonThread){
                                                        try {
                                                            backUpJsonThread.join();
                                                        } catch (Exception e) {
                                                            LogHandler.saveLog("Failed to join backup json thread : " +
                                                                    e.getLocalizedMessage(), true);
                                                        }
                                                    }
                                                    activity.runOnUiThread(() -> {
                                                        if (isBackedUp[0]) {
                                                            try {
                                                                item.setEnabled(false);
                                                                ViewGroup parentView = (ViewGroup) button.getParent();
                                                                parentView.removeView(button);
                                                            } catch (Exception e) {
                                                                LogHandler.saveLog(
                                                                        "Failed to handle ui after signout : "
                                                                        + e.getLocalizedMessage(), true
                                                                );
                                                            }
                                                        } else {
                                                            try {
                                                                button.setText(buttonText);
                                                            } catch (Exception e) {
                                                                LogHandler.saveLog(
                                                                        "Failed to handle ui when signout : "
                                                                                + e.getLocalizedMessage(), true
                                                                );
                                                            }
                                                        }
                                                    });
                                                }
                                            });

                                            deleteProfileJsonThread.start();
                                            signOutThread.start();
                                            backUpJsonThread.start();
                                            uiThread.start();
//                                            boolean isDeletedFromAccounts = dbHelper.deleteFromAccountsTable(buttonText,"backup");
//                                            boolean isAccountDeletedFromDriveTable = dbHelper.deleteAccountFromDriveTable(buttonText);
//                                            dbHelper.deleteRedundantAsset();
//                                            ViewGroup parentView = (ViewGroup) button.getParent();
//                                            parentView.removeView(button);
//                                            googleCloud.signOut(buttonText);
                                        }
                                        return true;
                                    });
                                    popupMenu.show();
                                }
//                                syncToBackUpAccountButton.setClickable(true);
//                                restoreButton.setClickable(true);
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
                GoogleCloud.Tokens tokens = googleCloud.requestAccessToken(refreshToken);
                Map<String, Object> updatedValues = new HashMap<String, Object>(){{
                        put("accessToken", tokens.getAccessToken());
                }};

                 dbHelper.updateAccounts(userEmail, updatedValues, type);

                if (type.equals("primary")){
//                    LinearLayout primaryLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
//                    Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryLinearLayout);
//                    newGoogleLoginButton.setText(userEmail);
                }else if (type.equals("backup")){
                    LinearLayout backupLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                    Button newGoogleLoginButton = googleCloud.createBackUpLoginButton(backupLinearLayout);
                    newGoogleLoginButton.setText(userEmail);
                }
            }
        }

        public static void reInitializeButtons(Activity activity,GoogleCloud googleCloud){
//            LinearLayout primaryLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
//            for (int i = 0; i < primaryLinearLayout.getChildCount(); i++) {
//                View child = primaryLinearLayout.getChildAt(i);
//                if (child instanceof Button) {
//                    primaryLinearLayout.removeView(child);
//                    i--;
//                }
//            }

            LinearLayout backupLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
            for (int i = 0; i < backupLinearLayout.getChildCount(); i++) {
                View child = backupLinearLayout.getChildAt(i);
                if (child instanceof Button) {
                    backupLinearLayout.removeView(child);
                    i--;
                }
            }

            initializeButtons(activity,googleCloud);

//            Button newGoogleLoginButton = googleCloud.createPrimaryLoginButton(primaryLinearLayout);
//            newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            Button newBackupLoginButton = googleCloud.createBackUpLoginButton(backupLinearLayout);
            newBackupLoginButton.setBackgroundTintList(UIHelper.backupAccountButtonColor);
        }

        public static void showAccountsAddPopup(JsonObject profileMapContent) {
            SharedPreferencesHandler.setDisplayDialogForRestoreAccountsDecision(preferences,true);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("New login");
            JsonArray profile = profileMapContent.get("profile").getAsJsonArray();
            String userEmail = profile.get(0).getAsJsonObject().get("userName").getAsString();
            builder.setMessage("We found older accounts connected to " + userEmail + ". " +
                            " Choose one of these two options:\n"  +
                            "Don't add older accounts to current accounts(Default).\n" + "Add older accounts to current accounts.")
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            SharedPreferencesHandler.setDisplayDialogForRestoreAccountsDecision(preferences,false);
                            Profile.insertBackupFromMap(profileMapContent.get("backupAccounts").getAsJsonArray());
                            Profile.insertPrimaryFromMap(profileMapContent.get("primaryAccounts").getAsJsonArray());
                            reInitializeButtons(activity, googleCloud);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Don't add", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            SharedPreferencesHandler.setDisplayDialogForRestoreAccountsDecision(preferences,false);

                            final boolean[] isSignedout = {false};
                            final boolean[] isBackedUp = {false};

                            backUpJsonThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
//                                    MainActivity.dbHelper.deleteFromAccountsTable(buttonText, "backup");
//                                    MainActivity.dbHelper.deleteAccountFromDriveTable(buttonText);
//                                    dbHelper.deleteRedundantAsset();
//                                    isBackedUp[0] = MainActivity.dbHelper.backUpProfileMap(true,buttonText);
//                                    System.out.println("isBackedUp " + isBackedUp[0]);

                                    synchronized (this){
                                        notify();
                                    }
                                }
                            });

                                        Thread signOutThread = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                synchronized (backUpJsonThread){
                                                    try {
                                                        backUpJsonThread.join();
                                                    } catch (InterruptedException e) {
                                                        LogHandler.saveLog("Failed to join back up json thread : "
                                                         + e.getLocalizedMessage(), true);
                                                    }
                                                }
//                                                if(isBackedUp[0]){
//                                                    isSignedout[0] = googleCloud.signOut(buttonText);
//                                                    System.out.println("isSignedOut " + isSignedout[0]);
//                                                }
                                            }
                                        });


                                        Thread uiThread = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                synchronized (signOutThread){
                                                    try {
                                                        signOutThread.join();
                                                    } catch (Exception e) {
                                                        LogHandler.saveLog(
                                                                "Failed to join sign out thread : "
                                                                + e.getLocalizedMessage(), true
                                                        );
                                                    }
                                                }
//                                                runOnUiThread(() -> {
//                                                    if (isBackedUp[0]) {
//                                                        try {
//                                                            item.setEnabled(false);
//                                                            ViewGroup parentView = (ViewGroup) button.getParent();
//                                                            parentView.removeView(button);
//                                                        } catch (Exception e) {
//                                                            e.printStackTrace();
//                                                        }
//                                                    } else {
//                                                        try {
//                                                            button.setText(buttonText);
//                                                        } catch (Exception e) {
//                                                            e.printStackTrace();
//                                                        }
//                                                    }
//                                                });
                                            }
                                        });

//                                        deleteProfileJsonThread.start();
                                        backUpJsonThread.start();
                                        signOutThread.start();
                                        uiThread.start();
        //                                            boolean isDeletedFromAccounts = dbHelper.deleteFromAccountsTable(buttonText,"backup");
        //                                            boolean isAccountDeletedFromDriveTable = dbHelper.deleteAccountFromDriveTable(buttonText);
        //                                            dbHelper.deleteRedundantAsset();
        //                                            ViewGroup parentView = (ViewGroup) button.getParent();
        //                                            parentView.removeView(button);
        //                                            googleCloud.signOut(buttonText);
                                    }
                                });

//                    }).setCancelable(false);

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }



