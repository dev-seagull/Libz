package com.example.cso;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {




    private static final int RC_SIGN_IN = 1001;

    private GoogleSignInClient googleSignInClient;
    TextView textViewAccessToken;
    String accessToken;
    ArrayList<String> baseUrls = new ArrayList<String>();;
    int Dcounter=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button googlephoto = findViewById(R.id.buttonphoto);
        Button downloadbtn = findViewById(R.id.buttonDownload);
        Button uploadButton = findViewById(R.id.uploadButton);
        textViewAccessToken = findViewById(R.id.accesstoken);


        btnLogin.setOnClickListener(v -> signIn());

        googlephoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GooglePhotosRequestTask().execute();
            }
        });

        downloadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GooglePhotosDownlaod().execute();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GooglePhotosUpload().execute();
            }
        });




        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary.readonly"))
                .requestServerAuthCode(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {

                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);

                String authCode = account.getServerAuthCode();
                new TokenRequestTask().execute(authCode);

            } catch (ApiException e) {
                Toast.makeText(this, "Sign-in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }




    private class TokenRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String authCode = params[0];
            int responseCode = 0;
            String  response= "";

            try {
                URL url = null;
                try {
                    url = new URL("https://oauth2.googleapis.com/token");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) url.openConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    connection.setRequestMethod("POST");
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }

                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Host", "oauth2.googleapis.com");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                String requestBody = "code=" + authCode +
                        "&client_id="+ R.string.client_id+
                        "&client_secret=" + R.string.client_secret+
                        "&grant_type=authorization_code";

                byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);
                connection.setRequestProperty("Content-Length", String.valueOf(postData.length));

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(postData);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    responseCode = connection.getResponseCode();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder responseBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    }
                    response = responseBuilder.toString();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            JSONObject jsonResponse = null;
            try {
                jsonResponse = new JSONObject(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                accessToken = jsonResponse.getString("access_token");
                textViewAccessToken.setText(accessToken);
                //new GooglePhotosTask().execute(accessToken);



            } catch (JSONException e) {
                e.printStackTrace();
            }

            Toast.makeText(MainActivity.this, response, Toast.LENGTH_LONG).show();
        }

    }

    private class GooglePhotosRequestTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {

            try {
                // Create the URL object with the API endpoint
                URL url = new URL("https://photoslibrary.googleapis.com/v1/mediaItems");

                // Create the HttpURLConnection object
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set the request method
                connection.setRequestMethod("GET");

                // Set the request headers
                connection.setRequestProperty("Content-type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);

                // Send the request
                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();

                    // Parse the response JSON
                    String jsonResponse = response.toString();
                    // Extract the first media item content
                    JSONObject responseJson = new JSONObject(jsonResponse);
                    JSONArray mediaItems = responseJson.getJSONArray("mediaItems");
                    StringBuilder filenames = new StringBuilder();
                    for (int i = 0; i < mediaItems.length(); i++) {
                        JSONObject mediaItem = mediaItems.getJSONObject(i);
                        String filename = mediaItem.getString("filename");
                        String baseUrl = mediaItem.getString("baseUrl");
                        baseUrls.add(baseUrl);
                        filenames.append(filename).append("\n");
                    }


                    return String.valueOf(mediaItems.length())+" "+ filenames.toString();
                }

                // Close the connection
                connection.disconnect();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String response) {
            if (response != null) {
                textViewAccessToken.setText(response);
                Toast.makeText(MainActivity.this, response, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Failed to retrieve data", Toast.LENGTH_LONG).show();
            }
        }
    }


    private class GooglePhotosDownlaod extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {

            try {
                // Create the URL object with the API endpoint
                for(String baseUrl: baseUrls){
                    URL url = new URL(baseUrl+"=d");

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    // Set the request method
                    connection.setRequestMethod("GET");

                    //BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    //StringBuilder response = new StringBuilder();
                    //String line;

                    //while ((line = reader.readLine()) != null) {
                    //    response.append(line);
                    //}

                    //reader.close();

                    // Parse the response JSON
                    //String jsonResponse = response.toString();

                    int responseCode = connection.getResponseCode();
                    System.out.println(responseCode);
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                        String destinationFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso";
                        File folder = new File(destinationFolder);
                        if (!folder.exists()) {
                            boolean folderCreated = folder.mkdirs();
                            if (!folderCreated) {
                                // Failed to create the folder, handle the error
                                textViewAccessToken.setText("couldn't create directory");
                            }

                        }
                        String fileName = ("filename"+Dcounter+".jpg").substring(("filename"+Dcounter+".jpg").lastIndexOf("/") + 1);
                        //System.out.println(fileName);
                        String filePath = destinationFolder + File.separator + fileName;
                        //System.out.println(filePath);

                        OutputStream outputStream = null;
                        try {
                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso"+File.separator + "picture"+Dcounter+".jpg");
                            System.out.println(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "cso"+File.separator + "picture"+Dcounter+".jpg");
                            file.createNewFile();
                            outputStream = new FileOutputStream(file);
                            //outputStream.write(jsonResponse.getBytes());

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                               Log.d("Write", "Bytes written: " + bytesRead);
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            textViewAccessToken.setText("Download completed");
                        } catch (IOException e) {
                            e.printStackTrace();
                            textViewAccessToken.setText("Failed to save the file");
                        } finally {
                            try {
                                if (outputStream != null) {
                                    System.out.println("it is not null");
                                    outputStream.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        outputStream.close();

                        inputStream.close();
                        textViewAccessToken.setText("Download completed");
                    }
                    connection.disconnect();
                    Dcounter = Dcounter + 1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String response) {
           textViewAccessToken.setText(Dcounter + " files were downloaded");
        }
    }

    private class GooglePhotosUpload extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            return null;
        }

        protected void onPostExecute(String response) {

        }

    }



}