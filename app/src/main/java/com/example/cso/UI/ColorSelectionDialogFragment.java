package com.example.cso.UI;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;

import com.example.cso.MainActivity;
import com.example.cso.R;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

public class ColorSelectionDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            layout.setForegroundGravity(Gravity.CENTER);
        }

        TextView titleTextView = new TextView(getActivity());
        titleTextView.setText("Select a Color");
        titleTextView.setTextSize(20);
        titleTextView.setGravity(Gravity.CENTER);
        layout.addView(titleTextView);

        LinearLayout temp = new LinearLayout(getActivity());
        temp.setOrientation(LinearLayout.HORIZONTAL);
        temp.setGravity(Gravity.CENTER);

        GridLayout gridLayout = new GridLayout(getActivity());
        int rowSize = 100;
        int columnSize = 100;
        int columnCount = 2;
        int rowCount = Theme.themes.size() / columnCount;
        int colorMargins = 10 ;
        gridLayout.setRowCount(rowCount);
        gridLayout.setColumnCount(columnCount);


        for (Theme theme : Theme.themes) {
            View colorView = new View(getActivity());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = rowSize;
            params.height = columnSize;
            params.setMargins(colorMargins,colorMargins,colorMargins,colorMargins);

            colorView.setLayoutParams(params);
            if(theme.name.equals("purple")){
                colorView.setBackgroundColor(Color.parseColor("#800080"));
            }else if(theme.name.equals("gray")){
                colorView.setBackgroundColor(Color.parseColor("#808080"));
            }

            colorView.setOnClickListener(v -> {
                Theme.applyTheme(theme);
                DrawerLayout drawerLayout = MainActivity.activity.findViewById(R.id.drawer_layout);
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }
                dismiss();
            });

            TextView textView = new TextView(getActivity());
            textView.setText(theme.name);
            textView.setGravity(Gravity.CENTER);

            gridLayout.addView(colorView);
            gridLayout.addView(textView);
        }
        temp.addView(gridLayout);
        layout.addView(temp);
        builder.setView(layout);
        return builder.create();
    }

}
