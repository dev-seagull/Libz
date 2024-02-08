package com.example.cso;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class CustomSignUpPage extends AppCompatActivity{
    private TextView alreadyHaveAccountTextView;
    private CheckBox recaptchaCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_signup_page);

        recaptchaCheckBox = findViewById(R.id.chechBoxSignUp);

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
        if(userName.isEmpty() | userName == null){
            return  "Enter your username!";
        }
        if(password.isEmpty() | password == null) {
            return  "Enter your password!";
        }

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
        if(!recaptchaCheckBox.isChecked()){
            return "Verify you're not a robot!";
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
}
