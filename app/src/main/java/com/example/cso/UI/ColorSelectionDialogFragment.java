package com.example.cso.UI;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.cso.MainActivity;

public class ColorSelectionDialogFragment extends DialogFragment {
    private final int[] colors = {
            0xFF000000, // Black
            0xFFFFFFFF, // White
            0xFFFF0000, // Red
            0xFF00FF00, // Green
            0xFF0000FF, // Blue
            0xFFFFFF00, // Yellow
            0xFFFF00FF, // Magenta
            0xFF00FFFF  // Cyan
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView titleTextView = new TextView(getActivity());
        titleTextView.setText("Select a Color");
        titleTextView.setTextSize(20);
        titleTextView.setGravity(Gravity.CENTER);
        layout.addView(titleTextView);

        GridLayout gridLayout = new GridLayout(getActivity());
        gridLayout.setRowCount(2);
        gridLayout.setColumnCount(4);
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.width = GridLayout.LayoutParams.WRAP_CONTENT;
        layoutParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
//        layoutParams.setGravity(Gravity.CENTER);
        gridLayout.setLayoutParams(layoutParams);

        for (int color : colors) {
            View colorView = new View(getActivity());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 100;
            params.height = 100;
//            params.setMargins(10, 10, 10, 10);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(color);

            colorView.setOnClickListener(v -> {
                applyThemeColor(color);
                dismiss();
            });

            gridLayout.addView(colorView);
        }

        layout.addView(gridLayout);
        builder.setView(layout);
        return builder.create();
    }

    private void applyThemeColor(int color) {
        Theme.applyTheme();
    }
}
