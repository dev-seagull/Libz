package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cso.DeviceStatusSync;
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

    public static LinearLayout createStackedBarChart(Context context, JsonObject jsonData, String deviceId) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        HorizontalBarChart stackedBarChart = new HorizontalBarChart(context);

        try {

            double total = 0.0;
            double unsynced = 0.0;
            if (jsonData.has("UnSynced")) {
                unsynced = jsonData.get("UnSynced").getAsDouble();
            }

            List<Double> syncedValues = new ArrayList<>();
            List<String> syncedLabels = new ArrayList<>();

            List<Pair<String, Double>> syncedValuePairs = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : jsonData.entrySet()) {
                if (!entry.getKey().equals("UnSynced")) {
                    double size = entry.getValue().getAsDouble();
                    if(size != 0.0){
                        total = total + size;
                        syncedValues.add(size);
                        syncedLabels.add(entry.getKey());
                    }
                }
            }

            syncedValuePairs.sort((p1, p2) -> Double.compare(p2.second, p1.second));


            for (Pair<String, Double> pair : syncedValuePairs) {
                syncedLabels.add(pair.first);
                syncedValues.add(pair.second);
            }


            float[] stackedValues = new float[syncedValues.size() + 1];
            for (int i = 0; i < syncedValues.size(); i++) {
                stackedValues[i] = syncedValues.get(i).floatValue();
            }
            stackedValues[syncedValues.size()] = (float) unsynced; // Unsynced goes at the end

            boolean isAllZero = true;
            for (float stackedValue: stackedValues){
                if(stackedValue != 0){
                    isAllZero = false;
                    break;
                }
            }
            if(isAllZero){
                layout.addView(Details.getErrorAsChartAlternative(context));
                return layout;
            }

            List<BarEntry> entries = new ArrayList<>();
            entries.add(new BarEntry(0f, stackedValues));

            int[] colors = new int[syncedLabels.size() + 1];
            int colorsToCopy = Math.min(syncedLabels.size(), MainActivity.currentTheme.deviceAssetsSyncedStatusChartColors.length);
            System.arraycopy(MainActivity.currentTheme.deviceAssetsSyncedStatusChartColors, 0, colors, 0, colorsToCopy);
            colors[syncedValues.size()] = Color.parseColor("#FAB34B");

            BarDataSet dataSet = new BarDataSet(entries, "Storage Usage");
            dataSet.setColors(colors);
            dataSet.setDrawValues(false);

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
            xAxis.setCenterAxisLabels(true);
            YAxis leftAxis = stackedBarChart.getAxisLeft();
            leftAxis.setDrawGridLines(false);
            stackedBarChart.getAxisRight().setEnabled(false);
            leftAxis.setDrawLabels(false);
            leftAxis.setDrawAxisLine(false);

            layout.addView(stackedBarChart);

            LinearLayout mainLegendLayout = new LinearLayout(context);
            mainLegendLayout.setOrientation(LinearLayout.VERTICAL);
            mainLegendLayout.setGravity(Gravity.CENTER);
            mainLegendLayout.setPadding(0, 10, 0, 0);

            LinearLayout legendLayout = new LinearLayout(context);
            legendLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            legendParams.gravity = Gravity.CENTER;
            legendLayout.setLayoutParams(legendParams);

            int parentWidth = (int) (UI.getDeviceWidth(context) * 0.90) ;

            for (int i = 0; i < syncedLabels.size(); i++) {
                int color = colors[i % colors.length];
                View legendItem = createLegendItem(context, syncedLabels.get(i), color, syncedValues.get(i));

                legendItem.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                legendLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                int totalWidth = legendLayout.getMeasuredWidth() + legendItem.getMeasuredWidth() + (int) (UI.getDeviceWidth(context) * 0.12) + (int) (UI.getDeviceWidth(context) * 0.12);

                if (totalWidth > parentWidth) {
                    mainLegendLayout.addView(legendLayout);
                    legendLayout = new LinearLayout(context);
                    legendLayout.setOrientation(LinearLayout.HORIZONTAL);
                    legendParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    legendLayout.setGravity(Gravity.LEFT);
                    legendParams.setMargins((int) (UI.getDeviceWidth(context) * 0.12),0,(int) (UI.getDeviceWidth(context) * 0.12),0);
                    legendLayout.setLayoutParams(legendParams);
                }

                legendLayout.addView(legendItem);

                if(i + 1 == syncedLabels.size()){
                    for(int j =0 ; j < 13; j++){
                        legendItem = createLegendItem(context, "Lagging behind", Color.parseColor("#FFD166"), unsynced);
                        legendItem.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        legendLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                        totalWidth = legendLayout.getMeasuredWidth() + legendItem.getMeasuredWidth() + (int) (UI.getDeviceWidth(context) * 0.12) + (int) (UI.getDeviceWidth(context) * 0.12);

                        if (totalWidth > parentWidth) {
                            mainLegendLayout.addView(legendLayout);
                            legendLayout = new LinearLayout(context);
                            legendLayout.setOrientation(LinearLayout.HORIZONTAL);
                            legendParams = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                            legendLayout.setGravity(Gravity.LEFT);
                            legendParams.setMargins((int) (UI.getDeviceWidth(context) * 0.12),0,(int) (UI.getDeviceWidth(context) * 0.12),0);
                            legendLayout.setLayoutParams(legendParams);
                        }

                        legendLayout.addView(legendItem);
                    }
                }
            }

            if (legendLayout.getChildCount() > 0) {
                mainLegendLayout.addView(legendLayout);
            }

            String updateDate = DeviceStatusSync.getDeviceStatusLastUpdateTime(deviceId);
            LinearLayout updateDateLabelsLayout = AreaSquareChart.createUpdateDateLabel(context, updateDate);
            LinearLayout.LayoutParams updateDateLabelsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            updateDateLabelsParams.gravity = Gravity.CENTER;
            updateDateLabelsLayout.setLayoutParams(updateDateLabelsParams);
            mainLegendLayout.addView(updateDateLabelsLayout);

            layout.addView(mainLegendLayout);

            int width = (int) (UI.getDeviceWidth(context) * 0.9);
            int height = (int) (UI.getDeviceHeight(context) * 0.085);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    width ,
                    height
            );
            layoutParams.gravity = Gravity.CENTER | Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
            layoutParams.setMargins(width / 32,0,0,0);
            stackedBarChart.setLayoutParams(layoutParams);

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
        legendItem.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        legendItem.setGravity(Gravity.LEFT);


        View colorBox = new View(context);
        LinearLayout.LayoutParams colorBoxParams = new LinearLayout.LayoutParams((int) (UI.getDeviceHeight(context) * 0.008), (int) (UI.getDeviceHeight(context) * 0.008));
        colorBoxParams.setMargins(20, 15, 10, 0);
        colorBox.setLayoutParams(colorBoxParams);
        colorBox.setBackgroundColor(color);

        TextView labelText = new TextView(context);
        labelText.setText(label);
        int textColor = MainActivity.currentTheme.primaryTextColor;
        labelText.setTextColor(textColor);
        labelText.setTextSize(12f);

        legendItem.addView(colorBox);
        legendItem.addView(labelText);

        return legendItem;
    }
}
