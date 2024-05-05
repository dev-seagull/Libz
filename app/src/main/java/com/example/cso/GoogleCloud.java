    package com.example.cso;

    import android.app.Activity;
    import android.content.Intent;
    import android.graphics.Color;
    import android.graphics.drawable.Drawable;
    import android.view.Gravity;
    import android.view.View;
    import android.view.ViewGroup;
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
        private final Activity activity;
        private GoogleSignInClient googleSignInClient;

        public GoogleCloud(FragmentActivity activity){
            this.activity = activity;
        }

        public void signInToGoogleCloud(ActivityResultLauncher<Intent> signInLauncher) {
            boolean forceCodeForRefreshToken = true;

            try {
                    GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(new Scope("https://www.googleapis.com/auth/drive"),
                            new Scope("https://www.googleapis.com/auth/photoslibrary.readonly"),
                            new Scope("https://www.googleapis.com/auth/drive.file"),
                            new Scope("https://www.googleapis.com/auth/photoslibrary.appendonly")
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

        public boolean signOut(String userEmail) {
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
                    GoogleCloud.Tokens tokens = requestAccessToken(refreshToken);
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

        public static class signInResult{
            private final String userEmail;
            private boolean isHandled;
            private boolean isInAccounts;
            private GoogleCloud.Tokens tokens;
            private Storage storage;

            private ArrayList<DriveAccountInfo.MediaItem> mediaItems;

            private signInResult(String userEmail, boolean isHandled, boolean isInAccounts,
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

        public signInResult handleSignInToPrimaryResult(Intent data){
            final boolean[] isHandled = {false};
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
            return new signInResult(userEmail, isHandled[0], isInAccounts, tokens, storage, new ArrayList<>());
        }

        public signInResult startSignInToBackUpThread(Intent data){
            final GoogleCloud.signInResult[] signInResult = new signInResult[1];
            Thread signInResultThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        signInResult[0] = handleSignInToBackupResult(data);
                    }catch (Exception e){
                        LogHandler.saveLog("Failed to join and run sign in to backUp thread : " + e.getLocalizedMessage(), true);
                    }
                }
            });
            signInResultThread.start();
            try {
                signInResultThread.join();
            }catch (Exception e){
                LogHandler.saveLog("Failed to join sign in to backUp thread : " + e.getLocalizedMessage(), true);
            }
            return signInResult[0];
        }

        public signInResult handleSignInToBackupResult(Intent data){
            String userEmail = null;
            boolean isInAccounts = false;
            GoogleCloud.Tokens tokens = null;
            Storage storage = null;
            ArrayList<DriveAccountInfo.MediaItem> mediaItems  = null;
            try{
                Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);
                userEmail = account.getEmail();

                if (userEmail != null && userEmail.toLowerCase().endsWith("@gmail.com")) {
                    userEmail = userEmail.replace("@gmail.com", "");
                }

                String[] columnsList = new String[]{"userEmail","type","accessToken"};
                List<String[]> accounts_rows = DBHelper.getAccounts(columnsList);
                for (String[] row : accounts_rows) {
                    if (row.length > 0 && row[0] != null && row[0].equals(userEmail)) {
                        isInAccounts = true;
                        break;
                    }
                }

                if(!isInAccounts){
                    String authCode = account.getServerAuthCode();
                    tokens = getTokens(authCode);
                    storage = getStorage(tokens);
                    mediaItems = GoogleDrive.getMediaItems(tokens.getAccessToken());
                    if (userEmail != null && tokens.getRefreshToken() != null && tokens.getAccessToken() != null) {
                        return new signInResult(userEmail, true, false,
                                tokens, storage, mediaItems);
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
            return new signInResult(userEmail, false, isInAccounts, tokens, storage, mediaItems);
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
            newLoginButton.setTextSize(18);
            newLoginButton.setTextColor(Color.WHITE);
            newLoginButton.setId(View.generateViewId());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200
            );
            layoutParams.setMargins(0,20,0,16);
            newLoginButton.setLayoutParams(layoutParams);

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
            newLoginButton.setGravity(Gravity.CENTER);
            newLoginButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            newLoginButton.setVisibility(View.VISIBLE);
            newLoginButton.setBackgroundTintList(UIHelper.addBackupAccountButtonColor);
            newLoginButton.setPadding(40,0,150,0);
            newLoginButton.setTextSize(18);
            newLoginButton.setTextColor(UIHelper.buttonTextColor);
            newLoginButton.setId(View.generateViewId());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200
            );
            layoutParams.setMargins(0,20,0,16);
            newLoginButton.setLayoutParams(layoutParams);

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

        protected Tokens requestAccessToken(String refreshToken){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<GoogleCloud.Tokens> backgroundTokensTask = () -> {
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
            }catch (Exception e){
                LogHandler.saveLog("failed to get access token from the future: " + e.getLocalizedMessage(), true);
            }finally {
                executor.shutdown();
            }
            return tokens_fromFuture;
        }

        public static String getMemeType(String fileName){
            int dotIndex = fileName.lastIndexOf(".");
            String memeType="";
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                memeType = fileName.substring(dotIndex + 1);
            }
            return memeType;
        }

        public Storage getStorage(GoogleCloud.Tokens tokens){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            String accessToken = tokens.getAccessToken();
            final Storage[] storage = new Storage[1];
            final Double[] totalStorage = new Double[1];
            final Double[] usedStorage = new Double[1];
            final Double[] usedInDriveStorage = new Double[1];
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
                this.UsedInGmailAndPhotosStorage = usedStorage - usedInDriveStorage;
            }
            public Double getUsedInDriveStorage() {return usedInDriveStorage;}
            public Double getUsedInGmailAndPhotosStorage() {return UsedInGmailAndPhotosStorage;}
            public Double getTotalStorage() {return totalStorage;}
            public Double getUsedStorage() {return usedStorage;}
        }
    }



