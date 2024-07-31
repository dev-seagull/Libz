package com.example.cso;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
}
