    package com.example.cso;

    import android.app.Activity;
    import android.content.Intent;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.graphics.drawable.Drawable;
    import android.os.AsyncTask;
    import android.os.Build;
    import android.os.Handler;
    import android.provider.CalendarContract;
    import android.util.TypedValue;
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
    import com.google.api.services.drive.model.File;
    import com.google.api.services.drive.model.FileList;

    import org.json.JSONObject;

    import java.io.BufferedReader;
    import java.io.InputStreamReader;
    import java.io.OutputStream;
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.nio.charset.StandardCharsets;
    import java.text.DecimalFormat;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.concurrent.Callable;
    import java.util.concurrent.CompletableFuture;
    import java.util.concurrent.ExecutionException;
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
                        .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary.readonly"),
                                new Scope("https://www.googleapis.com/auth/drive"),
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
                })
                ;
                LogHandler.saveLog("login to Account");

            } catch (Exception e){
                //Toast.makeText(activity,"Login failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }


        public PrimaryAccountInfo handleSignInToPrimaryResult(Intent data){
            String userEmail = "";
            String authCode = "";
            PrimaryAccountInfo.Tokens tokens = null;
            PrimaryAccountInfo.Storage storage = null;
            ArrayList<GooglePhotos.MediaItem> mediaItems = null;

            try{
                Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);

                userEmail = account.getEmail();
                if(userEmail.toLowerCase().endsWith("@gmail.com")){
                    userEmail = account.getEmail();
                    userEmail = userEmail.replace("@gmail.com","");
                }
                System.out.println("the user email is: " + userEmail);
                LogHandler.saveLog("User Email is " + userEmail);
                authCode = account.getServerAuthCode();
                GetTokensAsyncTask getTokensAsyncTask = new GetTokensAsyncTask();
                tokens = getTokensAsyncTask.execute(authCode).get();
                storage = getStorage(tokens);
                System.out.println("usages :" + storage.getTotalStorage() + " and "+  storage.getUsedStorage() +" and "+ storage.getUsedInDriveStorage() +" and "+ storage.getUsedInGmailAndPhotosStorage());
                LogHandler.saveLog("Total Storage is : " + storage.getTotalStorage() + " Total Usage is "+  storage.getUsedStorage() +"\n Used in Drive is : "+ storage.getUsedInDriveStorage() +" Used in Gmail and Photos is : "+ storage.getUsedInGmailAndPhotosStorage());
                mediaItems = GooglePhotos.getGooglePhotosMediaItems(tokens);;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout primaryAccountsButtonsLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
                        createPrimaryLoginButton(primaryAccountsButtonsLinearLayout);
                    }
                });
                System.out.println(mediaItems.size() + "File detected in Photos");
                LogHandler.saveLog(mediaItems.size() + "File detected in Photos");
            }catch (Exception e){
                System.out.println("catch login error: " + e.getMessage());
                //Toast.makeText(activity,"Login failed: " + e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
            }

            System.out.println("done with login");
            return new PrimaryAccountInfo(userEmail, tokens, storage, mediaItems);
        }


        public BackUpAccountInfo handleSignInToBackupResult(Intent data){
            String userEmail = "";
            String authCode = "";
            PrimaryAccountInfo.Tokens tokens = null;
            PrimaryAccountInfo.Storage storage = null;
            ArrayList<BackUpAccountInfo.MediaItem> mediaItems  = null;
            try{
                Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);

                userEmail = account.getEmail();
                if(userEmail.toLowerCase().endsWith("@gmail.com")){
                    userEmail = account.getEmail();
                    userEmail = userEmail.replace("@gmail.com","");
                }
                System.out.println("the user email is: " + userEmail);
                LogHandler.saveLog("Backup Email is : " + userEmail);
                authCode = account.getServerAuthCode();
                //here you should get the tokens, storage and display it then using chart, gphotos files,
                // and also drive files?
                // you should add more background tasks to do that
                GetTokensAsyncTask getTokensAsyncTask = new GetTokensAsyncTask();
                tokens = getTokensAsyncTask.execute(authCode).get();
                storage = getStorage(tokens);
                mediaItems = getMediaItems(tokens);
                LogHandler.saveLog("There is " + mediaItems.size() + " in Backup Account");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout backupAccountsButtonsLinearLayout = activity.findViewById(R.id.backUpAccountsButtons);
                        createBackUpLoginButton(backupAccountsButtonsLinearLayout);
                    }
                });
            }catch (Exception e){
                //Toast.makeText(activity,"Login failed: " + e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
            }
            return new BackUpAccountInfo(userEmail, tokens, storage,mediaItems);
        }


        public void createPrimaryLoginButton(LinearLayout linearLayout){
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
        }

        public void createBackUpLoginButton(LinearLayout linearLayout){
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
        }


        public class GetTokensAsyncTask extends AsyncTask<String, Void, PrimaryAccountInfo.Tokens> {
            public GetTokensAsyncTask() {
            }

            @Override
            protected PrimaryAccountInfo.Tokens doInBackground(String... params) {
                if (params.length == 0) {
                    return null;
                }

                String authCode = params[0];

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
                        String accessToken = responseJSONObject.getString("access_token");
                        String refreshToken = responseJSONObject.getString("refresh_token");

                        return new PrimaryAccountInfo.Tokens(accessToken, refreshToken);
                    } else {
                        // Handle the case where the HTTP response is not OK.
                        // You can return null or throw an exception as needed.
                        return null;
                    }
                } catch (Exception e) {
                    // Handle exceptions here (e.g., log or show a message).
                    return null;
                }
            }
        }

        public static String getMemeType(String fileName){
            int dotIndex = fileName.lastIndexOf(".");
            String memeType="";
            if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
                memeType = fileName.substring(dotIndex + 1);
            }
            return memeType;
        }

        public ArrayList<BackUpAccountInfo.MediaItem> getMediaItems(PrimaryAccountInfo.Tokens tokens) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            String refreshToken = tokens.getRefreshToken();
            String accessToken = tokens.getAccessToken();
            final ArrayList<BackUpAccountInfo.MediaItem> mediaItems = new ArrayList<>();
            Callable<ArrayList<BackUpAccountInfo.MediaItem>> backgroundTask = () -> {
                try {
                    final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
                    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                    HttpRequestInitializer httpRequestInitializer = request -> {
                        request.getHeaders().setAuthorization("Bearer " + accessToken);
                        request.getHeaders().setContentType("application/json");
                    };
                    Drive driveService = new Drive.Builder(netHttpTransport, jsonFactory, httpRequestInitializer)
                            .setApplicationName("cso").build();
                    FileList result = driveService.files().list()
                            .setFields("files(id, name, sha256Checksum)")
                            .execute();
                    List<File> files = result.getFiles();
                    if (files != null && !files.isEmpty()) {
                        for (File file : files) {
                            if (GooglePhotos.isVideo(getMemeType(file.getName())) |
                                    GooglePhotos.isImage(getMemeType(file.getName()))){
                                BackUpAccountInfo.MediaItem mediaItem = new BackUpAccountInfo.MediaItem(file.getName(),
                                        file.getSha256Checksum().toLowerCase(), file.getId());
                                mediaItems.add(mediaItem);
                                LogHandler.saveLog(mediaItem.getFileName() + "detected in Backup Account");
                            }

                        }
                    } else {
                        System.out.println("No files found in Google Drive.");
                    }

                    return mediaItems;
                }catch (Exception e) {
                    System.out.println(e.getLocalizedMessage());
                }
                return mediaItems;
            };
            Future<ArrayList<BackUpAccountInfo.MediaItem>> future = executor.submit(backgroundTask);

            try {
                ArrayList<BackUpAccountInfo.MediaItem> uploadFileIDs_fromFuture = future.get();
                System.out.println(uploadFileIDs_fromFuture.size());
                System.out.println("future is completed ");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                System.out.println("finished");
                executor.shutdown();
            }
            return mediaItems;
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


                    return storage[0];
                };

                Future<PrimaryAccountInfo.Storage> future = executor.submit(backgroundTask);
                storage[0] = future.get();
            }catch (Exception e){
               // Toast.makeText(activity,"Login failed: "+ e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            executor.shutdown();
            return  storage[0];
        }
    }


