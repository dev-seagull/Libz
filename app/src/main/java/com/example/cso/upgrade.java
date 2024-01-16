package com.example.cso;

import android.database.sqlite.SQLiteConstraintException;

public class upgrade {


//    public static void version_13(){
//        MainActivity.dbHelper.alter_table();
//        int version = 0;
//        if (version < 11){
//            version_12();
//        }
//        // do version 13 tasks
//    }
//
//    public static  void version_12(){
//        int version = 0;
//        if (version < 10){
//            version_11();
//        }
//        // do version 12 tasks
//    }

    public static void test(){
        String sqlQuery = "DELETE FROM USERPROFILE WHERE type = 'profile'";
        DBHelper.dbWritable.beginTransaction();
        try {
            DBHelper.dbWritable.execSQL(sqlQuery);
            DBHelper.dbWritable.setTransactionSuccessful();
        }catch (SQLiteConstraintException e) {
            LogHandler.saveLog("SQLiteConstraintException in insert profile method "+ e.getLocalizedMessage(),false);
        }finally {
            DBHelper.dbWritable.endTransaction();
        }
    }


}
