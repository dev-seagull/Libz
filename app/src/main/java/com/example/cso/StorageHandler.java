package com.example.cso;

import android.database.Cursor;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class StorageHandler {
    private double totalStorage;
    private long blockSize;
    private double optimizedFreeSpace = 0;
    private double optimizedPercent = 0.15;
    private double freeSpace;
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


    public StorageHandler(){
        this.blockSize = getDeviceBlockSize();
        this.totalStorage = getDeviceTotalStorage();
        this.optimizedFreeSpace = this.totalStorage * optimizedPercent;
        this.freeSpace = getDeviceFreeStorage();
        DeviceHandler.insertIntoDeviceTable(MainActivity.androidDeviceName,
                MainActivity.androidUniqueDeviceIdentifier);
    }

    public void freeStorageUpdater(){
        this.freeSpace = getDeviceFreeStorage();
    }


    public double getAmountSpaceToFreeUp() {
        String sqlQuery = "SELECT freeStorage FROM DEVICE WHERE deviceName = ?;";
        Cursor cursor = DBHelper.dbReadable.rawQuery(sqlQuery,new String[]{MainActivity.androidUniqueDeviceIdentifier});
        try{
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    this.freeSpace = cursor.getDouble(1);
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to read free space from database method: " + e.getLocalizedMessage());
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        if (this.freeSpace <= optimizedFreeSpace) {
            return optimizedFreeSpace - this.freeSpace;
        }
        return 0.0;
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
        return Double.valueOf(String.format(Locale.US,"%.1f", totalSpaceGB));
    }

    public double getDeviceFreeStorage(){
        String externalStorageDirectory = Environment.getExternalStorageDirectory().getPath();
        StatFs statFs = new StatFs(externalStorageDirectory);
        long availableBlocks = statFs.getAvailableBlocksLong();;
        double freeSpaceGB = (availableBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0);
        return Double.valueOf(String.format(Locale.US,"%.1f", freeSpaceGB));
    }


    public static  double convertStorageToGigaByte(float storage){
        double divider = (Math.pow(1024,3));
        double result = storage / divider;

        DecimalFormat decimalFormat = new DecimalFormat("#.###",new DecimalFormatSymbols(Locale.US));
        Double formattedResult = Double.parseDouble(decimalFormat.format(result));

        return formattedResult;
    }

    public static HashMap<String,String> directoryUIDisplay(){
        HashMap<String, Double> directoryMap = new HashMap<>();
        HashMap<String, String> directorySizes = new HashMap<>();
        try {
            File rootDirectory = Environment.getExternalStorageDirectory();
            File[] rootFolder = rootDirectory.listFiles();

            for (File file : rootFolder) {
                if (file.isDirectory()) {
                    directoryMap.put(file.getName(), 0.0);
                }
            }

            List<String[]> androidRows = MainActivity.dbHelper.getAndroidTable(new String[]{"filePath", "fileSize"});
            for (String[] row : androidRows) {
                String filePath = row[0];
                double fileSize = Double.valueOf(row[1]);
                for (String directory : directoryMap.keySet()) {
                    if (filePath.startsWith(rootDirectory+"/"+directory+"/")) {
                        double currentSize = Double.valueOf(directoryMap.get(directory));
                        directoryMap.put(directory, currentSize + fileSize);
                    }
                }
            }
            for (String directoryName : directoryMap.keySet()){
                Double size = directoryMap.get(directoryName);
                if (size == 0.0){
                    continue;
                } else if (0.0 <= size && size <= 1000) {
                    directorySizes.put(directoryName,"less than one ");
                }else {
                    directorySizes.put(directoryName,String.format(Locale.US,"%.1f", size / 1000));
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get directory UI display: " + e.getLocalizedMessage(),true);
        }
        return directorySizes;
    }


}
