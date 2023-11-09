    package com.example.cso;

    import android.Manifest;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Environment;
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
    import java.util.Map;
    import java.util.concurrent.Callable;
    import java.util.concurrent.ExecutionException;
    import java.util.concurrent.Executor;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    import java.util.concurrent.Future;


    public class MainActivity extends AppCompatActivity {

        private DrawerLayout drawerLayout;
        Button syncToBackUpAccountButton;
        GoogleCloud googleCloud;
        ActivityResultLauncher<Intent> signInToPrimaryLauncher;
        ActivityResultLauncher<Intent> signInToBackUpLauncher;
        GooglePhotos googlePhotos;
        HashMap<String, PrimaryAccountInfo> primaryAccountHashMap = new HashMap<>();
        HashMap<String, BackUpAccountInfo> backUpAccountHashMap = new HashMap<>();
        ArrayList<Android.MediaItem> androidMediaItems = new ArrayList<>();
        String androidDeviceName;
        public static String logFileName ;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
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

            syncToBackUpAccountButton = findViewById(R.id.syncToBackUpAccountButton);
            TextView androidDeviceNameTextView = findViewById(R.id.androidDeviceTextView);
            androidDeviceName = DeviceName.getDeviceName();
            androidDeviceNameTextView.setText(androidDeviceName);

            LinearLayout primaryAccountsButtonsLayout= findViewById(R.id.primaryAccountsButtons);
            LinearLayout backupAccountsButtonsLayout= findViewById(R.id.backUpAccountsButtons);
            googleCloud = new GoogleCloud(this);
            googleCloud.createPrimaryLoginButton(primaryAccountsButtonsLayout);
            googleCloud.createBackUpLoginButton(backupAccountsButtonsLayout);
            logFileName = LogHandler.CreateLogFile();
            LogHandler.saveLog("Attention : Don't remove this file - this file makes sure that CSO app is working well.",false);
            LogHandler.saveLog("if you have any questions or problems, please contact us by : ",false);
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

            LogHandler.saveLog("Starting to get files from you android device",false);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<ArrayList<Android.MediaItem>> androidBackgroundTask = () -> {
                Android android = new Android();
                androidMediaItems = android.getGalleryMediaItems(MainActivity.this);
                return androidMediaItems;
            };
            Future<ArrayList<Android.MediaItem>> future = executor.submit(androidBackgroundTask);
            try {
                androidMediaItems = future.get();
            } catch (ExecutionException | InterruptedException e) {
                LogHandler.saveLog("failed to get android files: " + e.getLocalizedMessage());
            }
            executor.shutdown();

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

                    BackUpAccountInfo firstBackUpAccountInfo = backUpAccountHashMap.values().iterator().next();
                    PrimaryAccountInfo.Tokens backupTokens = firstBackUpAccountInfo.getTokens();
                    String backUpAccessToken = firstBackUpAccountInfo.getTokens().getAccessToken();
                    ArrayList<BackUpAccountInfo.MediaItem> backupMediaItems = firstBackUpAccountInfo.getMediaItems();
                    Thread driveBackUpThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            GoogleDrive.deleteDuplicatedMediaItems(backupMediaItems,backupTokens);
                            synchronized (this){
                                notify();
                            }
                        }
                    });

                    Thread photosUploadThread = new Thread(() -> {
                        synchronized (driveBackUpThread){
                            try{
                                driveBackUpThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join drive back up thread: "  + e.getLocalizedMessage());
                            }
                        }
                        for (PrimaryAccountInfo primaryAccountInfo : primaryAccountHashMap.values()) {
                            ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems = firstBackUpAccountInfo.getMediaItems();
                            ArrayList<GooglePhotos.MediaItem> mediaItems = primaryAccountInfo.getMediaItems();
                            Upload upload = new Upload();

                            upload.uploadPhotosToGoogleDrive(mediaItems, backUpAccessToken
                                    ,backUpMediaItems,primaryAccountInfo.getUserEmail(),firstBackUpAccountInfo.getUserEmail());
                        }
                    });

                    Thread androidUploadThread = new Thread(() -> {
                        synchronized (photosUploadThread){
                            try{
                                photosUploadThread.join();
                            }catch (Exception e){
                                LogHandler.saveLog("failed to join android thread: "  + e.getLocalizedMessage());
                            }
                        }
                        uploadAndroidToDriveAccounts(backUpAccessToken);
                    });

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

                    driveBackUpThread.start();
                    photosUploadThread.start();
                    androidUploadThread.start();
                    updateUIThread.start();
                }
            });
        }

        void uploadAndroidToDriveAccounts(String backUpAccessToken){
            try{
                Executor uploadExecutor = Executors.newSingleThreadExecutor();
                Runnable backgroundTask = () -> {
                    BackUpAccountInfo firstBackUpAccountInfo = backUpAccountHashMap.values().iterator().next();
                    ArrayList<BackUpAccountInfo.MediaItem> backUpMediaItems = firstBackUpAccountInfo.getMediaItems();
                    for(PrimaryAccountInfo primaryAccountInfo: primaryAccountHashMap.values()){
                        ArrayList<GooglePhotos.MediaItem> primaryMediaItems = primaryAccountInfo.getMediaItems();
                        System.out.println("here to sync android!");
                        googlePhotos.uploadAndroidToGoogleDrive(androidMediaItems, primaryMediaItems ,backUpAccessToken,
                                backUpMediaItems, this);
                    }
                };
                uploadExecutor.execute(backgroundTask);
            }catch (Exception e){
                LogHandler.saveLog("Failed to upload from android to drive: " + e.getLocalizedMessage());
            }
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
                                } else {
                                    PopupMenu popupMenu = new PopupMenu(MainActivity.this, button, Gravity.CENTER);
                                    popupMenu.getMenuInflater().inflate(R.menu.account_button_menu, popupMenu.getMenu());
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        popupMenu.setGravity(Gravity.CENTER);
                                    }
                                    popupMenu.setOnMenuItemClickListener(item -> {
                                        if (item.getItemId() == R.id.sign_out) {
                                            googleCloud.signOut();
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
    }



