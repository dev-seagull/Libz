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

        if(!existsInAsset){
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
//                dbWritable.close();
            }
        }
        sqlQuery = "SELECT id FROM ASSET WHERE fileHash = ?";
//        dbReadable = getReadableDatabase();
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{fileHash});
        if(cursor != null && cursor.moveToFirst()){
            lastInsertedId = cursor.getInt(0);
        }else{
            LogHandler.saveLog("Failed to find the existing file id in Asset database");
        }
        cursor.close();
//        dbReadable.close();

        return lastInsertedId;
    }

    public void insertIntoDRIVETable(){

    }

//
//    "assetId INTEGER REFERENCES ASSET(id) ON UPDATE CASCADE ON DELETE CASCADE,"+
//            "fileId TEXT," +
//            "fileName TEXT," +
//            "userEmail TEXT REFERENCES USERPROFILE(userEmail) ON UPDATE CASCADE ON DELETE CASCADE,"+
//            "creationTime TEXT," +
//            "fileHash TEXT," +
//            "baseUrl TEXT)";
//    public void insertIntoPHOTOSTable(long assetId,String fileId,String fileName,String userEmail,
//                                       String creationTime, Double fileHash,String baseUrl) {
//
//        SQLiteDatabase db = getWritableDatabase();
//        db.beginTransaction();
//        String sqlQuery_2 = "SELECT * FROM ANDROID WHERE filePath = ? and fileSize = ? and dateModified = ?";
//        Cursor cursor = db.rawQuery(sqlQuery_2, new String[]{filePath,String.valueOf(fileSize),dateModified});
//        if (!cursor.moveToFirst()) {
//            try{
//                String sqlQuery = "INSERT INTO ANDROID (" +
//                        "id," +
//                        "fileName," +
//                        "filePath, " +
//                        "device, " +
//                        "fileSize, " +
//                        "fileHash," +
//                        "dateModified," +
//                        "memeType) VALUES (?,?,?,?,?,?,?,?)";
//                Object[] values = new Object[]{id,fileName,filePath,device,
//                        fileSize,fileHash,dateModified,memeType};
//                db.execSQL(sqlQuery, values);
//                db.setTransactionSuccessful();
//            }catch (Exception e){
//                LogHandler.saveLog("Failed to save into the database.in insertIntoAndroidTable method. "+e.getLocalizedMessage());
//            }finally {
//                db.endTransaction();
//            }
//        }else{
//            String sqlQuery = "DELETE FROM ASSET WHERE id = ?";
//            db.execSQL(sqlQuery, new Object[]{id});
//        }
//        db.close();
//    }

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
            sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ? and fileHash = ? and fileSize =? and filePath = ?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId), fileHash, String.valueOf(fileSize), filePath});
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
        SQLiteDatabase db = null;
        if(!existsInAndroid){
            try{
                db = getWritableDatabase();
                db.beginTransaction();
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


    public void deleteRedundantAndroid(){
        System.out.println("i'm here in delete redundant");
        SQLiteDatabase dbReadable = getReadableDatabase();
        ArrayList<String> filePaths = new ArrayList<>();
        ArrayList<String> assetIds = new ArrayList<>();

        String sqlQuery = "SELECT * FROM ANDROID";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
        if(cursor.moveToFirst()){
            do{
                int filePathColumnIndex = cursor.getColumnIndex("filePath");
                if(filePathColumnIndex >= 0){
                    String filePath = cursor.getString(filePathColumnIndex);
                    filePaths.add(filePath);
                }

                int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                if(filePathColumnIndex >= 0){
                    String assetId = cursor.getString(assetIdColumnIndex);
                    assetIds.add(assetId);
                }
            }while (cursor.moveToNext());
        }
        cursor.close();
        dbReadable.close();
        if(filePaths.size() != assetIds.size()){
            LogHandler.saveLog("Failed to get same file size of asset Ids and paths in insertIntoAndroidTable");
        }
        int i = 0;
        for (String filePath : filePaths){
            SQLiteDatabase dbWritable = getWritableDatabase();
            String assetId = assetIds.get(i);
            File androidFile = new File(filePath);
            if (!androidFile.exists()){
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
                System.out.println("check for bug 8 ");
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
                    System.out.println("check for bug 9 ");
                }catch (Exception e){
                    LogHandler.saveLog("Failed to check if the data exists in Database in insertIntoAndroidTable");
                }finally {
                    dbReadable.close();
                }
                System.out.println("check for bug 10 ");
                dbWritable = getWritableDatabase();
                System.out.println("check for bug 11 ");
                if(!existsInDatabase){
                    dbWritable.beginTransaction();
                    System.out.println("check for bug 12 ");
                    try {
                        sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
                        dbWritable.execSQL(sqlQuery, new Object[]{assetId});
                        dbWritable.setTransactionSuccessful();
                        System.out.println("check for bug 13 ");
                    } catch (Exception e) {
                        LogHandler.saveLog("Failed to delete the database in ASSET , deleteRedundantAndroid method. " + e.getLocalizedMessage());
                    } finally {
                        dbWritable.endTransaction();
                        dbWritable.close();
                        System.out.println("check for bug 14 ");
                    }
                }
            }
            i++;
        }
        cursor.close();
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


    public void insertIntoDriveTable(long id,String fileId,String fileName,String userEmail,
                                     String fileHash,String source) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try{
            String sqlQuery = "INSERT INTO DRIVE (" +
                    "id," +
                    "fileId," +
                    "fileName, " +
                    "userEmail, " +
                    "fileHash," +
                    "source) VALUES (?,?,?,?,?,?)";
            Object[] values = new Object[]{id,fileId,fileName,userEmail,fileHash,source};
            db.execSQL(sqlQuery, values);
            db.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to save into the database.in insertIntoDriveTable method. "+e.getLocalizedMessage());
        }finally {
            db.endTransaction();
            db.close();
        }
    }


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

    public boolean existsInDriveTable(String fileId){
        SQLiteDatabase db = getReadableDatabase();
        String sqlQuery = "SELECT * FROM DRIVE WHERE fileId = ?";
        Cursor cursor = db.rawQuery(sqlQuery, new String[]{fileId});
        if (cursor.moveToFirst()) {
            cursor.close();
            db.close();
            return true;
        }
        cursor.close();
        db.close();
        return false;
    }

}
