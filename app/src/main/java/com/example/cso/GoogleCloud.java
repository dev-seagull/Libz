    package com.example.cso;

    import android.app.Activity;
    import android.content.Intent;
    import android.util.Log;
    import android.widget.Toast;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.fragment.app.FragmentActivity;

    import com.google.android.gms.auth.api.signin.GoogleSignIn;
    import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
    import com.google.android.gms.auth.api.signin.GoogleSignInClient;
    import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
    import com.google.android.gms.common.api.ApiException;
    import com.google.android.gms.common.api.Scope;
    import com.google.android.gms.tasks.Task;
    import com.google.api.services.drive.Drive;
    import com.google.firebase.crashlytics.FirebaseCrashlytics;
    import com.google.gson.JsonArray;
    import com.google.gson.JsonObject;

    import org.json.JSONObject;

    import java.io.BufferedReader;
    import java.io.IOException;
    import java.io.InputStreamReader;
    import java.io.OutputStream;
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.nio.charset.StandardCharsets;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.concurrent.Callable;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    import java.util.concurrent.Future;

    public class GoogleCloud extends AppCompatActivity {
        private Activity activity;
        private static GoogleSignInClient googleSignInClient;

        public GoogleCloud(FragmentActivity activity){
            this.activity = activity;
        }

        public GoogleCloud(){

        }

        public static void signInToGoogleCloud(ActivityResultLauncher<Intent> signInLauncher, Activity activity) {
            new Thread( () -> {
                boolean forceCodeForRefreshToken = true;

                try {
                    GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestScopes(
                                    //auth in drive :
                                    new Scope("https://www.googleapis.com/auth/drive"),
                                    // read files from photos library :
                                    new Scope("https://www.googleapis.com/auth/photoslibrary.readonly"),
                                    // read and write files in drive :
                                    new Scope("https://www.googleapis.com/auth/drive.file"),
                                    // write files in photos library :
                                    new Scope("https://www.googleapis.com/auth/photoslibrary.appendonly"),
                                    //delete any files from drive :
                                    new Scope("https://www.googleapis.com/auth/drive.appdata")
                            )
                            .requestServerAuthCode(activity.getResources().getString(R.string.web_client_id), forceCodeForRefreshToken)
                            .requestEmail()
                            .build();

                    googleSignInClient = GoogleSignIn.getClient(activity, googleSignInOptions);

                    googleSignInClient.signOut().addOnCompleteListener(task -> {
                        Intent signInIntent = googleSignInClient.getSignInIntent();
                        signInLauncher.launch(signInIntent);
                    });
                } catch (Exception e){
                    LogHandler.saveLog("login failed in signInGoogleCloud : "+e.getLocalizedMessage(),true);
                }
            }).start();
        }

        public static boolean revokeToken(String userEmail) {
            boolean[] isRevoked = {false};
            Thread revokeTokenThread = new Thread(() -> {
                String type = "";
                String refreshToken = "";
                String[] accessTokens = new String[1];
                List<String[]> accountRows = DBHelper.getAccounts(new String[]{"type","userEmail","refreshToken","accessToken"});
                for (String[] row : accountRows) {
                    if (row.length > 0 && row[1] != null && row[1].equals(userEmail)) {
                        type = row[0];
                        refreshToken = row[2];
                        accessTokens[0] = row[3];
                        break;
                    }
                }

                try {
                    if (!isAccessTokenValid(accessTokens[0])) {
                        GoogleCloud.Tokens tokens = updateAccessToken(refreshToken);
                        Map<String, Object> updatedValues = new HashMap<String, Object>() {{
                            put("accessToken", tokens.getAccessToken());
                        }};
                        DBHelper.updateAccounts(userEmail, updatedValues, type);
                        accessTokens[0] = tokens.getAccessToken();
                    }
                }catch (Exception e) {
                    LogHandler.saveLog("Failed to update the access token: " + e.getLocalizedMessage(), true);
                }

                try {
                    String revokeUrl = "https://accounts.google.com/o/oauth2/revoke";
                    URL url = new URL(revokeUrl);

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    if (accessTokens[0] != null) {
                        String requestBody = "token=" + accessTokens[0];
                        try (OutputStream os = connection.getOutputStream()) {
                            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);
                        }
                    }
                    connection.getResponseCode();//don't delete this line , it is important
                    System.out.println("responseCode of signOut " + connection.getResponseCode());
                    connection.disconnect();
                    boolean isAccessTokenValid = isAccessTokenValid(accessTokens[0]);
                    if (!isAccessTokenValid) {
                        LogHandler.saveLog("Tokens revoked successfully.", false);
                        isRevoked[0] = true;
                    }else{
                        LogHandler.saveLog("Tokens revoked not successfully.", false);
                    }
                } catch (IOException e) {
                    LogHandler.saveLog("Error revoking tokens: " + e.getLocalizedMessage() ,true);
                }
            });
            revokeTokenThread.start();
            try{
                revokeTokenThread.join();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            return isRevoked[0];
        }


        public static boolean isAccessTokenValid(String accessToken){
            boolean[] isValid = {false};
            Thread isAccessTokenValidThread = new Thread( () -> {
                String tokenInfoUrl = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken;
                HttpURLConnection connection =null;
                try {
                    URL url = new URL(tokenInfoUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    isValid[0] = !response.toString().contains("error");
                } catch (Exception e){
                    FirebaseCrashlytics.getInstance().recordException(e);
                }finally{
                    if (connection != null){
                        connection.disconnect();
                    }
                }
            });
            isAccessTokenValidThread.start();
            try {
                isAccessTokenValidThread.join();
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
           return isValid[0];
        }

        public static class SignInResult {
            private String userEmail;
            private boolean isHandled;
            private boolean isInAccounts;
            private GoogleCloud.Tokens tokens;
            private Storage storage;
            private ArrayList<DriveAccountInfo.MediaItem> mediaItems;

            private SignInResult(String userEmail, boolean isHandled, boolean isInAccounts,
                                 GoogleCloud.Tokens tokens, Storage storage, ArrayList<DriveAccountInfo.MediaItem> mediaItems) {
                this.userEmail = userEmail;
                this.isHandled = isHandled;
                this.tokens = tokens;
                this.storage = storage;
                this.mediaItems = mediaItems;
                this.isInAccounts = isInAccounts;
            }

            public String getUserEmail() {return userEmail;}
            public boolean getHandleStatus() {return isHandled;}
            public GoogleCloud.Tokens getTokens() {return tokens;}
            public Storage getStorage() {return storage;}
            public boolean getIsInAccounts() {return isInAccounts;}
            public ArrayList<DriveAccountInfo.MediaItem> getMediaItems() {return mediaItems;}
        }

        public SignInResult handleSignInToPrimaryResult(Intent data){
            boolean[] isHandled = {false};
            boolean isInAccounts = false;
            String userEmail = "";
            String authCode;
            GoogleCloud.Tokens tokens = null;
            Storage storage = null;
            try{
                Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);
                userEmail = account.getEmail();
                if (userEmail != null && userEmail.toLowerCase().endsWith("@gmail.com")) {
                    userEmail = account.getEmail();
                    userEmail = userEmail.replace("@gmail.com", "");
                }
                List<String[]> userAccounts = DBHelper.getAccounts(new String[]{"userEmail","type"});
                for (String[] row : userAccounts) {
                    if (row.length > 0 && row[0] != null && row[0].equals(userEmail)) {
                        isInAccounts = true;
                        runOnUiThread(() -> {
                            CharSequence text = "This Account Already Exists !";
                            Toast.makeText(MainActivity.activity, text, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                if (!isInAccounts){
                    authCode = account.getServerAuthCode();
                    tokens = getTokens(authCode);
                    storage = getStorage(tokens);
                    if(userEmail!= null && tokens.getRefreshToken() != null && tokens.getAccessToken() != null){
                        isHandled[0] = true;
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("handle primary sign in result failed: " + e.getLocalizedMessage(), true);
            }
            return new SignInResult(userEmail, isHandled[0], isInAccounts, tokens, storage, new ArrayList<>());
        }


        public static SignInResult handleSignInLinkedBackupResult(String userEmail, String refreshToken){
            boolean isInAccounts = false;
            GoogleCloud.Tokens tokens = null;
            Storage storage = null;
            try{
                String[] columnsList = new String[]{"userEmail","type"};
                List<String[]> accounts_rows = DBHelper.getAccounts(columnsList);
                for (String[] row : accounts_rows) {
                    if (row.length > 0 && row[0] != null && row[0].equals(userEmail)) {
                        isInAccounts = true;
                        break;
                    }
                }
                if(!isInAccounts){
                    String accessToken = updateAccessToken(refreshToken).getAccessToken();
                    tokens = new Tokens(accessToken,refreshToken);
                    storage = getStorage(tokens);
                    if (userEmail != null && tokens.getRefreshToken() != null && tokens.getAccessToken() != null) {

                        return new SignInResult(userEmail, true, false,
                                tokens, storage, null);
                    }
                }else {
                    MainActivity.activity.runOnUiThread(() -> {
                        CharSequence text = "This Account Already Exists !";
                        Toast.makeText(MainActivity.activity, text, Toast.LENGTH_SHORT).show();
                    });
                }
            }catch (Exception e){
                LogHandler.saveLog("handle back up sign in result failed: " + e.getLocalizedMessage(), true);
            }
            return new SignInResult(userEmail, false, isInAccounts, tokens, storage, null);
        }

        public static SignInResult handleSignInToBackupResult(Intent data){
            String[] userEmail = {null};
            boolean[] isInAccounts = {false};
            Tokens[] tokens = {null};
            Storage[] storage = {null};
            boolean[] isHandled = {false};
            Thread handleSignInToBackupResultThread = new Thread(() -> {
                try{
                    Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                    GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);
                    userEmail[0] = account.getEmail();

                    if (userEmail[0] != null && userEmail[0].toLowerCase().endsWith("@gmail.com")) {
                        userEmail[0] = userEmail[0].replace("@gmail.com", "");
                    }

                    String[] columnsList = new String[]{"userEmail","type"};
                    List<String[]> accounts_rows = DBHelper.getAccounts(columnsList);
                    for (String[] row : accounts_rows) {
                        if (row.length > 0 && row[0] != null && row[0].equals(userEmail[0])) {
                            isInAccounts[0] = true;
                            break;
                        }
                    }

                    if(!isInAccounts[0]){
                        String authCode = account.getServerAuthCode();
                        tokens[0] = getTokens(authCode);
                        storage[0] = getStorage(tokens[0]);
                        isHandled[0] = true;
                    }else {
                        MainActivity.activity.runOnUiThread(() -> {
                            CharSequence text = "This Account Already Exists !";
                            Toast.makeText(MainActivity.activity, text, Toast.LENGTH_SHORT).show();
                        });
                    }

                }catch (Exception e){
                    LogHandler.saveLog("handle back up sign in result failed: " + e.getLocalizedMessage(), true);
                }
            });
            handleSignInToBackupResultThread.start();
            try{
                handleSignInToBackupResultThread.join();
            }catch (Exception e){
                LogHandler.saveLog("Failed to join   handleSignInToBackupResultThread.start(): " + e.getLocalizedMessage(), true );
            }
            return new SignInResult(userEmail[0], isHandled[0], isInAccounts[0], tokens[0], storage[0], null);
        }

        private static GoogleCloud.Tokens getTokens(String authCode){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<GoogleCloud.Tokens> backgroundTokensTask = () -> {
                String accessToken = null;
                String refreshToken = null;
                try {
                    URL googleAPITokenUrl = new URL("https://oauth2.googleapis.com/token");
                    HttpURLConnection httpURLConnection = (HttpURLConnection) googleAPITokenUrl.openConnection();
                    String clientId = MainActivity.activity.getResources().getString(R.string.client_id);
                    String clientSecret = MainActivity.activity.getString(R.string.client_secret);
                    String requestBody = "code=" + authCode +
                            "&client_id=" + clientId +
                            "&client_secret=" + clientSecret +
                            "&grant_type=authorization_code";
                    byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setRequestProperty("Content-Length", String.valueOf(postData.length));
                    httpURLConnection.setRequestProperty("Host", "oauth2.googleapis.com");//this line seems to be extra
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(true);
                    OutputStream outputStream = httpURLConnection.getOutputStream();
                    outputStream.write(postData);
                    outputStream.flush();

                    int responseCode = httpURLConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        StringBuilder responseBuilder = new StringBuilder();
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(httpURLConnection.getInputStream())
                        );
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        String response = responseBuilder.toString();
                        JSONObject responseJSONObject = new JSONObject(response);
                        System.out.println("here is response json object : " + responseJSONObject.toString());
                        accessToken = responseJSONObject.getString("access_token");
                        refreshToken = responseJSONObject.getString("refresh_token");
                        return new GoogleCloud.Tokens(accessToken, refreshToken);
                    }else {
                        LogHandler.saveLog("Getting tokens failed with response code of " + responseCode, true);
                    }
                } catch (Exception e) {
                    LogHandler.saveLog("Getting tokens failed: " + e.getLocalizedMessage(), true);
                }
                return new GoogleCloud.Tokens(accessToken, refreshToken);
            };
            Future<GoogleCloud.Tokens> future = executor.submit(backgroundTokensTask);
            GoogleCloud.Tokens tokens_fromFuture = null;
            try {
                tokens_fromFuture = future.get();
            }catch (Exception e){
                LogHandler.saveLog("failed to get tokens from the future: " + e.getLocalizedMessage(), true);
            }finally {
                executor.shutdown();
            }
            return tokens_fromFuture;
        }

        public static Tokens updateAccessToken(String refreshToken){
            GoogleCloud.Tokens[] tokens = {null};
            Thread updateAccessTokenThread = new Thread( () -> {
                String currentAccessToken = DBHelper.getAccessTokenFromDB(refreshToken);
                boolean isAccessTokenValid = isAccessTokenValid(currentAccessToken);
                Log.d("token","isAccessTokenValid(currentAccessToken) : " + isAccessTokenValid);
                if (isAccessTokenValid){
                    tokens[0] = new Tokens(currentAccessToken,refreshToken);
                    return;
                }
                String accessToken = null;
                try {
                    URL googleAPITokenUrl = new URL("https://www.googleapis.com/oauth2/v4/token");
                    HttpURLConnection httpURLConnection = (HttpURLConnection) googleAPITokenUrl.openConnection();
                    String clientId = MainActivity.activity.getResources().getString(R.string.client_id);
                    String clientSecret = MainActivity.activity.getString(R.string.client_secret);
                    String requestBody = "&client_id=" + clientId +
                            "&client_secret=" + clientSecret +
                            "&refresh_token= " + refreshToken +
                            "&grant_type=refresh_token";
                    byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(true);
                    OutputStream outputStream = httpURLConnection.getOutputStream();
                    outputStream.write(postData);
                    outputStream.flush();

                    int responseCode = httpURLConnection.getResponseCode();
                    Log.d("token", "result of update token , responseCode=" + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        StringBuilder responseBuilder = new StringBuilder();
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(httpURLConnection.getInputStream())
                        );
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        String response = responseBuilder.toString();
                        JSONObject responseJSONObject = new JSONObject(response);
                        accessToken = responseJSONObject.getString("access_token");
                        tokens[0] = new Tokens(accessToken, refreshToken);
                        return;
                    }else {
                        LogHandler.saveLog("Getting access token failed with response code of " + responseCode, true);
                    }
                } catch (Exception e) {
                    LogHandler.saveLog("Getting access token failed: " + e.getLocalizedMessage(), true);
                }
                tokens[0] = new Tokens(accessToken, refreshToken);
            });
            updateAccessTokenThread.start();
            try{
                updateAccessTokenThread.join();
            }catch (Exception e){
                LogHandler.saveLog("Failed to join   updateAccessTokenThread.start(): " + e.getLocalizedMessage(), true );
            }
            DBHelper.updateAccessTokenInDB(refreshToken,tokens[0].getAccessToken());
            return tokens[0];
        }

        public static Storage getStorage(GoogleCloud.Tokens tokens){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            String accessToken = tokens.getAccessToken();
            Storage[] storage = new Storage[1];
            Double[] totalStorage = new Double[1];
            Double[] usedStorage = new Double[1];
            Double[] usedInDriveStorage = new Double[1];
            Callable<Storage> backgroundTask = () -> {
                try {
                    Drive driveService = GoogleDrive.initializeDrive(accessToken);

                    totalStorage[0] = StorageHandler.convertStorageToGigaByte(driveService.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getLimit());

                    usedStorage[0] = StorageHandler.convertStorageToGigaByte(driveService.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getUsage());

                    usedInDriveStorage[0] = StorageHandler.convertStorageToGigaByte(driveService.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getUsageInDrive());

                    storage[0] = new Storage(totalStorage[0], usedStorage[0], usedInDriveStorage[0]);
                    return storage[0];
                } catch (Exception e) {
                    LogHandler.saveLog("Failed to get the storage : " + e.getLocalizedMessage(), true);
                }
                return storage[0];
            };

            try{
                Future<Storage> future = executor.submit(backgroundTask);
                storage[0] = future.get();
            }catch (Exception e){
                LogHandler.saveLog("Failed to get the storage in future: " + e.getLocalizedMessage(), true);
            }finally {
                executor.shutdown();
            }
            return storage[0];
        }

        public static class Tokens {
            private String refreshToken;
            private String accessToken;
            public Tokens(String accessToken, String refreshToken) {
                this.accessToken = accessToken;
                this.refreshToken = refreshToken;
            }

            public void setAccessToken(String accessToken) {
                this.accessToken = accessToken;
            }
            public void setRefreshToken(String refreshToken) {
                this.refreshToken= refreshToken;
            }
            public String getAccessToken() {
                return accessToken;
            }
            public String getRefreshToken() {
                return refreshToken;
            }
        }

        public static class Storage{
            private Double totalStorage;
            private Double usedStorage;
            private Double usedInDriveStorage;
            private Double UsedInGmailAndPhotosStorage;


            public Storage(Double totalStorage, Double usedStorage,
                           Double usedInDriveStorage){
                this.totalStorage = totalStorage * 1000;
                this.usedStorage = usedStorage * 1000;
                this.usedInDriveStorage = usedInDriveStorage * 1000;
                this.UsedInGmailAndPhotosStorage = (usedStorage - usedInDriveStorage) * 1000;
            }
            public Double getUsedInDriveStorage() {
                return usedInDriveStorage;
            }
            public Double getUsedInGmailAndPhotosStorage() {
                return UsedInGmailAndPhotosStorage;
            }
            public Double getTotalStorage() {
                return totalStorage;
            }
            public Double getUsedStorage() {
                return usedStorage;
            }
        }

        public static boolean startInvalidateTokenThread(String buttonText){
            Log.d("Unlink", "invalidate token thread started");
            boolean[] isInvalidated = {false};
            Thread invalidateTokenThread = new Thread(() -> {
                isInvalidated[0] = GoogleCloud.revokeToken(buttonText);
                if(!isInvalidated[0]){
                    LogHandler.saveLog("token is not invalidated." , true);
                }
            });
            invalidateTokenThread.start();
            try{
                invalidateTokenThread.join();
            }catch (Exception e){
                LogHandler.saveLog("Failed to join invalidate token Thread: " + e.getLocalizedMessage(), true );
            }
            Log.d("Unlink", "invalidate token thread finished : " + isInvalidated[0]);
            return isInvalidated[0];
        }

        public static void unlink(String buttonText, Activity activity){
            Log.d("Unlink", "start to get storage of drives");
            GoogleDrive.startUpdateStorageThread();
            Log.d("Unlink", "end of get storage of drives");
            boolean wantToUnlink = UIHandler.showMoveDriveFilesDialog(buttonText, activity);
            MainActivity.isAnyProccessOn = false;
        }

        public static ArrayList<SignInResult> signInLinkedAccounts(JsonObject resultJson, String userEmail){
            ArrayList<SignInResult> signInLinkedAccountsResult = new ArrayList<>();
            boolean[] isHandled = {true};

            Thread signInLinkedAccountsThread =  new Thread(() -> {
                try{
                    JsonArray backupAccounts =  resultJson.get("backupAccounts").getAsJsonArray();
                    for (int i = 0;i < backupAccounts.size();i++){
                        JsonObject backupAccount = backupAccounts.get(i).getAsJsonObject();
                        String linkedUserEmail = backupAccount.get("backupEmail").getAsString();
                        String refreshToken = backupAccount.get("refreshToken").getAsString();

                        if (linkedUserEmail.equals(userEmail)){
                            continue;
                        }

                        SignInResult signInResult =
                                handleSignInLinkedBackupResult(linkedUserEmail,refreshToken);
                        if (!signInResult.isHandled){
                            isHandled[0] = false;
                            break;
                        }

                        Log.d("signInToBackUpLauncher",userEmail + " handling: " + isHandled[0]);

                        signInLinkedAccountsResult.add(signInResult);
                    }
                }catch (Exception e){
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            });
            signInLinkedAccountsThread.start();
            try{
                signInLinkedAccountsThread.join();
            }catch (Exception e){
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            return isHandled[0] ? signInLinkedAccountsResult : null;
        }

      }



