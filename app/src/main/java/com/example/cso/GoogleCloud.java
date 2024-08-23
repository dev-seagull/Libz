    package com.example.cso;

    import android.app.Activity;
    import android.content.Intent;
    import android.graphics.Color;
    import android.graphics.drawable.Drawable;
    import android.util.DisplayMetrics;
    import android.util.Log;
    import android.view.Gravity;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.ViewGroup;
    import android.view.animation.AlphaAnimation;
    import android.widget.Button;
    import android.widget.LinearLayout;
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
        private GoogleSignInClient googleSignInClient;

        public GoogleCloud(FragmentActivity activity){
            this.activity = activity;
        }

        public void signInToGoogleCloud(ActivityResultLauncher<Intent> signInLauncher) {

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
        }

        public boolean revokeToken(String userEmail) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
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
                    MainActivity.dbHelper.updateAccounts(userEmail, updatedValues, type);
                    accessTokens[0] = tokens.getAccessToken();
                }
            }catch (Exception e) {
                LogHandler.saveLog("Failed to update the access token: " + e.getLocalizedMessage(), true);
            }

            Callable<Boolean> callableTask = () -> {
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
                        return true;
                    }else{
                        LogHandler.saveLog("Tokens revoked not successfully.", false);
                    }
                } catch (IOException e) {
                    LogHandler.saveLog("Error revoking tokens: " + e.getLocalizedMessage() ,true);
                }
                return false;
            };
            Future<Boolean> future = executor.submit(callableTask);
            Boolean isSignedOut = false;
            try{
                isSignedOut = future.get();
            }catch (Exception e){
                LogHandler.saveLog("Failed to get the sign out future: " + e.getLocalizedMessage(), true);
            }
            return isSignedOut;
        }


        public static boolean isAccessTokenValid(String accessToken) throws IOException {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<Boolean> callableTask = () -> {
                String tokenInfoUrl = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken;
                URL url = new URL(tokenInfoUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    LogHandler.saveLog("access token validity " + response, false);
                    boolean isValid = response.toString().contains("error");
                    return !isValid;
                    } finally {
                    connection.disconnect();
                }
            };
            Future<Boolean> future = executor.submit(callableTask);
            Boolean isValid = false;
            try{
                isValid = future.get();
            }catch (Exception e){
                LogHandler.saveLog("Failed to check validity from future in isAccessTokenValid: " + e.getLocalizedMessage(), true);
            }
            return isValid;
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


        public SignInResult handleSignInLinkedBackupResult(String userEmail, String refreshToken){
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
                    runOnUiThread(() -> {
                        CharSequence text = "This Account Already Exists !";
                        Toast.makeText(MainActivity.activity, text, Toast.LENGTH_SHORT).show();
                    });
                }
            }catch (Exception e){
                LogHandler.saveLog("handle back up sign in result failed: " + e.getLocalizedMessage(), true);
            }
            return new SignInResult(userEmail, false, isInAccounts, tokens, storage, null);
        }

        public SignInResult handleSignInToBackupResult(Intent data){
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
                        runOnUiThread(() -> {
                            CharSequence text = "This Account Already Exists !";
                            Toast.makeText(MainActivity.activity, text, Toast.LENGTH_SHORT).show();
                        });
                    }
                    GoogleDriveFolders.initializeAllFolders(userEmail[0],tokens[0].getAccessToken());
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

        public Button createPrimaryLoginButton(LinearLayout linearLayout){
            Button newLoginButton = new Button(activity);
            Drawable loginButtonLeftDrawable = activity.getApplicationContext().getResources()
                    .getDrawable(R.drawable.googlephotosimage);
            newLoginButton.setCompoundDrawablesWithIntrinsicBounds
                    (loginButtonLeftDrawable, null, null, null);

            newLoginButton.setText("Add a primary account");
            newLoginButton.setGravity(Gravity.CENTER);
            newLoginButton.setVisibility(View.VISIBLE);
            newLoginButton.setBackgroundTintList(UIHelper.primaryAccountButtonColor);
            newLoginButton.setPadding(40,0,150,0);
            newLoginButton.setTextSize(12);
            newLoginButton.setTextColor(Color.WHITE);
            newLoginButton.setId(View.generateViewId());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200
            );
            layoutParams.setMargins(0,20,0,16);
            newLoginButton.setLayoutParams(layoutParams);

            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(2000);
            newLoginButton.startAnimation(fadeIn);

            if(linearLayout != null){
                linearLayout.addView(newLoginButton);
            }else{
                LogHandler.saveLog("Creating a new login button failed", true);
            }
            return newLoginButton;
        }

        public Button createBackUpLoginButton(LinearLayout linearLayout){
            Button newLoginButton = new Button(activity);
            Drawable loginButtonLeftDrawable = UIHelper.driveImage;
            newLoginButton.setCompoundDrawablesWithIntrinsicBounds
                    (loginButtonLeftDrawable, null, null, null);
            newLoginButton.setText("Add a back up account");
            newLoginButton.setBackgroundResource(R.drawable.gradient_purple);
            newLoginButton.setGravity(Gravity.CENTER);
            newLoginButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            newLoginButton.setVisibility(View.VISIBLE);
            newLoginButton.setPadding(40,0,150,0);
            newLoginButton.setTextSize(10);
            UIHelper uiHelper = new UIHelper();
            newLoginButton.setTextColor(uiHelper.buttonTextColor);
            newLoginButton.setBackgroundResource(R.drawable.gradient_purple);
            newLoginButton.setId(View.generateViewId());

            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200
            );
            layoutParams.setMargins(0,20,0,16);
            newLoginButton.setLayoutParams(layoutParams);

            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(2000);
            newLoginButton.startAnimation(fadeIn);

            if(linearLayout != null){
                linearLayout.addView(newLoginButton);
            }else{
                LogHandler.saveLog("Creating a new login button failed", true);
            }
            return newLoginButton;
        }

        private GoogleCloud.Tokens getTokens(String authCode){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<GoogleCloud.Tokens> backgroundTokensTask = () -> {
                String accessToken = null;
                String refreshToken = null;
                try {
                    URL googleAPITokenUrl = new URL("https://oauth2.googleapis.com/token");
                    HttpURLConnection httpURLConnection = (HttpURLConnection) googleAPITokenUrl.openConnection();
                    String clientId = activity.getResources().getString(R.string.client_id);
                    String clientSecret = activity.getString(R.string.client_secret);
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

        public Tokens updateAccessToken(String refreshToken){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<GoogleCloud.Tokens> backgroundTokensTask = () -> {
                String currentAccessToken = DBHelper.getAccessTokenFromDB(refreshToken);
                if (isAccessTokenValid(currentAccessToken)){
                    return new GoogleCloud.Tokens(currentAccessToken,refreshToken);
                }
                String accessToken = null;
                try {
                    URL googleAPITokenUrl = new URL("https://www.googleapis.com/oauth2/v4/token");
                    HttpURLConnection httpURLConnection = (HttpURLConnection) googleAPITokenUrl.openConnection();
                    String clientId = activity.getResources().getString(R.string.client_id);
                    String clientSecret = activity.getString(R.string.client_secret);
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
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        LogHandler.saveLog("Updating access token with response code of " + responseCode,false);
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
                        return new GoogleCloud.Tokens(accessToken, refreshToken);
                    }else {
                        LogHandler.saveLog("Getting access token failed with response code of " + responseCode, true);
                    }
                } catch (Exception e) {
                    LogHandler.saveLog("Getting access token failed: " + e.getLocalizedMessage(), true);
                }
                return new GoogleCloud.Tokens(accessToken, refreshToken);
            };
            Future<GoogleCloud.Tokens> future = executor.submit(backgroundTokensTask);
            GoogleCloud.Tokens tokens_fromFuture = null;
            try {
                tokens_fromFuture = future.get();
                DBHelper.updateAccessTokenInDB(refreshToken,tokens_fromFuture.getAccessToken());
            }catch (Exception e){
                LogHandler.saveLog("failed to get access token from the future: " + e.getLocalizedMessage(), true);
            }finally {
                executor.shutdown();
            }
            return tokens_fromFuture;
        }

        public static String getMimeType(String fileName){
            int dotIndex = fileName.lastIndexOf(".");
            String mimeType="";
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                mimeType = fileName.substring(dotIndex + 1);
            }
            return mimeType;
        }

        public Storage getStorage(GoogleCloud.Tokens tokens){
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
            LogHandler.saveLog("Starting invalidate token Thread", false);
            boolean[] isInvalidated = {false};
            Thread invalidateTokenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    isInvalidated[0] = MainActivity.googleCloud.revokeToken(buttonText);
                    if(!isInvalidated[0]){
                        LogHandler.saveLog("token is not invalidated." , true);
                    }
                }
            });
            invalidateTokenThread.start();
            try{
                invalidateTokenThread.join();
            }catch (Exception e){
                LogHandler.saveLog("Failed to join invalidate token Thread: " + e.getLocalizedMessage(), true );
            }
            LogHandler.saveLog("Finished  Invalidate token Thread : " + isInvalidated[0], false);
            return isInvalidated[0];
        }

        private static boolean startDeleteProfileJsonThread(String buttonText){
            LogHandler.saveLog("Starting Delete Profile Json Thread", false);
            boolean[] isDeleted = {false};
            Thread deleteProfileJsonThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    isDeleted[0] = Profile.deleteProfileJson(buttonText);
                    if(!isDeleted[0]){
                        LogHandler.saveLog("Profile Json is not deleted when signing out.");
                    }
                }
            });
            deleteProfileJsonThread.start();
            try{
                deleteProfileJsonThread.join();
            }catch (Exception e){
                LogHandler.saveLog("Failed to join Delete Profile Json Thread: " + e.getLocalizedMessage(), true );
            }
            LogHandler.saveLog("Finished  Delete Profile Json Thread : " + isDeleted[0], false);
            return isDeleted[0];
        }

        private static boolean startDeleteDatabaseThread(String buttonText){
            LogHandler.saveLog("Starting Delete Database Thread", false);
            boolean[] isDeleted = {false};
            Thread deleteDatabaseThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String driveBackupAccessToken = null;
                    String driveBackupRefreshToken;
                    String[] selected_columns = {"userEmail", "type","refreshToken"};
                    List<String[]> account_rows = DBHelper.getAccounts(selected_columns);
                    for (String[] account_row : account_rows) {
                        if (account_row[1].equals("backup") && account_row[0].equals(buttonText)) {
                            driveBackupRefreshToken = account_row[2];
                            driveBackupAccessToken = MainActivity.googleCloud.updateAccessToken(driveBackupRefreshToken).getAccessToken();
                        }
                    }
                    if((driveBackupAccessToken != null) && !driveBackupAccessToken.isEmpty()){
                        Drive service = GoogleDrive.initializeDrive(driveBackupAccessToken);
                        String databaseFolderId = GoogleDriveFolders.getDatabaseFolderId(buttonText);
                        DBHelper.deleteDatabaseFiles(service,databaseFolderId);
                        isDeleted[0] = DBHelper.checkDeletionStatus(service,databaseFolderId);
                        if(!isDeleted[0]){
                            LogHandler.saveLog("Database is not deleted when signing out.");
                        }
                    }
                }
            });
            deleteDatabaseThread.start();
            try{
                deleteDatabaseThread.join();
            }catch (Exception e){
                LogHandler.saveLog("Failed to join Delete Database Thread: " + e.getLocalizedMessage(), true );
            }
            LogHandler.saveLog("Finished  Delete Database Thread : " + isDeleted[0], false);
            return isDeleted[0];
        }

        private static boolean startProfileJsonBackUpAfterSignOutThread(String buttonText){
            LogHandler.saveLog("Starting Profile Json BackUp After Sign Out Thread", false);
            boolean[] isBackedUp = {false};
            Thread profileJsonBackUpAfterSignOutThread = new Thread(() -> {
                MainActivity.dbHelper.deleteFromAccountsTable(buttonText, "backup");
                MainActivity.dbHelper.deleteAccountFromDriveTable(buttonText);
                MainActivity.dbHelper.deleteRedundantAsset();
                SharedPreferencesHandler.setJsonModifiedTime(MainActivity.preferences);
                isBackedUp[0] = Profile.backUpProfileMap(true,buttonText);
                if(!isBackedUp[0]){
                    LogHandler.saveLog("Profile Json back up is not working when signing out.", true);
                }
            });
            profileJsonBackUpAfterSignOutThread.start();
            try{
                profileJsonBackUpAfterSignOutThread.join();
            }catch (Exception e){
                LogHandler.saveLog("Failed to join Profile Json Back Up After Sign Out Thread: " + e.getLocalizedMessage(), true );
            }
            LogHandler.saveLog("Finished  Profile Json Back Up After Sign Out Thread : " + isBackedUp[0], false);
            return isBackedUp[0];
        }

        public static void startUnlinkThreads(String buttonText, MenuItem item, Button button){
            Thread startSignOutThreads = new Thread(() -> {
                GoogleDrive.startUpdateStorageThread();
                boolean wantToUnlink = UIHandler.showMoveDriveFilesDialog(buttonText);

                //note :  handle button text if click on wait button


//                boolean isProfileJsonDeleted = startDeleteProfileJsonThread(buttonText);
//                if(isProfileJsonDeleted){
//                    boolean isDatabaseDeleted = startDeleteDatabaseThread(buttonText);
//                    boolean isInvalidated = startInvalidateTokenThread(buttonText);
//                    if(isInvalidated){
//                        boolean isBackedUp = startProfileJsonBackUpAfterSignOutThread(buttonText);
//                        if(isBackedUp){
//                            UIHandler.startUiThreadForSignOut(item,button,buttonText,isBackedUp);
//                        }
//                    }
//                }
            });
            startSignOutThreads.start();
        }

        public ArrayList<SignInResult> signInLinkedAccounts(JsonObject resultJson, String userEmail){
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



