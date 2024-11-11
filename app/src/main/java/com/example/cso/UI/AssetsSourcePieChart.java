package com.example.cso.UI;

import static com.example.cso.MainActivity.activity;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.cso.DeviceStatusSync;
import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.gson.JsonObject;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AssetsSourcePieChart {

    public static void createStackedBarChartForDeviceSourceStatus(Context context,LinearLayout layout, JsonObject data) {
        try {
            LinearLayout tempLayout = new LinearLayout(context);
            tempLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tempLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tempLayoutParams.gravity = Gravity.CENTER;
            tempLayout.setLayoutParams(tempLayoutParams);

            if (data != null && data.size() > 0) {
                HorizontalBarChart barChart = new HorizontalBarChart(context);
                ArrayList<BarEntry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                List<Double> values = new ArrayList<>();

                List<Pair<String, Double>> sourceValuePairs = new ArrayList<>();
                for (String source : data.keySet()) {
                    double size = data.get(source).getAsDouble();
                    if (size != 0.0) {
                        sourceValuePairs.add(new Pair<>(source, size));
                    }
                }
                sourceValuePairs.sort((p1, p2) -> Double.compare(p2.second, p1.second));

                for (Pair<String, Double> pair : sourceValuePairs) {
                    values.add(pair.second);
                    labels.add(pair.first);
                }

                double syncedMaxValue = 0;
                for(int i = 0; i < values.size(); i++){
                    if(values.get(i) > syncedMaxValue){
                        syncedMaxValue  = values.get(i);
                    }
                }

                for(int i = 0; i < values.size(); i++){
                    if(values.get(i) / syncedMaxValue < 0.01){
                        values.set(i, syncedMaxValue / 100);
                    }
                }
                float[] stackedValues = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    stackedValues[i] = values.get(i).floatValue();
                }
                entries.add(new BarEntry(0f, stackedValues));

                configureBarChartDataFormatForDeviceSourceStatus(barChart, entries, labels, values);

                Legend legend = barChart.getLegend();
                legend.setEnabled(false);

                tempLayout.addView(barChart);
                tempLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        int parentWidth = (int) (UI.getDeviceWidth(context) * 0.85);
                        LinearLayout.LayoutParams barChartParams = new LinearLayout.LayoutParams(
                                parentWidth,
                                230
                        );
                        barChartParams.gravity = Gravity.CENTER;
                        barChartParams.setMargins(20, 42,0,0);
                        barChart.setLayoutParams(barChartParams);
                        barChart.invalidate();

                        ScrollView scrollView = new ScrollView(context);
                        Log.d("debug","p: " + UI.getDeviceWidth(context));
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

                        int[] colors = MainActivity.currentTheme.deviceAppStorageChartColors;
                        for (int i = 0; i < labels.size(); i++) {
//                            for(int j = 0 ; j < 10 ; j++){
                                int color = colors[i % colors.length];
                                View legendItem = CustomTreeMapChart.createLegendItem
                                        (context, labels.get(i), color, values.get(i));

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
//                            }
                        }
                        if (legendLayout.getChildCount() > 0) {
                            mainLegendLayout.addView(legendLayout);
                        }
                        scrollView.addView(mainLegendLayout);
                        tempLayout.addView(scrollView);
                    }
                });

            } else {
                View view = Details.getErrorAsChartAlternative(context);
                tempLayout.addView(view);
            }
            layout.addView(tempLayout);
        } catch (Exception e) {
            LogHandler.crashLog(e, "AssetsSourceChart");
            View view = Details.getErrorAsChartAlternative(context);
            layout.addView(view);
        }
}

    public static void configureBarChartDataFormatForDeviceSourceStatus(HorizontalBarChart barChart, ArrayList<BarEntry> entries, List<String> labels, List<Double> values) {
        BarDataSet dataSet = new BarDataSet(entries, null);

        int[] colors = MainActivity.currentTheme.deviceAppStorageChartColors;
        dataSet.setColors(colors);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawLabels(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setEnabled(false);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(false);
        dataSet.setDrawValues(false);
    }

}
