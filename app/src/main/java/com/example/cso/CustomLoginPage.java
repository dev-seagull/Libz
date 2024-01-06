package com.example.cso;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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
                EditText editTextUsernameSignIn = findViewById(R.id.editTextUsernameSignIn);
                EditText editTextPasswordSignIn = findViewById(R.id.editTextPasswordSignIn);
                String userName = editTextUsernameSignIn.getText().toString();
                String password = editTextPasswordSignIn.getText().toString();
                if(userName != null && !userName.isEmpty() && !password.isEmpty() && password != null){
                    System.out.println(userName + " " + password);
                    finish();
                }else{
                    TextView signInStateTextView = findViewById(R.id.signInState);
                    if(userName.isEmpty() | userName == null){
                        signInStateTextView.setText("Enter your username!");
                    }else if(password.isEmpty() | password == null) {
                        signInStateTextView.setText("Enter your password!");
                    }
                }
            }
        });
    }
}
