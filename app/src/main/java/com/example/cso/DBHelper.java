package com.example.cso;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
    }
    public void insertTestData(String testData, String columnName) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try{
            String sqlQuery = "INSERT INTO Test ("+ columnName +") VALUES (?)";
            db.execSQL(sqlQuery, new Object[]{testData});

            db.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to save " + testData + " into the database.");
        }finally {
            db.endTransaction();
            db.close();
        }
    }

    public String getTestValues(String columnName){
        SQLiteDatabase db = getReadableDatabase();
        String result = "No values in the 'Test' table.";

        String sqlQuery = "SELECT test FROM " + columnName;
        Cursor cursor = db.rawQuery(sqlQuery, null);
        if(cursor.moveToFirst()){
            int columnIndex = cursor.getColumnIndex("test");
            result = "";
            if(columnIndex >= 0){
                do{
                    result += cursor.getString(columnIndex) + "\n";
                } while (cursor.moveToNext());
            }
        }
        cursor.close();
        db.close();

        return result;
    }
}
