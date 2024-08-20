package com.example.cso;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionManager {
    private static int WAIT_INTERVAL = 1000;
    public static int READ_WRITE_PERMISSION_REQUEST_CODE = 1;
    public interface PermissionResultCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }
    public void requestPermissions(Activity activity){
        Thread s = new Thread( ()-> {
            requestStorageAccess(activity, new PermissionManager.PermissionResultCallback() {
                @Override
                public void onPermissionGranted() {
                    MainActivity.isStoragePermissionGranted = true;
                    requestReadAndWritePermissions(activity, new PermissionResultCallback() {
                        @Override
                        public void onPermissionGranted() {
                            MainActivity.isReadAndWritePermissionGranted = true;
                        }

                        @Override
                        public void onPermissionDenied() {
                            MainActivity.isReadAndWritePermissionGranted = false;
                        }
                    });
                }

                @Override
                public void onPermissionDenied() {
                    showPermissionDeniedMessage();
                }
            });
        });
        s.start();
        try{
            s.join();
        }catch (Exception e){ }
    }

    private void requestReadAndWritePermissions(Activity activity, PermissionResultCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };

            boolean isReadPermissionGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean isWritePermissionGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            if (isReadPermissionGranted && isWritePermissionGranted) {
                callback.onPermissionGranted();
            } else {
                ActivityCompat.requestPermissions(activity, permissions, READ_WRITE_PERMISSION_REQUEST_CODE);
                new Thread(() -> {
                    boolean granted = waitForReadAndWriteAccess(activity,callback);
                    if (granted) {
                        activity.runOnUiThread(callback::onPermissionGranted);
                    } else {
                        activity.runOnUiThread(callback::onPermissionDenied);
                    }
                }).start();
            }
        }else{
            callback.onPermissionGranted();
        }
    }

    public void requestStorageAccess(Activity activity, PermissionResultCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showPrePermissionDialog(activity,callback);
        }else{
            callback.onPermissionGranted();
        }
    }

    private void requestStorageAccessPermission(Activity activity) {
        try {
            Intent getPermission = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            activity.startActivity(getPermission);
        } catch (Exception e) {}
    }

    private boolean waitForStorageAccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                while (!Environment.isExternalStorageManager()) {
                    Thread.sleep(WAIT_INTERVAL);
                }
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean waitForReadAndWriteAccess(Activity activity,PermissionResultCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean isReadPermissionGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                boolean isWritePermissionGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

                while (!(isReadPermissionGranted && isWritePermissionGranted)) {
                    Thread.sleep(WAIT_INTERVAL);
                }
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void showPrePermissionDialog(Activity activity,PermissionResultCallback callback) {
        activity.runOnUiThread(() -> {
            new AlertDialog.Builder(activity)
                .setTitle("Storage Access Required")
                .setMessage("To fully operate, this app needs access to all files on your device. This includes the following permissions:\n\n" +
                        "1. Read External Storage: To read files from your device's storage.\n" +
                        "2. Write External Storage: To write files to your device's storage.\n\n" +
                        "Please allow 'All files access' on the next screen by selecting our app.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    requestStorageAccessPermission(activity);
                    new Thread(() -> {
                        boolean granted = waitForStorageAccess();
                        if (granted) {
                            activity.runOnUiThread(callback::onPermissionGranted);
                        } else {
                            activity.runOnUiThread(callback::onPermissionDenied);
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    callback.onPermissionDenied();
                })
                .show();
        });
    }


    public void showPermissionDeniedMessage() {
        try{
            MainActivity.activity.runOnUiThread(() -> {
                new AlertDialog.Builder(MainActivity.activity)
                        .setTitle("Permission Denied")
                        .setCancelable(false)
                        .setMessage("Reopen the app and grant the required permissions.")
                        .setPositiveButton("OK", (dialog, which) -> MainActivity.activity.finish())
                        .show();
            });
        }catch (Exception e){

        }
    }
}
