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
                String password = Hash.calculateSHA256(earlyPassword, getApplicationContext());
                System.out.println("input userName: " + userName);
                System.out.println("input password: " + earlyPassword + " h-> " + password);
                if (userName == null || password == null) {
                    signInStateTextView.setText("Please Try Again");//Enter your credential");
                    buttonCustomSignIn.setClickable(true);
                    return;
                }
                if (userName.isEmpty()) {
                    signInStateTextView.setText("Enter your username !");
                    buttonCustomSignIn.setClickable(true);
                    return;
                }
                if (earlyPassword.isEmpty()) {
                    signInStateTextView.setText("Enter your password !");
                    buttonCustomSignIn.setClickable(true);
                    return;
                }
                JsonObject profileMapContent = Profile.readProfileMapContent();
                JsonObject userProfileJson = profileMapContent.get("userProfile").getAsJsonObject();
                if (profileMapContent == null || !profileMapContent.has("userProfile")){
                    signInStateTextView.setText("No account was found with this Email !");
                    buttonCustomSignIn.setClickable(true);
                    return;
                }
                if (!userProfileJson.has("userName") || !userProfileJson.has("password")) {
                    signInStateTextView.setText("No username, password was found in this Email !");
                    buttonCustomSignIn.setClickable(true);
                    return;
                }
                runOnUiThread(() -> {
                    String userNameResult = userProfileJson.get("userName").getAsString();
                    String passwordResult = userProfileJson.get("password").getAsString();
                    if (!userNameResult.equals(userName)) {
                        signInStateTextView.setText("Username Not found !");
                        buttonCustomSignIn.setClickable(true);
                        return;
                    }
                    //should compare with signup page
//                    if (earlyPassword.length() < 8){
//                        signInStateTextView.setText("Wrong password Format please check again");
//                        buttonCustomSignIn.setClickable(true);
//                        return;
//                    }
                    if (!passwordResult.equals(password)){
                        signInStateTextView.setText("Wrong password please check again or ... ");
                        buttonCustomSignIn.setClickable(true);
                        return;
                    }
                    Profile.insertProfile(userName, password, getApplicationContext());
                    Profile.insertBackupAccounts(profileMapContent.get("backupAccounts").getAsJsonArray());
                    Profile.insertPrimaryAccounts(profileMapContent.get("primaryAccounts").getAsJsonArray());
                    Activity activity = MainActivity.activity;
                    if (activity != null) {
                        MainActivity.initializeButtons(activity, MainActivity.googleCloud);
                    }
                    finish();
                });
            }
        });
    }
}
