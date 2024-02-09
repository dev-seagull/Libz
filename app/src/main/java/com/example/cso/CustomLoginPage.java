package com.example.cso;

import static com.google.android.material.internal.ContextUtils.getActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;

public class CustomLoginPage extends AppCompatActivity {
    private TextView noAccountSignedUpTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_login_page);
        noAccountSignedUpTextView = findViewById(R.id.noAccountSignedUp);
        noAccountSignedUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomLoginPage.this, CustomSignUpPage.class);
                startActivity(intent);
                finish();
            }
        });


        Button buttonCustomSignIn = findViewById(R.id.buttonCustomSignIn);
        buttonCustomSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonCustomSignIn.setClickable(false);
                noAccountSignedUpTextView.setClickable(false);
                TextView signInStateTextView = findViewById(R.id.signInState);
                signInStateTextView.setText(" Wait... ");
                EditText editTextUsernameSignIn = findViewById(R.id.editTextUsernameSignIn);
                EditText editTextPasswordSignIn = findViewById(R.id.editTextPasswordSignIn);
                String userName = editTextUsernameSignIn.getText().toString();
                String earlyPassword = editTextPasswordSignIn.getText().toString();
                String password = Hash.calculateSHA256(earlyPassword);
                System.out.println("input userName: " + userName);
                System.out.println("input password: " + earlyPassword + " hash-> " + password);
                JsonObject profileMapContent = Profile.readProfileMapContent(1);
                String isCredentialsValid = isCredentialsValid(userName,earlyPassword,password,profileMapContent);
                if (isCredentialsValid.equals("ok")){
                    buttonCustomSignIn.setClickable(true);
                    noAccountSignedUpTextView.setClickable(true);
                    if (Profile.profileMapExists(userName,password)){
                        DBHelper.insertIntoProfile(userName, password);
                        Profile.insertBackupFromMap(profileMapContent.get("backupAccounts").getAsJsonArray());
                        Profile.insertPrimaryFromMap(profileMapContent.get("primaryAccounts").getAsJsonArray());
                        Upgrade.updateProfileIdsInAccounts();
                        Activity activity = MainActivity.activity;
                        if (activity != null) {
                            MainActivity.reInitializeButtons(activity, MainActivity.googleCloud);
                        }
                    }
                }else{
                    signInStateTextView.setText(isCredentialsValid);
                    buttonCustomSignIn.setClickable(true);
                    noAccountSignedUpTextView.setClickable(true);
                }
                finish();
            }
        });
    }

    private String isCredentialsValid(String userName , String earlyPassword,String password,JsonObject profileMapContent){
        if (userName == null || earlyPassword == null) {
            return "Please Try Again";
        }
        if (userName.isEmpty()) {
            return "Enter your username !";
        }
        if (earlyPassword.isEmpty()) {
            return "Enter your password !";
        }
        if (profileMapContent == null || !profileMapContent.has("profile")){
            return "No account was found with this Email !";
        }
        JsonObject profileJson = profileMapContent.get("profile").getAsJsonObject();
        if (!profileJson.has("userName") || !profileJson.has("password")) {
            return "No username, password was found for this Email !";
        }
        String userNameResult = profileJson.get("userName").getAsString();
        String passwordResult = profileJson.get("password").getAsString();
        if (!userNameResult.equals(userName)) {
            return "Username Not found !";
        }
        if (!passwordResult.equals(password)){
            return "Wrong password please check again or ... ";
        }
        return "ok";

    }
}
