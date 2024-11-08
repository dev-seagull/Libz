package com.example.cso.UI;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.example.cso.DBHelper;
import com.example.cso.GoogleCloud;
import com.example.cso.GoogleDrive;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.example.cso.Profile;
import com.example.cso.Unlink;
import com.google.api.services.drive.Drive;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;

public class Dialogs {
    public static void displayLinkProfileDialog(JsonObject resultJson, GoogleCloud.SignInResult signInResult, View lastButton){
        MainActivity.activity.runOnUiThread(() -> {
            try{
                String userEmail = signInResult.getUserEmail();

                String message = "This account is already linked with an existing profile. This action " +
                        "will link a profile to this device. If you want to add "
                        + userEmail + " alone, you have to unlink from the existing profile.";
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                builder.setTitle("Existing profile detected")
                        .setMessage(message)
                        .setPositiveButton("I acknowledge, proceed. ", (dialog, id) -> {
                            Log.d("signInToBackUpLauncher","Proceed pressed");
                            dialog.dismiss();
                            new Thread(() -> Profile.startSignInToProfileThread(resultJson,signInResult, lastButton)).start();

                        })

                        .setNegativeButton("Cancel.", (dialog, id) -> {
                            Log.d("signInToBackUpLauncher","Cancel pressed");
                            MainActivity.activity.runOnUiThread(() -> {
                                lastButton.setClickable(true);
                            });
                            UI.update("Cancel Link profile Dialog");
                            dialog.dismiss();
                        }).setCancelable(false);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
    }

    public static void showMoveDriveFilesDialog(String userEmail, Activity activity, View lastButton){
        try {
            double totalFreeSpace = Unlink.getTotalLinkedCloudsFreeSpace(userEmail);
            boolean isSingleAccountUnlink = Unlink.isSingleUnlink(userEmail);

            Log.d("Unlink", "Free storage of accounts except " + userEmail + " is " + totalFreeSpace);
            Log.d("Unlink", "Is single account unlink: " + isSingleAccountUnlink);

            double assetsSize = DBHelper.getSizeOfSyncedAssetsFromAccount(userEmail);
            Log.d("Unlink", "getAssetsSizeOfDriveAccountThread finished for " + userEmail + ":" + assetsSize);

            MainActivity.activity.runOnUiThread(() -> {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                    boolean isAbleToMoveAllAssets = handleUnlinkBuilderTitleAndMessage(builder,userEmail,
                            isSingleAccountUnlink,assetsSize, totalFreeSpace);

                    builder.setPositiveButton("I acknowledge, unlink anyway.", (dialog, id) -> {
                        Log.d("Unlink", "Proceed pressed");
                        dialog.dismiss();
                        if (isSingleAccountUnlink) {
                            new Thread(() -> {
                                Log.d("Unlink", "Just unlink from single account ");
                                String accessToken = DBHelper.getDriveBackupAccessToken(userEmail);
                                Drive service = GoogleDrive.initializeDrive(accessToken);
                                Log.d("Unlink", "Drive and access token : " + accessToken + service);
                                Unlink.unlinkSingleAccount(userEmail, service,false,true, lastButton);
                            }).start();
                        }else{
                            new Thread( () -> {
                                Unlink.unlinkAccount(userEmail,isAbleToMoveAllAssets, activity, lastButton);
                            }).start();
                        }
                    }).setNegativeButton("Cancel.", (dialog, id) -> {
                        MainActivity.activity.runOnUiThread(() -> {
                            lastButton.setClickable(true);
                        });
                        UI.update("cancel unlink Dialog");
                        Log.d("Unlink", "end of unlink: canceled");
                        dialog.dismiss();
                    });

                    builder.setCancelable(false);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e);
                }
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
                builder.setMessage("This action will unlink the backup storage currently used for syncing and backup." +
                        " If you want your files on devices to continue syncing, please first add another backup storage.");
            }
            else{
                if (totalFreeSpace < assetsSize) {
                    builder.setTitle("Not enough space");
                    builder.setMessage("This action will only move about " + AreaSquareChartForAccount.formatStorageSize(totalFreeSpace) + " out of " + AreaSquareChartForAccount.formatStorageSize(assetsSize) +
                            " of your libz files in " + userEmail + " to other available accounts.\n" +
                            "This process may take several minutes or hours depending on the size of your files.");

                } else {
                    builder.setTitle("Unlink Backup Account");
                    builder.setMessage("All of your libz files in " + userEmail + " will be moved to your other available accounts.\n" +
                            "This process may take several minutes or hours depending on the size of your files.");
                    isAbleToMoveAllAssets = true;
                }
            }
        }catch (Exception e) { LogHandler.crashLog(e,"unlink"); }

        return isAbleToMoveAllAssets;
    }


}
