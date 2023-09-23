    package com.example.cso;

    import android.app.Activity;
    import android.content.Intent;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.util.TypedValue;
    import android.view.View;
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
    import com.google.api.client.http.javanet.NetHttpTransport;
    import com.google.api.client.json.JsonFactory;
    import com.google.api.client.json.gson.GsonFactory;

    import org.json.JSONObject;

    import java.io.BufferedReader;
    import java.io.InputStreamReader;
    import java.io.OutputStream;
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.nio.charset.StandardCharsets;
    import java.util.concurrent.Callable;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    import java.util.concurrent.Future;


    public class GoogleCloud extends AppCompatActivity {
        private final Activity activity;
        private GoogleSignInClient googleSignInClient;

        String userEmail = "";
        boolean firstLoginButton = true;

        public GoogleCloud(FragmentActivity activity){
            this.activity = activity;
        }


        public void signInToGoogleCloud(ActivityResultLauncher<Intent> signInLauncher) {
            boolean forceCodeForRefreshToken = true;
            final Button finalLoginButton;

            try {
                        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary.readonly"),
                                new Scope("https://www.googleapis.com/auth/drive.readonly"),
                                new Scope("https://www.googleapis.com/auth/photoslibrary.appendonly"),
                                new Scope("https://www.googleapis.com/auth/drive.file"))
                        .requestServerAuthCode(activity.getResources().getString(R.string.web_client_id), forceCodeForRefreshToken)
                        .requestEmail()
                        .build();

                googleSignInClient = GoogleSignIn.getClient(activity, googleSignInOptions);

                googleSignInClient.signOut().addOnCompleteListener(task -> {
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    signInLauncher.launch(signInIntent);
                    //till here now try to handle result


                    LinearLayout primaryAccountsButtonsLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
                });

            } catch (Exception e){
                Toast.makeText(activity,"Login failed: "+ e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }


        public PrimaryAccountInfo handleSignInResult(Intent data){
            String userEmail = "";
            String authCode = "";
            PrimaryAccountInfo.Tokens tokens = null;
            Button finalLoginButton = null;

            try{
                Task<GoogleSignInAccount> googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = googleSignInTask.getResult(ApiException.class);

                userEmail = account.getEmail();
                if(userEmail.toLowerCase().endsWith("@gmail.com")){
                    userEmail = account.getEmail();
                    userEmail = userEmail.replace("@gmail.com","");
                }

                authCode = account.getServerAuthCode();
                //here you should get the tokens, storage and display it then using chart, gphotos files,
                // and also drive files?
                // you should add more background tasks to do that
                tokens = getTokens(authCode); // done with this

                LinearLayout primaryAccountsButtonsLinearLayout = activity.findViewById(R.id.primaryAccountsButtons);
                createLoginButton(primaryAccountsButtonsLinearLayout);

            }catch (Exception e){
                Toast.makeText(activity,"Login failed: " + e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }

            return new PrimaryAccountInfo(userEmail, authCode);
        }


        public void createLoginButton(LinearLayout linearLayout){
            Button newLoginButton = new Button(activity);
            newLoginButton.setText("Add a primary account");
            newLoginButton.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            layoutParams.setMargins(16,0,0,0);
            newLoginButton.setLayoutParams(layoutParams);
            newLoginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D47A1")));
            newLoginButton.setPadding(0,0,70,0);
            newLoginButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP,18);

            if(linearLayout != null){
                linearLayout.addView(newLoginButton);
            }else{
                Toast.makeText(activity,"Creating a new login button failed",
                        Toast.LENGTH_SHORT).show();
            }
        }


        public PrimaryAccountInfo.Tokens getTokens(String authCode){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            final PrimaryAccountInfo.Tokens[] tokens = new PrimaryAccountInfo.Tokens[1];
            final int[] responseCode = new int[1];
            final String[] response = new String[1];
            final JSONObject[] responseJSONObject = new JSONObject[1];
            final String[] accessToken = new String[1];
            final String[] refreshToken = new String[1];

            try{
                Callable<PrimaryAccountInfo.Tokens> backgroundTask = () -> {

                    try{
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
                        responseCode[0] = httpURLConnection.getResponseCode();

                        if(responseCode[0] == HttpURLConnection.HTTP_OK){
                            StringBuilder responseBuilder = new StringBuilder();
                            BufferedReader bufferedReader = new BufferedReader(
                                    new InputStreamReader(httpURLConnection.getInputStream())
                            );
                            String line;
                            while ((line = bufferedReader.readLine()) != null){
                                responseBuilder.append(line);
                            }

                            response[0] = responseBuilder.toString();
                            responseJSONObject[0] =  new JSONObject(response[0]);
                            accessToken[0] = responseJSONObject[0].getString("access_token");
                            refreshToken[0] = responseJSONObject[0].getString("refresh_token");

                        }
                        else{
                            Toast.makeText(activity,"Login failed", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        //Toast.makeText(activity,"Login failed:" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        System.out.println("Login failed: " + e.getLocalizedMessage());
                    }

                    tokens[0] = new PrimaryAccountInfo.Tokens(accessToken[0], refreshToken[0]);
                    return tokens[0];
                };

                Future<PrimaryAccountInfo.Tokens> future = executor.submit(backgroundTask);
                tokens[0] = future.get();
            }catch (Exception e){
                System.out.println(e.getLocalizedMessage());
            }

            return tokens[0];
        }


        public PrimaryAccountInfo.Tokens getStorage(PrimaryAccountInfo.Tokens tokens){
            ExecutorService executor = Executors.newSingleThreadExecutor();
            String refreshToken = tokens.getRefreshToken();
            String accessToken = tokens.getAccessToken();

            try{
                Callable<PrimaryAccountInfo.Tokens> backgroundTask = () -> {

                    final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
                    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    return null;
                };

                Future<PrimaryAccountInfo.Tokens> future = executor.submit(backgroundTask);
                //tokens[0] = future.get();
            }catch (Exception e){
                System.out.println(e.getLocalizedMessage());
            }

           // return tokens[0];
            return  null;
        }


    }


