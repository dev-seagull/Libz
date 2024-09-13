package com.example.cso.UI;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;

import com.example.cso.DBHelper;
import com.example.cso.GoogleCloud;
import com.example.cso.GoogleDrive;
import com.example.cso.MainActivity;
import com.example.cso.Profile;
import com.example.cso.Unlink;
import com.google.api.services.drive.Drive;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;

public class Dialogs {
    public static void displayLinkProfileDialog(JsonObject resultJson, GoogleCloud.SignInResult signInResult){
        MainActivity.activity.runOnUiThread(() -> {
            try{
                String userEmail = signInResult.getUserEmail();
                String message = "This account is already linked with an existing profile. This action " +
                        "will link a profile to this device. If you want to add "
                        + userEmail + " alone, you have to unlink from the existing profile.";

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                builder.setTitle("Existing profile detected")
                        .setMessage(message)
                        .setPositiveButton("Proceed", (dialog, id) -> {
                            Log.d("signInToBackUpLauncher","Proceed pressed");
                            dialog.dismiss();
                            new Thread(() -> Profile.startSignInToProfileThread(resultJson,signInResult)).start();

                        })

                        .setNegativeButton("Cancel", (dialog, id) -> {
                            Log.d("signInToBackUpLauncher","Cancel pressed");

                            UI.update();
                            dialog.dismiss();
                        }).setCancelable(false);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
    }

    public static void showMoveDriveFilesDialog(String userEmail, Activity activity){
        try {
            double totalFreeSpace = Unlink.getTotalLinkedCloudsFreeSpace(userEmail);
            boolean isSingleAccountUnlink = Unlink.isSingleUnlink(userEmail);

            Log.d("Unlink", "Free storage of accounts except " + userEmail + " is " + totalFreeSpace);
            Log.d("Unlink", "Is single account unlink: " + isSingleAccountUnlink);

            int assetsSize = GoogleDrive.getAssetsSizeOfDriveAccount(userEmail);
            Log.d("Unlink", "getAssetsSizeOfDriveAccountThread finished for " + userEmail + ":" + assetsSize);

            MainActivity.activity.runOnUiThread(() -> {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                    boolean isAbleToMoveAllAssets = handleUnlinkBuilderTitleAndMessage(builder,userEmail,
                            isSingleAccountUnlink,assetsSize,totalFreeSpace);

                    builder.setPositiveButton("Proceed", (dialog, id) -> {
                        Log.d("Unlink", "Proceed pressed");
                        dialog.dismiss();
                        if (isSingleAccountUnlink) {
                            new Thread(() -> {
                                Log.d("Unlink", "Just unlink from single account ");
                                String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);
                                Drive service = GoogleDrive.initializeDrive(accessToken);
                                Log.d("Unlink", "Drive and access token : " + accessToken + service);
                                Unlink.unlinkSingleAccount(userEmail, service,false);
                            }).start();
                        }else{
                            new Thread( () -> {
                                Unlink.unlinkAccount(userEmail,isAbleToMoveAllAssets, activity);
                            }).start();
                        }
                    }).setNegativeButton("Cancel", (dialog, id) -> {
                        UI.update();// cancel unlink
                        Log.d("Unlink", "end of unlink: canceled");
                        dialog.dismiss();
                    });

                    builder.setCancelable(false);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }
            });

        }catch(Exception e){ FirebaseCrashlytics.getInstance().recordException(e); }
    }

    public static boolean handleUnlinkBuilderTitleAndMessage(AlertDialog.Builder builder,String userEmail,
                                                              boolean isSingleAccountUnlink, double assetsSize,
                                                              double totalFreeSpace){
        boolean isAbleToMoveAllAssets = false;
        try{
            if (isSingleAccountUnlink){
                builder.setTitle("No other available account");
                builder.setMessage("Caution : All of your assets in " + userEmail + " will be out of sync.");
            }else{
                if (totalFreeSpace < assetsSize) {
                    builder.setTitle("Not enough space");
                    builder.setMessage("Approximately " + totalFreeSpace / 1024 + " GB out of " + assetsSize / 1024 + " GB of your assets in " + userEmail + " will be moved to other available accounts." +
                            (assetsSize - totalFreeSpace) / 1024 + " GB of your assets will be out of sync.\n" +
                            "This process may take several minutes or hours depending on the size of your assets. ");

                } else {
                    builder.setTitle("Unlink Backup Account");
                    builder.setMessage("All of your assets in " + userEmail + " will be moved to your other available accounts.\n" +
                            "This process may take several minutes or hours depending on the size of your assets. ");
                    isAbleToMoveAllAssets = true;
                }
            }
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return isAbleToMoveAllAssets;
    }


}
