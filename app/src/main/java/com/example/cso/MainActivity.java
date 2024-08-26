    package com.example.cso;

    import android.app.Activity;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.provider.Settings;
    import android.util.Log;
    import android.view.View;
    import android.widget.LinearLayout;
    import android.widget.Toast;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AppCompatActivity;

    import com.google.firebase.analytics.FirebaseAnalytics;
    import com.google.firebase.crashlytics.FirebaseCrashlytics;
    import com.google.gson.JsonObject;
    import com.jaredrummler.android.device.DeviceName;

    import java.util.Timer;
    import java.util.TimerTask;


    public class MainActivity extends AppCompatActivity {
        public static boolean isStoragePermissionGranted = false;
        public static boolean isReadAndWritePermissionGranted = false;
        public static boolean isGettingReadAndWritePermission = false;
        public static Activity activity ;
        public static GoogleCloud googleCloud;
        public static ActivityResultLauncher<Intent> signInToBackUpLauncher;
        public static String androidUniqueDeviceIdentifier;
        public static String androidDeviceName;
        public static SharedPreferences preferences;
        public static boolean isAnyProccessOn = false;
        public static DBHelper dbHelper;
        public static StorageHandler storageHandler;
        public static Timer androidTimer;
        public static Timer UITimer;
        public static Intent serviceIntent;
        public static boolean androidTimerIsRunning = false;
        private PermissionManager permissionManager = new PermissionManager();;
        public static FirebaseAnalytics mFirebaseAnalytics;
        public static String dataBaseName = "StashDatabase";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            activity = this;
            Log.d("state","start of onCreate");

            preferences = getPreferences(Context.MODE_PRIVATE);
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            dbHelper = DBHelper.getInstance(this);
            googleCloud = new GoogleCloud(this);
            androidUniqueDeviceIdentifier = Settings.Secure.getString(getApplicationContext().getContentResolver(),Settings.Secure.ANDROID_ID);
            serviceIntent = new Intent(this , TimerService.class);
            androidDeviceName = DeviceName.getDeviceName();
            DeviceHandler.insertIntoDeviceTable(MainActivity.androidDeviceName,
                    MainActivity.androidUniqueDeviceIdentifier);
            Log.d("androidId", androidUniqueDeviceIdentifier);
            Log.d("androidDeviceName", androidDeviceName);

            UIHandler.initAppUI();

//            boolean isFirstTime = SharedPreferencesHandler.getFirstTime(preferences);
//            if(isFirstTime){
//                System.out.println("it is first time!");
//                DBHelper.startOldDatabaseDeletionThread(getApplicationContext());
//                SharedPreferencesHandler.setFirstTime(preferences);
//            }

            signInToBackUpLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d("signInToBackUpLauncher", "result code: " + result.getResultCode());
                        Log.d("signInToBackUpLauncher", "result data: " + result.getData());
                        if(result.getResultCode() == RESULT_OK){
                            isAnyProccessOn = true;
                            LinearLayout backupButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                            View[] lastButton = {backupButtonsLinearLayout.getChildAt(
                                    backupButtonsLinearLayout.getChildCount() - 1)};
                            Thread signInToBackUpThread = new Thread(() -> {
                                try {
                                    Log.d("signInToBackUpLauncher","Sign in started");
                                    final GoogleCloud.SignInResult signInResult =
                                            googleCloud.handleSignInToBackupResult(result.getData());
                                    Log.d("signInToBackUpLauncher","Sign in finished");

                                    String userEmail = signInResult.getUserEmail();
                                    String accessToken = signInResult.getTokens().getAccessToken();
                                    Log.d("signInToBackUpLauncher","userEmail: " + userEmail);
                                    Log.d("signInToBackUpLauncher","accessTokenNull : " + (accessToken == null));

                                    Log.d("signInToBackUpLauncher","Reading profile map started");
                                    JsonObject resultJson = Profile.readProfileMapContent(userEmail,accessToken);
                                    Log.d("signInToBackUpLauncher","Reading profile map finished");

                                    if(resultJson != null){
                                        Log.d("signInToBackUpLauncher","Profile map: " + resultJson.toString());
                                    }

                                    Log.d("signInToBackUpLauncher", "Setting json modified time started");
                                    SharedPreferencesHandler.setJsonModifiedTime(preferences);
                                    Log.d("signInToBackUpLauncher", "Setting json modified time finished: " +  SharedPreferencesHandler.getJsonModifiedTime(preferences));

                                    Log.d("signInToBackUpLauncher","Checking if it's linked to accounts started");
                                    boolean isLinked = Profile.isLinkedToAccounts(resultJson,userEmail);
                                    Log.d("signInToBackUpLauncher","Checking if it's linked to accounts finished: " + isLinked);

                                    if (isLinked){
                                        UIHandler.displayLinkProfileDialog(signInToBackUpLauncher, lastButton,
                                                resultJson, signInResult);
                                    }else{
                                        Profile profile = new Profile();
                                        profile.loginSingleAccount(signInResult,lastButton,signInToBackUpLauncher);
                                    }
                                    lastButton[0].setClickable(true);
                                }catch (Exception e){
                                    FirebaseCrashlytics.getInstance().recordException(e);
                                }finally {
                                    isAnyProccessOn = false;
                                }
                            });
                            signInToBackUpThread.start();
                        }else{
                            UIHandler.setupAccountButtons();
                        }
                    });

            Log.d("state","end of onCreate");
        }



    @Override
    protected void onStart(){
        super.onStart();
        Log.d("state","start of onStart");

        Log.d("permissions","isStoragePermissionGranted : " + isStoragePermissionGranted);
        Log.d("permissions","isReadAndWritePermissionGranted : " + isReadAndWritePermissionGranted);
        Thread permissionThread = new Thread(() -> {
            permissionManager.requestPermissions(this);
        });
        permissionThread.start();
        try{
            permissionThread.join();
        }catch (Exception e){
            Log.d("error","Permission error: " + e.getLocalizedMessage());
        }

        Log.d("state","end of onStart");
    }


    @Override
    public void onResume(){
        super.onResume();
        Log.d("state","start of onResume");

        if(isGettingReadAndWritePermission){
            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                System.out.println("Error while waiting : " + e.getLocalizedMessage());
            }
        }
        Log.d("permissions","isStoragePermissionGranted : " + isStoragePermissionGranted);
        Log.d("permissions","isReadAndWritePermissionGranted : " + isReadAndWritePermissionGranted);
        if(isStoragePermissionGranted && isReadAndWritePermissionGranted){
            boolean hasCreated = LogHandler.createLogFile();
            Log.d("logFile","Log file is created :"  + hasCreated);

            Upgrade.versionHandler(preferences);

            setupTimers();

            new Thread(() -> {
                if (Deactivation.isDeactivationFileExists()){ UIHandler.handleDeactivatedUser(); }

                Support.checkSupportBackupRequired();

                if (Profile.hasJsonChanged()){
                    dbHelper.updateDatabaseBasedOnJson();
                }
            }).start();

            UIHandler.setupAccountButtons();
        }else{
            if(!isReadAndWritePermissionGranted && isStoragePermissionGranted){
                Toast.makeText(getApplicationContext(),"Read and Write Permissions Required", Toast.LENGTH_LONG).show();
            }
        }
        Log.d("state","end of onResume");
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d("state","start of onPause");
        Log.d("state","end of onPause");
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.d("state","start of onStop");
        Log.d("state","end of onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("state","start of onDestroyed");
        destroyAndroidTimer();
        destroyUITimer();
//        finishAffinity();
//        System.exit(0);
//        finish();
        Log.d("state","end of onDestroyed");
    }


    private void destroyAndroidTimer(){
        if(androidTimer != null){
            androidTimer.cancel();
            androidTimer.purge();
        }
        if(androidTimer == null){
            Log.d("Timers", "stopped android timer");
        }
    }

    private void destroyUITimer(){
        if(UITimer != null){
            UITimer.cancel();
            UITimer.purge();
        }
        if(UITimer == null){
            Log.d("Timers", "stopped ui timer");
        }
    }

    public void setupTimers(){
        setupAndroidTimer();
        setupUITimer();
    }

    private static void setupAndroidTimer(){
        try{
            if (MainActivity.androidTimer == null){
                MainActivity.androidTimer = new Timer();
                MainActivity.androidTimer.schedule(new TimerTask() {
                    public void run() {
                        if (androidTimerIsRunning){
                            return;
                        }
                        androidTimerIsRunning = true;
                        Log.d("Threads","Android timer started");
                        if(!TimerService.isMyServiceRunning(activity.getApplicationContext(), TimerService.class).equals("on")){
                            Thread androidUpdateThread = new Thread(() -> { Android.startThreads(activity); } );
                            androidUpdateThread.start();
                        }
                        Log.d("Threads","Android timer finished");
                    }
                }, 1000, 5000);
            }
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private static void setupUITimer(){
        try{
            if (MainActivity.UITimer == null){
                MainActivity.UITimer = new Timer();
                MainActivity.UITimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try{
                            UIHandler.startUpdateUIThread(activity);
                        }catch (Exception e){
                            LogHandler.saveLog("Failed to run on ui thread : " + e.getLocalizedMessage() , true);
                        }
                    }
                }, 1000, 1000);
            }
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e);}
    }
}



