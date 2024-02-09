package com.example.cso;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DBHelper extends SQLiteOpenHelper {
    private static final String OLD_DATABASE_NAME = "CSODatabase";
    public static final int DATABASE_VERSION = 11;
    public static SQLiteDatabase dbReadable;
    public static SQLiteDatabase dbWritable;

    private Context context;

    public DBHelper(Context context) {
        super(context, OLD_DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        dbReadable = getReadableDatabase();
        dbWritable = getWritableDatabase();
        onCreate(getWritableDatabase());
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String ACCOUNTS = "CREATE TABLE IF NOT EXISTS ACCOUNTS("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                +"profileId INTEGER ,"
                + "userEmail TEXT ," +
                "type TEXT CHECK (type IN ('primary','backup')), " +
                "refreshToken TEXT, " +
                "accessToken TEXT, " +
                "totalStorage REAL," +
                "usedStorage REAL," +
                "usedInDriveStorage REAL,"+
                "UsedInGmailAndPhotosStorage REAL," +
                "FOREIGN KEY (profileId) REFERENCES PROFILE(id));";
        sqLiteDatabase.execSQL(ACCOUNTS);

        String PROFILE = "CREATE TABLE IF NOT EXISTS PROFILE(" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "userName TEXT ," +
            "password TEXT," +
            "joined DATE);";
        sqLiteDatabase.execSQL(PROFILE);

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

    private void copyDatabase(Context context, String oldDatabaseName, String newDatabaseName) {
        try {
            InputStream inputStream = context.getAssets().open(oldDatabaseName);
            OutputStream outputStream = new FileOutputStream(new File
                    (context.getDatabasePath(newDatabaseName).getPath()));

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to copy database from csoDatabase : " +  e.getLocalizedMessage());
        }
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
                LogHandler.saveLog("Failed to insert data into ASSET : "  + e.getLocalizedMessage());
            }finally {
                dbWritable.endTransaction();
            }
        }
        sqlQuery = "SELECT id FROM ASSET WHERE fileHash = ?;";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{fileHash});
        if(cursor != null && cursor.moveToFirst()){
            lastInsertedId = cursor.getInt(0);
        }else{
            LogHandler.saveLog("Failed to find the existing file id in Asset database.", true);
        }
        cursor.close();
        return lastInsertedId;
    }


    public void deleteRedundantPhotos(ArrayList<String> fileIds, String userEmail){
        String sqlQuery = "SELECT * FROM PHOTOS where userEmail = ?";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{userEmail});
        if(cursor.moveToFirst()){
            do{
                int fileIdColumnIndex = cursor.getColumnIndex("fileId");
                if(fileIdColumnIndex >= 0) {
                    String fileId = cursor.getString(fileIdColumnIndex);
                    if (!fileIds.contains(fileId)) {
                        dbWritable.beginTransaction();
                        try {
                            sqlQuery = "DELETE FROM PHOTOS WHERE fileId = ?";
                            dbWritable.execSQL(sqlQuery, new Object[]{fileId});
                            dbWritable.setTransactionSuccessful();
                        } catch (Exception e) {
                            LogHandler
                                    .saveLog("Failed to delete the database in" +
                                            " deleteRedundantPhotos method. " + e.getLocalizedMessage());
                        } finally {
                            dbWritable.endTransaction();
                        }

                        int assetIdColumnIndex = cursor.getColumnIndex("assetId");
                        boolean existsInDatabase = false;
                        String assetId = "";
                        if (assetIdColumnIndex >= 0) {
                            dbReadable = getReadableDatabase();
                            assetId = cursor.getString(assetIdColumnIndex);
                            existsInDatabase = assetExistsInDatabase(assetId);
                        }

                        if (existsInDatabase == false) {
                            dbWritable.beginTransaction();
                            try {
                                sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
                                dbWritable.execSQL(sqlQuery, new Object[]{assetId});
                                dbWritable.setTransactionSuccessful();
                            } catch (Exception e) {
                                LogHandler.saveLog("Failed to delete the database in" +
                                        " deleteRedundantPhotos method. " + e.getLocalizedMessage());
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

    private boolean assetExistsInDatabase(String assetId){
        boolean existsInDatabase = false;
        try {
            String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE assetId = ?) " +
                    "OR EXISTS(SELECT 1 FROM PHOTOS WHERE assetId = ?) " +
                    "OR EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{assetId, assetId, assetId});
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

    public void insertIntoPhotosTable(Long assetId, String fileId,String fileName, String fileHash,
                                     String userEmail, String creationTime, String baseUrl){
        String sqlQuery = "";
        Boolean existsInPhotos = false;
        try{
            sqlQuery = "SELECT EXISTS(SELECT 1 FROM PHOTOS WHERE assetId = ?" +
                    " and fileHash = ? and fileId =? and userEmail = ?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId),
                    fileHash, fileId, userEmail});
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    existsInPhotos = true;
                }
            }
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from PHOTOS in " +
                    "insertIntoPhotosTable method: " + e.getLocalizedMessage());
        }
        if(existsInPhotos == false){
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



    public void insertIntoDriveTable(Long assetId, String fileId,String fileName, String fileHash,String userEmail){
        String sqlQuery = "";
        Boolean existsInDrive = false;
        try{
            sqlQuery = "SELECT EXISTS(SELECT 1 FROM DRIVE WHERE assetId = ? " +
                    "and fileHash = ? and fileId =? and userEmail = ?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId),
                    fileHash, fileId, userEmail});
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    existsInDrive = true;
                }
            }
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to select from DRIVE " +
                    "in insertIntoDriveTable method: " + e.getLocalizedMessage());
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
            LogHandler.saveLog("Failed to insert data into ASSET : " + e.getLocalizedMessage() , true);
        }finally {
            dbWritable.endTransaction();
        }
    }


    public static void insertIntoProfile(String userName, String password){
        if (Profile.profileMapExists(userName,password)){
            return;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String joined = dateFormat.format(new Date());
        dbWritable.beginTransaction();
        try{
            String sqlQuery = "INSERT INTO PROFILE (" +
                    "userName, "+
                    "password," +
                    "joined) VALUES (?,?,?)";
            Object[] values = new Object[]{userName,Hash.calculateSHA256(password), joined};
            dbWritable.execSQL(sqlQuery, values);
            dbWritable.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to save into the database " +
                    "in insertIntoProfile method : "+e.getLocalizedMessage());
        }finally {
            dbWritable.endTransaction();
        }
    }

    public static List<String []> getProfile(String[] columns){
        List<String[]> resultList = new ArrayList<>();
        String sqlQuery = "SELECT ";
        for (String column:columns){
            sqlQuery += column + ", ";
        }
        sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 2);
        sqlQuery += " FROM PROFILE" ;
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

    public void insertIntoAccounts(String userEmail,String type,String refreshToken ,String accessToken,
                            Double totalStorage , Double usedStorage , Double usedInDriveStorage , Double UsedInGmailAndPhotosStorage) {
        String[] profile_selected_columns = {"id"};
        List<String[]> profile_rows = getProfile(profile_selected_columns);
        String profileId = "0";
        for(String[] profile_row:profile_rows){
            profileId = profile_row[0];
            if(profileId != null && !profileId.isEmpty()){
                break;
            }
        }
        if(profileId == null | profileId.isEmpty()){
            LogHandler.saveLog("Profile id is empty or null !",true);
        }

        dbWritable.beginTransaction();
        try{
            String sqlQuery = "INSERT INTO ACCOUNTS (" +
                    "profileId, "+
                    "userEmail," +
                    "type, " +
                    "refreshToken, " +
                    "accessToken, " +
                    "totalStorage," +
                    "usedStorage," +
                    "usedInDriveStorage,"+
                    "UsedInGmailAndPhotosStorage) VALUES (?,?,?,?,?,?,?,?,?)";
            Object[] values = new Object[]{profileId,userEmail, type,refreshToken ,accessToken,
                    totalStorage ,usedStorage ,usedInDriveStorage ,UsedInGmailAndPhotosStorage};
            dbWritable.execSQL(sqlQuery, values);
            dbWritable.setTransactionSuccessful();
        }catch (Exception e){
            LogHandler.saveLog("Failed to save into the database " +
                    "in insertIntoAccounts method : "+e.getLocalizedMessage());
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



    public void updateAccounts(String userEmail, Map<String, Object> updateValues,String type) {
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


    public void deleteAccounts(String userEmail,String type) {
        dbWritable.beginTransaction();
        try {
            String sqlQuery = "DELETE FROM ACCOUNTS WHERE userEmail = ? and type = ?";
            dbWritable.execSQL(sqlQuery, new Object[]{userEmail,type});
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            LogHandler.saveLog("Failed to delete the database in deleteAccounts method. " + e.getLocalizedMessage());
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
        boolean existsInAndroid = existsInAndroid(assetId, filePath, device, fileSize, fileHash);
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
            }catch (Exception e){
                LogHandler.saveLog("Failed to save into the database.in insertIntoAndroidTable method. "+e.getLocalizedMessage());
            }finally {
                dbWritable.endTransaction();
            }
        }
    }

    private boolean existsInAndroid(long assetId, String filePath, String device,
                                    Double fileSize, String fileHash){
        try{
            String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ANDROID WHERE" +
                    " assetId = ? and filePath = ? and fileHash = ? and fileSize = ? and device = ?)";
            Cursor cursor = dbReadable.rawQuery(sqlQuery,new String[]{String.valueOf(assetId), filePath, fileHash,
                    String.valueOf(fileSize), device});
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    return true;
                }
            }
            cursor.close();
        }catch (Exception e){
            LogHandler.saveLog("Failed to check existing in Android : " + e.getLocalizedMessage());
        }
        return false;
    }


    //done but to another class delete method
    public void deleteFileFromDriveTable(String fileHash, String id, String assetId, String fileId, String userEmail){
        String sqlQuery  = "DELETE FROM DRIVE WHERE fileHash = ? and id = ? and assetId = ? and fileId = ? and userEmail = ?";
        dbWritable.execSQL(sqlQuery, new String[]{fileHash, id, assetId, fileId, userEmail});

        boolean existsInDatabase = assetExistsInDatabase(assetId);
        if (existsInDatabase == false) {
            dbWritable = getWritableDatabase();
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

                File androidFile = new File(filePath);
                if (!androidFile.exists() && device.equals(MainActivity.androidDeviceName)){
                    dbWritable.beginTransaction();
                    try {
                        sqlQuery = "DELETE FROM ANDROID WHERE filePath = ? and assetId = ?";
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
        String sqlQuery = "SELECT COUNT(filePath) AS pathCount FROM ANDROID where device = ?";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{MainActivity.androidDeviceName});
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

    public int countAndroidSyncedAssets(){ //
        String sqlQuery = "SELECT COUNT(DISTINCT androidTable.filePath) AS rowCount FROM ANDROID androidTable\n" +
                "JOIN DRIVE driveTable ON driveTable.assetId = androidTable.assetId WHERE androidTable.device = ?;";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, new String[]{MainActivity.androidDeviceName});
        int count = 0;
        if(cursor != null && cursor.moveToFirst()){
            count = cursor.getInt(0);
        }
        if(count == 0){
            LogHandler.saveLog("No android synced asset was found in countAndroidSyncedAssets",false);
        }
        cursor.close();
        return count;
    }

    public void deleteRedundantAsset(){
        String sqlQuery = "SELECT id FROM ASSET";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
        ArrayList<String> assetIds = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                int assetIdColumnIndex = cursor.getColumnIndex("id");
                if (assetIdColumnIndex >= 0) {
                    String assetId = cursor.getString(assetIdColumnIndex);
                    assetIds.add(assetId);
                }
            } while (cursor.moveToNext());
        }

        for (String assetId : assetIds){
            boolean existsInDatabase = false;
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
                LogHandler.saveLog("Failed to check if the data exists in Database in deleteRedundantPhotos");
            }
            if (existsInDatabase == false) {
                dbWritable.beginTransaction();
                try {
                    sqlQuery = "DELETE FROM ASSET WHERE id = ? ";
                    dbWritable.execSQL(sqlQuery, new Object[]{assetId});
                    dbWritable.setTransactionSuccessful();
                } catch (Exception e) {
                    LogHandler.saveLog("Failed to delete the database in ASSET , deleteRedundantPhotos method. " + e.getLocalizedMessage());
                } finally {
                    dbWritable.endTransaction();
                }
            }
        }
    }

    public List<String> backUpDataBase(Context context) {

        String dataBasePath = context.getDatabasePath("CSODatabase").getPath();
        System.out.println("db path -- >  " + dataBasePath);
        final String[] userEmail = {""};
        final String[] uploadFileId = new String[1];

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<String> uploadTask = () -> {
            try {
                String driveBackupAccessToken = "";
                String[] drive_backup_selected_columns = {"userEmail", "type", "accessToken"};
                List<String[]> drive_backUp_accounts = MainActivity.dbHelper.getAccounts(drive_backup_selected_columns);
                for (String[] drive_backUp_account : drive_backUp_accounts) {
                    if (drive_backUp_account[1].equals("backup")) {
                        driveBackupAccessToken = drive_backUp_account[2];
                        userEmail[0] = drive_backUp_account[0];
                        break;
                    }
                }
                NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();;
                final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
                String bearerToken = "Bearer " + driveBackupAccessToken;
                System.out.println("access token to upload is " + driveBackupAccessToken);
                HttpRequestInitializer requestInitializer = request -> {
                    request.getHeaders().setAuthorization(bearerToken);
                    request.getHeaders().setContentType("application/json");
                };

                Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                        .setApplicationName("cso")
                        .build();
                String folder_name = "Stash_DataBase";
                String folderId = null;
                com.google.api.services.drive.model.File folder = null;

                FileList fileList = service.files().list()
                        .setQ("mimeType='application/vnd.google-apps.folder' and name='"
                                + folder_name + "'")
                        .setSpaces("drive")
                        .setFields("files(id)")
                        .execute();
                List<com.google.api.services.drive.model.File> driveFolders = fileList.getFiles();
                for(com.google.api.services.drive.model.File driveFolder: driveFolders){
                    folderId = driveFolder.getId();
                }

                if (folderId == null) {
                    com.google.api.services.drive.model.File folder_metadata =
                            new com.google.api.services.drive.model.File();
                    folder_metadata.setName(folder_name);
                    folder_metadata.setMimeType("application/vnd.google-apps.folder");
                    folder = service.files().create(folder_metadata)
                            .setFields("id").execute();

                    folderId = folder.getId();
                }


                com.google.api.services.drive.model.File fileMetadata =
                            new com.google.api.services.drive.model.File();
                fileMetadata.setName("CSODatabase.db");
                fileMetadata.setParents(java.util.Collections.singletonList(folderId));

                File androidFile = new File(dataBasePath);
                if (!androidFile.exists()) {
                    LogHandler.saveLog("Failed to upload database from Android to backup because it doesn't exist");
                }
                FileContent mediaContent = new FileContent("application/x-sqlite3", androidFile);
                if (mediaContent == null) {
                    LogHandler.saveLog("Failed to upload database from Android to backup because it's null");
                }
                com.google.api.services.drive.model.File uploadFile =
                        service.files().create(fileMetadata, mediaContent).setFields("id").execute();
                uploadFileId[0] = uploadFile.getId();
                System.out.println("upload id is " + uploadFileId[0]);
                while (uploadFileId[0] == null) {
                    wait();
                    System.out.println("waiting ... ");
                }
                if (uploadFileId[0] == null | uploadFileId[0].isEmpty()) {
                    LogHandler.saveLog("Failed to upload database from Android to backup because it's null");
                } else {
                    LogHandler.saveLog("Uploading database from android into backup " +
                            "account uploadId : " + uploadFileId[0], false);
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to upload database from Android to backup : " + e.getLocalizedMessage());
            }
            return uploadFileId[0];
        };
        Future<String> future = executor.submit(uploadTask);
        String uploadFileIdFuture = new String();
        try{
            uploadFileIdFuture = future.get();
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        List<String> result = Arrays.asList(userEmail[0],uploadFileIdFuture);
        return result;
    }

    public boolean backupAccountExists(){
        String sqlQuery = "SELECT EXISTS(SELECT 1 FROM ACCOUNTS WHERE type = 'backup')";
        Cursor cursor = dbReadable.rawQuery(sqlQuery, null);
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


    public String backUpProfileMap() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<String> uploadTask = () -> {
            String uploadFileId = "";
            try {
                String driveBackupAccessToken = "";
                String[] selected_columns = {"userEmail", "type", "accessToken"};
                List<String[]> account_rows = MainActivity.dbHelper.getAccounts(selected_columns);
                for (String[] account_row : account_rows) {
                    if (account_row[1].equals("backup")) {
                        driveBackupAccessToken = account_row[2];

                        Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                        String folder_name = "stash_user_profile";
                        String folderId = null;
                        com.google.api.services.drive.model.File folder = null;

                        FileList fileList = service.files().list()
                                .setQ("mimeType='application/vnd.google-apps.folder' and name='"
                                        + folder_name + "' and trashed=false")
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute();
                        List<com.google.api.services.drive.model.File> driveFolders = fileList.getFiles();
                        for(com.google.api.services.drive.model.File driveFolder: driveFolders){
                            folderId = driveFolder.getId();
                        }

                        if (folderId == null) {
                            com.google.api.services.drive.model.File folder_metadata =
                                    new com.google.api.services.drive.model.File();
                            folder_metadata.setName(folder_name);
                            folder_metadata.setMimeType("application/vnd.google-apps.folder");
                            folder = service.files().create(folder_metadata)
                                    .setFields("id").execute();

                            folderId = folder.getId();
                        }


                        fileList = service.files().list()
                                .setQ("name contains 'profileMap' and '" + folderId + "' in parents")
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute();
                        List<com.google.api.services.drive.model.File> existingFiles = fileList.getFiles();
                        for (com.google.api.services.drive.model.File existingFile : existingFiles) {
                            service.files().delete(existingFile.getId()).execute();
                        }

                        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                        fileMetadata.setName("profileMap.json");
                        fileMetadata.setParents(java.util.Collections.singletonList(folderId));

                        String content = Profile.createProfileMapContent().toString();

                        ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", content);

                        com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                                .setFields("id")
                                .execute();

                        String uploadedFileId = uploadedFile.getId();

                        while (uploadedFileId == null) {
                            wait();
                            System.out.println("waiting for profile map backup... ");
                        }
                        if (uploadedFileId == null | uploadedFileId.isEmpty()) {
                            LogHandler.saveLog("Failed to upload profileMap from Android to backup because it's null");
                        }
                    }
                }
            } catch (Exception e) {
                LogHandler.saveLog("Failed to upload profileMap from Android to backup : " + e.getLocalizedMessage());
            }
            return uploadFileId;
        };

        Future<String> future = executor.submit(uploadTask);
        String uploadFileIdFuture = new String();
        try{
            uploadFileIdFuture = future.get();
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return uploadFileIdFuture;
    }

}
