package com.example.cso;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "CSODatabase";
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("PRAGMA journal_mode=DELETE;",null);
        if (cursor.moveToFirst()) {
            String result = cursor.getString(0);
            System.out.println("Result : : : : : " + result);
        }
        cursor.close();
        onCreate(db);
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
                "fileHash TEXT);";
        sqLiteDatabase.execSQL(ASSET);

        String DRIVE = "CREATE TABLE IF NOT EXISTS DRIVE("
                +"id PRIMARY KEY AUTOINCREMENT,"+
                "assetId INTEGER REFERENCES ASSET(id),"+
                "fildId TEXT," +
                "fileName TEXT," +
                "userEmail TEXT REFERENCES USERPROFILE(userEmail) ON UPDATE CASCADE ON DELETE CASCADE, " +
                "fileHash TEXT)";
        sqLiteDatabase.execSQL(DRIVE);

        String ANDROID = "CREATE TABLE IF NOT EXISTS ANDROID("
                +"id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "assetId INTEGER,"+
                "fileName TEXT," +
                "filePath TEXT," +
                "device TEXT," +
                "fileSize REAL," +
                "fileHash TEXT," +
                "dateModified TEXT,"+
                "memeType TEXT,"+
                "CONSTRAINT fk_assetId FOREIGN KEY (assetId) REFERENCES ASSET(id));";
        sqLiteDatabase.execSQL(ANDROID);

        String PHOTOS = "CREATE TABLE IF NOT EXISTS PHOTOS("
                +"id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "assetId INTEGER REFERENCES ASSET(id) ON UPDATE CASCADE ON DELETE CASCADE,"+
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
    public long insertAssetData(String fileName,String fileHash) {
        long lastInsertedId = -1;
        String sqlQuery;

        Boolean existsInAsset = false;
        SQLiteDatabase dbReadable = getReadableDatabase();
        try{
           sqlQuery = "SELECT EXISTS(SELECT 1 FROM ASSET WHERE fileHash = ?)";
           Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{fileHash});
           if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    existsInAsset = true;
                }
            }
           cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from ASSET in insertAssetData method: " + e.getLocalizedMessage());
        }

        if(existsInAsset == false){
            SQLiteDatabase dbWritable = getWritableDatabase();
            try{
                dbWritable.beginTransaction();
                sqlQuery = "INSERT INTO ASSET(fileName,fileHash) VALUES (?,?);";
                dbWritable.execSQL(sqlQuery, new Object[]{fileName,fileHash});
                dbWritable.setTransactionSuccessful();
            }catch (Exception e){
                LogHandler.saveLog("Failed to insert data into ASSET.");
            }finally {
                dbWritable.endTransaction();
                dbWritable.close();
            }
        }
        sqlQuery = "SELECT id FROM ASSET WHERE fileHash = ?";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{fileHash});
        if(cursor != null && cursor.moveToFirst()){
            lastInsertedId = cursor.getInt(0);
        }else{
            LogHandler.saveLog("Failed to find the existing file id in Asset database");
        }
        cursor.close();
        dbReadable.close();

        return lastInsertedId;
    }


    public void insertIntoDriveTable(Long assetId, String fileId,String fileName, String fileHash,String userEmail){
        String sqlQuery = "";
        Boolean existsInDrive = false;
        SQLiteDatabase dbReadable = getReadableDatabase();
        try{
            sqlQuery = "SELECT EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ? and fileHash = ? and fileId =?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId), fileHash, fileId});
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    existsInDrive = true;
                }
            }
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from DRIVE in insertIntoDriveTable method: " + e.getLocalizedMessage());
        }finally {
            dbReadable.close();
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        if(existsInDrive == false){
            try{

                sqlQuery = "INSERT INTO DRIVE (" +
                        "assetId," +
                        "fileId," +
                        "fileName, " +
                        "userEmail, " +
                        "fileHash) VALUES (?,?,?,?,?)";
                Object[] values = new Object[]{assetId,fileId,fileName,userEmail,fileHash};
                db.execSQL(sqlQuery, values);
                db.setTransactionSuccessful();
            }catch (Exception e){
                LogHandler.saveLog("Failed to save into the database in insertIntoDriveTable method. "+e.getLocalizedMessage());
            }finally {
                db.endTransaction();
                db.close();
            }
        }
    }


    public void insertTransactionsData(String source, String fileName, String destination
            , String operation, String fileHash) {
        SQLiteDatabase dbWritable = getWritableDatabase();
        dbWritable.beginTransaction();
       try{
            String sqlQuery = "INSERT INTO TRANSACTIONS(source, fileName, destination, operation, hash, date)" +
                    " VALUES (?,?,?,?,?,?);";
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            dbWritable.execSQL(sqlQuery, new Object[]{source,fileName, destination, operation, fileHash, timestamp});
            dbWritable.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to insert data into ASSET.");
        }finally {
            dbWritable.endTransaction();
            dbWritable.close();
        }
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




    public void insertIntoAndroidTable(long assetId,String fileName,String filePath,String device,
                                       String fileHash, Double fileSize,String dateModified,String memeType) {
        String sqlQuery = "";
        Boolean existsInAndroid = false;
        SQLiteDatabase dbReadable = getReadableDatabase();
        try{
            sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ? and fileHash = ? and fileSize =? and device =?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId), fileHash,
                    String.valueOf(fileSize), device});
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    existsInAndroid = true;
                }
            }
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from ANDROID in insertIntoAndroidTable method: " + e.getLocalizedMessage());
        }finally {
            dbReadable.close();
        }
        if(existsInAndroid == false){
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try{
                sqlQuery = "INSERT INTO ANDROID (" +
                        "assetId," +
                        "fileName," +
                        "filePath, " +
                        "device, " +
                        "fileSize, " +
                        "fileHash," +
                        "dateModified," +
                        "memeType) VALUES (?,?,?,?,?,?,?,?)";
                Object[] values = new Object[]{assetId,fileName,filePath,device,
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

    public void deleteFileFromDriveTable(String fileHash, String id, String assetId, String fileId, String userEmail){
        SQLiteDatabase dbWritable = getWritableDatabase();
        String sqlQuery  = "DELETE FROM DRIVE WHERE fileHash = ? and id = ? and assetId = ? and fileId = ? and userEmail = ?";
        dbWritable.execSQL(sqlQuery, new String[]{fileHash, id, assetId, fileId, userEmail});
        dbWritable.close();

        boolean existsInDatabase = false;
        SQLiteDatabase dbReadable = getReadableDatabase();
        try {
            sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ?) " +
                    "OR EXISTS(SELECT 1 FROM PHOTOS WHERE assetId = ?) " +
                    "OR EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{assetId});
            if (cursor != null && cursor.moveToFirst()) {
                int result = cursor.getInt(0);
                if (result == 1) {
                    existsInDatabase = true;
                }
            }
            cursor.close();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to check if the data exists in Database in deleteFileFromDriveTable");
        } finally {
            dbReadable.close();
        }

        if (existsInDatabase == false) {
            dbWritable = getWritableDatabase();
            dbWritable.beginTransaction();
            try {
                sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
                dbWritable.execSQL(sqlQuery, new Object[]{assetId});
                dbWritable.setTransactionSuccessful();
            } catch (Exception e) {
                LogHandler.saveLog("Failed to delete the database in ASSET , deleteFileFromDriveTable method. " + e.getLocalizedMessage());
            } finally {
                dbWritable.endTransaction();
                dbWritable.close();
            }
        }
    }

    public void deleteRedundantDrive(ArrayList<String> fileIds, String userEmail){
        SQLiteDatabase dbReadable = getReadableDatabase();

        String sqlQuery = "SELECT * FROM DRIVE where userEmail = ?";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
        if(cursor.moveToFirst()){
            do{
                int fileIdColumnIndex = cursor.getColumnIndex("fileId");
                if(fileIdColumnIndex >= 0) {
                    String fileId = cursor.getString(fileIdColumnIndex);
                    if (!fileIds.contains(fileId)) {
                        SQLiteDatabase dbWritable = getWritableDatabase();
                        dbWritable.beginTransaction();
                        try {
                            sqlQuery = "DELETE FROM DRIVE WHERE fileId = ?";
                            dbWritable.execSQL(sqlQuery, new Object[]{fileId});
                            dbWritable.setTransactionSuccessful();
                        } catch (Exception e) {
                            LogHandler.saveLog("Failed to delete the database in DRIVE, deleteRedundantDRIVE method. " + e.getLocalizedMessage());
                        } finally {
                            dbWritable.endTransaction();
                            dbWritable.close();
                        }

                        int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                        boolean existsInDatabase = false;
                        String assetId = "";
                        if (assetIdColumnIndex >= 0) {
                            dbReadable = getReadableDatabase();
                            assetId = cursor.getString(assetIdColumnIndex);
                            try {
                                sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ?) " +
                                        "OR EXISTS(SELECT 1 FROM PHOTOS WHERE assetId = ?) " +
                                        "OR EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ?)";
                                cursor = dbReadable.rawQuery(sqlQuery, new String[]{assetId});
                                if (cursor != null && cursor.moveToFirst()) {
                                    int result = cursor.getInt(0);
                                    if (result == 1) {
                                        existsInDatabase = true;
                                    }
                                }
                            } catch (Exception e) {
                                LogHandler.saveLog("Failed to check if the data exists in Database in insertIntoDriveTable");
                            } finally {
                                dbReadable.close();
                            }
                        }

                        if (existsInDatabase == false) {
                            dbWritable = getWritableDatabase();
                            dbWritable.beginTransaction();
                            try {
                                sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
                                dbWritable.execSQL(sqlQuery, new Object[]{assetId});
                                dbWritable.setTransactionSuccessful();
                            } catch (Exception e) {
                                LogHandler.saveLog("Failed to delete the database in ASSET , deleteRedundantDrive method. " + e.getLocalizedMessage());
                            } finally {
                                dbWritable.endTransaction();
                                dbWritable.close();
                            }
                        }
                    }
                }
            }while (cursor.moveToNext());
        }
        cursor.close();
        dbReadable.close();
    }


    public void deleteRedundantAndroid(){
        SQLiteDatabase dbReadable = getReadableDatabase();
        String sqlQuery = "SELECT * FROM ANDROID";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
        if(cursor.moveToFirst()){
            do{
                String filePath = "";
                String assetId = "";
                String device = "";

                int filePathColumnIndex = cursor.getColumnIndex("filePath");
                if(filePathColumnIndex >= 0){
                    filePath = cursor.getString(filePathColumnIndex);
                }

                int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                if(assetIdColumnIndex >= 0){
                    assetId = cursor.getString(assetIdColumnIndex);
                }

                int deviceColumnIndex = cursor.getColumnIndex("device");
                if(deviceColumnIndex >= 0){
                    device = cursor.getString(deviceColumnIndex);
                }

                File androidFile = new File(filePath);
                if (!androidFile.exists() && !device.equals(MainActivity.androidDeviceName)){
                    SQLiteDatabase dbWritable = getWritableDatabase();
                    dbWritable.beginTransaction();
                    try {
                        sqlQuery = "DELETE FROM ANDROID WHERE filePath = ? and assetId = ? ";
                        dbWritable.execSQL(sqlQuery, new Object[]{filePath, assetId});
                        dbWritable.setTransactionSuccessful();
                    } catch (Exception e) {
                        LogHandler.saveLog("Failed to delete the database in ANDROID , deleteRedundantAndroid method. " + e.getLocalizedMessage());
                    } finally {
                        dbWritable.endTransaction();
                        dbWritable.close();
                    }

                    dbReadable = getReadableDatabase();
                    boolean existsInDatabase = false;
                    try{
                        sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ?) " +
                                "OR EXISTS(SELECT 1 FROM PHOTOS WHERE assetId = ?) " +
                                "OR EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ?)";
                        cursor = dbReadable.rawQuery(sqlQuery, new String[]{assetId});
                        if(cursor != null && cursor.moveToFirst()){
                            int result = cursor.getInt(0);
                            if(result == 1){
                                existsInDatabase = true;
                            }
                        }
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to check if the data exists in Database in deleteRedundantAndroid");
                    }finally {
                        dbReadable.close();
                    }

                    if(existsInDatabase == false){
                        dbWritable = getWritableDatabase();
                        dbWritable.beginTransaction();
                        try {
                            sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
                            dbWritable.execSQL(sqlQuery, new Object[]{assetId});
                            dbWritable.setTransactionSuccessful();
                        } catch (Exception e) {
                            LogHandler.saveLog("Failed to delete the database in ASSET , deleteRedundantAndroid method. " + e.getLocalizedMessage());
                        } finally {
                            dbWritable.endTransaction();
                            dbWritable.close();
                        }
                    }
                }

            }while (cursor.moveToNext());
        }
        cursor.close();
        dbReadable.close();
    }

