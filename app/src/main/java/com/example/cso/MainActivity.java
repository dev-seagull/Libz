    package com.example.cso;

    import android.app.Activity;
    import android.app.AlertDialog;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Build;
    import android.os.Bundle;
    import android.provider.Settings;
    import android.view.View;
    import android.widget.Button;
    import android.widget.LinearLayout;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AppCompatActivity;

    import com.bumptech.glide.Glide;
    import com.google.android.material.switchmaterial.SwitchMaterial;
    import com.google.gson.JsonArray;
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
                SharedPreferencesHandler.setFirstTime(preferences, false);
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
            System.out.println("I'm here and alive 4 ");
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

                                        boolean isBackedUp = Profile.backUpJsonFile(signInResult, signInToBackUpLauncher);

                                        UIHandler.addAbackUpAccountToUI(activity,isBackedUp,signInToBackUpLauncher,child,signInResult);

                                        DBHelper.insertMediaItemsAfterSignInToBackUp(signInResult);

                                        GoogleDrive.startThreads();

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

            UIHandler.initializeButtons(activity,googleCloud);

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
                            DBHelper.insertBackupFromProfileMap(profileMapContent.get("backupAccounts").getAsJsonArray());
                            DBHelper.insertPrimaryFromProfileMap(profileMapContent.get("primaryAccounts").getAsJsonArray());
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



