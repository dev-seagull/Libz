package com.example.cso;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
public class CustomSignUpPage extends AppCompatActivity {
    private TextView alreadyHaveAccountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_signup_page);
        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");


        alreadyHaveAccountTextView = findViewById(R.id.alreadyHaveAccount);
        alreadyHaveAccountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomSignUpPage.this, CustomLoginPage.class);
                startActivity(intent);
            }
        });
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, CustomSignUpPage.class);
        startActivity(intent);
    }
}
