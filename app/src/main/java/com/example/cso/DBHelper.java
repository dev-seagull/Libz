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
    public SQLiteDatabase dbReadable;
    public SQLiteDatabase dbWritable;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        dbReadable = getReadableDatabase();
        dbWritable = getWritableDatabase();
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

        String DEVICE = "CREATE TABLE IF NOT EXISTS DEVICE("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "deviceName TEXT," +
                "totalStorage TEXT," +
                "freeStorage TEXT)";
        sqLiteDatabase.execSQL(DEVICE);

        String ASSET = "CREATE TABLE IF NOT EXISTS ASSET("
                +"id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "fileHash TEXT);";
        sqLiteDatabase.execSQL(ASSET);

        String DRIVE = "CREATE TABLE IF NOT EXISTS DRIVE("
                +"id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "assetId INTEGER REFERENCES ASSET(id),"+
                "fileId TEXT," +
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
                "assetId TEXT,"+
                "operation TEXT CHECK (operation IN ('duplicated','sync','download','deletedInDevice')),"+
                "hash TEXT,"+
                "date TEXT)";
        sqLiteDatabase.execSQL(TRANSACTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }
    public long insertAssetData(String fileHash) {
        long lastInsertedId = -1;
        String sqlQuery;

        Boolean existsInAsset = false;
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
            try{
                dbWritable.beginTransaction();
                sqlQuery = "INSERT INTO ASSET(fileHash) VALUES (?);";
                dbWritable.execSQL(sqlQuery, new Object[]{fileHash});
                dbWritable.setTransactionSuccessful();
            }catch (Exception e){
                LogHandler.saveLog("Failed to insert data into ASSET.");
            }finally {
                dbWritable.endTransaction();
            }
        }

        sqlQuery = "SELECT id FROM ASSET WHERE fileHash = ?;";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{fileHash});
        if(cursor != null && cursor.moveToFirst()){
            lastInsertedId = cursor.getInt(0);
        }else{
            LogHandler.saveLog("Failed to find the existing file id in Asset database");
        }
        cursor.close();
        return lastInsertedId;
    }


    public void insertIntoDriveTable(Long assetId, String fileId,String fileName, String fileHash,String userEmail){
        String sqlQuery = "";
        Boolean existsInDrive = false;
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
        }

        if(existsInDrive == false){
            dbWritable.beginTransaction();
            try{
                sqlQuery = "INSERT INTO DRIVE (" +
                        "assetId," +
                        "fileId," +
                        "fileName, " +
                        "userEmail, " +
                        "fileHash) VALUES (?,?,?,?,?)";
                Object[] values = new Object[]{assetId,fileId,fileName,userEmail,fileHash};
                dbWritable.execSQL(sqlQuery, values);
                dbWritable.setTransactionSuccessful();
            }catch (Exception e){
                LogHandler.saveLog("Failed to save into the database in insertIntoDriveTable method. "+e.getLocalizedMessage());
            }finally {
                dbWritable.endTransaction();
            }
        }
    }


    public void insertTransactionsData(String source, String fileName, String destination
            ,String assetId, String operation, String fileHash) {
        dbWritable.beginTransaction();
       try{
            String sqlQuery = "INSERT INTO TRANSACTIONS(source, fileName, destination, assetId, operation, hash, date)" +
                    " VALUES (?,?,?,?,?,?,?);";
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            dbWritable.execSQL(sqlQuery, new Object[]{source,fileName, destination, assetId, operation, fileHash, timestamp});
            dbWritable.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to insert data into ASSET.");
        }finally {
            dbWritable.endTransaction();
        }
    }

    public void insertUserProfileData(String userEmail,String type,String refreshToken ,String accessToken,
                            Double totalStorage , Double usedStorage , Double usedInDriveStorage , Double UsedInGmailAndPhotosStorage) {
        dbWritable.beginTransaction();
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
            dbWritable.execSQL(sqlQuery, values);
            dbWritable.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to save into the database.in insertUserProfileData method. "+e.getLocalizedMessage());
        }finally {
            dbWritable.endTransaction();
        }
    }


    public List<String []> getUserProfile(String[] columns){
        List<String[]> resultList = new ArrayList<>();

        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }
        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM USERPROFILE" ;
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
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
        return resultList;
    }


    public void updateUserProfileData(String userEmail, Map<String, Object> updateValues) {
        dbWritable.beginTransaction();
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
            dbWritable.execSQL(sqlQuery, values);
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to update the database in updateUserProfileData method. " + e.getLocalizedMessage());
        } finally {
            dbWritable.endTransaction();
        }
    }



    public void deleteUserProfileData(String userEmail) {
        dbWritable.beginTransaction();
        try {
            String sqlQuery = "DELETE FROM USERPROFILE WHERE userEmail = ?";
            dbWritable.execSQL(sqlQuery, new Object[]{userEmail});
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete the database in deleteUserProfileData method. " + e.getLocalizedMessage());
        } finally {
            dbWritable.endTransaction();
        }
    }


    public List<String []> getAndroidTable(String[] columns){
        List<String[]> resultList = new ArrayList<>();

        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }
        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM ANDROID" ;
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
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
        return resultList;
    }




    public void insertIntoAndroidTable(long assetId,String fileName,String filePath,String device,
                                       String fileHash, Double fileSize,String dateModified,String memeType) {
        fileHash = fileHash.toLowerCase();
        String sqlQuery = "";
        Boolean existsInAndroid = false;
        System.out.println("assetId : " + assetId + " fileName : " + fileName + " filePath : " + filePath + " device : " + device + " fileHash : " + fileHash + " fileSize : " + fileSize + " dateModified : " + dateModified + " memeType : " + memeType);
        try{
            sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ? and fileHash = ? and fileSize = ? and device = ?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId), fileHash,
                    String.valueOf(fileSize), device});
            System.out.println("Result in inserting into android table method kkk: " + cursor.getCount() +" "+ fileName + " "+ assetId);
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                System.out.println("Result in inserting into android table method : " + result +" "+ fileName + " "+ assetId);
                if(result == 1){
                    existsInAndroid = true;
                }
            }
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from ANDROID in insertIntoAndroidTable method: " + e.getLocalizedMessage());
        }

        if(existsInAndroid == false){
            System.out.println("try to insert into android table method : " + fileName + " "+ assetId);
            dbWritable.beginTransaction();
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
                dbWritable.execSQL(sqlQuery, values);
                dbWritable.setTransactionSuccessful();
                System.out.println("Inserted into android table method : " + fileName + " "+ assetId);
            }catch (Exception e){
                LogHandler.saveLog("Failed to save into the database.in insertIntoAndroidTable method. "+e.getLocalizedMessage());
            }finally {
                dbWritable.endTransaction();
            }
        }
    }

    public void deleteFileFromDriveTable(String fileHash, String id, String assetId, String fileId, String userEmail){
        String sqlQuery  = "DELETE FROM DRIVE WHERE fileHash = ? and id = ? and assetId = ? and fileId = ? and userEmail = ?";
        dbWritable.execSQL(sqlQuery, new String[]{fileHash, id, assetId, fileId, userEmail});

        boolean existsInDatabase = false;
        try {
            sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ?) " +
                    "OR EXISTS(SELECT 1 FROM PHOTOS WHERE assetId = ?) " +
                    "OR EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{assetId,assetId,assetId});
            if (cursor != null && cursor.moveToFirst()) {
                int result = cursor.getInt(0);
                if (result == 1) {
                    existsInDatabase = true;
                }
            }
            cursor.close();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to check if the data exists in Database in deleteFileFromDriveTable");
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
            }
        }
    }

    public void deleteRedundantDrive(ArrayList<String> fileIds, String userEmail){
        String sqlQuery = "SELECT * FROM DRIVE where userEmail = ?";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{userEmail});
        if(cursor.moveToFirst()){
            do{
                int fileIdColumnIndex = cursor.getColumnIndex("fileId");
                if(fileIdColumnIndex >= 0) {
                    String fileId = cursor.getString(fileIdColumnIndex);
                    if (!fileIds.contains(fileId)) {
                        dbWritable.beginTransaction();
                        try {
                            sqlQuery = "DELETE FROM DRIVE WHERE fileId = ?";
                            dbWritable.execSQL(sqlQuery, new Object[]{fileId});
                            dbWritable.setTransactionSuccessful();
                        } catch (Exception e) {
                            LogHandler.saveLog("Failed to delete the database in DRIVE, deleteRedundantDRIVE method. " + e.getLocalizedMessage());
                        } finally {
                            dbWritable.endTransaction();
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
                                Cursor cursor2 = dbReadable.rawQuery(sqlQuery, new String[]{assetId, assetId, assetId});
                                if (cursor2 != null && cursor2.moveToFirst()) {
                                    int result = cursor2.getInt(0);
                                    if (result == 1) {
                                        existsInDatabase = true;
                                    }
                                }
                            } catch (Exception e) {
                                LogHandler.saveLog("Failed to check if the data exists in Database in deleteRedundantDrive");
                            }
                        }

                        if (existsInDatabase == false) {
                            dbWritable.beginTransaction();
                            try {
                                sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
                                dbWritable.execSQL(sqlQuery, new Object[]{assetId});
                                dbWritable.setTransactionSuccessful();
                            } catch (Exception e) {
                                LogHandler.saveLog("Failed to delete the database in ASSET , deleteRedundantDrive method. " + e.getLocalizedMessage());
                            } finally {
                                dbWritable.endTransaction();
                            }
                        }
                    }
                }
            }while (cursor.moveToNext());
        }
        cursor.close();
    }


    public void deleteRedundantAndroid(){
        String sqlQuery = "SELECT * FROM ANDROID";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
        if(cursor.moveToFirst()){
            do{
                String filePath = "";
                String assetId = "";
                String device = "";
                String fileHash = "";
                String fileName = "";

                int filePathColumnIndex = cursor.getColumnIndex("filePath");
                if(filePathColumnIndex >= 0){
                    filePath = cursor.getString(filePathColumnIndex);
                }

                int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                if(assetIdColumnIndex >= 0){
                    assetId = cursor.getString(assetIdColumnIndex);
                }

                int fileNameColumnIndex = cursor.getColumnIndex("fileName");
                if(fileNameColumnIndex >= 0){
                    fileName = cursor.getString(fileNameColumnIndex);
                }

                int deviceColumnIndex = cursor.getColumnIndex("device");
                if(deviceColumnIndex >= 0){
                    device = cursor.getString(deviceColumnIndex);
                }

                int fileHashColumnIndex = cursor.getColumnIndex("fileHash");
                if(fileHashColumnIndex >= 0){
                    fileHash = cursor.getString(fileHashColumnIndex);
                }
                System.out.println("filePath : " + filePath + " assetId : " + assetId + " device : " + device + " fileHash : " + fileHash + " fileName : " + fileName);
                File androidFile = new File(filePath);
                if (!androidFile.exists() && device.equals(MainActivity.androidDeviceName)){
                    dbWritable.beginTransaction();
                    try {
                        sqlQuery = "DELETE FROM ANDROID WHERE filePath = ? and assetId = ? ";
                        dbWritable.execSQL(sqlQuery, new Object[]{filePath, assetId});
                        dbWritable.setTransactionSuccessful();
                    } catch (Exception e) {
                        LogHandler.saveLog("Failed to delete the database in ANDROID , deleteRedundantAndroid method. " + e.getLocalizedMessage());
                    } finally {
                        dbWritable.endTransaction();
                    }

                    dbWritable.beginTransaction();
                    try {
                        sqlQuery = "INSERT INTO TRANSACTIONS(source, fileName, destination, " +
                                "assetId, operation, hash, date) values(?,?,?,?,?,?,?)";
                        System.out.println("adding deleted in device to transactions table:  " + filePath);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String timestamp = dateFormat.format(new Date());
                        dbWritable.execSQL(sqlQuery, new Object[]{filePath, fileName, device,
                                assetId, "deletedInDevice", fileHash, timestamp});
                        dbWritable.setTransactionSuccessful();
                    } catch (Exception e) {
                        LogHandler.saveLog("Failed to add deleted file in device to TRANSACTIONS , deleteRedundantAndroid method. " + e.getLocalizedMessage());
                    } finally {
                        dbWritable.endTransaction();
                    }

                    boolean existsInDatabase = false;
                    try{
                        sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ?) " +
                                "OR EXISTS(SELECT 1 FROM PHOTOS WHERE assetId = ?) " +
                                "OR EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ?)";
                        Cursor cursor2 = dbReadable.rawQuery(sqlQuery, new String[]{assetId,assetId,assetId});
                        if(cursor2 != null && cursor2.moveToFirst()){
                            int result = cursor2.getInt(0);
                            if(result == 1){
                                existsInDatabase = true;
                            }
                        }
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to check if the data exists in Database in deleteRedundantAndroid");
                    }

                    if(existsInDatabase == false){
                        dbWritable.beginTransaction();
                        try {
                            sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
                            dbWritable.execSQL(sqlQuery, new Object[]{assetId});
                            dbWritable.setTransactionSuccessful();
                        } catch (Exception e) {
                            LogHandler.saveLog("Failed to delete the database in ASSET , deleteRedundantAndroid method. " + e.getLocalizedMessage());
                        } finally {
                            dbWritable.endTransaction();
                        }
                    }
                }

            }while (cursor.moveToNext());
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

    public List<String[]> getDriveTable(String[] columns, String userEmail){
        List<String[]> resultList = new ArrayList<>();

        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }

        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM DRIVE WHERE userEmail = ?" ;
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{userEmail});
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
        return resultList;
    }


    public int countAndroidAssets(){
        String sqlQuery = "SELECT COUNT(*) FROM ANDROID";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
        int count = 0;
        if(cursor != null && cursor.moveToFirst()){
            count = cursor.getInt(0);
        }
        if(count == 0){
            LogHandler.saveLog("No android file was found in count android assets.");
        }
        cursor.close();
        return count;
    }

    public int countAndroidSyncedAssets(){
        String sqlQuery = "SELECT COUNT(*) AS rowCount FROM ANDROID androidTable " +
                "JOIN DRIVE driveTable ON driveTable.assetId = androidTable.assetId";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
        int count = 0;
        if(cursor != null && cursor.moveToFirst()){
            count = cursor.getInt(0);
        }
        if(count == 0){
            LogHandler.saveLog("No android synced asset was found in countAndroidSyncedAssets");
        }
        cursor.close();
        return count;
    }
}
