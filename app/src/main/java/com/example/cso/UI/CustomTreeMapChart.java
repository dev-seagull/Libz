package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cso.LogHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomTreeMapChart {
    public static LinearLayout createChart(Context context, JsonObject jsonData){
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 10, 0, 10);
        layout.setLayoutParams(layoutParams);

        try{
            int width =(int) (UI.getDeviceWidth(context) * 0.65);
            int height = (int) (UI.getDeviceWidth(context) * 0.35);

            double unsynced = 0.0;
            if (jsonData.has("UnSynced")) {
                unsynced = jsonData.get("UnSynced").getAsDouble();
            }

            List<String> accountKeys = new ArrayList<>();
            List<Double> accountSizes = new ArrayList<>();

            double total = 0.0;
            for (Map.Entry<String, JsonElement> entry : jsonData.entrySet()) {
                total += entry.getValue().getAsDouble();
                String key = entry.getKey();
                double size = entry.getValue().getAsDouble();
                if (!key.equals("UnSynced")) {
                    accountKeys.add(key);
                    accountSizes.add(size);
                }
            }

            layoutParams = new LinearLayout.LayoutParams(width, height);
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

            LinearLayout unsyncedView = createUnsyncedView(context,width,height,total,unsynced);
            LinearLayout accountsView = createAccountsView(context,accountKeys,accountSizes,height,
                    width - unsyncedView.getWidth(),total-unsynced);
            temp.addView(unsyncedView);
            temp.addView(accountsView);
            layout.addView(temp);
        }catch (Exception e){
            LogHandler.crashLog(e,"customTreeMapChart");
            layout.removeAllViews();
            layout.addView(Details.getErrorAsChartAlternative(context));
        }
        return layout;
    }

    private static LinearLayout createUnsyncedView(Context context, int width, int height
            , double total, double unsynced) {
        double relation = Math.min(0.55,(unsynced / total));
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
        unsyncedTextView.setText("Unsynced\n" + AreaSquareChart.formatStorageSize(unsynced / 1024));
        unsyncedTextView.setGravity(Gravity.CENTER);
        unsyncedTextView.setTextColor(Color.WHITE);
        unsyncedTextView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        layout.addView(unsyncedTextView);

        return layout;
    }

    private static LinearLayout createAccountsView(Context context, List<String> accountKeys,
                                                   List<Double> accountSizes,int height
                                                    , int remainingWidth, double totalAccountSize) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(remainingWidth, height);
        layoutParams.gravity = Gravity.CENTER;
        layout.setLayoutParams(layoutParams);

        int[] colors = new int[]{Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW, Color.BLACK};
        for (int i = 0; i < accountSizes.size(); i++) {
            double accountSize = accountSizes.get(i);

            int viewHeight = (int) (Math.log10(accountSize + 1) / Math.log10(totalAccountSize + 1) * height);
            LinearLayout parentLayout = new LinearLayout(context);
            parentLayout.setOrientation(LinearLayout.VERTICAL);
            parentLayout.setBackgroundColor(colors[i % colors.length]);
            parentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    remainingWidth, viewHeight
            ));
            parentLayout.setGravity(Gravity.CENTER);

            TextView textView = new TextView(context);
            textView.setText(accountKeys.get(i)+ "\n" + AreaSquareChart
                    .formatStorageSize(accountSizes.get(i) / 1024));
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(Color.WHITE);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));

            parentLayout.addView(textView);
            layout.addView(parentLayout);
        }

        return layout;
    }
}
