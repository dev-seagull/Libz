package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomTreeMapChart {
//    public static LinearLayout createChart(Context context, JsonObject jsonData){
//        LinearLayout layout = new LinearLayout(context);
//        layout.setOrientation(LinearLayout.VERTICAL);
//        layout.setGravity(Gravity.CENTER);
//        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT);
//        layoutParams.setMargins(0, 10, 0, 10);
//        layout.setLayoutParams(layoutParams);
//
//        try{
//            int width =(int) (UI.getDeviceWidth(context) * 0.65);
//            int height = (int) (UI.getDeviceWidth(context) * 0.35);
//
//            double unsynced = 0.0;
//            if (jsonData.has("UnSynced")) {
//                unsynced = jsonData.get("UnSynced").getAsDouble();
//            }
//
//            List<String> accountKeys = new ArrayList<>();
//            List<Double> accountSizes = new ArrayList<>();
//
//            double total = 0.0;
//            for (Map.Entry<String, JsonElement> entry : jsonData.entrySet()) {
//                total += entry.getValue().getAsDouble();
//                String key = entry.getKey();
//                double size = entry.getValue().getAsDouble();
//                if (!key.equals("UnSynced")) {
//                    accountKeys.add(key);
//                    accountSizes.add(size);
//                }
//            }
//
//            layoutParams = new LinearLayout.LayoutParams(width, height);
//            layoutParams.setMargins(0, 10, 0, 10);
//            layout.setLayoutParams(layoutParams);
//
//            LinearLayout temp = new LinearLayout(context);
//            temp.setOrientation(LinearLayout.HORIZONTAL);
//            LinearLayout.LayoutParams tempParams = new LinearLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT
//            );
//            tempParams.setMargins(0, 10, 0, 10);
//            temp.setLayoutParams(tempParams);
//
//            LinearLayout unsyncedView = createUnsyncedView(context,width,height,total,unsynced);
//            LinearLayout accountsView = createAccountsView(context,accountKeys,accountSizes,height,
//                    width - unsyncedView.getWidth(),total-unsynced);
//            temp.addView(unsyncedView);
//            temp.addView(accountsView);
//            layout.addView(temp);
//        }catch (Exception e){
//            LogHandler.crashLog(e,"customTreeMapChart");
//            layout.removeAllViews();
//            layout.addView(Details.getErrorAsChartAlternative(context));
//        }
//        return layout;
//    }
//
//    private static LinearLayout createUnsyncedView(Context context, int width, int height
//            , double total, double unsynced) {
//        double relation = Math.min(0.55,(unsynced / total));
//        width = (int) (relation * width);
//        LinearLayout layout = new LinearLayout(context);
//        layout.setOrientation(LinearLayout.VERTICAL);
//        layout.setBackgroundColor(Color.GRAY);
//        layout.setGravity(Gravity.CENTER);
//        if(total == unsynced){
//            layout.setLayoutParams(new LinearLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
//            ));
//        }else{
//            layout.setLayoutParams(new LinearLayout.LayoutParams(
//                    width, height
//            ));
//        }
//
//        TextView unsyncedTextView = new TextView(context);
//        unsyncedTextView.setText("UnSynced\n" + AreaSquareChart.formatStorageSize(unsynced / 1024));
//        unsyncedTextView.setGravity(Gravity.CENTER);
//        unsyncedTextView.setTextColor(Color.WHITE);
//        unsyncedTextView.setLayoutParams(new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//        ));
//        layout.addView(unsyncedTextView);
//
//        return layout;
//    }
//
//    private static LinearLayout createAccountsView(Context context, List<String> accountKeys,
//                                                   List<Double> accountSizes,int height
//                                                    , int remainingWidth, double totalAccountSize) {
//        LinearLayout layout = new LinearLayout(context);
//        layout.setOrientation(LinearLayout.VERTICAL);
//        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(remainingWidth, height);
//        layoutParams.gravity = Gravity.CENTER;
//        layout.setLayoutParams(layoutParams);
//
//        int[] colors = new int[]{Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW, Color.BLACK};
//        for (int i = 0; i < accountSizes.size(); i++) {
//            double accountSize = accountSizes.get(i);
//
//            int viewHeight = (int) (Math.log10(accountSize + 1) / Math.log10(totalAccountSize + 1) * height);
//            LinearLayout parentLayout = new LinearLayout(context);
//            parentLayout.setOrientation(LinearLayout.VERTICAL);
//            parentLayout.setBackgroundColor(colors[i % colors.length]);
//            parentLayout.setLayoutParams(new LinearLayout.LayoutParams(
//                    remainingWidth, viewHeight
//            ));
//            parentLayout.setGravity(Gravity.CENTER);
//
//            TextView textView = new TextView(context);
//            textView.setText(accountKeys.get(i)+ "\n" + AreaSquareChart
//                    .formatStorageSize(accountSizes.get(i) / 1024));
//            textView.setGravity(Gravity.CENTER);
//            textView.setTextColor(Color.WHITE);
//            textView.setLayoutParams(new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.MATCH_PARENT
//            ));
//
//            parentLayout.addView(textView);
//            layout.addView(parentLayout);
//        }
//
//        return layout;
//    }

    public static LinearLayout createStackedBarChart(Context context, JsonObject jsonData) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        HorizontalBarChart stackedBarChart = new HorizontalBarChart(context);

        try {
            int width = (int) (UI.getDeviceWidth(context) * 0.8);
            int height = (int) (UI.getDeviceHeight(context) * 0.06);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
            stackedBarChart.setLayoutParams(layoutParams);

            double unsynced = 0.0;
            if (jsonData.has("UnSynced")) {
                unsynced = jsonData.get("UnSynced").getAsDouble();
            }

            List<Double> syncedValues = new ArrayList<>();
            List<String> syncedLabels = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : jsonData.entrySet()) {
                if (!entry.getKey().equals("UnSynced")) {
                    double size = entry.getValue().getAsDouble();
                    if(size != 0.0){
                        syncedValues.add(size);
                        syncedLabels.add(entry.getKey());
                    }
                }
            }

            float[] stackedValues = new float[syncedValues.size() + 1];
            for (int i = 0; i < syncedValues.size(); i++) {
                stackedValues[i] = syncedValues.get(i).floatValue();
            }
            stackedValues[syncedValues.size()] = (float) unsynced; // Unsynced goes at the end

            List<BarEntry> entries = new ArrayList<>();
            entries.add(new BarEntry(0f, stackedValues));

            int[] colors = MainActivity.currentTheme.deviceAssetsSyncedStatusChartColors;

            BarDataSet dataSet = new BarDataSet(entries, "Storage Usage");
            dataSet.setColors(colors);
            
            String[] stackLabels = new String[syncedValues.size() + 1];
            for (int i = 0; i < syncedLabels.size(); i++) {
                stackLabels[i] = syncedLabels.get(i);
            }
            stackLabels[syncedLabels.size()] = "Unsynced";
            dataSet.setStackLabels(stackLabels);

            BarData barData = new BarData(dataSet);
            stackedBarChart.setData(barData);

            stackedBarChart.getDescription().setEnabled(false);
            stackedBarChart.getLegend().setEnabled(false);
            XAxis xAxis = stackedBarChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setDrawLabels(false);
            xAxis.setDrawAxisLine(false);
            YAxis leftAxis = stackedBarChart.getAxisLeft();
            leftAxis.setDrawGridLines(false);
            stackedBarChart.getAxisRight().setEnabled(false);
            leftAxis.setDrawLabels(false);
            leftAxis.setDrawAxisLine(false);
            dataSet.setDrawValues(false);

            layout.addView(stackedBarChart);

            LinearLayout legendLayout = new LinearLayout(context);
            legendLayout.setOrientation(LinearLayout.VERTICAL);
            legendLayout.setGravity(Gravity.CENTER);

            for (int i = 0; i < syncedLabels.size(); i++) {
                int color = colors[i % colors.length];
                legendLayout.addView(createLegendItem(context, syncedLabels.get(i), color, syncedValues.get(i)));
                if(i + 1 == syncedLabels.size()){
                    color = colors[(i+1) % colors.length];
                    legendLayout.addView(createLegendItem(context, "Unsynced", color, unsynced));
                }
            }

            layout.addView(legendLayout);

        } catch (Exception e) {
            LogHandler.crashLog(e, "createStackedBarChart");
            layout.removeAllViews();
            layout.addView(Details.getErrorAsChartAlternative(context));
        }

        return layout;
    }
    public static LinearLayout createLegendItem(Context context, String label, int color, double value) {
        LinearLayout legendItem = new LinearLayout(context);
        legendItem.setOrientation(LinearLayout.HORIZONTAL);
        legendItem.setGravity(Gravity.CENTER);

        View colorBox = new View(context);
        LinearLayout.LayoutParams colorBoxParams = new LinearLayout.LayoutParams((int) (UI.getDeviceHeight(context) * 0.008), (int) (UI.getDeviceHeight(context) * 0.008));
        colorBoxParams.setMargins(10, 0, 10, 0);
        colorBox.setLayoutParams(colorBoxParams);
        colorBox.setBackgroundColor(color);

        TextView labelText = new TextView(context);
        labelText.setText(label+" : ");
        int textColor = MainActivity.currentTheme.primaryTextColor;
        labelText.setTextColor(textColor);
        labelText.setTextSize((int) (UI.getDeviceHeight(context) * 0.004));

        TextView valueText = new TextView(context);
        valueText.setText(new PieChartValueFormatter().getFormattedValue((float) value));
        valueText.setTextColor(textColor);
        valueText.setTextSize((int) (UI.getDeviceHeight(context) * 0.004));

        legendItem.addView(colorBox);
        legendItem.addView(labelText);
        legendItem.addView(valueText);

        return legendItem;
    }
}
