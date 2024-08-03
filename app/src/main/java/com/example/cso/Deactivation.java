package com.example.cso;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.gson.JsonObject;

public class Deactivation {
    private JsonObject readDeActivation()
    {
        return new JsonObject();

    }

    public static void deactivate(String deviceId){
        boolean[] isDeactivated = {false};
        Thread DeactivationThread = new Thread(() -> {
            try{
                String accessToken = MainActivity.googleCloud.updateAccessToken(Support.getSupportRefreshToken()).getAccessToken();
                Drive service = GoogleDrive.initializeDrive(accessToken);
                String uploadFileId = "";
                String fileName = MainActivity.androidUniqueDeviceIdentifier + ".json";
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);
                ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", null);
                com.google.api.services.drive.model.File uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
                uploadFileId = uploadedFile.getId();
                LogHandler.saveLog("@@@" + "profile map upload file id is : "+ uploadFileId,false);
                if (uploadFileId == null | uploadFileId.isEmpty()) {
                    LogHandler.saveLog("Failed to upload deactivation json for + " + MainActivity.androidUniqueDeviceIdentifier, true);
                }else{
                    isDeactivated[0] = true;
                }
            }catch (Exception e){
                LogHandler.saveLog("Failed to backup json file: " + e.getLocalizedMessage(), true);
            }
        });
        DeactivationThread.start();
        try {
            DeactivationThread.join();
        } catch (Exception e) {
            LogHandler.saveLog("failed to join deactivation thread: " + e.getLocalizedMessage(), true);
        }
    }
}
