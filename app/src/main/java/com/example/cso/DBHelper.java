package com.example.cso;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "CSODatabase";
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        onCreate(getWritableDatabase());
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

        String ASSET = "CREATE TABLE IF NOT EXISTS ASSET("
                +"id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "fileName TEXT," +
                "type TEXT CHECK (type IN ('PHOTOS','DRIVE','ANDROID')),"+
                "fileHash TEXT);";
        sqLiteDatabase.execSQL(ASSET);

        String DRIVE = "CREATE TABLE IF NOT EXISTS DRIVE("
                +"id INTEGER REFERENCES ASSET(id) ON UPDATE CASCADE ON DELETE CASCADE,"+
                "fildId TEXT," +
                "fileName TEXT," +
                "userEmail TEXT REFERENCES USERPROFILE(userEmail) ON UPDATE CASCADE ON DELETE CASCADE, " +
                "fileHash TEXT, " +
                "source TEXT)";
        sqLiteDatabase.execSQL(DRIVE);

        String ANDROID = "CREATE TABLE IF NOT EXISTS ANDROID("
                +"id INTEGER REFERENCES ASSET(id) ON UPDATE CASCADE ON DELETE CASCADE,"+
                "fileName TEXT," +
                "filePath TEXT," +
                "device TEXT," +
                "fileSize REAL," +
                "fileHash TEXT," +
                "dateModified TEXT,"+
                "memeType TEXT)";
        sqLiteDatabase.execSQL(ANDROID);

        String PHOTOS = "CREATE TABLE IF NOT EXISTS PHOTOS("
                +"id INTEGER REFERENCES ASSET(id) ON UPDATE CASCADE ON DELETE CASCADE,"+
                "fileId TEXT," +
                "fileName TEXT," +
                "userEmail TEXT REFERENCES USERPROFILE(userEmail) ON UPDATE CASCADE ON DELETE CASCADE,"+
                "creationTime TEXT," +
                "fileHash TEXT," +
                "baseUrl TEXT)";
        sqLiteDatabase.execSQL(PHOTOS);

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
                "operation TEXT CHECK (operation IN ('duplicated','sync','download')),"+
                "hash TEXT,"+
                "date TEXT)";
        sqLiteDatabase.execSQL(TRANSACTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }
    public long insertAssetData(String fileName, String type, String hash) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        long lastInsertedId = -1;

        try{
            String sqlQuery = "INSERT INTO ASSET(fileName, type, fileHash) VALUES (?,?,?);";
            db.execSQL(sqlQuery, new Object[]{fileName, type, hash});

            db.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to insert data into ASSET.");
        }finally {
            db.endTransaction();
            db.close();
        }
        if (getWritableDatabase().isDbLockedByCurrentThread()){
            db.
        }
        db = getWritableDatabase();
        db.beginTransaction();

        try{
            String sqlQuery = "SELECT LAST_INSERT_ROWID() AS last_id;";
            Cursor cursor = db.rawQuery(sqlQuery, null);

            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("last_id");
                if (columnIndex != -1) {
                    lastInsertedId = cursor.getLong(columnIndex);
                } else {
                    LogHandler.saveLog("Getting column index -1 inside insertAssetData method");
                }
            }

            cursor.close();
            db.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to get last inserted id inside ASSET");
        }finally {
            db.endTransaction();
            db.close();
        }
        return lastInsertedId;
    }

    public void insertUserProfileData(String userEmail,String type,String refreshToken ,String accessToken,
                            Double totalStorage , Double usedStorage , Double usedInDriveStorage , Double UsedInGmailAndPhotosStorage) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try{
            String sqlQuery = "INSERT INTO USERPROFILE (" +
                    "userEmail," +
                    "type, " +
                    "refreshToken, " +
                    "accessToken, " +
                    "totalStorage," +
                    "usedStorage," +
                    "usedInDriveStorage,"+
                    "UsedInGmailAndPhotosStorage) VALUES (?,?,?,?,?,?,?,?)";
            Object[] values = new Object[]{userEmail,type,refreshToken ,accessToken,
                    totalStorage ,usedStorage ,usedInDriveStorage ,UsedInGmailAndPhotosStorage};
            db.execSQL(sqlQuery, values);
            db.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to save into the database.in insertUserProfileData method. "+e.getLocalizedMessage());
        }finally {
            db.endTransaction();
            db.close();
        }
    }


    public List<String []> getUserProfile(String[] columns){
        List<String[]> resultList = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        String result = "No values in the 'UserProfile' table.";

        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }
        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM USERPROFILE" ;
        Cursor cursor = db.rawQuery(sqlQuery, null);
        if (cursor.moveToFirst()) {
            do {
                String[] row = new String[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    int columnIndex = cursor.getColumnIndex(columns[i]);
                    if (columnIndex >= 0) {
                        row[i] = cursor.getString(columnIndex);
                    }
                }
                resultList.add(row);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return resultList;
    }


    public void updateUserProfileData(String userEmail, Map<String, Object> updateValues) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            StringBuilder sqlQueryBuilder = new StringBuilder("UPDATE USERPROFILE SET ");

            List<Object> valuesList = new ArrayList<>();
            for (Map.Entry<String, Object> entry : updateValues.entrySet()) {
                String columnName = entry.getKey();
                Object columnValue = entry.getValue();

                sqlQueryBuilder.append(columnName).append(" = ?, ");
                valuesList.add(columnValue);
            }

            sqlQueryBuilder.delete(sqlQueryBuilder.length() - 2, sqlQueryBuilder.length());
            sqlQueryBuilder.append(" WHERE userEmail = ?");
            valuesList.add(userEmail);

            String sqlQuery = sqlQueryBuilder.toString();
            Object[] values = valuesList.toArray(new Object[0]);
            db.execSQL(sqlQuery, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to update the database in updateUserProfileData method. " + e.getLocalizedMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }



    public void deleteUserProfileData(String userEmail) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String sqlQuery = "DELETE FROM USERPROFILE WHERE userEmail = ?";
            db.execSQL(sqlQuery, new Object[]{userEmail});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete the database in deleteUserProfileData method. " + e.getLocalizedMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }


    public List<String []> getAndroidTable(String[] columns){
        List<String[]> resultList = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        String result = "No values in the 'ANDROID' table.";

        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }
        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM ANDROID" ;
        Cursor cursor = db.rawQuery(sqlQuery, null);
        if (cursor.moveToFirst()) {
            do {
                String[] row = new String[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    int columnIndex = cursor.getColumnIndex(columns[i]);
                    if (columnIndex >= 0) {
                        row[i] = cursor.getString(columnIndex);
                    }
                }
                resultList.add(row);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return resultList;
    }

    public void insertIntoAndroidTable(long id,String fileName,String filePath,String device,
                                       Double fileSize,String fileHash,String dateModified,String memeType) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        String sqlQuery_2 = "SELECT * FROM ANDROID WHERE filePath = ? and fileSize = ? and dateModified = ?";
        Cursor cursor = db.rawQuery(sqlQuery_2, new String[]{filePath,String.valueOf(fileSize),dateModified});
        if (!cursor.moveToFirst()) {
            try{
                String sqlQuery = "INSERT INTO ANDROID (" +
                        "id," +
                        "fileName," +
                        "filePath, " +
                        "device, " +
                        "fileSize, " +
                        "fileHash," +
                        "dateModified," +
                        "memeType) VALUES (?,?,?,?,?,?,?,?)";
                Object[] values = new Object[]{id,fileName,filePath,device,
                        fileSize,fileHash,dateModified,memeType};
                db.execSQL(sqlQuery, values);
                db.setTransactionSuccessful();
            }catch (Exception e){
                LogHandler.saveLog("Failed to save into the database.in insertIntoAndroidTable method. "+e.getLocalizedMessage());
            }finally {
                db.endTransaction();
                db.close();
            }
        }
    }


    public void updateAndroid(String id, Map<String, Object> updateValues) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            StringBuilder sqlQueryBuilder = new StringBuilder("UPDATE ANDROID SET ");

            List<Object> valuesList = new ArrayList<>();
            for (Map.Entry<String, Object> entry : updateValues.entrySet()) {
                String columnName = entry.getKey();
                Object columnValue = entry.getValue();

                sqlQueryBuilder.append(columnName).append(" = ?, ");
                valuesList.add(columnValue);
            }

            sqlQueryBuilder.delete(sqlQueryBuilder.length() - 2, sqlQueryBuilder.length());
            sqlQueryBuilder.append(" WHERE id = ?");
            valuesList.add(id);

            String sqlQuery = sqlQueryBuilder.toString();
            Object[] values = valuesList.toArray(new Object[0]);
            db.execSQL(sqlQuery, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to update the database in updateAndroid method. " + e.getLocalizedMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void deleteRedundantAndroid(List<String[]> androidRows){
        String filePath;
        for (String[] androidRow : androidRows){
            filePath = androidRow[2];
            String id = androidRow[0];
            System.out.println("filePath :" + filePath);
            File androidFile = new File(filePath);
            if (!androidFile.exists()){
                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();
                try {
                    String sqlQuery = "DELETE FROM ANDROID WHERE id = ? ";
                    db.execSQL(sqlQuery, new Object[]{id});
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    LogHandler.saveLog("Failed to delete the database in deleteRedundantAndroid method. " + e.getLocalizedMessage());
                } finally {
                    db.endTransaction();
                    db.close();
                }
            }
        }
    }
}
