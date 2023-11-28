    package com.example.cso;

    import android.Manifest;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.pm.PackageManager;
    import android.os.Build;
    import android.os.Bundle;
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
        public static String logFileName ;
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
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            int requestCode =1;
            String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED |
                            ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, permissions, requestCode);
            }
            googleCloud = new GoogleCloud(this);
            logFileName = LogHandler.CreateLogFile();
            LogHandler.saveLog("Attention : Don't remove this file - this file makes sure that CSO app is working well.",false);
            LogHandler.saveLog("if you have any questions or problems, please contact us by : ",false);


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
                                PrimaryAccountInfo primaryAccountInfo = googleCloud.handleSignInToPrimaryResult(result.getData());
                                String userEmail = primaryAccountInfo.getUserEmail();
                                primaryAccountHashMap.put(primaryAccountInfo.getUserEmail(), primaryAccountInfo);
                                LogHandler.saveLog("Number of primary accounts : " + primaryAccountHashMap.size(),false);

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
                            runOnUiThread(() -> childview[0].setClickable(true));
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
                                LogHandler.saveLog("failed to join drive back up thread: "  + e.getLocalizedMessage());
                            }
                        }
                        Android.getGalleryMediaItems(MainActivity.this);
                        LogHandler.saveLog("End of getting files from your android device",false);
                    });

                    Thread deleteRedundantDriveThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (updateAndroidFilesThread){
                                try{
                                    updateAndroidFilesThread.join();
                                }catch (Exception e){
                                    LogHandler.saveLog("failed to join updateAndroidFilesThread : "  + e.getLocalizedMessage());
                                }
                            }
//                            GoogleDrive.getMediaItems();
//                            String[] columns = {"fileId"};
//                            List<String[]> backupRows = dbHelper.getDriveTable(columns);
//                            dbHelper.deleteRedundantDrive(backupRows,);
//note
                        }
                    });

//                    Thread driveBackUpThread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            synchronized (updateAndroidFilesThread){
//                                try{
//                                    updateAndroidFilesThread.join();
//                                }catch (Exception e){
//                                    LogHandler.saveLog("failed to join drive back up thread: "  + e.getLocalizedMessage());
//                                }
//                            }
//
//                            MainActivity.dbHelper.getDriveTable();
//                            GoogleDrive.deleteDuplicatedMediaItems(backupMediaItems,backupTokens);
//
//                        }
//                    });

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
                        synchronized (updateAndroidFilesThread){
                            try{
                                updateAndroidFilesThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join android thread: "  + e.getLocalizedMessage());
                            }
                        }
                        Upload upload = new Upload();
                        upload.upload();
                    });
//
                    Thread updateUIThread =  new Thread(() -> {
                        synchronized (androidUploadThread){
                            try{
                                androidUploadThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join update ui thread: "  + e.getLocalizedMessage());
                            }
                        }
                        runOnUiThread(() -> {
                            NotificationHandler.sendNotification("1","syncingAlert", MainActivity.this,
                                            "Syncing is finished","You're files are backed-up!");
                            TextView syncToBackUpAccountTextView = findViewById(R.id.syncToBackUpAccountTextView);
                            syncToBackUpAccountTextView.setText("Uploading process is finished");
                        });
                    });

                    deleteRedundantAndroidThread.start();
                    updateAndroidFilesThread.start();
//                    driveBackUpThread.start();
//                    photosUploadThread.start();
                    androidUploadThread.start();
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

                if (type.equals("primary")) {
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



