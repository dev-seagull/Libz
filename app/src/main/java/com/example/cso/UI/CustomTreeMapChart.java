package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

    public static HorizontalBarChart createStackedBarChart(Context context, LinearLayout layout,
                                                     JsonObject jsonData, String deviceId) {
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

            double syncedMaxValue = 0;
            for(int i = 0; i < syncedValues.size(); i++){
                if(syncedValues.get(i) > syncedMaxValue){
                    syncedMaxValue  =syncedValues.get(i);
                }
            }
            if(unsynced > syncedMaxValue){
                syncedMaxValue = unsynced;
            }

            for(int i = 0; i < syncedValues.size(); i++){
                if(syncedValues.get(i) / syncedMaxValue < 0.01){
                    syncedValues.set(i, syncedMaxValue / 100);
                }
            }

            if(unsynced / syncedMaxValue < 0.01){
                unsynced =  syncedMaxValue / 100;
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
                return null;
            }

            List<BarEntry> entries = new ArrayList<>();
            entries.add(new BarEntry(0f, stackedValues));

            BarDataSet dataSet = new BarDataSet(entries, "Storage Usage");

            try{
                List<Integer> colorList = new ArrayList<>();
                for(String syncedLabel: syncedLabels){
                    colorList.add(Accounts.accountMap.get(syncedLabel));
                }
                colorList.add(Color.parseColor("#FFD166"));
                int[] colors = new int[colorList.size()];
                for (int i = 0; i < colorList.size(); i++) {
                    colors[i] = colorList.get(i);
                }
                dataSet.setColors(colors);
            }catch (Exception e) {
                LogHandler.crashLog(e,"AreaSquareChartForAccount");
            }

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
            double finalUnsynced = unsynced;
            layout.post(new Runnable() {
                @Override
                public void run() {
                    int parentWidth = (int) (UI.getDeviceWidth(context) * 0.85);
                    LinearLayout.LayoutParams stackedBarLayoutParams = new LinearLayout.LayoutParams(
                            parentWidth,
                            230
                    );
                    stackedBarLayoutParams.gravity = Gravity.CENTER;
                    stackedBarLayoutParams.setMargins(20, 42,0,0);
                    stackedBarChart.setLayoutParams(stackedBarLayoutParams);

                    ScrollView scrollView = new ScrollView(context);
                    LinearLayout.LayoutParams scrollViewParams = new LinearLayout.LayoutParams(
                            parentWidth, 140
                    );
                    scrollViewParams.gravity = Gravity.CENTER_HORIZONTAL;
                    scrollViewParams.setMargins(20, 0,0,10);
                    scrollView.setLayoutParams(scrollViewParams);

                    LinearLayout mainLegendLayout = new LinearLayout(context);
                    mainLegendLayout.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams mainLegendParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    mainLegendParams.gravity = Gravity.CENTER_HORIZONTAL;
                    mainLegendParams.setMargins( (int) (UI.getDeviceWidth(context) * 0.1),0,0,0);
                    mainLegendLayout.setLayoutParams(mainLegendParams);

                    LinearLayout legendLayout = new LinearLayout(context);
                    legendLayout.setOrientation(LinearLayout.HORIZONTAL);

                    for (int i = 0; i < syncedLabels.size(); i++) {
                        int color = Color.BLUE;
                        for(String syncedValue: syncedLabels){
                            color = Accounts.accountMap.get(syncedValue);
                            break;
                        }

                        View legendItem = createLegendItem(context, syncedLabels.get(i), color, syncedValues.get(i));

                        legendItem.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        legendLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                        int totalWidth = legendLayout.getMeasuredWidth() + legendItem.getMeasuredWidth() + (int) (UI.getDeviceWidth(context) * 0.2)
                                + 37 + 37 ;

                        if (totalWidth > parentWidth) {
                            mainLegendLayout.addView(legendLayout);
                            legendLayout = new LinearLayout(context);
                            legendLayout.setOrientation(LinearLayout.HORIZONTAL);
                            legendLayout.setGravity(Gravity.LEFT);
                        }

                        legendLayout.addView(legendItem);

                        if(i + 1 == syncedLabels.size()){
//                    for(int j =0 ; j < 13; j++){
                            legendItem = createLegendItem(context, "Lagging behind", Color.parseColor("#FFD166"), finalUnsynced);
                            legendItem.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                            legendLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                            totalWidth = legendLayout.getMeasuredWidth() + legendItem.getMeasuredWidth() + (int) (UI.getDeviceWidth(context) * 0.2)
                                    + 37 + 37 ;

                            if (totalWidth > parentWidth) {
                                mainLegendLayout.addView(legendLayout);
                                legendLayout = new LinearLayout(context);
                                legendLayout.setGravity(Gravity.LEFT);
                                legendLayout.setOrientation(LinearLayout.HORIZONTAL);
                            }

                            legendLayout.addView(legendItem);
//                    }
                        }
                    }
                    if(syncedLabels.size() == 0){
                        View legendItem = createLegendItem(context, "Lagging behind", Color.parseColor("#FFD166"), finalUnsynced);
                        legendItem.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        legendLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                        int totalWidth = legendLayout.getMeasuredWidth() + legendItem.getMeasuredWidth() + 240;

                        if (totalWidth > parentWidth) {
                            mainLegendLayout.addView(legendLayout);
                            legendLayout = new LinearLayout(context);
                            legendLayout.setGravity(Gravity.LEFT);
                            legendLayout.setOrientation(LinearLayout.HORIZONTAL);
                        }

                        legendLayout.addView(legendItem);
                    }

                    if (legendLayout.getChildCount() > 0) {
                        mainLegendLayout.addView(legendLayout);
                    }

                    scrollView.addView(mainLegendLayout);
                    layout.addView(scrollView);
                }
            });
        } catch (Exception e) {
            LogHandler.crashLog(e, "createStackedBarChart");
            layout.removeAllViews();
            layout.addView(Details.getErrorAsChartAlternative(context));
        }
        return stackedBarChart;
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
