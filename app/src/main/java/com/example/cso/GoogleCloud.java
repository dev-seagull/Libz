    package com.example.cso;

    import android.app.Activity;
    import android.content.Intent;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.graphics.drawable.Drawable;
    import android.view.Gravity;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.LinearLayout;

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
    import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
    import com.google.api.client.http.HttpRequestInitializer;
    import com.google.api.client.http.javanet.NetHttpTransport;
    import com.google.api.client.json.JsonFactory;
    import com.google.api.client.json.gson.GsonFactory;
    import com.google.api.services.drive.Drive;

    import org.json.JSONObject;

    import java.io.BufferedReader;
    import java.io.InputStreamReader;
    import java.io.OutputStream;
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.nio.charset.StandardCharsets;
    import java.text.DecimalFormat;
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


        public double convertStorageToGigaByte(float storage){
            double divider = (Math.pow(1024,3));
            double result = storage / divider;

            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            Double formattedResult = Double.parseDouble(decimalFormat.format(result));

            return formattedResult;
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
                LogHandler.saveLog("login failed in signInGoogleCloud : "+e.getLocalizedMessage());
            }
        }


        public void signOut(){
            boolean forceCodeForRefreshToken = true;
            try {
                GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary.readonly"),
                                new Scope("https://www.googleapis.com/auth/drive"),
                                new Scope("https://www.googleapis.com/auth/drive.file"),
                                new Scope("https://www.googleapis.com/auth/photoslibrary.appendonly")
                        )
                        .requestServerAuthCode(activity.getResources().getString(R.string.web_client_id), forceCodeForRefreshToken)
                        .requestEmail()
                        .build();

                googleSignInClient = GoogleSignIn.getClient(activity, googleSignInOptions);
                googleSignInClient.signOut();
            } catch (Exception e) {
                LogHandler.saveLog("sign out from account failed");
            }
        }

        public String handleSignInToPrimaryResult(Intent data){
            String userEmail = "";
            String authCode;
            PrimaryAccountInfo.Tokens tokens = null;
            PrimaryAccountInfo.Storage storage = null;
            try{
                Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);

                userEmail = account.getEmail();
                if (userEmail != null && userEmail.toLowerCase().endsWith("@gmail.com")) {
                    userEmail = account.getEmail();
                    userEmail = userEmail.replace("@gmail.com", "");
                }
                authCode = account.getServerAuthCode();
                tokens = getTokens(authCode);
                storage = getStorage(tokens);
                String[] columnsList = new String[]{"userEmail"};
                List<String[]> userProfileData = MainActivity.dbHelper.getUserProfile(columnsList);
                boolean isInUserProfileData = false;
                for (String[] row : userProfileData) {
                    if (row.length > 0 && row[0] != null && row[0].equals(userEmail)) {
                        PrimaryAccountInfo.Tokens finalTokens = tokens;
                        PrimaryAccountInfo.Storage finalStorage = storage;
                        Map<String, Object> updatedValues = new HashMap<String, Object>(){{
                            put("type", "primary");
                            put("refreshToken", finalTokens.getRefreshToken());
                            put("accessToken", finalTokens.getRefreshToken());
                            put("totalStorage", finalStorage.getTotalStorage());
                            put("usedStorage", finalStorage.getUsedStorage());
                            put("usedInDriveStorage", finalStorage.getUsedInDriveStorage());
                            put("usedInGmailAndPhotosStorage", finalStorage.getUsedInGmailAndPhotosStorage());
                            put("accessToken", finalTokens.getAccessToken());
                        }};
                        MainActivity.dbHelper.updateUserProfileData(userEmail, updatedValues);
                        isInUserProfileData = true;
                    }
                }
                if (!isInUserProfileData){
                    MainActivity.dbHelper.insertUserProfileData(userEmail,"primary",tokens.getRefreshToken(),tokens.getAccessToken(),
                            storage.getTotalStorage(),storage.getUsedStorage(),storage.getUsedInDriveStorage(),storage.getUsedInGmailAndPhotosStorage());
                    runOnUiThread(() -> {
                        LinearLayout primaryAccountsButtonsLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
                        Button newGoogleLoginButton = createPrimaryLoginButton(primaryAccountsButtonsLinearLayout);
                        newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

                    });
                }
            }catch (Exception e){
                LogHandler.saveLog("handle primary sign in result failed: " + e.getLocalizedMessage());
            }
            return userEmail;
        }

        public BackUpAccountInfo handleSignInToBackupResult(Intent data){
            String userEmail = "";
            String authCode;
            PrimaryAccountInfo.Tokens tokens = null;
            PrimaryAccountInfo.Storage storage = null;
            ArrayList<BackUpAccountInfo.MediaItem> mediaItems  = null;
            try{
                Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);

                userEmail = account.getEmail();
                if (userEmail != null && userEmail.toLowerCase().endsWith("@gmail.com")) {
                    userEmail = account.getEmail();
                    userEmail = userEmail.replace("@gmail.com", "");
                }
                authCode = account.getServerAuthCode();
                tokens = getTokens(authCode);
                storage = getStorage(tokens);
                mediaItems = GoogleDrive.getMediaItems(tokens.getAccessToken());

                String[] columnsList = new String[]{"userEmail","type"};
                List<String[]> userProfileData = MainActivity.dbHelper.getUserProfile(columnsList);
                boolean isInUserProfileData = false;

                for (String[] row : userProfileData) {
                    if (row.length > 0 && row[0] != null && row[0].equals(userEmail)) {
                        PrimaryAccountInfo.Tokens finalTokens = tokens;
                        PrimaryAccountInfo.Storage finalStorage = storage;
                        Map<String, Object> updatedValues = new HashMap<String, Object>(){{
                            put("type", "backup");
                            put("refreshToken", finalTokens.getRefreshToken());
                            put("accessToken", finalTokens.getRefreshToken());
                            put("totalStorage", finalStorage.getTotalStorage());
                            put("usedStorage", finalStorage.getUsedStorage());
                            put("usedInDriveStorage", finalStorage.getUsedInDriveStorage());
                            put("usedInGmailAndPhotosStorage", finalStorage.getUsedInGmailAndPhotosStorage());
                            put("accessToken", finalTokens.getAccessToken());
                        }};
                        MainActivity.dbHelper.updateUserProfileData(userEmail,updatedValues);
                        isInUserProfileData = true;
                    }
                }
                System.out.println("user email is : " + userEmail);
                if (!isInUserProfileData){
                    MainActivity.dbHelper.insertUserProfileData(userEmail,"backup",tokens.getRefreshToken(),tokens.getAccessToken(),
                            storage.getTotalStorage(),storage.getUsedStorage(),storage.getUsedInDriveStorage(),storage.getUsedInGmailAndPhotosStorage());

                    for(BackUpAccountInfo.MediaItem mediaItem : mediaItems){
                        Long last_insertId = MainActivity.dbHelper.insertAssetData(mediaItem.getHash());
                        if (last_insertId != -1) {
                            MainActivity.dbHelper.insertIntoDriveTable(last_insertId, mediaItem.getId(), mediaItem.getFileName(),
                                    mediaItem.getHash(), userEmail);
                        } else {
                            LogHandler.saveLog("Failed to insert file into drive table: " + mediaItem.getFileName());
                        }
                    }

                    runOnUiThread(() -> {
                        LinearLayout backupAccountsButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                        Button newGoogleLoginButton = createBackUpLoginButton(backupAccountsButtonsLinearLayout);
                        newGoogleLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

                    });
                }
            }catch (Exception e){
                LogHandler.saveLog("handle back up sign in result failed: " + e.getLocalizedMessage());
            }
            return new BackUpAccountInfo(userEmail, tokens, storage,mediaItems);
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
            newLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D47A1")));
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
                LogHandler.saveLog("Creating a new login button failed");
            }
            return newLoginButton;
        }

        public Button createBackUpLoginButton(LinearLayout linearLayout){
            Button newLoginButton = new Button(activity);
            Drawable loginButtonLeftDrawable = activity.getApplicationContext().getResources()
                    .getDrawable(R.drawable.googledriveimage);
            newLoginButton.setCompoundDrawablesWithIntrinsicBounds
                    (loginButtonLeftDrawable, null, null, null);
            newLoginButton.setText("Add a back up account");
            newLoginButton.setGravity(Gravity.CENTER);
            newLoginButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            newLoginButton.setVisibility(View.VISIBLE);
            newLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#42A5F5")));
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
                LogHandler.saveLog("Creating a new login button failed");
            }
            return newLoginButton;
        }

        private PrimaryAccountInfo.Tokens getTokens(String authCode){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<PrimaryAccountInfo.Tokens> backgroundTokensTask = () -> {
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
                    httpURLConnection.setRequestProperty("Host", "oauth2.googleapis.com");
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
                    }else {
                        LogHandler.saveLog("Getting tokens failed");
                    }
                } catch (Exception e) {
                    LogHandler.saveLog("Getting tokens failed: " + e.getLocalizedMessage());
                }
                return new PrimaryAccountInfo.Tokens(accessToken, refreshToken);
            };
            Future<PrimaryAccountInfo.Tokens> future = executor.submit(backgroundTokensTask);
            PrimaryAccountInfo.Tokens tokens_fromFuture = null;
            try {
                tokens_fromFuture = future.get();
            }catch (Exception e){
                LogHandler.saveLog("failed to get tokens from the future: " + e.getLocalizedMessage());
            }finally {
                executor.shutdown();
            }
            return tokens_fromFuture;
        }

        protected PrimaryAccountInfo.Tokens requestAccessToken(final String refreshToken){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<PrimaryAccountInfo.Tokens> backgroundTokensTask = () -> {
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
                    }else {
                        LogHandler.saveLog("Getting access token failed with response code of " + responseCode);
                    }
                } catch (Exception e) {
                    LogHandler.saveLog("Getting access token failed: " + e.getLocalizedMessage());
                }
                return new PrimaryAccountInfo.Tokens(accessToken, refreshToken);
            };
            Future<PrimaryAccountInfo.Tokens> future = executor.submit(backgroundTokensTask);
            PrimaryAccountInfo.Tokens tokens_fromFuture = null;
            try {
                tokens_fromFuture = future.get();
            }catch (Exception e){
                LogHandler.saveLog("failed to get access token from the future: " + e.getLocalizedMessage());
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

        public PrimaryAccountInfo.Storage getStorage(PrimaryAccountInfo.Tokens tokens){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            String refreshToken = tokens.getRefreshToken();
            String accessToken = tokens.getAccessToken();
            final PrimaryAccountInfo.Storage[] storage = new PrimaryAccountInfo.Storage[1];
            final Double[] totalStorage = new Double[1];
            final Double[] usedStorage = new Double[1];
            final Double[] usedInDriveStorage = new Double[1];

            try{
                Callable<PrimaryAccountInfo.Storage> backgroundTask = () -> {
                    final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
                    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                    HttpRequestInitializer httpRequestInitializer = request -> {
                        request.getHeaders().setAuthorization("Bearer " + accessToken);
                        request.getHeaders().setContentType("application/json");
                    };

                    Drive driveService = new Drive.Builder(netHttpTransport, jsonFactory, httpRequestInitializer)
                            .setApplicationName("cso").build();

                    totalStorage[0] = convertStorageToGigaByte(driveService.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getLimit());

                    usedStorage[0] = convertStorageToGigaByte(driveService.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getUsage());

                    usedInDriveStorage[0] = convertStorageToGigaByte(driveService.about().get()
                            .setFields("user, storageQuota")
                            .execute().getStorageQuota().getUsageInDrive());

                    storage[0] = new PrimaryAccountInfo.Storage(totalStorage[0], usedStorage[0],usedInDriveStorage[0]);
                    LogHandler.saveLog("Account total storage: " + totalStorage[0],false);
                    LogHandler.saveLog("Account used storage: "  + usedStorage[0],false);
                    LogHandler.saveLog("Account used in drive storage: " + usedInDriveStorage[0],false);
                    LogHandler.saveLog("Used in Gmail and Photos is : " + (usedStorage[0] - usedInDriveStorage[0]),false);

                    return storage[0];
                };
                Future<PrimaryAccountInfo.Storage> future = executor.submit(backgroundTask);
                storage[0] = future.get();
            }catch (Exception e){
                LogHandler.saveLog("Failed to get the storage: " + e.getLocalizedMessage());
            }
            executor.shutdown();
            return storage[0];
        }
    }


