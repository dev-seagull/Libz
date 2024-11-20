package com.example.cso;

import android.database.Cursor;
import android.os.Environment;
import android.os.StatFs;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class StorageHandler {
    private double totalStorage;
    private static long blockSize;
    private static double desiredFreeSpace = 0;
    private double optimizedPercent = 0.15;
    private static double freeSpace;
    public double getTotalStorage() {
        return totalStorage;
    }
    public void setTotalStorage(double totalStorage) {
        this.totalStorage = totalStorage;
    }
    public double getFreeSpace() {
        return freeSpace;
    }


    public StorageHandler(){
        this.blockSize = getDeviceBlockSize();
        this.totalStorage = getDeviceTotalStorage();
        System.out.println("total space:"  + totalStorage);
        this.desiredFreeSpace = this.totalStorage * optimizedPercent;
        System.out.println("desired space:"  + desiredFreeSpace);
        this.freeSpace = getDeviceFreeStorage();
        System.out.println("free space:"  + freeSpace);
    }

    public static double getAmountSpaceToFreeUp() {
        double amount = 0.0;
        try{
            double blockSize = getDeviceBlockSize();
            double totalStorage = getDeviceTotalStorage() * 1024;
            double desiredFreeSpace = (totalStorage * 0.15);
            double freeSpace = getDeviceFreeStorage() * 1024;
            System.out.println(desiredFreeSpace);
            if(freeSpace < desiredFreeSpace){
                amount = desiredFreeSpace - freeSpace;
            }
        }catch (Exception e) { FirebaseCrashlytics.getInstance().recordException(e); }

        return amount;
    }


    public static long getDeviceBlockSize(){
        String externalStorageDirectory = Environment.getExternalStorageDirectory().getPath();
        StatFs statFs = new StatFs(externalStorageDirectory);
        return statFs.getBlockSizeLong();
    }

    public static double getDeviceTotalStorage(){
        String externalStorageDirectory = Environment.getExternalStorageDirectory().getPath();
        StatFs statFs = new StatFs(externalStorageDirectory);
        long totalBlocks = statFs.getBlockCountLong();
        double totalSpaceGB = (totalBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0);
        return Double.valueOf(String.format(Locale.US,"%.1f", totalSpaceGB));
    }

    public static double getDeviceFreeStorage(){
        String externalStorageDirectory = Environment.getExternalStorageDirectory().getPath();
        StatFs statFs = new StatFs(externalStorageDirectory);
        long availableBlocks = statFs.getAvailableBlocksLong();;
        double freeSpaceGB = (availableBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0);
        return Double.valueOf(String.format(Locale.US,"%.1f", freeSpaceGB));
    }

}
