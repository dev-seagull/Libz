package com.example.cso;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import com.example.cso.UI.UI;

import kotlin.text._OneToManyTitlecaseMappingsKt;

public class Upgrade {

    public static void versionHandler(SharedPreferences preferences){
        try {
           int savedVersionCode = preferences.getInt("currentVersionCode", -1);
            PackageInfo pInfo = MainActivity.activity.getApplicationContext()
                    .getPackageManager().getPackageInfo(MainActivity.activity.getApplicationContext()
                            .getPackageName(), 0);
            int currentVersionCode = pInfo.versionCode;
           if (savedVersionCode == -1){
               DBHelper.deleteTableContent("ACCOUNTS");
           }
           else if (savedVersionCode <= currentVersionCode) {
               switch (savedVersionCode){
                   case 13:
                       upgrade_13_to_14();
                       break;
                   case 14:
                       upgrade_14_to_15();
                       break;
                   case 15:
                       upgrade_15_to_16();
                       break;
                   case 16:
                       upgrade_16_to_17();
                       break;
                   case 17:
                       upgrade_17_to_18();
                       break;
                   case 18:
                       upgrade_18_to_19();
                       break;
                   case 19 :
                       upgrade_19_to_20();
                       break;
                   case 20 :
                       upgrade_20_to_21();
                       break;
                   case 21:
                       upgrade_21_to_22();
                       break;
                   case 22:
                       upgrade_22_to_23();
                       break;
                   case 23:
                       upgrade_23_to_24();
                       break;
                   case 24 :
                   case 25 :
                   case 26 :
                   case 27 :
                   case 28 :
                   case 29 :
                   case 30 :
                   case 31 :
                   case 32 :
                   case 33 :
                       upgrade_33_to_34();
                       break;
                   case 34 :
                       upgrade_34_to_35();
                       break;
                   case 35 :
                       upgrade_35_to_36();
                       break;
                   case 49:
                        upgrade_49_to_50();
                        break;
                   default:
                       lastVersion();
                       break;
               }
           } else if (savedVersionCode > currentVersionCode) {
               UI .makeToast("Please install last version of App");
           }
           SharedPreferences.Editor editor = preferences.edit();
           editor.putInt("currentVersionCode", currentVersionCode);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                editor.apply();
            }
        }catch (Exception e){
           LogHandler.saveLog("Failed to upgrade: "+ e.getLocalizedMessage());
       }
    }


    public static void upgrade_17_to_18() {
        DBHelper.deleteTableContent("DEVICE");
        DBHelper.deleteTableContent("ACCOUNTS");
        upgrade_18_to_19();
    }


    public static void lastVersion() {
        SharedPreferencesHandler.setSwitchState("syncSwitchState",false,MainActivity.preferences);
    }

    public static void upgrade_49_to_50(){
        SharedPreferencesHandler.setSwitchState("syncSwitchState",false,MainActivity.preferences);
        DBHelper.deleteFromAndroidTable();
        UI.makeToast("Your Upgraded to last version, 49 to 50");
    }


    public static void upgrade_16_to_17() {
        DBHelper.deleteTableContent("DEVICE");
        upgrade_17_to_18();
    }

    public static void upgrade_15_to_16() {
        upgrade_16_to_17();
    }

    public static void upgrade_14_to_15() {
//        dropProfileIdColumn();
        DBHelper.deleteTableContent("PROFILE");
        upgrade_15_to_16();
    }

    public static void upgrade_13_to_14(){
//        cutFromUserProfileToAccounts();
        upgrade_14_to_15();
    }

    public static void upgrade_18_to_19(){
        DBHelper.removeColumn("folderId","ACCOUNTS");
        upgrade_19_to_20();
    }

    public static void upgrade_19_to_20(){
        DBHelper.removeColumn("profileId","ACCOUNTS");
        DBHelper.dropTable("PROFILE");
        upgrade_20_to_21();
    }

    public static void upgrade_20_to_21(){

//        oldDBHelper.copyDataFromOldToNew(MainActivity.dbHelper);
        upgrade_21_to_22();
    }

    public static void upgrade_21_to_22(){
        upgrade_22_to_23();
    }

    public static void upgrade_22_to_23(){
        DBHelper.dropTable("ERRORS");
        upgrade_23_to_24();
    }

    public static void upgrade_23_to_24(){
        DBHelper.alterAccountsTableConstraint();
    }

    public static void upgrade_33_to_34(){
        DBHelper.dropTable("DEVICE");
        DBHelper.dbWritable.beginTransaction();
        String DEVICE = "CREATE TABLE IF NOT EXISTS DEVICE("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "deviceName TEXT," +
                "deviceId TEXT UNIQUE)";
        DBHelper.dbWritable.execSQL(DEVICE);
        DBHelper.dbWritable.setTransactionSuccessful();
        DBHelper.dbWritable.endTransaction();
        upgrade_34_to_35();
    }

    public static void upgrade_34_to_35(){
        upgrade_35_to_36();
    }

    public static void upgrade_35_to_36(){
        System.out.println("adding columns to accounts");
        DBHelper.addColumn("parentFolderId","TEXT","ACCOUNTS");
        DBHelper.addColumn("assetsFolderId","TEXT","ACCOUNTS");
        DBHelper.addColumn("profileFolderId","TEXT","ACCOUNTS");
        DBHelper.addColumn("databaseFolderId","TEXT","ACCOUNTS");

//        new Thread(GoogleDrive::cleanDriveFolders).start();
    }



}



