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
                EditText editTextUsernameSignIn = findViewById(R.id.editTextUsernameSignIn);
                EditText editTextPasswordSignIn = findViewById(R.id.editTextPasswordSignIn);
                String userName = editTextUsernameSignIn.getText().toString();
                String password = editTextPasswordSignIn.getText().toString();
                if(userName != null && !userName.isEmpty() && !password.isEmpty() && password != null){
                    runOnUiThread(() -> {
                        List<String> auth = MainActivity.dbHelper.readProfile();
                        String userNameResult = auth.get(0);
                        String passResult = auth.get(1);
                        TextView signInStateTextView = findViewById(R.id.signInState);
                        if (!userNameResult.isEmpty() && userNameResult != null &&
                                !passResult.isEmpty() && passResult != null) {
                            if(userNameResult.equals(userName)){
                                if(passResult.equals(password)){
                                    finish();
                                }
                            }else{
                                signInStateTextView.setText("Wrong password!");
                            }
                        }else{
                            signInStateTextView.setText("No account was found with this username and password!");
                        }
                    });
                    TextView signInStateTextView = findViewById(R.id.signInState);
                    if(userName.isEmpty() | userName == null){
                        signInStateTextView.setText("Enter your username!");
                    }else if(password.isEmpty() | password == null) {
                        signInStateTextView.setText("Enter your password!");
                    }
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
