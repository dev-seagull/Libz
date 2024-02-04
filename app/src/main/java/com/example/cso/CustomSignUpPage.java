package com.example.cso;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;


public class CustomSignUpPage extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks {
    private TextView alreadyHaveAccountTextView;
    private CheckBox recaptchaCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_signup_page);

        recaptchaCheckBox = findViewById(R.id.chechBoxSignUp);

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(SafetyNet.API)
                .addConnectionCallbacks(CustomSignUpPage.this)
                .build();
        googleApiClient.connect();
        recaptchaCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recaptchaCheckBox.isChecked()){
                    SafetyNet.SafetyNetApi.verifyWithRecaptcha(googleApiClient, "6LdddGYpAAAAACYvp5CkJwCRvyuG77FPrUGIGLjU")
                            .setResultCallback(new ResultCallback<SafetyNetApi.RecaptchaTokenResult>() {
                                @Override
                                public void onResult(@NonNull SafetyNetApi.RecaptchaTokenResult recaptchaTokenResult) {
                                    Status status = recaptchaTokenResult.getStatus();
                                    if((status != null) && status.isSuccess()){
                                        System.out.println("success");
                                        recaptchaCheckBox.setTextColor(Color.GREEN);
                                    }
                                }
                            });
                }else{
                    recaptchaCheckBox.setTextColor(Color.BLACK);
                }
            }
        });



        alreadyHaveAccountTextView = findViewById(R.id.alreadyHaveAccount);
        alreadyHaveAccountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomSignUpPage.this, CustomLoginPage.class);
                startActivity(intent);
                finish();
            }
        });


        Button buttonCustomSignUp = findViewById(R.id.buttonCustomSignUp);
        buttonCustomSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonCustomSignUp.setClickable(false);
                alreadyHaveAccountTextView.setClickable(false);
                TextView signUpStateTextView = findViewById(R.id.signUpState);
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        signUpStateTextView.setText(" Wait... ");
                    }
                }, 10000);

                EditText editTextUsernameSignUp = findViewById(R.id.editTextUsernameSignUp);
                EditText editTextPasswordSignUp = findViewById(R.id.editTextPasswordSignUp);
                String userName = editTextUsernameSignUp.getText().toString();
                String password = editTextPasswordSignUp.getText().toString();
                if(userName != null && !userName.isEmpty() && !password.isEmpty() && password != null){
                    String isCredentialSecure = isCredentialSecure(password, userName);
                    if(isCredentialSecure.equals("ok")){
                        DBHelper.insertIntoProfile(userName, password);
                        MainActivity.dbHelper.backUpProfileMap();

                        buttonCustomSignUp.setClickable(true);
                        alreadyHaveAccountTextView.setClickable(true);
                        finish();
                        view.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                buttonCustomSignUp.setClickable(true);
                            }
                        }, 5000);
                    }else{
                        signUpStateTextView.setText(isCredentialSecure);
                    }
                }else{
                    buttonCustomSignUp.setClickable(true);
                    alreadyHaveAccountTextView.setClickable(true);
                    if(userName.isEmpty() | userName == null){
                        signUpStateTextView.setText("Enter your username!");
                    }else if(password.isEmpty() | password == null) {
                        signUpStateTextView.setText("Enter your password!");
                    }
                }
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        buttonCustomSignUp.setClickable(true);
                    }
                }, 5000);

            }
        });
    }
    private String isCredentialSecure(String password, String userName) {
        if (userName.length() <= 3) {
            return "Username must be more than 3 characters.";
        }
        if (!userName.matches(".*[a-z].*")) {
            return "Username must contain at least one lowercase letter.";
        }
        if(password.length() <= 8){
            return  "Password must be more than 8 characters.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter.";
        }

        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one capital letter.";
        }
        if (containsNumber(password) == false) {
            return "Password must contain at least one number.";
        }
        if (containsSpecialCharacters(password) == false) {
            return "Password must contain at least one special character (# or !).";
        }
        return "ok";
    }

    private boolean containsNumber(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSpecialCharacters(String password) {
        return password.matches(".*[#\\!_\\-@$%^&*()+\\-?<>{}\\[\\]].*");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
