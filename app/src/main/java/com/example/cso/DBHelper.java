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
        String USERPROFILE = "CREATE TABLE IF NOT EXISTS USERPROFILE("
                + "userEmail TEXT PRIMARY KEY," +
                "type TEXT CHECK (type IN ('primary','backup')), " +
                "refreshToken TEXT, " +
                "accessToken TEXT, " +
                "totalStorage REAL," +
                "usedStorage REAL," +
                "usedInDriveStorage REAL,"+
                "UsedInGmailAndPhotosStorage REAL)";
        sqLiteDatabase.execSQL(USERPROFILE);

        String DRIVE = "CREATE TABLE IF NOT EXISTS DRIVE("
                +"id INTERGER PRIMARY KEY AUTOINCREMENT,"+
                "fildId TEXT," +
                "fileName TEXT," +
                "userEmail TEXT REFERENCES USERPROFILE(userEmail) ON UPDATE CASCADE ON DELETE CASCADE, " +
                "fileHash TEXT, " +
                "source TEXT)";
        sqLiteDatabase.execSQL(DRIVE);

        String ANDROID = "CREATE TABLE IF NOT EXISTS ANDROID("
                +"id INTERGER PRIMARY KEY AUTOINCREMENT,"+
                "fileName TEXT," +
                "filePath TEXT," +
                "fileSize REAL," +
                "fileHash TEXT," +
                "dateAdded TEXT,"+
                "dateModified TEXT,"+
                "memeType TEXT)";
        sqLiteDatabase.execSQL(ANDROID);

        String ERRORS = "CREATE TABLE IF NOT EXISTS ERRORS(" +
                "descriptionError TEXT," +
                "error TEXT," +
                "date TEXT)";
        sqLiteDatabase.execSQL(ERRORS);

        String TRANSACTIONS = "CREATE TABLE IF NOT EXISTS TRANSACTIONS(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "source TEXT,"+
                "fileName TEXT," +
                "destination TEXT,"+
                "operation TEXT CHECK (operation IN ('duplicated','sync')),"+
                "hash TEXT,"+
                "date TEXT)";
        sqLiteDatabase.execSQL(TRANSACTIONS);
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
