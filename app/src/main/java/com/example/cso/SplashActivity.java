//package com.example.cso;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.Handler;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//public class SplashActivity extends AppCompatActivity {
//
//    private static final int SPLASH_TIME_OUT = 0;
//    public static boolean complete_loaded = false;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.splash_screen);
//
//        new Handler().postDelayed(() -> {
//            Intent i = new Intent(SplashActivity.this, MainActivity.class);
//            startActivity(i);
//
//            finish();
//        }, SPLASH_TIME_OUT);
//        complete_loaded = true;
//    }
//}
//
