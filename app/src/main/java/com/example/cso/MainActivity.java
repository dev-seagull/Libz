    package com.example.cso;

    import android.app.Activity;
    import android.app.AlertDialog;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Build;
    import android.os.Bundle;
    import android.provider.Settings;
    import android.util.Log;
    import android.view.View;
    import android.widget.LinearLayout;
    import android.widget.Toast;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.fragment.app.FragmentActivity;
    import androidx.fragment.app.FragmentManager;

    import com.google.firebase.FirebaseApp;
    import com.google.firebase.analytics.FirebaseAnalytics;
    import com.google.firebase.crashlytics.FirebaseCrashlytics;
    import com.google.gson.JsonObject;
    import com.jaredrummler.android.device.DeviceName;

    import java.io.IOException;
    import java.util.List;
    import java.util.Timer;


    public class MainActivity extends AppCompatActivity {
        public static boolean isStoragePermissionGranted = false;
        public static boolean isReadAndWritePermissionGranted = false;
        public static Activity activity ;
        public static GoogleCloud googleCloud;
        public static ActivityResultLauncher<Intent> signInToBackUpLauncher;
        public static String androidUniqueDeviceIdentifier;
        public static String androidDeviceName;
        public static SharedPreferences preferences;
        public static boolean isLoginProcessOn = false;
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

            initAppUI();

//            boolean isFirstTime = SharedPreferencesHandler.getFirstTime(preferences);
//            if(isFirstTime){
//                System.out.println("it is first time!");
//                DBHelper.startOldDatabaseDeletionThread(getApplicationContext());
//                SharedPreferencesHandler.setFirstTime(preferences);
//            }

            Log.d("state","end of onCreate");
        }



        @Override
        protected void onStart(){
            super.onStart();
            Log.d("state","start of onStart");

            System.out.println(isStoragePermissionGranted);
            System.out.println(isReadAndWritePermissionGranted);
            Thread permissionThread = new Thread(() -> {
                permissionManager.requestPermissions(this);
            });
            permissionThread.start();
            try{
                permissionThread.join();
            }catch (Exception e){
                System.out.println("permission error:" + e.getLocalizedMessage());
            }

            Log.d("state","end of onStart");
        }

    @Override
    public void onResume(){
        super.onResume();
        Log.d("state","start of onResume");
        System.out.println("is: " +isStoragePermissionGranted  + " " + isReadAndWritePermissionGranted);
        if(isStoragePermissionGranted && isReadAndWritePermissionGranted){
            boolean hasCreated = LogHandler.createLogFile();
            System.out.println("Log file is created :"  + hasCreated);

            storageHandler = new StorageHandler();

            UIHandler uiHandler = new UIHandler();
            uiHandler.initializeUI(activity,preferences);

            Log.d("state","start of onStart");
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

            new Thread(() -> {
                if (Deactivation.isDeactivationFileExists()){
                    MainActivity.activity.runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                            "you're deActivated, Call support", Toast.LENGTH_SHORT).show());
                    MainActivity.activity.finish();
                }
                if (Profile.hasJsonChanged()){
                    System.out.println("profile json changed");
                    dbHelper.updateDatabaseBasedOnJson();
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
                                Thread signInToBackUpThread = new Thread(() -> {
                                    LogHandler.saveLog("Started to get signInResult.",false);
                                    final GoogleCloud.signInResult signInResult =
                                            googleCloud.handleSignInToBackupResult(result.getData());
                                    LogHandler.saveLog("Finished to get signInResult.",false);
                                    String userEmail = signInResult.getUserEmail();
                                    String accessToken = signInResult.getTokens().getAccessToken();

                                    LogHandler.saveLog("Started to read profile map content.",false);
                                    JsonObject resultJson = Profile.readProfileMapContent(userEmail,accessToken);
                                    LogHandler.saveLog("Finished to read profile map content.",false);

                                    if(resultJson != null){
                                        LogHandler.saveLog("@@@" + "read profile : " + resultJson.toString(),false);
                                    }

                                    LogHandler.saveLog("Started to set json modified time.",false);
                                    SharedPreferencesHandler.setJsonModifiedTime(preferences);
                                    LogHandler.saveLog("Finished to set json modified time.",false);

                                    LogHandler.saveLog("@@@" + "json last modified : " + SharedPreferencesHandler.getJsonModifiedTime(preferences),false);

                                    LogHandler.saveLog("Started to check if it's linked to accounts.",false);
                                    boolean isLinked = Profile.isLinkedToAccounts(resultJson,userEmail);
                                    LogHandler.saveLog("@@@" + "is linked to other accounts : " + isLinked,false);
                                    LogHandler.saveLog("Finished to check if it's linked to accounts.",false);
                                    if (isLinked){
                                        UIHandler.displayLinkProfileDialog(signInToBackUpLauncher, child,
                                                resultJson, signInResult);
                                    }else{
                                        Profile profile = new Profile();
                                        profile.linkToAccounts(signInResult,child);
                                    }

                                    child[0].setClickable(true);
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
        }else{
            if(!isReadAndWritePermissionGranted && isStoragePermissionGranted){
                Toast.makeText(getApplicationContext(),"Read and Write Permissions Denied", Toast.LENGTH_LONG).show();
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
//        finish();
        System.out.println("Here stopping the timer on ui in onDestroy");
        if(androidTimer != null){
            androidTimer.cancel();
            androidTimer.purge();
        }
        if(UITimer != null){
            UITimer.cancel();
            UITimer.purge();
        }
        Log.d("state","end of onDestroyed");
    }
//
//    @Override
//    public void onBackPressed() {
//        Log.d("state","start of onBackPressed");
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        if (fragmentManager.getBackStackEntryCount() > 0) {
//            fragmentManager.popBackStack();
//        } else {
//            // Example 2: Show confirmation dialog before finishing
//            new AlertDialog.Builder(this)
//                    .setTitle("Confirm Exit")
//                    .setMessage("Do you really want to exit?")
//                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
//                        stopService(serviceIntent);
//                        if (androidTimer != null) {
//                            androidTimer.cancel();
//                            androidTimer.purge();
//                        }
//                        if (UITimer != null) {
//                            UITimer.cancel();
//                            UITimer.purge();
//                        }
//                        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
//                        SharedPreferences.Editor editor = preferences.edit();
//                        editor.clear();
//                        editor.apply();
//
//                        finishAffinity();
//                        System.exit(0);
//                    })
//                    .setNegativeButton(android.R.string.no, null)
//                    .show();
//        }
//        Log.d("state","end of onBackPressed");
//    }

    private void initAppUI(){
        UIHandler uiHandler = new UIHandler();
        uiHandler.initializeDrawerLayout();
        uiHandler.initializeDeviceButton(false);
        uiHandler.initializeSyncButton();
        uiHandler.initializeWifiOnlyButton();
        uiHandler.initializeButtons(MainActivity.googleCloud);
    }

}



