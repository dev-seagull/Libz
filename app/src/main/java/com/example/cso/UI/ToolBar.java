package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.example.cso.MainActivity;
import com.example.cso.R;

public class ToolBar {

    public static int menuButtonId;
    public static int toolbarButtonId;
    public static Toolbar createCustomToolbar(Context context) {
        Toolbar toolbar = new Toolbar(context);
        toolbarButtonId = View.generateViewId();
        toolbar.setId(toolbarButtonId);
        Toolbar.LayoutParams toolbarParams = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                0//(int) (UI.getDeviceHeight(context) * 0.08)
        );
        toolbar.setLayoutParams(toolbarParams);
        toolbar.setBackgroundColor(MainActivity.currentTheme.toolbarBackgroundColor); // Setting background color

        // Create TextView
        TextView headerTextView = new TextView(context);
        headerTextView.setId(View.generateViewId());
        Toolbar.LayoutParams textParams = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.START;
        headerTextView.setLayoutParams(textParams);
        headerTextView.setGravity(Gravity.CENTER);
        headerTextView.setPaddingRelative(5, 0, 0, 0); // Padding start
        headerTextView.setText(context.getResources().getString(R.string.app_name));
        headerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        headerTextView.setTextColor(MainActivity.currentTheme.toolbarElementsColor);
        headerTextView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);

        AppCompatButton infoButton = new AppCompatButton(context);
        menuButtonId = View.generateViewId(); // Generate unique ID for menu button
        infoButton.setId(menuButtonId);
        Toolbar.LayoutParams buttonParams = new Toolbar.LayoutParams(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, context.getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, context.getResources().getDisplayMetrics())
        );
        buttonParams.gravity = Gravity.END;
        buttonParams.setMarginEnd(5); // Margin end
        infoButton.setLayoutParams(buttonParams);
        infoButton.setBackground(ContextCompat.getDrawable(context, R.drawable.hamburgermenu));
        infoButton.setTextColor(MainActivity.currentTheme.toolbarElementsColor);
        infoButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        infoButton.setAllCaps(false);

        toolbar.addView(headerTextView);
        toolbar.addView(infoButton);

        return toolbar;
    }

}
