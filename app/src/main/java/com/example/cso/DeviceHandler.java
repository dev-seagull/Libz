package com.example.cso;

import static com.example.cso.DBHelper.dbReadable;
import static com.example.cso.DBHelper.dbWritable;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.sqlcipher.Cursor;

import java.util.ArrayList;

public class DeviceHandler {
    String deviceName;
    String deviceId;
    public DeviceHandler(String deviceName, String deviceId){
        this.deviceName = deviceName;
        this.deviceId = deviceId;
    }
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceId(String deviceId) {this.deviceId = deviceId;}

    public String getDeviceName() {return deviceName;}

    public String getDeviceId() {return deviceId;}

    public static ArrayList<DeviceHandler> getDevicesFromJson(JsonObject resultJson){
        ArrayList<DeviceHandler> devices =new ArrayList<>();
        try{
            JsonArray devicesInfo = resultJson.getAsJsonArray("deviceInfo");
            for(JsonElement deviceInfo: devicesInfo){
                JsonObject deviceInfoObject = deviceInfo.getAsJsonObject();
                String deviceName = deviceInfoObject.get("deviceName").getAsString();
                String deviceId = deviceInfoObject.get("deviceId").getAsString();
                devices.add(new DeviceHandler(deviceName,deviceId));
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get devices from json: " + e.getLocalizedMessage(), true);
        }
        return devices;
    }

    public static void insertIntoDeviceTable(String deviceName , String deviceId){
        if (deviceNameExists(deviceId)){
            return;
        }
        try {
            dbWritable.beginTransaction();
            String sqlQuery = "INSERT INTO DEVICE (deviceName, deviceId) VALUES (?,?)";
            dbWritable.execSQL(sqlQuery, new Object[]{deviceName, deviceId});
            dbWritable.setTransactionSuccessful();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            dbWritable.endTransaction();
        }
    }

    public static boolean deviceNameExists(String deviceId){
        boolean exists = false;
        Cursor cursor =null;
        try{
            String sqlQuery = "SELECT EXISTS(SELECT 1 FROM DEVICE WHERE deviceId = ?)";
            cursor = dbReadable.rawQuery(sqlQuery,new String[]{deviceId});
            if(cursor != null && cursor.moveToFirst()){
                int result = cursor.getInt(0);
                if(result == 1){
                    exists = true;
                }
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to check if deviceId exists : " + e.getLocalizedMessage());
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return exists;
    }

    public static ArrayList<DeviceHandler> getDevicesFromDB(){
        Cursor cursor = null;
        ArrayList<DeviceHandler> resultList = new ArrayList<>();
        try{
            String sqlQuery = "SELECT deviceName, deviceId FROM DEVICE;";
            cursor = dbReadable.rawQuery(sqlQuery,new String[]{});
            if (cursor.moveToFirst()) {
                do {
                    int deviceNameColumnIndex = cursor.getColumnIndex("deviceName");
                    int deviceIdColumnIndex = cursor.getColumnIndex("deviceId");
                    if (deviceNameColumnIndex >= 0 && deviceIdColumnIndex >= 0) {
                        String deviceName = cursor.getString(deviceNameColumnIndex);
                        String deviceId = cursor.getString(deviceIdColumnIndex);
                        resultList.add(new DeviceHandler(deviceName,deviceId));
                    }
                } while (cursor.moveToNext());
            }
        }catch (Exception e){
            LogHandler.saveLog("Failed to get devices from DB : " + e.getLocalizedMessage());
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return resultList;
    }

}