//    public void deleteRedundantDrive(list<String[]> driveFileRows,ArrayList<String> driveFileIds){
//        String filePath;
//        for (String driveFileId : driveFileIds){
//            for (String[] row : driveFileRows){
//                if (row[1].equals(driveFileId)){
//                    filePath = row[2];
//                    String id = row[0];
//                    File driveFile = new File(filePath);
//                    if (!driveFile.exists()){
//                        SQLiteDatabase db = getWritableDatabase();
//                        db.beginTransaction();
//                        try {
//                            String sqlQuery = "DELETE FROM DRIVE WHERE id = ? ";
//                            db.execSQL(sqlQuery, new Object[]{id});
//                            db.setTransactionSuccessful();
//                        } catch (Exception e) {
//                            LogHandler.saveLog("Failed to delete the database in DRIVE , deleteRedundantDrive method. " + e.getLocalizedMessage());
//                        } finally {
//                            db.endTransaction();
//                        }
//
//                        db.beginTransaction();
//                        try {
//                            String sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
//                            db.execSQL(sqlQuery, new Object[]{id});
//                            db.setTransactionSuccessful();
//                        } catch (Exception e) {
//                            LogHandler.saveLog("Failed to delete the database in ASSET , deleteRedundantDrive method. " + e.getLocalizedMessage());
//                        } finally {
//                            db.endTransaction();
//                            db.close();
//                        }
//                    }
//                }
//            }
//        }
//            filePath = androidRow[2];
//            String id = androidRow[0];
//            File androidFile = new File(filePath);
//            if (!androidFile.exists()){
//                SQLiteDatabase db = getWritableDatabase();
//                db.beginTransaction();
//                try {
//                    String sqlQuery = "DELETE FROM ANDROID WHERE id = ? ";
//                    db.execSQL(sqlQuery, new Object[]{id});
//                    db.setTransactionSuccessful();
//                } catch (Exception e) {
//                    LogHandler.saveLog("Failed to delete the database in ANDROID , deleteRedundantAndroid method. " + e.getLocalizedMessage());
//                } finally {
//                    db.endTransaction();
//                }
//
//                db.beginTransaction();
//                try {
//                    String sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
//                    db.execSQL(sqlQuery, new Object[]{id});
//                    db.setTransactionSuccessful();
//                } catch (Exception e) {
//                    LogHandler.saveLog("Failed to delete the database in ASSET , deleteRedundantAndroid method. " + e.getLocalizedMessage());
//                } finally {
//                    db.endTransaction();
//                    db.close();
//                }
//            }
//        }

    public List<String[]> getDriveTable(String[] columns){
        List<String[]> resultList = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }

        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM DRIVE" ;
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
}
