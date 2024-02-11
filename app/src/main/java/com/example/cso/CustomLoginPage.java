package com.example.cso;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONObject;

public class CustomLoginPage extends AppCompatActivity {
    private TextView noAccountSignedUpTextView;
    private CheckBox recaptchaCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_login_page);

        recaptchaCheckBox = findViewById(R.id.checkBoxLogin);

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
                String isCredentialsValid = isCredentialsValid(userName,earlyPassword);
                if(isCredentialsValid.equals("ok")){
                    signInStateTextView.setText("Wait...");
                    JsonObject profileMapContent = Profile.readProfileMapContent(1, userName, password);
                    String isProfileJsonValid = isProfileJsonValid(profileMapContent);
                    if (isProfileJsonValid.equals("ok")){
                        buttonCustomSignIn.setClickable(true);
                        noAccountSignedUpTextView.setClickable(true);
                        if (!Profile.profileMapExists(userName,password)){
                            JsonArray profilesArray = profileMapContent.getAsJsonArray("profile");
                            JsonObject profile = profilesArray.get(0).getAsJsonObject();
                            DBHelper.insertIntoProfile(profile.get("userName").getAsString(),
                                    profile.get("password").getAsString(), profile.get("joined").getAsString());
                            Profile.insertBackupFromMap(profileMapContent.get("backupAccounts").getAsJsonArray());
                            Profile.insertPrimaryFromMap(profileMapContent.get("primaryAccounts").getAsJsonArray());
                            Upgrade.updateProfileIdsInAccounts();
                            Activity activity = MainActivity.activity;
                            if (activity != null) {
                                MainActivity.reInitializeButtons(activity, MainActivity.googleCloud);
                                finish();
                            }
                        }else{
                            signInStateTextView.setText("Try again!");
                        }
                    }else{
                        signInStateTextView.setText(isProfileJsonValid);
                        buttonCustomSignIn.setClickable(true);
                        noAccountSignedUpTextView.setClickable(true);
                    }
                }else{
                    signInStateTextView.setText(isCredentialsValid);
                    buttonCustomSignIn.setClickable(true);
                    noAccountSignedUpTextView.setClickable(true);
                }
            }
        });
    }

    private String isCredentialsValid(String userName , String earlyPassword){
        if (userName == null || earlyPassword == null) {
            return "Please Try Again";
        }
        if (userName.isEmpty()) {
            return "Enter your username !";
        }
        if (earlyPassword.isEmpty()) {
            return "Enter your password !";
        }
        if(!recaptchaCheckBox.isChecked()){
            return "Verify you're not a robot!";
        }
        return "ok";
    }

    private String isProfileJsonValid(JsonObject profileMapContent){
        if (profileMapContent == null || !profileMapContent.has("profile")){
            return "No account was found!";
        }
        return "ok";
    }
}
