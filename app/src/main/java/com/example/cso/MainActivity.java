    package com.example.cso;

    import android.app.Activity;
    import android.app.ActivityManager;
    import android.app.AlertDialog;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.os.Build;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.Gravity;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.LinearLayout;
    import android.widget.PopupMenu;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AppCompatActivity;

    import com.google.android.material.switchmaterial.SwitchMaterial;
    import com.google.gson.JsonArray;
    import com.google.gson.JsonObject;
    import com.jaredrummler.android.device.DeviceName;

    import org.checkerframework.checker.guieffect.qual.UI;

    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.List;
    import java.util.Timer;
    import java.util.TimerTask;


    public class MainActivity extends AppCompatActivity {
        public static Activity activity ;
        static GoogleCloud googleCloud;
//        ActivityResultLauncher<Intent> signInToPrimaryLauncher;
        public ActivityResultLauncher<Intent> signInToBackUpLauncher;
//        GooglePhotos googlePhotos;
        public static String androidDeviceName;
        static SharedPreferences preferences;
        public static DBHelper dbHelper;
        public static StorageHandler storageHandler;
        public Thread secondUiThread;
        public static Timer timer;
        public static Thread backUpJsonThread;
        public static Intent serviceIntent;
        SwitchMaterial wifiOnlySwitchMaterial;
        List<Thread> threads = new ArrayList<>(Arrays.asList( secondUiThread,
                backUpJsonThread));

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            activity = this;
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            PermissionManager permissionManager = new PermissionManager();
            boolean isStorageAccessGranted = permissionManager.requestStorageAccess(activity);

            boolean hasCreated = LogHandler.createLogFile();
            System.out.println("Log file is created :"  + hasCreated);

            preferences = getPreferences(Context.MODE_PRIVATE);
            dbHelper = new DBHelper(this,"StashDatabase");
            googleCloud = new GoogleCloud(this);
            androidDeviceName = DeviceName.getDeviceName();
            UIHelper uiHelper = new UIHelper();
            storageHandler = new StorageHandler();

            LogHandler.saveLog("---------------Start of app--------------",false);
            LogHandler.saveLog("Build.VERSION.SDK_INT and Build.VERSION_CODES.M : " + Build.VERSION.SDK_INT +
                    Build.VERSION_CODES.M, false);

            UIHandler.initializeDrawerLayout(activity);
            UIHandler.initializeButtons(this,googleCloud);
            UIHandler.handleSwitchMaterials();
            boolean areReadAndWritePermissionsAccessGranted = permissionManager.requestManageReadAndWritePermissions(activity);

//            LogHandler.actionOnLogFile();
//            Upgrade.versionHandler(preferences);
//            if(dbHelper.DATABASE_VERSION < 11) {
//                LogHandler.saveLog("Starting to update database from version 1 to version 2.", false);
//            }
//            dbHelper.insertSupportCredential();

            serviceIntent = new Intent(this.getApplicationContext(), TimerService.class);

            uiHelper.deviceStorageTextView.setText("Wait until we get an update of your assets ...");

            Android.startThreads(activity);

            GoogleDrive.startThreads();

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    LogHandler.saveLog("Started the timer",false);
                    try{
                        UIHandler.startUpdateUIThread(activity);
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to run on ui thread : " + e.getLocalizedMessage() , true);
                    }
                    LogHandler.saveLog("Finished the timer",false);
                }
            }, 0, 3000);

            LogHandler.saveLog("--------------------------end of onCreate----------------------------",false);
        }

        @Override
        protected void onStart(){
            LogHandler.saveLog("--------------------------start of onStart----------------------------",false);
            super.onStart();
            UIHelper uiHelper = new UIHelper();
//            dbHelper.backUpDataBase(getApplicationContext());
//            String exportPath = getDatabasePath(DBHelper.NEW_DATABASE_NAME + "_decrypted.db").getPath();
//            dbHelper.exportDecryptedDatabase(exportPath);

            signInToBackUpLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if(result.getResultCode() == RESULT_OK){
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
                            }
                        }
                        else{
                            UIHandler.handleFailedSignInToBackUp(activity, signInToBackUpLauncher, result);
                        }
                    });

            updateButtonsListeners(signInToBackUpLauncher);

            uiHelper.syncSwitchMaterialButton = findViewById(R.id.syncSwitchMaterial);
            uiHelper.syncSwitchMaterialButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(uiHelper.syncSwitchMaterialButton.isChecked()){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (!isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
                                startService(serviceIntent);
                                wifiOnlySwitchMaterial.setThumbTintList(UIHelper.onSwitchMaterialThumb);
                                wifiOnlySwitchMaterial.setTrackTintList(UIHelper.onSwitchMaterialTrack);
                                wifiOnlySwitchMaterial.setAlpha(1.0f);
                                SharedPreferencesHandler.setSwitchState("wifiOnlySwitchState",true,preferences);
                            }
                        }
                    }else{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (isMyServiceRunning(activity.getApplicationContext(),TimerService.class).equals("on")){
                                stopService(serviceIntent);
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uiHelper.syncMessageTextView.setVisibility(View.GONE);
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
                                                        isBackedUp[0] = Profile.backUpProfileMap(true,buttonText);
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



