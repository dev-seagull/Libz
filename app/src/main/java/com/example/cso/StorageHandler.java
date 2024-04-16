package com.example.cso;

import android.database.Cursor;
import android.os.Environment;
import android.os.StatFs;

import java.text.DecimalFormat;

public class StorageHandler {

    public double getTotalStorage() {
        return totalStorage;
    }

    public void setTotalStorage(double totalStorage) {
        this.totalStorage = totalStorage;
    }

    public long getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(long blockSize) {
        this.blockSize = blockSize;
    }

    public double getOptimizedFreeSpace() {
        return optimizedFreeSpace;
    }

    public void setOptimizedFreeSpace(double optimizedFreeSpace) {
        this.optimizedFreeSpace = optimizedFreeSpace;
    }

    public double getOptimizedPercent() {
        return optimizedPercent;
    }

    public void setOptimizedPercent(double optimizedPercent) {
        this.optimizedPercent = optimizedPercent;
    }

    public double getFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(double freeSpace) {
        this.freeSpace = freeSpace;
    }

    private double totalStorage;
    private long blockSize;
    private double optimizedFreeSpace = 0;
    private double optimizedPercent = 0.15;
//    private double maxFreeStorageNeeded = 80;
    private double freeSpace;
    private Upload upload = new Upload();


    public StorageHandler(){
        this.blockSize = getDeviceBlockSize();
        this.totalStorage = getDeviceTotalStorage();
        this.optimizedFreeSpace = this.totalStorage * optimizedPercent;
        System.out.println("optimizedFreeSpace is : " + optimizedFreeSpace);
        this.freeSpace = getDeviceFreeStorage();
        MainActivity.dbHelper.insertIntoDeviceTable(MainActivity.androidDeviceName,
                String.format("%.3f", this.totalStorage), String.format("%.3f", this.freeSpace));
    }

    public void storageUpdater(){
        this.freeSpace = getDeviceFreeStorage();
        MainActivity.dbHelper.updateDeviceTable(MainActivity.androidDeviceName,String.valueOf(this.freeSpace));
    }


    public double getAmountSpaceToFreeUp() {
        return 3.3;
//        String sqlQuery = "SELECT freeStorage FROM DEVICE WHERE deviceName = ?;";
//        Cursor cursor = MainActivity.dbHelper.getReadableDatabase()
//                .rawQuery(sqlQuery,new String[]{MainActivity.androidDeviceName});
//        try{
//            if(cursor != null && cursor.moveToFirst()){
//                int result = cursor.getInt(0);
//                if(result == 1){
//                    this.freeSpace = cursor.getDouble(1);
//                }
//            }
//        }catch (Exception e){
//            LogHandler.saveLog("Failed to read free space from database method: " + e.getLocalizedMessage());
//        }finally {
//            if(cursor != null){
//                cursor.close();
//            }
//        }
//        if (this.freeSpace <= optimizedFreeSpace) {
//            return optimizedFreeSpace - this.freeSpace;
//        }
//        return 0.0;
    }


    public static long getDeviceBlockSize(){
        String externalStorageDirectory = Environment.getExternalStorageDirectory().getPath();
        StatFs statFs = new StatFs(externalStorageDirectory);
        return statFs.getBlockSizeLong();
    }

    public double getDeviceTotalStorage(){
        String externalStorageDirectory = Environment.getExternalStorageDirectory().getPath();
        StatFs statFs = new StatFs(externalStorageDirectory);
        long totalBlocks = statFs.getBlockCountLong();
        double totalSpaceGB = (totalBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0);
        return Double.valueOf(String.format("%.3f", totalSpaceGB));
    }

    public double getDeviceFreeStorage(){
        String externalStorageDirectory = Environment.getExternalStorageDirectory().getPath();
        StatFs statFs = new StatFs(externalStorageDirectory);
        long availableBlocks = statFs.getAvailableBlocksLong();;
        double freeSpaceGB = (availableBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0);
        return Double.valueOf(String.format("%.3f", freeSpaceGB));
    }


    public static  double convertStorageToGigaByte(float storage){
        double divider = (Math.pow(1024,3));
        double result = storage / divider;

        DecimalFormat decimalFormat = new DecimalFormat("#.###");
        Double formattedResult = Double.parseDouble(decimalFormat.format(result));

        return formattedResult;
    }



    
}
