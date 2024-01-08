package com.example.cso;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
public class CustomSignUpPage extends AppCompatActivity {
    private TextView alreadyHaveAccountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_signup_page);

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
                TextView signUpStateTextView = findViewById(R.id.signUpState);
                signUpStateTextView.setText("Wait...");
                EditText editTextUsernameSignUp = findViewById(R.id.editTextUsernameSignUp);
                EditText editTextPasswordSignUp = findViewById(R.id.editTextPasswordSignUp);
                String userName = editTextUsernameSignUp.getText().toString();
                String password = editTextPasswordSignUp.getText().toString();
                if(userName != null && !userName.isEmpty() && !password.isEmpty() && password != null){
                    MainActivity.dbHelper.insertProfile(userName,password);
                    MainActivity.dbHelper.backUpProfileMap(getApplicationContext());
                    finish();
                    buttonCustomSignUp.setClickable(true);
                }else{
                    buttonCustomSignUp.setClickable(true);
                    if(userName.isEmpty() | userName == null){
                        signUpStateTextView.setText("Enter your username!");
                    }else if(password.isEmpty() | password == null) {
                        signUpStateTextView.setText("Enter your password!");
                    }
                }
            }
        });
    }
}
