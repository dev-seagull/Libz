package com.example.cso.UI;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.anychart.enums.MapAsTableMode;
import com.example.cso.DBHelper;
import com.example.cso.MainActivity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomTreeMapChart {
    public static LinearLayout createChart(Context context, JsonObject jsonData){
        int width =(int) (UI.getDeviceWidth(context) * 0.65);
        int height = (int) (UI.getDeviceWidth(context) * 0.35);

        double unsynced = 0.0;
        if (jsonData.has("UnSynced")) {
            unsynced = jsonData.get("UnSynced").getAsDouble();
        }

        double total = 0.0;
        for (Map.Entry<String, JsonElement> entry : jsonData.entrySet()) {
            total += entry.getValue().getAsDouble();
        }

        double totalAccountSize = 0.0;
        List<String> accountKeys = new ArrayList<>();
        List<Double> accountSizes = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : jsonData.entrySet()) {
            String key = entry.getKey();
            double size = entry.getValue().getAsDouble();
            if (!key.equals("UnSynced")) {
                totalAccountSize += size;
                accountKeys.add(key);
                accountSizes.add(size);
            }
        }

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
        LinearLayout accountsView = createAccountsView(context,accountKeys,accountSizes,height,width - unsyncedView.getWidth(),total-unsynced);
        temp.addView(unsyncedView);
        temp.addView(accountsView);
        layout.addView(temp);
        return layout;
    }

    private static LinearLayout createUnSyncedView(Context context, int width,int height
            , double total, double unsynced) {
        double relation = Math.min(0.4,(unsynced / total));
        width = (int) (relation * width);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.GRAY);
        layout.setGravity(Gravity.CENTER);
        if(total == unsynced){
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }else{
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    width, height
            ));
        }


        TextView unsyncedTextView = new TextView(context);
        unsyncedTextView.setText("Unsynced\n" + AreaSquareChart.formatStorageSize(unsynced /1000));
        unsyncedTextView.setGravity(Gravity.CENTER);
        unsyncedTextView.setTextColor(Color.WHITE);
        unsyncedTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        layout.addView(unsyncedTextView);

        return layout;
    }

    private static LinearLayout createAccountsView(Context context, List<String> accountKeys,
                                                   List<Double> accountSizes,int height
                                                    , int remainingWidth, double totalAccountSize) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                remainingWidth, height
        ));
        int[] colors = new int[]{Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW, Color.BLACK};
        for (int i = 0; i < accountSizes.size(); i++) {
            double accountSize = accountSizes.get(i);

            View view = new View(context);
            int viewHeight = (int) (accountSize * height / totalAccountSize);
            view.setBackgroundColor(colors[i % colors.length]);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(remainingWidth, viewHeight);
            view.setLayoutParams(layoutParams);

            layout.addView(view);

            TextView textView = new TextView(context);
            textView.setText(accountKeys.get(i)+ "\n" + AreaSquareChart
                    .formatStorageSize(accountSizes.get(i) / 1000));
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(Color.WHITE);

            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setLayoutParams(new LinearLayout.LayoutParams(remainingWidth, viewHeight));
            textContainer.setGravity(Gravity.CENTER);
            textContainer.addView(textView);

            layout.addView(textContainer);
        }

        return layout;
    }
}
