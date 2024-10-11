package com.example.cso;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class DBHelper extends SQLiteOpenHelper {

    public static int DATABASE_VERSION = 12;
    public static SQLiteDatabase dbReadable;
    public static SQLiteDatabase dbWritable;
    private static DBHelper instance;

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DBHelper(Context context) {
        super(context, "StashDatabase", null, DATABASE_VERSION);
        String ENCRYPTION_KEY = context.getResources().getString(R.string.ENCRYPTION_KEY);
        SQLiteDatabase.loadLibs(context);
        dbReadable = getReadableDatabase(ENCRYPTION_KEY);
        dbWritable = getReadableDatabase(ENCRYPTION_KEY);
        onCreate(getWritableDatabase(ENCRYPTION_KEY));
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String ACCOUNTS = "CREATE TABLE IF NOT EXISTS ACCOUNTS("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                +"userEmail TEXT ," +
                "type TEXT CHECK (type IN ('primary','backup','support')), " +
                "refreshToken TEXT, " +
                "accessToken TEXT, " +
                "totalStorage REAL," +
                "usedStorage REAL," +
                "usedInDriveStorage REAL,"+
                "UsedInGmailAndPhotosStorage REAL,"+
                "parentFolderId TEXT, " +
                "assetsFolderId TEXT, " +
                "profileFolderId TEXT, " +
                "databaseFolderId TEXT" +
                ");";
        sqLiteDatabase.execSQL(ACCOUNTS);

        String DEVICE = "CREATE TABLE IF NOT EXISTS DEVICE("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "deviceName TEXT," +
                "deviceId TEXT UNIQUE)";
        sqLiteDatabase.execSQL(DEVICE);

        String ASSET = "CREATE TABLE IF NOT EXISTS ASSET("
                +"id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "fileHash TEXT);";
        sqLiteDatabase.execSQL(ASSET);

        String BACKUPDB = "CREATE TABLE IF NOT EXISTS BACKUPDB("
                +"userEmail TEXT REFERENCES ACCOUNTS(userEmail), "+
                "fileId TEXT);";

        sqLiteDatabase.execSQL(BACKUPDB);


        String DRIVE = "CREATE TABLE IF NOT EXISTS DRIVE("
                +"id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "assetId INTEGER REFERENCES ASSET(id),"+
                "fileId TEXT," +
                "fileName TEXT," +
                "userEmail TEXT REFERENCES ACCOUNTS(userEmail) ON UPDATE CASCADE ON DELETE CASCADE, " +
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
                "userEmail TEXT REFERENCES ACCOUNTS(userEmail) ON UPDATE CASCADE ON DELETE CASCADE,"+
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
                "operation TEXT CHECK (operation IN ('duplicated','sync','syncPhotos','download','deletedInDevice')),"+
                "hash TEXT,"+
                "date TEXT)";
        sqLiteDatabase.execSQL(TRANSACTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    }

    public static void startOldDatabaseDeletionThread(Context context) {
        Thread deleteOldDataBaseThread = new Thread() {
            @Override
            public void run() {
                try{
                    File oldDatabaseFile = context.getDatabasePath("StashDatabase");
                    if (oldDatabaseFile.exists()) {
                        boolean deleted = oldDatabaseFile.delete();
                        if (deleted) {
                            System.out.println("Old database deleted successfully.");
                        } else {
                            System.out.println("Failed to delete old database.");
                        }
                    } else {
                        System.out.println("Old database does not exist.");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        deleteOldDataBaseThread.start();
        try {
            deleteOldDataBaseThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void copyDataFromOldToNew(DBHelper newDBHelper){
//        String[] tableNames = {"ACCOUNTS", "DEVICE", "ASSET", "BACKUPDB", "DRIVE", "ANDROID", "PHOTOS", "ERRORS", "TRANSACTIONS"};
//        SQLiteDatabase oldDatabase = getReadableDatabase(ENCRYPTION_KEY);
//        for (String tableName : tableNames) {
//            String selectQuery = "SELECT * FROM " + tableName;
//            Cursor cursor = oldDatabase.rawQuery(selectQuery, null);
//
//            newDBHelper.getWritableDatabase(ENCRYPTION_KEY).beginTransaction();
//            try {
//                while (cursor.moveToNext()) {
//                    ContentValues values = new ContentValues();
//                    for (int i = 0; i < cursor.getColumnCount(); i++) {
//                        String columnName = cursor.getColumnName(i);
//                        switch (cursor.getType(i)) {
//                            case Cursor.FIELD_TYPE_INTEGER:
//                                values.put(columnName, cursor.getInt(i));
//                                break;
//                            case Cursor.FIELD_TYPE_FLOAT:
//                                values.put(columnName, cursor.getFloat(i));
//                                break;
//                            case Cursor.FIELD_TYPE_STRING:
//                                values.put(columnName, cursor.getString(i));
//                                break;
//                            // Handle other data types if necessary
//                        }
//                    }
//                    newDBHelper.getWritableDatabase(ENCRYPTION_KEY).insert(tableName, null, values);
//                }
//                newDBHelper.getWritableDatabase(ENCRYPTION_KEY).setTransactionSuccessful();
//            } catch (Exception e) {
//                LogHandler.saveLog( "Error copying data from " + tableName + ": " + e.getMessage(), true);
//            } finally {
//                newDBHelper.getWritableDatabase(ENCRYPTION_KEY).endTransaction();
//                cursor.close();
//            }
//        }
//    }

    public static void removeColumn(String column, String table) {
        try{
            List<String> existingColumns = getTableColumns(table);
            existingColumns.remove(column);
            String columnList = TextUtils.join(",", existingColumns);

            String createNewTableQuery = "CREATE TABLE temp_table AS SELECT " +
                    columnList +
                    " FROM " + table + ";";
            dbWritable.execSQL(createNewTableQuery);

//            String copyDataQuery = "INSERT INTO temp_table (" + columnList + ") " +
//                    "SELECT " + columnList + " FROM " + table + ";";
//            dbWritable.execSQL(copyDataQuery);

            dropTable(table);

            String renameTableQuery = "ALTER TABLE temp_table RENAME TO " + table + ";";
            dbWritable.execSQL(renameTableQuery);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static List<String> getTableColumns(String tableName) {
        List<String> columns = new ArrayList<>();
        try{
            Cursor cursor = dbWritable.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int columnIndex = cursor.getColumnIndex("name");
                    if(columnIndex >=0){
                        String columnName = cursor.getString(columnIndex);
                        columns.add(columnName);
                    }
                }
                cursor.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return columns;
    }

    public static long insertAssetData(String fileHash) {

        long lastInsertedId = -1;
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ASSET WHERE fileHash = ?)";
        Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{fileHash});
        boolean existsInAsset = false;
        try{
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    existsInAsset = true;
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from ASSET in insertAssetData method: " + e.getLocalizedMessage());
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }

        if(!existsInAsset){
            try{
                dbWritable.beginTransaction();
                sqlQuery = "INSERT INTO ASSET(fileHash) VALUES (?);";
                dbWritable.execSQL(sqlQuery, new Object[]{fileHash});
                dbWritable.setTransactionSuccessful();
            }catch (Exception e){
                LogHandler.saveLog("Failed to insert data into ASSET : "  + e.getLocalizedMessage());
            }finally {
                dbWritable.endTransaction();
            }
        }
        try{
            sqlQuery = "SELECT id FROM ASSET WHERE fileHash = ?;";
            Cursor cursor2 = dbReadable.rawQuery(sqlQuery, new String[]{fileHash});
            if(cursor2 != null && cursor2.moveToFirst()){
                lastInsertedId = cursor2.getInt(0);
            }else{
                LogHandler.saveLog("Failed to find the existing file id in Asset database.", true);
            }
            cursor2.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed select asset id from asset table: " + e.getLocalizedMessage());
        }
        return lastInsertedId;
    }

//    public void deleteRedundantPhotos(ArrayList<String> fileIds, String userEmail){
//        String sqlQuery = "SELECT * FROM PHOTOS where userEmail = ?";
//        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{userEmail});
//        if(cursor.moveToFirst()){
//            do{
//                int fileIdColumnIndex = cursor.getColumnIndex("fileId");
//                if(fileIdColumnIndex >= 0) {
//                    String fileId = cursor.getString(fileIdColumnIndex);
//                    if (!fileIds.contains(fileId)) {
//                        dbWritable.beginTransaction();
//                        try {
//                            sqlQuery = "DELETE FROM PHOTOS WHERE fileId = ?";
//                            dbWritable.execSQL(sqlQuery, new Object[]{fileId});
//                            dbWritable.setTransactionSuccessful();
//                        } catch (Exception e) {
//                            LogHandler
//                                    .saveLog("Failed to delete the database in" +
//                                            " deleteRedundantPhotos method. " + e.getLocalizedMessage());
//                        } finally {
//                            dbWritable.endTransaction();
//                        }
//
//                        int assetIdColumnIndex = cursor.getColumnIndex("assetId");
//                        boolean existsInDatabase = false;
//                        String assetId = "";
//                        if (assetIdColumnIndex >= 0) {
//                            dbReadable = getReadableDatabase(ENCRYPTION_KEY);
//                            assetId = cursor.getString(assetIdColumnIndex);
//                            existsInDatabase = assetExistsInDatabase(assetId);
//                        }
//
//                        if (!existsInDatabase) {
//                            dbWritable.beginTransaction();
//                            try {
//                                sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
//                                dbWritable.execSQL(sqlQuery, new Object[]{assetId});
//                                dbWritable.setTransactionSuccessful();
//                            } catch (Exception e) {
//                                LogHandler.saveLog("Failed to delete the database in" +
//                                        " deleteRedundantPhotos method. " + e.getLocalizedMessage());
//                            } finally {
//                                dbWritable.endTransaction();
//                            }
//                        }
//                    }
//                }
//            }while (cursor.moveToNext());
//        }
//        cursor.close();
//    }

    private static boolean assetExistsInDatabase(String assetId){
        boolean existsInDatabase = false;
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ?) " +
                "OR EXISTS(SELECT 1 FROM PHOTOS WHERE assetId = ?) " +
                "OR EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ?)";
        try (Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{assetId, assetId, assetId})) {
            if (cursor != null && cursor.moveToFirst()) {
                int result = cursor.getInt(0);
                if (result == 1) {
                    existsInDatabase = true;
                }
            }
        } catch (Exception e) {
            LogHandler.saveLog("Failed to check if the data exists in Database : " + e.getLocalizedMessage());
        }
        return existsInDatabase;
    }

    public static void insertIntoPhotosTable(Long assetId, String fileId,String fileName, String fileHash,
                                      String userEmail, String creationTime, String baseUrl){
        boolean existsInPhotos = false;
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM PHOTOS WHERE assetId = ?" +
                " and fileHash = ? and fileId =? and userEmail = ?)";
        try (Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{String.valueOf(assetId),
                fileHash, fileId, userEmail})) {
            if (cursor != null && cursor.moveToFirst()) {
                int result = cursor.getInt(0);
                if (result == 1) {
                    existsInPhotos = true;
                }
            }
        } catch (Exception e) {
            LogHandler.saveLog("Failed to select from PHOTOS in " +
                    "insertIntoPhotosTable method: " + e.getLocalizedMessage());
        }
        if(!existsInPhotos){
            dbWritable.beginTransaction();
            try{
                sqlQuery = "INSERT INTO PHOTOS (" +
                        "assetId," +
                        "fileId," +
                        "fileName, " +
                        "userEmail, " +
                        "creationTime, " +
                        "fileHash, " +
                        "baseUrl) VALUES (?,?,?,?,?,?,?)";
                Object[] values = new Object[]{assetId,fileId,
                        fileName,userEmail,creationTime,fileHash,baseUrl};
                dbWritable.execSQL(sqlQuery, values);
                dbWritable.setTransactionSuccessful();
            }catch (Exception e){
                LogHandler.saveLog("Failed to save into the" +
                        " database in insertIntoPhotosTable method. "+e.getLocalizedMessage());
            }finally {
                dbWritable.endTransaction();
            }
        }
    }

    public static void insertIntoDriveTable(Long assetId, String fileId,String fileName, String fileHash,String userEmail){
        String sqlQuery = "";
        Boolean existsInDrive = false;
        sqlQuery = "SELECT EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ? " +
                "and fileHash = ? and fileId =? and userEmail = ?)";
        Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId),
                fileHash, fileId, userEmail});
        try{
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    existsInDrive = true;
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from DRIVE " +
                    "in insertIntoDriveTable method: " + e.getLocalizedMessage());
        }finally {
            if(cursor != null){
                cursor.close();
            }
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
                LogHandler.saveLog("Failed to save into the database" +
                        " in insertIntoDriveTable method. "+e.getLocalizedMessage());
            }finally {
                dbWritable.endTransaction();
            }
        }
    }

    public static void insertTransactionsData(String source, String fileName, String destination
            ,String assetId, String operation, String fileHash) {
        dbWritable.beginTransaction();
        try{
            String sqlQuery = "INSERT INTO TRANSACTIONS(source, fileName, destination, assetId, operation, hash, date)" +
                    " VALUES (?,?,?,?,?,?,?);";
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timestamp = dateFormat.format(new Date());
            dbWritable.execSQL(sqlQuery, new Object[]{source,fileName, destination, assetId, operation, fileHash, timestamp});
            dbWritable.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to insert data into ASSET : " + e.getLocalizedMessage() , true);
        }finally {
            dbWritable.endTransaction();
        }
    }

    public static void insertIntoAccounts(String userEmail,String type,String refreshToken ,String accessToken,
                                   double totalStorage , double usedStorage , double usedInDriveStorage ,
                                   double UsedInGmailAndPhotosStorage,String parentFolderId,
                                   String profileFolderId, String assetsFolderId, String databaseFolderId) {
        dbWritable.beginTransaction();
        try{
            String sqlQuery = "INSERT INTO ACCOUNTS (" +
                    "userEmail," +
                    "type, " +
                    "refreshToken, " +
                    "accessToken, " +
                    "totalStorage," +
                    "usedStorage," +
                    "usedInDriveStorage,"+
                    "UsedInGmailAndPhotosStorage," +
                    "parentFolderId," +
                    "profileFolderId," +
                    "assetsFolderId," +
                    "databaseFolderId) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
            Object[] values = new Object[]{userEmail, type,refreshToken ,accessToken,
                    totalStorage ,usedStorage ,usedInDriveStorage ,UsedInGmailAndPhotosStorage,
                    parentFolderId, profileFolderId, assetsFolderId, databaseFolderId};
            dbWritable.execSQL(sqlQuery, values);
            dbWritable.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to save into the database " +
                    "in insertIntoAccounts method : "+e.getLocalizedMessage());
        }finally {
            dbWritable.endTransaction();
        }
    }

    public static void dropTable(String tableName){
        try{
            dbWritable.beginTransaction();
            String sql = "DROP TABLE IF EXISTS " + tableName;
            dbWritable.execSQL(sql);
            dbWritable.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to drop the table in dropTable method : " + e.getLocalizedMessage());
        }finally {
            dbWritable.endTransaction();
        }

    }

    public static List<String []> getAccounts(String[] columns){
        List<String[]> resultList = new ArrayList<>();

        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }
        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM ACCOUNTS" ;
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

    public static void updateAccounts(String userEmail, Map<String, Object> updateValues,String type) {
        dbWritable.beginTransaction();
        try {
            StringBuilder sqlQueryBuilder = new StringBuilder("UPDATE ACCOUNTS SET ");

            List<Object> valuesList = new ArrayList<>();
            for (Map.Entry<String, Object> entry : updateValues.entrySet()) {
                String columnName = entry.getKey();
                Object columnValue = entry.getValue();

                sqlQueryBuilder.append(columnName).append(" = ?, ");
                valuesList.add(columnValue);
            }

            sqlQueryBuilder.delete(sqlQueryBuilder.length() - 2, sqlQueryBuilder.length());
            sqlQueryBuilder.append(" WHERE userEmail = ? and type = ?");
            valuesList.add(userEmail);
            valuesList.add(type);

            String sqlQuery = sqlQueryBuilder.toString();
            Object[] values = valuesList.toArray(new Object[0]);
            dbWritable.execSQL(sqlQuery, values);
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to update the database in updateAccounts method : " + e.getLocalizedMessage(), true);
        } finally {
            dbWritable.endTransaction();
        }
    }

    public static void deleteFromAccountsTable(String userEmail, String type) {
        dbWritable.beginTransaction();
        try {
            String sqlQuery = "DELETE FROM ACCOUNTS WHERE userEmail = ? and type = ?";
            dbWritable.execSQL(sqlQuery, new Object[]{userEmail,type});
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            dbWritable.endTransaction();
        }
    }

    public static void deleteAccountFromDriveTable(String userEmail) {
        dbWritable.beginTransaction();
        try {
            String sqlQuery = "DELETE FROM DRIVE WHERE userEmail = ?";
            dbWritable.execSQL(sqlQuery, new Object[]{userEmail});
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            dbWritable.endTransaction();
        }
    }

    public static boolean deleteAccountFromPhotosTable(String userEmail) {
        dbWritable.beginTransaction();
        try {
            String sqlQuery = "DELETE FROM PHOTOS WHERE userEmail = ?";
            dbWritable.execSQL(sqlQuery, new Object[]{userEmail});
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete from photos in deleteFromPhotosTable method. " + e.getLocalizedMessage());
        } finally {
            dbWritable.endTransaction();
        }
        if(!accountExistsInPhotosTable(userEmail)){
            return true;
        }else{
            return false;
        }
    }

    public static List<String []> getAndroidTable(String[] columns){
        List<String[]> resultList = new ArrayList<>();

        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }
        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM ANDROID ORDER BY dateModified ASC" ;
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

    public static List<String []> getAndroidTableOnThisDevice(String[] columns, String deviceId){
        List<String[]> resultList = new ArrayList<>();

        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }
        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM ANDROID where device = ? ORDER BY dateModified ASC" ;
        Cursor cursor = dbReadable.rawQuery(sqlQuery,  new String[]{deviceId});
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

    public static void insertIntoAndroidTable(long assetId,String fileName,String filePath,String device,
                                       String fileHash, Double fileSize,String dateModified,String mimeType) {
        fileHash = fileHash.toLowerCase();
        String sqlQuery = "";
        boolean existsInAndroid = existsInAndroid(assetId, filePath, device, fileSize, fileHash);
        if(existsInAndroid == false){
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
                        fileSize,fileHash,dateModified,mimeType};

                dbWritable.execSQL(sqlQuery, values);
                dbWritable.setTransactionSuccessful();
            }catch (Exception e){
                LogHandler.saveLog("Failed to save into the database in insertIntoAndroidTable method. "+e.getLocalizedMessage());
            }finally {
                dbWritable.endTransaction();
            }
        }
    }

    private static void deleteFromAndroidTable(String filePath, String assetId){
        dbWritable.beginTransaction();
        try {
            String sqlQuery = "DELETE FROM ANDROID WHERE filePath = ? and assetId = ?";
            dbWritable.execSQL(sqlQuery, new Object[]{filePath, assetId});
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete the database in ANDROID, deleteRedundantAndroid method. " + e.getLocalizedMessage());
        } finally {
            dbWritable.endTransaction();
        }
    }

    public static boolean deleteFromAndroidTable(String assetId,String fileSize, String filePath, String fileName, String fileHash){
        String sqlQuery = "DELETE FROM ANDROID WHERE fileSize = ?  and fileHash = ? and fileName =  ? and filePath = ?";
        dbWritable.beginTransaction();
        try {
            Object[] values = new Object[]{fileSize,fileHash,fileName,filePath};
            dbWritable.execSQL(sqlQuery, values);
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete from the database in deleteFromAndroidTable method. "
                    + e.getLocalizedMessage(), true);
        } finally {
            dbWritable.endTransaction();
        }

        boolean existsInAndroid = existsInAndroid(Long.valueOf(assetId), filePath, MainActivity.androidUniqueDeviceIdentifier,
                Double.valueOf(fileSize), fileHash);
        if(!existsInAndroid){
            return true;
        }
        return false;
    }

    private static boolean existsInAndroid(long assetId, String filePath, String device,
                                    Double fileSize, String fileHash){
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE" +
                " assetId = ? and filePath = ? and fileHash = ? and fileSize = ? and device = ?)";
        Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId), filePath, fileHash,
                String.valueOf(fileSize), device});
        try{
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    return true;
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to check existing in Android : " + e.getLocalizedMessage());
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return false;
    }

    public static boolean existsInAndroidWithoutHash(String filePath, String device,String date,
                                              Double fileSize){
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE" +
                " filePath = ? and fileSize = ? and device = ? and dateModified = ?)";
        Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{filePath,
                String.valueOf(fileSize), device, date});
        try{
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    return true;
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to check existing in Android : " + e.getLocalizedMessage());
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return false;
    }

    public static void deleteFileFromDriveTable(String fileHash, String id, String assetId, String fileId, String userEmail){
        String sqlQuery  = "DELETE FROM DRIVE WHERE fileHash = ? and id = ? and assetId = ? and fileId = ? and userEmail = ?";
        dbWritable.execSQL(sqlQuery, new String[]{fileHash, id, assetId, fileId, userEmail});

        boolean existsInDatabase = assetExistsInDatabase(assetId);
        if (existsInDatabase == false) {
            dbWritable.beginTransaction();
            try {
                sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
                dbWritable.execSQL(sqlQuery, new Object[]{assetId});
                dbWritable.setTransactionSuccessful();
            } catch (Exception e) {
                LogHandler.saveLog("Failed to delete the database" +
                        " in ASSET , deleteFileFromDriveTable method : " + e.getLocalizedMessage());
            } finally {
                dbWritable.endTransaction();
            }
        }
    }

    public static List<String[]> getDriveTable(String[] columns, String userEmail){
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

    public static int countAndroidAssetsOnThisDevice(String deviceId){
        String sqlQuery = "SELECT COUNT(DISTINCT assetId) AS pathCount FROM ANDROID where device = ?";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{deviceId});
        int pathCount = 0;
        if(cursor != null){
            cursor.moveToFirst();
            int pathCountColumnIndex = cursor.getColumnIndex("pathCount");
            if(pathCountColumnIndex >= 0){
                pathCount = cursor.getInt(pathCountColumnIndex);
            }
        }
        if(pathCount == 0){
            LogHandler.saveLog("No android file was found in count android assets.",false);
        }
        cursor.close();
        return pathCount;
    }

    public static int countAndroidAssets(){
        String sqlQuery = "SELECT COUNT(DISTINCT assetId) AS pathCount FROM ANDROID;";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{});
        int pathCount = 0;
        if(cursor != null){
            cursor.moveToFirst();
            int pathCountColumnIndex = cursor.getColumnIndex("pathCount");
            if(pathCountColumnIndex >= 0){
                pathCount = cursor.getInt(pathCountColumnIndex);
            }
        }
        if(pathCount == 0){
            LogHandler.saveLog("No android file was found in count android assets.",false);
        }
        cursor.close();
        return pathCount;
    }

    public static int countAssets(){
        int assetsCount = 0;
        Cursor cursor = null;
        String sqlQuery = "SELECT COUNT(id) AS assetsCount FROM ASSET";
        try{
            cursor = dbReadable.rawQuery(sqlQuery, null);
            if(cursor != null){
                cursor.moveToFirst();
                int pathCountColumnIndex = cursor.getColumnIndex("assetsCount");
                if(pathCountColumnIndex >= 0){
                    assetsCount = cursor.getInt(pathCountColumnIndex);
                }
            }
            if(assetsCount == 0){
                LogHandler.saveLog("No asset was found.",false);
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to count assets: " + e.getLocalizedMessage(), true);
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return assetsCount;
    }

    public static int countAndroidSyncedAssetsOnThisDevice(String deviceId){
        String sqlQuery = "SELECT COUNT(DISTINCT androidTable.assetId) AS rowCount FROM ANDROID androidTable\n" +
                "JOIN DRIVE driveTable ON driveTable.assetId = androidTable.assetId WHERE androidTable.device = ?;";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{deviceId});
        int count = 0;
        if(cursor != null && cursor.moveToFirst()){
            count = cursor.getInt(0);
        }
        if(count == 0){
            LogHandler.saveLog("No android synced asset was found in countAndroidSyncedAssets",false);
        }
        if (cursor != null) {
            cursor.close();
        }
        return count;
    }

    public static int getAndroidSyncedAssetsOnThisDevice(){
        String sqlQuery = "SELECT DISTINCT androidTable.assetId FROM ANDROID androidTable\n" +
                "JOIN DRIVE driveTable ON driveTable.assetId = androidTable.assetId WHERE androidTable.device = ?;";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{MainActivity.androidUniqueDeviceIdentifier});
        String count = "";
        if(cursor != null && cursor.moveToFirst()){
            count = cursor.getString(0);
        }
        if (cursor != null) {
            cursor.close();
        }
        System.out.println("count you want to see is : " + count);
        return 0;
    }

    public static int countAndroidUnsyncedAssets(){
        String sqlQuery = "SELECT COUNT(DISTINCT androidTable.assetId) AS rowCount FROM ANDROID androidTable\n" +
                "JOIN DRIVE driveTable ON driveTable.assetId = androidTable.assetId;";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{});
        int count = 0;
        if(cursor != null && cursor.moveToFirst()){
            count = cursor.getInt(0);
        }
        if(count == 0){
            LogHandler.saveLog("No android synced asset was found in countAndroidSyncedAssets",false);
        }
        if (cursor != null) {
            cursor.close();
        }
        return count;
    }

    public static boolean anyBackupAccountExists(){
        boolean exists = false;
        try{
            String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ACCOUNTS WHERE type = 'backup')";
            Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    exists = true;
                }
            }
            cursor.close();
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        Log.d("service","anyBackUpaAccountExists: " + exists);
        return exists;
    }

    public static boolean accountExists(String userEmail, String type){
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ACCOUNTS WHERE userEmail = ? and type = ?)";
        Cursor cursor = dbReadable.rawQuery(sqlQuery,  new String[]{userEmail, type});
        boolean exists = false;
        if(cursor != null && cursor.moveToFirst()){
            int result = cursor.getInt(0);
            if(result == 1){
                exists = true;
            }
        }
        cursor.close();
        return exists;
    }

    public static boolean accountExistsInDriveTable(String userEmail){
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM DRIVE WHERE userEmail = ?)";
        Cursor cursor = dbReadable.rawQuery(sqlQuery,  new String[]{userEmail});
        boolean exists = false;
        if(cursor != null && cursor.moveToFirst()){
            int result = cursor.getInt(0);
            if(result == 1){
                exists = true;
            }
        }
        cursor.close();
        return exists;
    }

    public static boolean accountExistsInPhotosTable(String userEmail){
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM PHOTOS WHERE userEmail = ?)";
        Cursor cursor = dbReadable.rawQuery(sqlQuery,  new String[]{userEmail});
        boolean exists = false;
        if(cursor != null && cursor.moveToFirst()){
            int result = cursor.getInt(0);
            if(result == 1){
                exists = true;
            }
        }
        cursor.close();
        return exists;
    }

    public static boolean assetExistsInAssetTable(String assetId){
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ASSET WHERE id = ?)";
        Cursor cursor = dbReadable.rawQuery(sqlQuery,  new String[]{assetId});
        boolean exists = false;
        if(cursor != null && cursor.moveToFirst()){
            int result = cursor.getInt(0);
            if(result == 1){
                exists = true;
            }
        }
        cursor.close();
        return exists;
    }

    public static String getAccessTokenFromDB(String refreshToken){
        Cursor cursor = null;
        String accessToken = "";
        try {
            String sqlQuery = "SELECT accessToken FROM ACCOUNTS WHERE refreshToken = ?";
            cursor = dbReadable.rawQuery(sqlQuery, new String[]{refreshToken});
            if (cursor != null && cursor.moveToFirst()) {
                int result = cursor.getColumnIndex("accessToken");
                if (result >= 0) {
                    accessToken = cursor.getString(result);
                }
            }
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }finally {
            cursor.close();
        }
        return accessToken;
    }

    public static void deleteRedundantDriveFromDB(ArrayList<String> fileIds, String userEmail){
        String sqlQuery = "SELECT * FROM DRIVE where userEmail = ?";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{userEmail});
        if(cursor.moveToFirst()){
            do{
                int fileIdColumnIndex = cursor.getColumnIndex("fileId");
                if(fileIdColumnIndex >= 0) {
                    String fileId = cursor.getString(fileIdColumnIndex);
                    if (!fileIds.contains(fileId)) {
                        deleteDriveEntry(fileId);

                        int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                        boolean existsInDatabase = false;
                        String assetId = "";
                        if (assetIdColumnIndex >= 0) {
                            assetId = cursor.getString(assetIdColumnIndex);
                            existsInDatabase = assetExistsInDatabase(assetId);
                        }

                        if (existsInDatabase == false) {
                            deleteFromAssetTable(assetId);
                        }
                    }
                }
            }while (cursor.moveToNext());
        }
        cursor.close();
    }

    private static void deleteDriveEntry(String fileId) {
        String sqlQuery = "DELETE FROM DRIVE WHERE fileId = ?";
        dbWritable.beginTransaction();
        try {
            dbWritable.execSQL(sqlQuery, new Object[]{fileId});
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete the database in DRIVE, deleteRedundantDRIVE method. " + e.getLocalizedMessage());
        } finally {
            dbWritable.endTransaction();
        }
    }

    private static void deleteFromAssetTable(String assetId){
        String sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
        dbWritable.beginTransaction();
        try {
            dbWritable.execSQL(sqlQuery, new Object[]{assetId});
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete the database in ASSET , deleteRedundantDrive method. " + e.getLocalizedMessage());
        } finally {
            dbWritable.endTransaction();
        }
    }

    public static boolean androidFileExistsInDrive(Long assetId,String fileHash){
        Boolean existsInDrive = false;
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ? " +
                "and fileHash = ?)";
        Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId), fileHash});
        try{
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    existsInDrive = true;
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to check if android file exists in drive : " +  e.getLocalizedMessage(), true);
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return existsInDrive;
    }

    public static void deleteRedundantAndroidFromDB(){
        String sqlQuery = "SELECT * FROM ANDROID";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
        if(cursor.moveToFirst()){
            do{
                String filePath = "";String assetId = "";String device = "";String fileHash = "";String fileName = "";
                int filePathColumnIndex = cursor.getColumnIndex("filePath");
                int deviceColumnIndex = cursor.getColumnIndex("device");
                int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                int fileHashColumnIndex = cursor.getColumnIndex("fileHash");
                int fileNameColumnIndex = cursor.getColumnIndex("fileName");
                if(filePathColumnIndex >= 0){
                    filePath = cursor.getString(filePathColumnIndex);
                    assetId = cursor.getString(assetIdColumnIndex);
                    fileName = cursor.getString(fileNameColumnIndex);
                    device = cursor.getString(deviceColumnIndex);
                    fileHash = cursor.getString(fileHashColumnIndex);
                }

                File androidFile = new File(filePath);
                if (!androidFile.exists() && device.equals(MainActivity.androidUniqueDeviceIdentifier)){
                    deleteFromAndroidTable(filePath, assetId);
                    insertTransactionsData(filePath,fileName,device,assetId,"deletedInDevice",fileHash);

                    boolean existsInDatabase = false;
                    existsInDatabase = assetExistsInDatabase(assetId);

                    if(existsInDatabase == false){
                        deleteFromAssetTable(assetId);
                    }
                }
            }while (cursor.moveToNext());
        }
        cursor.close();
    }

    public static void deleteRedundantAsset(){
        ArrayList<String> assetIds = selectAllAssetIds();
        for (String assetId : assetIds){
            boolean existsInDatabase = assetExistsInDatabase(assetId);
            if (existsInDatabase == false) {
                deleteFromAssetTable(assetId);
            }
        }
    }


    private static ArrayList<String> selectAllAssetIds() {
        ArrayList<String> assetIds = new ArrayList<>();
        String sqlQuery = "SELECT id FROM ASSET";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
        if (cursor.moveToFirst()) {
            do {
                int assetIdColumnIndex = cursor.getColumnIndex("id");
                if (assetIdColumnIndex >= 0) {
                    String assetId = cursor.getString(assetIdColumnIndex);
                    assetIds.add(assetId);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return assetIds;
    }


    public static boolean backUpDataBaseToDrive(Context context) {
        String dataBasePath = context.getDatabasePath("StashDatabase").getPath();
        String[] userEmail = {""};
        boolean[] isBackedUp = {false};

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> uploadTask = () -> {
            try {
                String driveBackupAccessToken = "";
                String driveBackUpRefreshToken = "";
                int backUpAccountCounts = 0;

                String[] drive_backup_selected_columns = {"userEmail","type", "totalStorage","usedStorage", "refreshToken"};
                List<String[]> drive_backUp_accounts = DBHelper.getAccounts(drive_backup_selected_columns);
                for (String[] drive_backUp_account : drive_backUp_accounts) {
                    if (drive_backUp_account[1].equals("backup")) {
                        backUpAccountCounts++;
//                        double driveFreeSpace = Sync.calculateDriveFreeSpace(drive_backUp_account);
//                        System.out.println("This is drive free space " + driveFreeSpace);
//                        if (driveFreeSpace > 30){
                        driveBackUpRefreshToken = drive_backUp_account[4];
                        driveBackupAccessToken = GoogleCloud.updateAccessToken(driveBackUpRefreshToken).getAccessToken();
                        userEmail[0] = drive_backUp_account[0];
                        Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                        String folderName = GoogleDriveFolders.databaseFolderName;
                        String databaseFolderId = GoogleDriveFolders.getSubFolderId(userEmail[0], folderName, driveBackupAccessToken,false);
//                        deleteDatabaseFiles(service, databaseFolderId);
//                        boolean isDeleted = checkDeletionStatus(service,databaseFolderId);
                        if(true){
                            String uploadedFileId = setAndCreateDatabaseContent(service,databaseFolderId,dataBasePath);
                            //while (uploadFileId == null) {
                            // wait();
                            //}
                            if (uploadedFileId == null | uploadedFileId.isEmpty()) {
                                LogHandler.saveLog("Failed to upload profileMap from Android to backup because it's null");
                            }else{
                                isBackedUp[0] = true;
                            }
                        }
                        //}
                    }
                }

                driveBackUpRefreshToken = Support.getSupportRefreshToken();
                driveBackupAccessToken = Support.requestAccessToken(driveBackUpRefreshToken).getAccessToken();
                userEmail[0] = "sofatest40";
                Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                String folderName = GoogleDriveFolders.databaseFolderName;
                String databaseFolderId = GoogleDriveFolders.getSubFolderId(userEmail[0], folderName, driveBackupAccessToken, false);
//                        deleteDatabaseFiles(service, databaseFolderId);
//                        boolean isDeleted = checkDeletionStatus(service,databaseFolderId);
                if(true){
                    String uploadedFileId = setAndCreateDatabaseContent(service,databaseFolderId,dataBasePath);
                    //while (uploadFileId == null) {
                    // wait();
                    //}
                    if (uploadedFileId == null | uploadedFileId.isEmpty()) {
                        LogHandler.saveLog("Failed to upload profileMap from Android to backup because it's null");
                    }else{
                        isBackedUp[0] = true;
                    }
                }


                if(backUpAccountCounts == 0){
                    isBackedUp[0] = true;
                    return isBackedUp[0];
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to upload database from Android to backup : " + e.getLocalizedMessage());
            }
            return isBackedUp[0];
        };
        Future<Boolean> isBackedUpFuture = executor.submit(uploadTask);
        try{
            isBackedUp[0] = isBackedUpFuture.get();
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return isBackedUp[0];
    }

    public static void deleteDatabaseFiles(Drive service, String folderId){
        try {
            FileList fileList = service.files().list()
                    .setQ("name contains 'stashDatabase' and '" + folderId + "' in parents")
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute();
            List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
            for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                service.files().delete(existingFile.getId()).execute();
            }
        }catch (Exception e) {
            LogHandler.saveLog("Failed to delete database files: " + e.getLocalizedMessage(), true);
        }
    }

    public static boolean checkDeletionStatus(Drive service, String folderId){
        try{
            FileList fileList = service.files().list()
                    .setQ("name contains 'stashDatabase' and '" + folderId + "' in parents")
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute();
            List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
            if (existingFiles.size() == 0) {
                return true;
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to check deletion status of database files: " + e.getLocalizedMessage(), true);
        }
        return false;
    }

    private static String setAndCreateDatabaseContent(Drive service,String databaseFolderId, String dataBasePath){
        String uploadFileId = "";
        try{
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName("libzDatabase.db");
            fileMetadata.setParents(java.util.Collections.singletonList(databaseFolderId));
            File androidFile = new File(dataBasePath);
            if (!androidFile.exists()) {
                LogHandler.saveLog("Failed to upload database from Android to backup because it doesn't exist", true);
            }
            FileContent mediaContent = new FileContent("application/x-sqlite3", androidFile);
            if (mediaContent == null) {
                LogHandler.saveLog("Failed to upload database from Android to backup because it's null", true);
            }
            com.google.api.services.drive.model.File uploadFile =
                    service.files().create(fileMetadata, mediaContent).setFields("id").execute();
            uploadFileId = uploadFile.getId();
        }catch (Exception e){
            LogHandler.saveLog("Failed to set profile map content:" + e.getLocalizedMessage(), true);
        }finally {
            return uploadFileId;
        }
    }

    public static double getPhotosAndVideosStorageOnThisDevice(){
        Log.d("media","try to get photos and videos");
        double sum = 0.0;
        String query = "SELECT SUM(fileSize) as result FROM ANDROID where device = ? ;";
        Cursor cursor = dbReadable.rawQuery(query, new String[]{MainActivity.androidUniqueDeviceIdentifier});

        try {
            if (cursor!= null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("result");
                Log.d("AreaSquareChart","media size result index is " + columnIndex);
                if (columnIndex >= 0){
                    sum = cursor.getDouble(columnIndex) / 1024;
                    Log.d("AreaSquareChart","media size sum is " + sum);
                }
            }
        } catch (Exception e) {
            LogHandler.crashLog(e,"AreaSquareChart");
        } finally {
            if (cursor != null) {
                cursor.close();
            }

        }
        return sum;
    }

//    public void createIndex() {
//        SQLiteDatabase db = getWritableDatabase(ENCRYPTION_KEY);
//        try {
//            db.execSQL("CREATE INDEX IF NOT EXISTS fileSize_index ON ANDROID(fileSize)");
//        } catch (Exception e) {
//            LogHandler.saveLog("Failed to create index: " + e.getLocalizedMessage(), true);
//        }
//    }


//    public void exportDecryptedDatabase(String exportPath) {
//        SQLiteDatabase encryptedDatabase = getWritableDatabase(ENCRYPTION_KEY);
//        encryptedDatabase.rawExecSQL(String.format("ATTACH DATABASE '%s' AS plaintext KEY '';", exportPath));
//        encryptedDatabase.rawExecSQL("SELECT sqlcipher_export('plaintext')");
//
//        Cursor cursor = encryptedDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                String tableName = cursor.getString(0);
//                System.out.println("Table Name: " + tableName);
//            }
//            cursor.close();
//        }
//
//        encryptedDatabase.rawExecSQL("DETACH DATABASE plaintext");
//    }


    public static void deleteTableContent(String tableName){
        try{
            dbWritable.beginTransaction();
            String deleteAndroidContent = "DELETE FROM "+tableName+" ;";
            dbWritable.execSQL(deleteAndroidContent);
            dbWritable.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to delete Android table because in (deleteTableContent "+ tableName + ") : " + e.getLocalizedMessage());
        }finally {
            dbWritable.endTransaction();
        }
    }

    public static void alterAccountsTableConstraint() {
        try {
            List<String> existingColumns = getTableColumns("ACCOUNTS");
            String columnList = TextUtils.join(",", existingColumns);
            dbWritable.beginTransaction();

            String ACCOUNTS = "CREATE TABLE IF NOT EXISTS ACCOUNTS_TEMP("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    +"userEmail TEXT ," +
                    "type TEXT CHECK (type IN ('primary','backup','support')), " +
                    "refreshToken TEXT, " +
                    "accessToken TEXT, " +
                    "totalStorage REAL," +
                    "usedStorage REAL," +
                    "usedInDriveStorage REAL,"+
                    "UsedInGmailAndPhotosStorage REAL);";
            dbWritable.execSQL(ACCOUNTS);

            String copyDataQuery = "INSERT INTO ACCOUNTS_TEMP (" + columnList + ") " +
                    "SELECT " + columnList + " FROM  ACCOUNTS ;";
            dbWritable.execSQL(copyDataQuery);

            String dropTableQuery = "DROP TABLE ACCOUNTS;";
            dbWritable.execSQL(dropTableQuery);

            String renameTableQuery = "ALTER TABLE ACCOUNTS_TEMP RENAME TO ACCOUNTS;";
            dbWritable.execSQL(renameTableQuery);
            dbWritable.setTransactionSuccessful();

        } catch (SQLiteException e) {
            LogHandler.saveLog("Failed to alter table in alterAccountsTableConstraint method : " + e.getLocalizedMessage());
        } finally {
            dbWritable.endTransaction();
        }
    }

    public static boolean updateAccessTokenInDB(String refreshToken,String accessToken){
        dbWritable.beginTransaction();
        try{
            String sqlQuery = "UPDATE ACCOUNTS SET accessToken = ? WHERE refreshToken = ?";
            dbWritable.execSQL(sqlQuery, new Object[]{accessToken,refreshToken});
            dbWritable.setTransactionSuccessful();
            return true;
        }catch (Exception e){
            LogHandler.saveLog("Failed to update accessToken in updateAccessTokenInDB : " + e.getLocalizedMessage());
            return false;
        }finally {
            dbWritable.endTransaction();
        }
    }

    public static void updateDatabaseBasedOnJson(){
        JsonObject profileContent = Profile.getJsonFromAccounts();
        if (profileContent != null){
            Log.d("jsonChange","json content from accounts : " + profileContent.toString());
            JsonArray accounts = profileContent.get("backupAccounts").getAsJsonArray();
            Log.d("jsonChange","accounts in json : " + accounts.toString());
            JsonArray devices = profileContent.get("deviceInfo").getAsJsonArray();
            Log.d("jsonChange","devices in json : " + devices.toString());
            updateAccountsBasedOnJson(accounts);
            updateDevicesBasedOnJson(devices);
        }
        GoogleDrive.startThreads();
    }

    public static void updateAccountsBasedOnJson(JsonArray profileAccounts){
        try{
            List<String[]> accounts = getAccounts(new String[]{"userEmail", "type", "refreshToken"});
            ArrayList<String> databaseUserEmails = new ArrayList<>();
            for (String[] account : accounts) {
                if (account[1].equals("backup")){
                    databaseUserEmails.add(account[0]);
                }
            }
            Log.d("jsonChange","accounts in database :  " + databaseUserEmails.toString());
            JsonArray newAccounts = Profile.getNewAccountsFromJson(profileAccounts, databaseUserEmails);
            Log.d("jsonChange","new accounts in json :  " + newAccounts.toString());
            for (JsonElement newAccount : newAccounts){
                String userEmail = newAccount.getAsJsonObject().get("backupEmail").getAsString();
                String refreshToken = newAccount.getAsJsonObject().get("refreshToken").getAsString();
                Log.d("jsonChange","try to insert new account : " + userEmail );
                GoogleCloud.SignInResult signInResult = GoogleCloud.handleSignInLinkedBackupResult(userEmail,refreshToken);
                Log.d("jsonChange","signInResult.getHandleStatus : " + signInResult.getHandleStatus());
                if (!signInResult.getHandleStatus()){
                    continue;
                }
                String accessToken = signInResult.getTokens().getAccessToken();
                String parentFolderId = GoogleDriveFolders.getParentFolderId(userEmail,true,accessToken);
                String profileFolderId = GoogleDriveFolders.getSubFolderId(userEmail,GoogleDriveFolders.profileFolderName,accessToken,true);
                String assetsFolderId = GoogleDriveFolders.getSubFolderId(userEmail,GoogleDriveFolders.assetsFolderName,accessToken,true);
                String databaseFolderId = GoogleDriveFolders.getSubFolderId(userEmail,GoogleDriveFolders.databaseFolderName,accessToken,true);
                Log.d("jsonChange","folder IDs : " + parentFolderId + " " + profileFolderId + " " + assetsFolderId + " " + databaseFolderId);

                insertIntoAccounts(userEmail, "backup", refreshToken, accessToken,
                        signInResult.getStorage().getTotalStorage(),
                        signInResult.getStorage().getUsedStorage(),
                        signInResult.getStorage().getUsedInDriveStorage(),
                        signInResult.getStorage().getUsedInGmailAndPhotosStorage(),
                        parentFolderId,profileFolderId,assetsFolderId,databaseFolderId
                );

                Log.d("jsonChange",userEmail + "inserted successfully");
            }

            ArrayList<String> removedAccounts = Profile.getRemovedAccountsFromJson(profileAccounts, databaseUserEmails);
            Log.d("jsonChange","removed accounts : " + removedAccounts.toString());
            for (String account : removedAccounts) {
                Log.d("jsonChange","try to delete : " + account);
                deleteAccountAndRelatedAssets(account);
            }
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void updateDevicesBasedOnJson(JsonArray profileDevices){
        try{
            ArrayList<DeviceHandler> databaseDevicesObject = getDevicesFromDB();
            ArrayList<String> databaseDeviceIds = new ArrayList<>();
            for (DeviceHandler deviceHandler: databaseDevicesObject){
                Log.d("jsonChange","device in database : " + deviceHandler.getDeviceName() + "," + deviceHandler.getDeviceId());
                databaseDeviceIds.add(deviceHandler.getDeviceId());
            }

            JsonArray newDevices = Profile.getNewDevicesFromJson(profileDevices,databaseDeviceIds);
            Log.d("jsonChange","new devices in json : " + newDevices.toString());
            for (JsonElement jsonElement: newDevices){
                String deviceName = jsonElement.getAsJsonObject().get("deviceName").getAsString();
                String deviceId = jsonElement.getAsJsonObject().get("deviceId").getAsString();
                Log.d("jsonChange","try to insert new device : " + deviceName + "," + deviceId);
                DeviceHandler.insertIntoDeviceTable(deviceName,deviceId);
                Log.d("jsonChange",deviceName + "," + deviceId + " inserted successfully");
            }
            ArrayList<String> removedDevices = Profile.getRemovedDevicesFromJson(profileDevices, databaseDeviceIds);
            Log.d("jsonChange","removed devices : " + removedDevices.toString());
            for (String deviceId : removedDevices){
                Log.d("jsonChange","try to delete : " + deviceId);
                DeviceHandler.deleteDevice(deviceId);
                Log.d("jsonChange","deviceId : " + deviceId + " deleted successfully");
            }

        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }


    public static void deleteAccountAndRelatedAssets(String userEmail){
        Thread deleteAccountAndRelatedAssetsThread = new Thread(() -> {
            Log.d("Unlink", "deleteAccountAndRelatedAssetsThread started");
            deleteFromAccountsTable(userEmail,"backup");
            deleteAccountFromDriveTable(userEmail);
            deleteRedundantAsset();
        });
        deleteAccountAndRelatedAssetsThread.start();
        try{
            deleteAccountAndRelatedAssetsThread.join();
        }catch (Exception e){FirebaseCrashlytics.getInstance().recordException(e);}
        Log.d("Unlink", "deleteAccountAndRelatedAssetsThread finished");
    }

    public static ArrayList<DeviceHandler> getDevicesFromDB(){
        Cursor cursor = null;
        ArrayList<DeviceHandler> devices = new ArrayList<>();
        String sqlQuery = "SELECT * FROM DEVICE";

        try{
            cursor = dbReadable.rawQuery(sqlQuery,new String[]{});
            if(cursor != null && cursor.moveToFirst()){
                int deviceIdColumnIndex = cursor.getColumnIndex("deviceId");
                int  deviceNameColumnIndex = cursor.getColumnIndex("deviceName");

                while (!cursor.isAfterLast()) {
                    if (deviceIdColumnIndex >= 0 && deviceNameColumnIndex >= 0) {
                        String deviceId = cursor.getString(deviceIdColumnIndex);
                        String deviceName = cursor.getString(deviceNameColumnIndex);
                        devices.add(new DeviceHandler(deviceName, deviceId));
                    }
                    cursor.moveToNext();
                }
            }

        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }finally {

            if(cursor != null){
                cursor.close();
            }

            return devices;
        }
    }

    public static String[] getAssetByDriveFileId(String driveFileId){
        String sqlQuery = "SELECT * FROM DRIVE WHERE fileId =?";
        Cursor cursor = null;
        try {
            cursor = dbReadable.rawQuery(sqlQuery, new String[]{driveFileId});
            if (cursor != null && cursor.moveToFirst()) {
                int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                int fileHashColumnIndex = cursor.getColumnIndex("fileHash");
                if (assetIdColumnIndex >= 0 && fileHashColumnIndex >= 0) {
                    int assetId = cursor.getInt(assetIdColumnIndex);
                    String fileHash = cursor.getString(fileHashColumnIndex);
                    return new String[]{String.valueOf(assetId), fileHash};
                }
                return null;
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from ASSET in getAssetByDriveFileId method: " + e.getLocalizedMessage());
        }
        return null;
    }


    public static void addColumn(String columnName, String columnType, String tableName) {
        try {
            // check if the column already exists
            String columnExistsQuery = "PRAGMA table_info(" + tableName + ");";
            Cursor cursor = dbReadable.rawQuery(columnExistsQuery, null);

            boolean columnExists = false;
            if (cursor!= null && cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") String dbColumnName = cursor.getString(cursor.getColumnIndex("name"));
                    if (dbColumnName.equalsIgnoreCase(columnName)) {
                        columnExists = true;
                        break;
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();

            if (!columnExists) {
                try {
                    dbWritable.beginTransaction();
                    String alterTableQuery = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType + ";";
                    dbWritable.execSQL(alterTableQuery);
                    dbWritable.setTransactionSuccessful();
                    dbWritable.endTransaction();
                }catch (Exception e) {
                    LogHandler.saveLog("Failed to add column: " + columnName + " to table: " + tableName + " due to: " + e.getLocalizedMessage(), true);
                    return;
                }
            }
        } catch (Exception e) {
            LogHandler.saveLog("Failed to check column existence: " + columnName + " to table: " + tableName,true);
        }
    }

    public static String getAssetsFolderId(String userEmail){
        String sqlQuery = "SELECT assetsFolderId FROM ACCOUNTS WHERE userEmail =?";
        Cursor cursor ;
        try {
            cursor = dbReadable.rawQuery(sqlQuery, new String[]{userEmail});
            if (cursor!= null && cursor.moveToFirst()) {
                int folderIdColumnIndex = cursor.getColumnIndex("assetsFolderId");
                if (folderIdColumnIndex >= 0) {
                    return cursor.getString(folderIdColumnIndex);
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from ACCOUNTS in getAssetsFolderId method: " + e.getLocalizedMessage());
        }
        return null;
    }

    public static String getParentFolderIdFromDB(String userEmail){
        String sqlQuery = "SELECT parentFolderId FROM ACCOUNTS WHERE userEmail =?";
        Cursor cursor = null;
        String parentFolderId = null;
        try {
            cursor = dbReadable.rawQuery(sqlQuery, new String[]{userEmail});
            if (cursor!= null && cursor.moveToFirst()) {
                int folderIdColumnIndex = cursor.getColumnIndex("parentFolderId");
                if (folderIdColumnIndex >= 0) {
                    parentFolderId = cursor.getString(folderIdColumnIndex);
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from ACCOUNTS in getParentFolderIdFromDB method: " + e.getLocalizedMessage());
        }finally {
            if(cursor!= null) {
                cursor.close();
            }
        }
        return parentFolderId;
    }

    public static String getSubFolderIdFromDB(String userEmail, String folderName){
        String sqlQuery = null;
        String columnName = null;
        if(folderName.equals(GoogleDriveFolders.databaseFolderName)){
            columnName = "databaseFolderId";
            sqlQuery = "SELECT " + columnName + " FROM ACCOUNTS WHERE userEmail =?";
        }else if(folderName.equals(GoogleDriveFolders.assetsFolderName)){
            columnName = "assetsFolderId";
            sqlQuery = "SELECT " + columnName + " FROM ACCOUNTS WHERE userEmail =?";
        }else if(folderName.equals(GoogleDriveFolders.profileFolderName)){
            columnName = "profileFolderId";
            sqlQuery = "SELECT " + columnName + " FROM ACCOUNTS WHERE userEmail =?";
        }
        Cursor cursor = null;
        String folderId = null;
        try {
            cursor = dbReadable.rawQuery(sqlQuery, new String[]{userEmail});
            if (cursor!= null && cursor.moveToFirst()) {
                int folderIdColumnIndex = cursor.getColumnIndex(columnName);
                if (folderIdColumnIndex >= 0) {
                    folderId = cursor.getString(folderIdColumnIndex);
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from ACCOUNTS in getParentFolderIdFromDB method: " + e.getLocalizedMessage());
        }finally {
            if(cursor!= null) {
                cursor.close();
            }
        }
        return folderId;
    }

    public static String getDriveBackupAccessToken(String userEmail){
        String[] driveBackupAccessToken = {null};
        Thread getDriveBackupAccessTokenThread = new Thread( () -> {
            try{
                List<String[]> accountRows = DBHelper.getAccounts(new String[]{"userEmail","type", "refreshToken"});
                for (String[] accountRow : accountRows) {
                    String selectedUserEmail = accountRow[0];
                    String type = accountRow[1];
                    if (selectedUserEmail.equals(userEmail) && type.equals("backup")) {
                        String driveBackupRefreshToken = accountRow[2];
                        driveBackupAccessToken[0] =  GoogleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();
                        break;
                    }
                }
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
        getDriveBackupAccessTokenThread.start();
        try {
            getDriveBackupAccessTokenThread.join();
        }catch (Exception e){
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return driveBackupAccessToken[0];
    }

    public static ArrayList<String> getBackupAccountsInDevice(String userEmail) {
        List<String[]> accountRows = DBHelper.getAccounts(new String[]{"userEmail", "type"});
        ArrayList<String> backupAccountsInDevice = new ArrayList<>();

        for (String[] accountRow : accountRows) {
            if (accountRow[1].equals("backup")) {
                backupAccountsInDevice.add(accountRow[0]);
            }
        }

        backupAccountsInDevice.add(userEmail);
        return backupAccountsInDevice;
    }

    public static int getNumberOfAssets() {
        int count = 0;
        String query = "SELECT COUNT(*) as result FROM ASSET;" ;
        Cursor cursor = null;
        try {
            cursor = dbReadable.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                int column = cursor.getColumnIndex("result");
                if(column >= 0){
                    count = cursor.getInt(column);
                }
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    public static int getNumberOfSyncedAssets() {
        int count = 0;
        String query = "SELECT COUNT(*) as result FROM DRIVE d " +
                "INNER JOIN ASSET a ON d.fileHash = a.fileHash";
        Cursor cursor = null;
        try {
            cursor = dbReadable.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("result");
                if(columnIndex >= 0){
                    count = cursor.getInt(columnIndex);
                }
                count = cursor.getInt(0);
            }
            Log.d("ui","synced assets: " + count);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    public static double getPercentageOfSyncedAssets() {
        int totalAssets = getNumberOfAssets();
        int syncedAssets = getNumberOfSyncedAssets();
        Log.d("ui","total assets: " + totalAssets);
        Log.d("ui","synced assets: " + syncedAssets);
        if (totalAssets == 0) {
            return 100.0;
        }
        double percentage = ((double) syncedAssets / totalAssets) * 100;
        return percentage;
    }
    
    public static void printDriveTable(){
        String query = "SELECT * " +
                "FROM DRIVE;";

        Cursor cursor = null;
        try {
            cursor = dbReadable.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    int columnIndex = cursor.getColumnIndex("fileName");
                    if(columnIndex >= 0){
                        String fileName  = cursor.getString(columnIndex);
                        Log.d("ui", "file name of drive item is : " + fileName);
                    }
                }while (cursor.moveToNext());
            }
        } catch (Exception e) {
            LogHandler.crashLog(e,"ui");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void printAndroidTable(){
        String query = "SELECT * " +
                "FROM ANDROID;";

        Cursor cursor = null;
        try {
            cursor = dbReadable.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    int columnIndex = cursor.getColumnIndex("filePath");
                    if(columnIndex >= 0){
                        String filePath  = cursor.getString(columnIndex);
                        Log.d("database", "file path of android item is : " + filePath);
                    }
                }while (cursor.moveToNext());
            }
        } catch (Exception e) {
            LogHandler.crashLog(e,"ui");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static double getRootFolderMediaSize(String pattern){

        String query = "SELECT sum(fileSize) as result FROM ANDROID WHERE filePath LIKE ? ";
        Cursor cursor = null;
        double sum = 0.0;
        try {
            cursor = dbReadable.rawQuery(query, new String[] {pattern});

            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("result");
                if (columnIndex >= 0) {
                    sum = cursor.getDouble(columnIndex);
                    Log.d("database", "sum of " + pattern + " : " + sum);
                }
            }
        } catch (Exception e) {
            LogHandler.crashLog(e, "ui");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return sum;
    }

    public static double getRootFolderMediaExcludeChildrenSize(String rootPath){
        String pattern1 = rootPath + "/%.%";
        String pattern2 = rootPath + "/%/%.%";
        String query = "SELECT sum(fileSize) as result FROM ANDROID WHERE filePath LIKE ? and filePath not LIKE ? ";

        Cursor cursor = null;
        double sum = 0.0;
        try {
            cursor = dbReadable.rawQuery(query, new String[]{pattern1, pattern2});

            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("result");
                if (columnIndex >= 0) {
                    sum = cursor.getDouble(columnIndex);
                    Log.d("database", "sum of root : " + sum);
                }
            }
        } catch (Exception e) {
            LogHandler.crashLog(e, "ui");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return sum;
    }

    public static double getSizeOfSyncedAssetsOnThisDevice() {

//        printDriveTable();
        double totalSize = 0.0;
        String query = "SELECT SUM(a.fileSize) AS totalSize " +
                "FROM ANDROID a " +
                "INNER JOIN DRIVE d ON a.assetId = d.assetId " +
                "WHERE a.device = ?";

        Cursor cursor = null;
        try {
            cursor = dbReadable.rawQuery(query, new String[]{MainActivity.androidUniqueDeviceIdentifier});

            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("totalSize");
                if (columnIndex >= 0) {
                    totalSize = cursor.getDouble(columnIndex) / 1024;
                    Log.d("AreaSquareChart", "synced assets Total size: " + totalSize + " KB");
                }
            }
        } catch (Exception e) {
            LogHandler.crashLog(e, "AreaSquareChart");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return totalSize;
    }

}
