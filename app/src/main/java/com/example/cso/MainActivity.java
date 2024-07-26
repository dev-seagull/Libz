    package com.example.cso;

    import android.app.Activity;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Build;
    import android.os.Bundle;
    import android.provider.Settings;
    import android.util.Log;
    import android.view.View;
    import android.widget.LinearLayout;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AppCompatActivity;

    import com.bumptech.glide.Glide;
    import com.google.android.material.switchmaterial.SwitchMaterial;
    import com.google.gson.JsonObject;
    import com.jaredrummler.android.device.DeviceName;

    import java.io.IOException;
    import java.util.List;
    import java.util.Timer;
    import java.util.TimerTask;


    public class MainActivity extends AppCompatActivity {
        public static Activity activity ;
        static GoogleCloud googleCloud;
//        ActivityResultLauncher<Intent> signInToPrimaryLauncher;
        public ActivityResultLauncher<Intent> signInToBackUpLauncher;
//        GooglePhotos googlePhotos;
        public static String androidUniqueDeviceIdentifier;
        public static String androidDeviceName;
        static SharedPreferences preferences;
        public static boolean isLoginProcessOn = false;
        public static DBHelper dbHelper;
        public static StorageHandler storageHandler;
        public static Timer timer;
        public static Thread backUpJsonThread;
        public static Intent serviceIntent;
        SwitchMaterial wifiOnlySwitchMaterial;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            activity = this;
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            System.out.println("I'm here and alive 1 ");
            PermissionManager permissionManager = new PermissionManager();
            boolean isStorageAccessGranted = permissionManager.requestStorageAccess(activity);
            boolean areReadAndWritePermissionsAccessGranted = permissionManager.requestManageReadAndWritePermissions(activity);

            boolean hasCreated = LogHandler.createLogFile();
            System.out.println("Log file is created :"  + hasCreated);

            preferences = getPreferences(Context.MODE_PRIVATE);
            boolean isFirstTime = SharedPreferencesHandler.getFirstTime(preferences);
            if(isFirstTime){
                System.out.println("it is first time!");
                DBHelper.startOldDatabaseDeletionThread(getApplicationContext());
                SharedPreferencesHandler.setFirstTime(preferences);
            }
            System.out.println("I'm here and alive 2 ");
            dbHelper = new DBHelper(this,DBHelper.NEW_DATABASE_NAME);
            googleCloud = new GoogleCloud(this);
            androidUniqueDeviceIdentifier = Settings.Secure.getString(getApplicationContext().getContentResolver(),Settings.Secure.ANDROID_ID);
            androidDeviceName = DeviceName.getDeviceName();
            UIHelper uiHelper = new UIHelper();
            storageHandler = new StorageHandler();

            LogHandler.saveLog("---------------Start of app--------------", false);

            UIHandler.initializeDrawerLayout(activity);
            UIHandler.initializeButtons(this,googleCloud);
            UIHandler.handleSwitchMaterials();

//            Upgrade.versionHandler(preferences);
//            if(dbHelper.DATABASE_VERSION < 11) {
//                LogHandler.saveLog("Starting to update database from version 1 to version 2.", false);
//            }
            System.out.println("I'm here and alive 3 ");
            serviceIntent = new Intent(this.getApplicationContext(), TimerService.class);

            uiHelper.deviceStorageTextView.setText("Wait until we get an update of your assets ...");
            Glide.with(this).asGif().load(R.drawable.gifwaiting).into(UIHelper.waitingGif);
            new Thread(() -> { Android.startThreads(activity); }).start();

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    LogHandler.saveLog("Started the timer",false);
                    Thread androidUpdate = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if(!TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on")){
                                Android.startThreads(activity);
                            }
                        }
                    });
                    androidUpdate.start();
                    try{
                        UIHandler.startUpdateUIThread(activity);
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to run on ui thread : " + e.getLocalizedMessage() , true);
                    }
                    LogHandler.saveLog("Finished the timer",false);
                }
            }, 0, 3000);


            LogHandler.saveLog("--------------------------end of onCreate----------------------------", false);
        }

        @Override
        protected void onStart(){
            super.onStart();
            LogHandler.saveLog("--------------------------start of onStart----------------------------",false);
            LogHandler.saveLog("Build.VERSION.SDK_INT and Build.VERSION_CODES.M : " + Build.VERSION.SDK_INT +
                    Build.VERSION_CODES.M, false);
            LogHandler.saveLog("The action on log file was performed", false);

            UIHelper uiHelper = new UIHelper();

            new Thread(() -> {
                if(!InternetManager.getInternetStatus(getApplicationContext()).equals("noInternet")) {
                    boolean databaseIsBackedUp = dbHelper.backUpDataBaseToDrive(getApplicationContext());
                    if(!databaseIsBackedUp){
                        LogHandler.saveLog("Database is not backed up ", true);
                    }
                }
            }).start();

            String refreshToken = "";
            String[] accessTokens = new String[1];
            List<String[]> accountRows = DBHelper.getAccounts(new String[]{"type","userEmail","refreshToken","accessToken"});
            for (String[] row : accountRows) {
                if (row.length > 0 && row[1] != null) {
                    refreshToken = row[2];
                    String finalRefreshToken1 = refreshToken;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                System.out.println("for Email : " + GoogleCloud.isAccessTokenValid(googleCloud.updateAccessToken(finalRefreshToken1).getAccessToken()));
                            } catch (IOException e) {
                                System.out.println("error in here "+e.getLocalizedMessage());
                            }
                        }
                    }).start();
                }
            }

            signInToBackUpLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        System.out.println("result of login is: " + result.toString());
                        if(result.getResultCode() == RESULT_OK){
                            isLoginProcessOn = true;
                            LinearLayout backupButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                            View[] child = {backupButtonsLinearLayout.getChildAt(
                                    backupButtonsLinearLayout.getChildCount() - 1)};
                            UIHandler.setLastBackupAccountButtonClickableFalse(activity);

                            try{
                                Thread signInToBackUpThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final GoogleCloud.signInResult signInResult =
                                                googleCloud.startSignInToBackUpThread(result.getData());
                                        System.out.println("Refresh token is:" + signInResult.getTokens().getRefreshToken());

                                        String userEmail = signInResult.getUserEmail();
                                        String accessToken = signInResult.getTokens().getAccessToken();
                                        JsonObject resultJson = Profile.readProfileMapContent(userEmail,accessToken);

                                        boolean isLinked = Profile.isLinkedToAccounts(resultJson,userEmail);
                                        if (isLinked){
                                            UIHandler.displayLinkProfileDialog(resultJson,userEmail);
                                        }

                                        LogHandler.saveLog("Starting backupJsonFile thread",false);
                                        boolean isBackedUp = Profile.backUpJsonFile(signInResult, signInToBackUpLauncher);
                                        LogHandler.saveLog("Finished backupJsonFile thread",false);

                                        LogHandler.saveLog("Starting addAbackUpAccountToUI thread",false);
                                        UIHandler.addAbackUpAccountToUI(activity,true,signInToBackUpLauncher,child,signInResult);
                                        LogHandler.saveLog("Finished addAbackUpAccountToUI thread",false);

                                        LogHandler.saveLog("Starting insertMediaItemsAfterSignInToBackUp thread",false);
                                        DBHelper.insertMediaItemsAfterSignInToBackUp(signInResult);
                                        LogHandler.saveLog("Finished insertMediaItemsAfterSignInToBackUp thread",false);

                                        LogHandler.saveLog("Starting Drive.startThreads thread",false);
                                        GoogleDrive.startThreads();
                                        LogHandler.saveLog("Finished Drive.startThreads thread",false);

                                        child[0].setClickable(true);
                                    }
                                });
                                signInToBackUpThread.start();
                            }catch (Exception e){
                                LogHandler.saveLog("Failed to sign in to backup : "  + e.getLocalizedMessage());
                            }finally {
                                isLoginProcessOn = false;
                            }
                        }
                        else{
                            UIHandler.handleFailedSignInToBackUp(activity, signInToBackUpLauncher, result);
                        }
                    });

            UIHandler.updateButtonsListeners(signInToBackUpLauncher);

            uiHelper.syncSwitchMaterialButton = findViewById(R.id.syncSwitchMaterial);
            uiHelper.syncSwitchMaterialButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(uiHelper.syncSwitchMaterialButton.isChecked()){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (!TimerService.isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
                                startService(serviceIntent);
                                wifiOnlySwitchMaterial.setThumbTintList(UIHelper.onSwitchMaterialThumb);
                                wifiOnlySwitchMaterial.setTrackTintList(UIHelper.onSwitchMaterialTrack);
                                wifiOnlySwitchMaterial.setAlpha(1.0f);
                                SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",true,preferences);
                            }
                        }
                    }else{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (TimerService.isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
                                stopService(serviceIntent);
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uiHelper.syncMessageTextView.setVisibility(View.GONE);
                                UIHelper.waitingSyncGif.setVisibility(View.GONE);
                            }
                        });
                    }
                    UIHandler.handleSwitchMaterials();
                }
            });

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
            LogHandler.saveLog("--------------------------end of onStart----------------------------",false);
        }

        @Override
        public void onResume(){
            super.onResume();
        }

        @Override
        public void onPause(){
            super.onPause();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            System.out.println("Here stopping the timer on ui in onDestroy");
            timer.cancel();
            timer.purge();
        }

    }



