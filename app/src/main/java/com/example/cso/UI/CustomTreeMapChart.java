package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.gson.JsonObject;

public class CustomTreeMapChart {
    public static LinearLayout createChart(Context context, JsonObject data){
        int width =(int) (UI.getDeviceWidth(context) * 0.35);
        double unsynced = 0.5;
        double acc1 = 0.4;
        double acc2 = 3;
        double acc3 = 15;
        double total = unsynced + acc1 + acc2 + acc3;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        layoutParams.setMargins(10, 10, 10, 10);
        layout.setLayoutParams(layoutParams);

        addTreeMapItem(context, layout, width, unsynced, total, Color.RED);  // unsynced (0.5)
        addTreeMapItem(context, layout, width, acc1, total, Color.BLUE);     // acc1 (0.4)
        addTreeMapItem(context, layout, width, acc2, total, Color.GREEN);    // acc2 (3)
        addTreeMapItem(context, layout, width, acc3, total, Color.YELLOW);   // acc3 (15)

        return layout;
    }

    private static void addTreeMapItem(Context context, LinearLayout layout, int totalWidth, double value, double total, int color) {
        int width = (int) ((value / total) * totalWidth);

        View rectangle = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(5, 0, 5, 0);
        rectangle.setLayoutParams(params);
        rectangle.setBackgroundColor(color);

        layout.addView(rectangle);
    }
}
