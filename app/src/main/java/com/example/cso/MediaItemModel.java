package com.example.cso;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;


public class MediaItemModel extends AppCompatActivity {
    private int unique_request_code = 456;

    public static float all_volumn;
        public void createMediaItemsModel(String name,String productUrl,LinearLayout parentLayout){
            String mediaItemName = name;
            TextView textViewName = new TextView(parentLayout.getContext());
            textViewName.setText(mediaItemName);

            TextView textViewBackUpStatus = new TextView(parentLayout.getContext());
            textViewBackUpStatus.setText("no back-up");

            Button mediaItemDisplayButton = new Button(parentLayout.getContext());
            mediaItemDisplayButton.setVisibility(View.VISIBLE);
            mediaItemDisplayButton.setText("Click here to see the image");


            mediaItemDisplayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(productUrl));
                    parentLayout.getContext().startActivity(intent);
                }
            });




            parentLayout.addView(textViewName);
            parentLayout.addView(textViewBackUpStatus);
            parentLayout.addView(mediaItemDisplayButton);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_mediaitems);

            LinearLayout mediaItemsLinearLayout = findViewById(R.id.mediaItemsLinearLayout);
            LinearLayout.LayoutParams mediaItemsLinearLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            mediaItemsLinearLayoutParams.setMargins(0,5,0, 0);

            Intent intent = getIntent();
            ArrayList<String> fileNames = intent.getStringArrayListExtra("fileNames");
            ArrayList<String> productUrls = intent.getStringArrayListExtra("productUrls");

            for (int i=0;i<fileNames.size();i++) {
                createMediaItemsModel(fileNames.get(i)
                        , productUrls.get(i)
                        ,mediaItemsLinearLayout);
            }

            Button mediaItemsBackButton =  findViewById(R.id.mediaItemsBackButton);
            mediaItemsBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
        }
}
