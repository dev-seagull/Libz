package com.example.cso;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "CSODatabase";
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String userProfile = "CREATE TABLE IF NOT EXISTS UserProfile("
                + "AccountName TEXT)";
        sqLiteDatabase.execSQL(userProfile);

        String test = "CREATE TABLE IF NOT EXISTS Test(" +
                "test TEXT)";
        sqLiteDatabase.execSQL(test);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
//        public void insertTestData(String testData) {
//            SQLiteDatabase db = getWritableDatabase();
//            ContentValues values = new ContentValues();
//            values.put("test", testData);
//            db.insert("Test", null, values);
//            db.close();
//        }
    }
}
