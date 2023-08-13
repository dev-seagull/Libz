package com.example.cso;

import android.widget.Button;
import android.widget.TextView;

public class MediaItemModel {

    TextView textViewMediaItemName;
    TextView textViewMediaItemBackUpStatus;
    Button buttonMediaItemDisplay;

    public MediaItemModel(TextView textViewMediaItemName,TextView textViewMediaItemBackUpStatus, Button buttonMediaItemDisplay) {
        this.textViewMediaItemBackUpStatus = textViewMediaItemBackUpStatus;
        this.textViewMediaItemName = textViewMediaItemName;
        this.buttonMediaItemDisplay = buttonMediaItemDisplay;
    }

    public TextView getTextViewMediaItemName() {
        return textViewMediaItemName;
    }

    public Button getButtonMediaItemDisplay() {
        return buttonMediaItemDisplay;
    }

    public TextView  gettextViewMediaItemBackUpStatus() {
        return textViewMediaItemBackUpStatus;
    }

}
