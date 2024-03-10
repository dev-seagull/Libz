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
    import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
    import com.google.api.client.http.HttpRequestInitializer;
    import com.google.api.client.http.javanet.NetHttpTransport;
    import com.google.api.client.json.JsonFactory;
    import com.google.api.client.json.gson.GsonFactory;
    import com.google.api.services.drive.Drive;

    import org.json.JSONObject;

    import java.io.BufferedReader;
    import java.io.IOException;
    import java.io.InputStreamReader;
    import java.io.OutputStream;
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.nio.charset.StandardCharsets;
    import java.text.DecimalFormat;
    import java.util.ArrayList;
    import java.util.List;
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

//        public boolean signOut(String userEmail){
//            String accessToken = MainActivity.dbHelper.getAccessToken(userEmail);
//            HttpTransport httpTransport = new NetHttpTransport();
//            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
//            try {
//                GenericUrl revokeUrl = new GenericUrl("https://accounts.google.com/o/oauth2/revoke?token=" + accessToken);
//                HttpRequest revokeRequest = requestFactory.buildGetRequest(revokeUrl);
//                HttpResponse revokeResponse = revokeRequest.execute();
//                revokeResponse.disconnect();
//                boolean isAccessTokenValid = isAccessTokenValid(accessToken);
//                if (!isAccessTokenValid) {
//                    System.out.println("Tokens revoked successfully.");
//                    httpTransport.shutdown();
//                    return true;
//                }
//            } catch (IOException e) {
//                LogHandler.saveLog("Error revoking tokens: " + e.getLocalizedMessage());
//            }finally {
//                try {
//                    httpTransport.shutdown();
//                }catch (Exception e){
//                    LogHandler.saveLog("Failed to shutdown http transport: " + e.getLocalizedMessage());
//                }
//            }
//            return false;
//        }

        public boolean signOut(String userEmail) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<Boolean> callableTask = () -> {
                String accessToken = MainActivity.dbHelper.getAccessToken(userEmail);
                try {
                    String revokeUrl = "https://accounts.google.com/o/oauth2/revoke";
                    URL url = new URL(revokeUrl);

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    if (accessToken != null) {
                        String requestBody = "token=" + accessToken;
                        try (OutputStream os = connection.getOutputStream()) {
                            byte[] input = requestBody.getBytes("utf-8");
                            os.write(input, 0, input.length);
                        }
                        int responseCode = connection.getResponseCode();
                    }
                    connection.disconnect();
                    boolean isAccessTokenValid = isAccessTokenValid(accessToken);
                    if (!isAccessTokenValid) {
                        System.out.println("Tokens revoked successfully.");
                        return true;
                    }else{
                        System.out.println("Tokens revoked not successfully.");
                    }
                } catch (IOException e) {
                    LogHandler.saveLog("Error revoking tokens: " + e.getLocalizedMessage());
                }
                return false;
            };
            Future<Boolean> future = executor.submit(callableTask);
            Boolean isSignedOut = false;
            try{
                isSignedOut = future.get();
            }catch (Exception e){
                LogHandler.saveLog("Failed to get the sign out future: " + e.getLocalizedMessage());
            }
            return isSignedOut;
        }


        private static boolean isAccessTokenValid(String accessToken) throws IOException {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<Boolean> callableTask = () -> {
                String tokenInfoUrl = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken;
                System.out.println("tokenInfoUrl " + tokenInfoUrl);
                URL url = new URL(tokenInfoUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("access token validity " + response.toString());
                    return !response.toString().toLowerCase().contains("error");
                } finally {
                    connection.disconnect();
                }
            };
            Future<Boolean> future = executor.submit(callableTask);
            Boolean isValid = false;
            try{
                isValid = future.get();
            }catch (Exception e){
                LogHandler.saveLog("Failed to check validity from future: " + e.getLocalizedMessage());
            }
            return isValid;
        }


        public class signInResult{
            private String userEmail;
            private boolean isHandled;
            private GoogleCloud.Tokens tokens;
            private PhotosAccountInfo.Storage storage;

            private ArrayList<DriveAccountInfo.MediaItem> mediaItems;

            private signInResult(String userEmail, boolean isHandled, boolean isInAccounts,
                                 GoogleCloud.Tokens tokens, PhotosAccountInfo.Storage storage, ArrayList<DriveAccountInfo.MediaItem> mediaItems) {
                this.userEmail = userEmail;
                this.isHandled = isHandled;
                this.tokens = tokens;
                this.storage = storage;
                this.mediaItems = mediaItems;
            }

            public String getUserEmail() {return userEmail;}
            public boolean getHandleStatus() {return isHandled;}

            public GoogleCloud.Tokens getTokens() {return tokens;}
            public PhotosAccountInfo.Storage getStorage() {return storage;}

            public ArrayList<DriveAccountInfo.MediaItem> getMediaItems() {return mediaItems;}



        }

        public signInResult handleSignInToPrimaryResult(Intent data){
            final boolean[] isHandled = {false};
            boolean isInAccounts = false;
            String userEmail = "";
            String authCode;
            GoogleCloud.Tokens tokens = null;
            PhotosAccountInfo.Storage storage = null;
            try{
                Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);
                userEmail = account.getEmail();
                if (userEmail != null && userEmail.toLowerCase().endsWith("@gmail.com")) {
                    userEmail = account.getEmail();
                    userEmail = userEmail.replace("@gmail.com", "");
                }
                List<String[]> userAccounts = MainActivity.dbHelper.getAccounts(new String[]{"userEmail","type"});
                for (String[] row : userAccounts) {
                    if (row.length > 0 && row[0] != null && row[0].equals(userEmail)) {
                        isInAccounts = true;
                        runOnUiThread(() -> {
                            CharSequence text = "This Account Already Exists !";
                            Toast.makeText(MainActivity.activity, text, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                if (isInAccounts == false){
                    authCode = account.getServerAuthCode();
                    tokens = getTokens(authCode);
                    storage = getStorage(tokens);
                    if(userEmail!= null && tokens.getRefreshToken() != null && tokens.getAccessToken() != null){
                        isHandled[0] = true;
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("handle primary sign in result failed: " + e.getLocalizedMessage());
            }
            return new signInResult(userEmail, isHandled[0], isInAccounts, tokens, storage,new ArrayList<>());
        }

        public signInResult handleSignInToBackupResult(Intent data){
            final boolean[] isHandled = {false};
            String userEmail = "";
            String authCode;
            boolean isInAccounts = false;
            GoogleCloud.Tokens tokens = null;
            PhotosAccountInfo.Storage storage = null;
            ArrayList<DriveAccountInfo.MediaItem> mediaItems  = null;
            try{
                Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);

                userEmail = account.getEmail();
                if (userEmail != null && userEmail.toLowerCase().endsWith("@gmail.com")) {
                    userEmail = account.getEmail();
                    userEmail = userEmail.replace("@gmail.com", "");
                }

                String[] columnsList = new String[]{"userEmail","type"};
                List<String[]> accounts_rows = MainActivity.dbHelper.getAccounts(columnsList);
                isInAccounts = false;
                for (String[] row : accounts_rows) {
                    if (row.length > 0 && row[0] != null && row[0].equals(userEmail)) {
                        isInAccounts = true;
                        runOnUiThread(() -> {
                            CharSequence text = "This Account Already Exists !";
                            Toast.makeText(MainActivity.activity, text, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                if (isInAccounts == false){
                    authCode = account.getServerAuthCode();
                    tokens = getTokens(authCode);
                    storage = getStorage(tokens);
                    mediaItems = GoogleDrive.getMediaItems(tokens.getAccessToken());
                    if (userEmail != null && tokens.getRefreshToken() != null && tokens.getAccessToken() != null) {
                        isHandled[0] = true;
                    }
                }
            }catch (Exception e){
                LogHandler.saveLog("handle back up sign in result failed: " + e.getLocalizedMessage());
            }
            return new signInResult(userEmail, isHandled[0], isInAccounts, tokens, storage,mediaItems);
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
                return new GoogleCloud.Tokens(accessToken, refreshToken);
            };
            Future<GoogleCloud.Tokens> future = executor.submit(backgroundTokensTask);
            GoogleCloud.Tokens tokens_fromFuture = null;
            try {
                tokens_fromFuture = future.get();
            }catch (Exception e){
                LogHandler.saveLog("failed to get tokens from the future: " + e.getLocalizedMessage());
            }finally {
                executor.shutdown();
            }
            return tokens_fromFuture;
        }

        protected Tokens requestAccessToken(final String refreshToken){
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
                    }else {
                        LogHandler.saveLog("Getting access token failed with response code of " + responseCode);
                    }
                } catch (Exception e) {
                    LogHandler.saveLog("Getting access token failed: " + e.getLocalizedMessage());
                }
                return new GoogleCloud.Tokens(accessToken, refreshToken);
            };
            Future<GoogleCloud.Tokens> future = executor.submit(backgroundTokensTask);
            GoogleCloud.Tokens tokens_fromFuture = null;
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

        public PhotosAccountInfo.Storage getStorage(GoogleCloud.Tokens tokens){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            String refreshToken = tokens.getRefreshToken();
            String accessToken = tokens.getAccessToken();
            final PhotosAccountInfo.Storage[] storage = new PhotosAccountInfo.Storage[1];
            final Double[] totalStorage = new Double[1];
            final Double[] usedStorage = new Double[1];
            final Double[] usedInDriveStorage = new Double[1];

            try{
                Callable<PhotosAccountInfo.Storage> backgroundTask = () -> {
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

                    storage[0] = new PhotosAccountInfo.Storage(totalStorage[0], usedStorage[0],usedInDriveStorage[0]);
                    LogHandler.saveLog("Account total storage: " + totalStorage[0],false);
                    LogHandler.saveLog("Account used storage: "  + usedStorage[0],false);
                    LogHandler.saveLog("Account used in drive storage: " + usedInDriveStorage[0],false);
                    LogHandler.saveLog("Used in Gmail and Photos is : " + (usedStorage[0] - usedInDriveStorage[0]),false);

                    return storage[0];
                };
                Future<PhotosAccountInfo.Storage> future = executor.submit(backgroundTask);
                storage[0] = future.get();
            }catch (Exception e){
                LogHandler.saveLog("Failed to get the storage: " + e.getLocalizedMessage());
            }
            executor.shutdown();
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

    }



