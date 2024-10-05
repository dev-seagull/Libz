package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomTreeMapChart {
    public static LinearLayout createChart(Context context, JsonObject data){
        int width =(int) (UI.getDeviceWidth(context) * 0.65);
        int height = (int) (UI.getDeviceWidth(context) * 0.35);
        double unsynced = 200;
        double acc1 = 4;
        double acc2 = 1;
        double acc3 = 2;
        double total = unsynced + acc1 + acc2 + acc3;
        HashMap<String, Double> dataEntries = new HashMap<>();
        dataEntries.put("acc1",acc1);
        dataEntries.put("acc2",acc2);
        dataEntries.put("acc3",acc3);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
        layoutParams.setMargins(0, 10, 0, 10);
        layout.setLayoutParams(layoutParams);

        LinearLayout temp = new LinearLayout(context);
        temp.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tempParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        tempParams.setMargins(0, 10, 0, 10);
        temp.setLayoutParams(tempParams);

        LinearLayout unsyncedView = createUnSyncedView(context,width,height,total,unsynced);

        LinearLayout accountsView = createAccountsView(context,new double[]{acc1,acc2,acc3},dataEntries,height,width - unsyncedView.getWidth(),total-unsynced);
        temp.addView(unsyncedView);
        temp.addView(accountsView);
        layout.addView(temp);
        return layout;
    }

    private static LinearLayout createUnSyncedView(Context context, int width,int height, double total, double unsynced) {
        double relation = Math.min(0.4,(unsynced / total));
        width = (int) (relation * width);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.GRAY);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                width, height
        ));
        return layout;
    }

    private static LinearLayout createAccountsView(Context context, double[] accountsSize,
                                                   HashMap<String, Double> dataEntries,
                                                   int height, int remainingWidth
                                                    , double totalAccountSize) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                remainingWidth, height
        ));
        int[] colors = new int[]{Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW, Color.BLACK};
        for(int i = 0; i < accountsSize.length; i++) {
            View view = new View(context);
            int viewHeight = (int) (accountsSize[i] * height/ totalAccountSize);
            view.setBackgroundColor(colors[i]);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    remainingWidth, viewHeight
            );
            view.setLayoutParams(layoutParams);

            TextView textView = new TextView(context);
            List<String> keys = new ArrayList<>(dataEntries.keySet());
            textView.setText(keys.get(i));
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(Color.WHITE);

            layout.addView(view);
//            layout.addView(textView);
        }

//        LinearLayout layout = new LinearLayout(context);
//        layout.setOrientation(LinearLayout.VERTICAL);
//        layout.setLayoutParams(new LinearLayout.LayoutParams(
//                remainingWidth, height
//        ));
//        int[] colors = new int[]{Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW, Color.BLACK};
//        int index = 0; // To keep track of the index for colors
//        for (Map.Entry<String, Double> entry : dataEntries.entrySet()) {
//            LinearLayout accountLayout = new LinearLayout(context);
//            accountLayout.setOrientation(LinearLayout.VERTICAL);
//            accountLayout.setLayoutParams(new LinearLayout.LayoutParams(remainingWidth, height));
//
//            View view = new View(context);
//            int viewHeight = (int) (entry.getValue() * height / totalAccountSize);
//            view.setBackgroundColor(colors[index % colors.length]); // Use modulo for color cycling
//            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//                    remainingWidth, viewHeight
//            );
//            view.setLayoutParams(layoutParams);
//
//            TextView textView = new TextView(context);
//            textView.setText(entry.getKey());
//            textView.setLayoutParams(new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//            ));
//            textView.setGravity(Gravity.CENTER);
//            textView.setTextColor(Color.WHITE);
//            textView.setBackgroundColor(colors[index % colors.length]);
//
//            accountLayout.addView(view);
//            accountLayout.addView(textView);
//
//            // Add the accountLayout to the main layout
//            layout.addView(accountLayout);
//
//            index++; // Increment the index for color cycling
//        }
        return layout;
    }
}
