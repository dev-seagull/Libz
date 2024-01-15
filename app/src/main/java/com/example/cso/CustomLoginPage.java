package com.example.cso;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

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
                String password = Hash.calculateSHA256(editTextPasswordSignIn.getText().toString(), getApplicationContext());
                System.out.println("input userName: " + userName);
                System.out.println("input password: " + password);

                if(userName != null && password != null){
                    if (!userName.isEmpty() && !password.isEmpty() ){
                        runOnUiThread(() -> {
                            String userNameResult = null;
                            String passResult = null ;
                            List<String> auth = MainActivity.dbHelper.readProfile();
                            if (auth != null) {
                                if (!auth.isEmpty()){
                                    userNameResult = auth.get(0);
                                    passResult = auth.get(1);
                                }else {
                                    System.out.println("auth is empty + auth.isEmpty() : " + auth.isEmpty());
                                }
                            }else {
                                System.out.println("auth is null  : ");
                            }
                            if (!userNameResult.isEmpty() && userNameResult != null &&
                                    !passResult.isEmpty() && passResult != null) {
                                buttonCustomSignIn.setClickable(true);
                                noAccountSignedUpTextView.setClickable(true);
                                System.out.println("i'm here 1");
                                if(userNameResult.equals(userName)){
                                    System.out.println("i'm here 2");
                                    if(passResult.equals(password)){
                                        Profile.insertProfile(userName, password, getApplicationContext());
                                        finish();
                                    }else{
                                        buttonCustomSignIn.setClickable(true);
                                        noAccountSignedUpTextView.setClickable(true);
                                        signInStateTextView.setText("Wrong password!");
                                    }
                                }else{
                                    buttonCustomSignIn.setClickable(true);
                                    noAccountSignedUpTextView.setClickable(true);
                                    signInStateTextView.setText("Wrong Username!");
                                }
                            }else{
                                buttonCustomSignIn.setClickable(true);
                                noAccountSignedUpTextView.setClickable(true);
                                signInStateTextView.setText("No account was found with this username and password!");
                            }
                        });
                        if(userName.isEmpty() | userName == null){
                            buttonCustomSignIn.setClickable(true);
                            noAccountSignedUpTextView.setClickable(true);
                            signInStateTextView.setText("Enter your username!");
                        }else if(password.isEmpty() | password == null) {
                            buttonCustomSignIn.setClickable(true);
                            noAccountSignedUpTextView.setClickable(true);
                            signInStateTextView.setText("Enter your password!");
                        }
                    }else{
                        buttonCustomSignIn.setClickable(true);
                        noAccountSignedUpTextView.setClickable(true);
                        if(userName.isEmpty()){
                            signInStateTextView.setText("Enter your username!");
                        }else if(password.isEmpty()) {
                            signInStateTextView.setText("Enter your password!");
                        }
                    }
                }else{
                    buttonCustomSignIn.setClickable(true);
                    noAccountSignedUpTextView.setClickable(true);
                    if(userName == null){
                        signInStateTextView.setText("Enter your username!");
                    }else if(password == null) {
                        signInStateTextView.setText("Enter your password!");
                    }
                }
            }
        });

    }
}
