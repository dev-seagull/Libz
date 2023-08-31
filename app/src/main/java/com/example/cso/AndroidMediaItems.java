package com.example.cso;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class AndroidMediaItems extends AppCompatActivity {
    private  int unique_request_code = 789;

    void openMediaInGallery(String filePath) {
        File mediaFile = new File(filePath);

        Uri contentUri = FileProvider.getUriForFile(this,  getApplicationContext().getPackageName() + ".provider", mediaFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        String memeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        intent.setDataAndType(contentUri, memeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    public void createAndroidMediaItemsModel(String name, String filePath, LinearLayout parentLayout){
        String mediaItemName = name;
        TextView textViewName = new TextView(parentLayout.getContext());
        textViewName.setText(mediaItemName);

        TextView textViewBackUpStatus = new TextView(parentLayout.getContext());
        textViewBackUpStatus.setText("backed-up to Google Drive");

        Button mediaItemDisplayButton = new Button(parentLayout.getContext());
        mediaItemDisplayButton.setVisibility(View.VISIBLE);
        mediaItemDisplayButton.setText("Click here to see the image");


        mediaItemDisplayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMediaInGallery(filePath);
            }
        });


        parentLayout.addView(textViewName);
        parentLayout.addView(textViewBackUpStatus);
        parentLayout.addView(mediaItemDisplayButton);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_androidmediaitems);

        LinearLayout androidMediaItemsLinearLayout = findViewById(R.id.
                androidMediaItemsLinearLayout);
        LinearLayout.LayoutParams androidMediaItemsLinearLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        androidMediaItemsLinearLayoutParams.setMargins(0,5,0, 0);

        Intent intent = getIntent();
        ArrayList<String> serializedAndroidFileInfoList = intent.getStringArrayListExtra("androidFileInfoList");
        ArrayList<ArrayList<String>> androidFileInfoList = new ArrayList<>();

        for (String serializedFileInfo : serializedAndroidFileInfoList) {
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            ArrayList<String> fileInfo = new Gson().fromJson(serializedFileInfo, type);
            androidFileInfoList.add(fileInfo);
        }

        for (int i=0;i<androidFileInfoList.size();i++) {
            createAndroidMediaItemsModel(androidFileInfoList.get(i).get(0)
                    , androidFileInfoList.get(i).get(1)
                    ,androidMediaItemsLinearLayout);

        }

        Button androidMediaItemsBackButton = findViewById(R.id.androidMediaItemsBackButton);
        androidMediaItemsBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });

    }

}
