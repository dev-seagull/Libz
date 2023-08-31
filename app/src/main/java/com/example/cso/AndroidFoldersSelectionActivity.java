package com.example.cso;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.util.ArrayList;

public class AndroidFoldersSelectionActivity extends AppCompatActivity {
    ArrayList<String> selectedFolders = new ArrayList<String>();
    ArrayList<String> formats = new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_androidfolders_and_format_selection);

        LinearLayout formatCheckboxContainer  = findViewById(R.id.formatCheckboxContainer);
        LinearLayout folderCheckboxContainer  = findViewById(R.id.folderCheckboxContainer);

        String[] imageAndVideoExtensions = {".jpg", ".jpeg", ".png", ".webp",
                ".gif", ".mp4", ".mkv", ".webm"};

        for(int i=0; i<imageAndVideoExtensions.length;i++){
            CheckBox checkBox = new CheckBox(getApplicationContext());
            checkBox.setText(imageAndVideoExtensions[i]);
            checkBox.setChecked(true);
            formatCheckboxContainer.addView(checkBox);
        }

        Button formatConfirmationButton = findViewById(R.id.formatCheckboxConfirmationButton);
        formatConfirmationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(int i=0;i< formatCheckboxContainer.getChildCount();i++){
                    View childView = formatCheckboxContainer.getChildAt(i);
                    if(childView instanceof CheckBox){
                        CheckBox checkBox = (CheckBox) childView;
                        if (checkBox.isChecked()) {
                            String checkBoxText = checkBox.getText().toString();
                            formats.add(checkBoxText);
                        }
                    }
                }
                folderCheckboxContainer.setVisibility(View.VISIBLE);
                Button folderConfirmationButton = findViewById(R.id.folderCheckboxConfirmationButton);
                folderConfirmationButton.setVisibility(View.VISIBLE);

                File rootDirectory = Environment.getExternalStorageDirectory();
                File[] androidFolders = rootDirectory.listFiles();

                for(int i=0; i<androidFolders.length;i++){
                    if(androidFolders[i].isDirectory()){
                        CheckBox checkBox = new CheckBox(getApplicationContext());
                        checkBox.setText(androidFolders[i].getName());
                        checkBox.setChecked(true);
                        folderCheckboxContainer.addView(checkBox);
                    }
                }
            }
        });



        Button folderConfirmationButton = findViewById(R.id.folderCheckboxConfirmationButton);
        folderConfirmationButton.setOnClickListener(view -> {

            for(int i=0;i< folderCheckboxContainer.getChildCount();i++){
                View childView = folderCheckboxContainer.getChildAt(i);
                if(childView instanceof CheckBox){
                    CheckBox checkBox = (CheckBox) childView;
                    if (checkBox.isChecked()) {
                        String checkBoxText = checkBox.getText().toString();
                        selectedFolders.add(checkBoxText);
                    }
                }
            }

            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra("selectedFolders", selectedFolders);
            resultIntent.putStringArrayListExtra("formats", formats);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
