package com.example.cso;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

public class PermissionManager {
    private static final int WAIT_INTERVAL = 1000;
    public boolean requestStorageAccess(Activity activity) {
        Thread manageAccessThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    checkAndRequestStorageAccess(activity);
                    waitForStorageAccess();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void checkAndRequestStorageAccess(Activity activity) {
                try{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        requestStorageAccessPermission(activity);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            private void requestStorageAccessPermission(Activity activity) {
                try{
                    Intent getPermission = new Intent();
                    getPermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivity(getPermission);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            private void waitForStorageAccess(){
                try{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        while (!Environment.isExternalStorageManager()){
                            Thread.sleep(WAIT_INTERVAL);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        manageAccessThread.start();
        try {
            manageAccessThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean isAccessGiven = isAccessConfirmed();
        return isAccessGiven;
    }
    private boolean isAccessConfirmed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            System.out.println("Access granted. Starting to get access from your Android device.");
            return true;
        }
        return false;
    }

}
