package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
//    public static View createPieChartForDeviceSourceStatus(Context context, JsonObject data) {
//        try{
//            PieChart pieChart = new PieChart(context);
//
//            configurePieChartDimensions(pieChart, context);
//            if (data != null && data.size() > 0){
//                configurePieChartDataForDeviceSourceStatus(pieChart, data);
//                configurePieChartLegend(pieChart);
//            }else{
//                return Details.getErrorAsChartAlternative(context);
//            }
//            pieChart.invalidate();
//            return pieChart;
//        }catch (Exception e){
//            LogHandler.crashLog(e,"AssetsSourceChart");
//            return Details.getErrorAsChartAlternative(context);
//        }
//    }
//
//    public static void configurePieChartDataForDeviceSourceStatus(PieChart pieChart, JsonObject sourcesData) {
//        ArrayList<PieEntry> entries = new ArrayList<>();
//        for (String source : sourcesData.keySet()){
//            double size = sourcesData.get(source).getAsDouble();
//            entries.add(new PieEntry((float) size, source));
//        }
//        configurePieChartDataFormatForDeviceSourceStatus(pieChart, entries);
//    }
//
//    public static void configurePieChartDataFormatForDeviceSourceStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
//        Collections.sort(entries, (e1, e2) -> Float.compare(e2.getValue(), e1.getValue()));
//
//        PieDataSet dataSet = new PieDataSet(entries, null);
//
//        int[] colors = MainActivity.currentTheme.deviceAppStorageChartColors;
//        dataSet.setColors(colors);
//        dataSet.setValueTextColor(Color.WHITE);
//        dataSet.setValueTextSize(10f);
//
//        dataSet.setValueFormatter(new PieChartValueFormatter());
//
//        dataSet.setDrawValues(false);
//        dataSet.setValueLinePart1OffsetPercentage(200f);
//        dataSet.setValueLinePart1Length(0.5f);
//        dataSet.setValueLinePart2Length(0.8f);
//        dataSet.setValueLineWidth(2f);
//        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
//        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
//        dataSet.setValueLinePart1OffsetPercentage(100f);
//        dataSet.setValueLineVariableLength(true);
//
//
//        PieData data = new PieData(dataSet);
//        pieChart.setData(data);
//        pieChart.getDescription().setEnabled(false);
//        pieChart.setDrawEntryLabels(false);
//        pieChart.setDrawHoleEnabled(false);
//    }
//
//    public static void configurePieChartDimensions(PieChart pieChart, Context context) {
//        int width =(int) (UI.getDeviceWidth(context) * 0.35);
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, width);
//        params.setMargins(0,10,0,10);
//        pieChart.setLayoutParams(params);
//    }
//
//    public static void configurePieChartLegend(PieChart pieChart) {
//        Legend legend = pieChart.getLegend();
//        legend.setEnabled(false);
//    }
//
//    public static void createTextAreaForAssetSourcePieChart(View chartLayout, LinearLayout layout, Context context){
//        if(chartLayout instanceof PieChart){
//            PieChart pieChart = (PieChart) chartLayout;
//            PieData chartData = pieChart.getData();
//            PieDataSet dataSet = (PieDataSet) chartData.getDataSet();
//            List<PieEntry> entries = dataSet.getValues();
//            int[] colors = MainActivity.currentTheme.deviceAppStorageChartColors;
//
//            SpannableStringBuilder coloredText = new SpannableStringBuilder();
//
//            for (int i = 0; i < entries.size(); i++) {
//                PieEntry entry = entries.get(i);
//                String label = entry.getLabel();
//                String value = String.valueOf(entry.getValue());
//                if (Double.valueOf(value) >= 100){
//                    value =  String.format("%.1f GB", Double.valueOf(value) / 1024);
//                }else{
//                    value =  String.format("%.1f MB", Double.valueOf(value));
//                }
//
//                int colorIndex = i % colors.length;
//                SpannableString entryText = new SpannableString(label + ": " + value + "\n");
//                entryText.setSpan(new ForegroundColorSpan(colors[colorIndex]), 0, entryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                coloredText.append(entryText);
//            }
//
//            TextView statusTextView = new TextView(context);
//            statusTextView.setText(coloredText);
//            statusTextView.setTextSize(10);
//            statusTextView.setPadding(32, 0, 0, 0);
//            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//            );
//            layoutParams.gravity = Gravity.CENTER_VERTICAL;
//            layoutParams.setMargins(0,10,0,10);
//            statusTextView.setLayoutParams(layoutParams);
//
//            layout.addView(statusTextView);
//        }
//    }
    public static View createStackedBarChartForDeviceSourceStatus(Context context, JsonObject data) {
        try {
            HorizontalBarChart barChart = new HorizontalBarChart(context);

            configureBarChartDimensions(barChart, context);
            if (data != null && data.size() > 0) {
                configureStackedBarChartDataForDeviceSourceStatus(barChart, data);
                Legend legend = barChart.getLegend();
                legend.setEnabled(false);
            } else {
                return Details.getErrorAsChartAlternative(context);
            }
            barChart.invalidate();
            return barChart;
        } catch (Exception e) {
            LogHandler.crashLog(e, "AssetsSourceChart");
            return Details.getErrorAsChartAlternative(context);
        }
}

    public static void configureStackedBarChartDataForDeviceSourceStatus(HorizontalBarChart barChart, JsonObject sourcesData) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (String source : sourcesData.keySet()) {
            double size = sourcesData.get(source).getAsDouble();
            if(size != 0.0){
                values.add(size);
                labels.add(source);
            }
        }
        float[] stackedValues = new float[values.size() + 1];
        for (int i = 0; i < values.size(); i++) {
            stackedValues[i] = values.get(i).floatValue();
        }
        entries.add(new BarEntry(0f, stackedValues));

        configureBarChartDataFormatForDeviceSourceStatus(barChart, entries, labels, values);
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

    public static void configureBarChartDimensions(HorizontalBarChart barChart, Context context) {
        int width = (int) (UI.getDeviceWidth(context) * 0.8);
        int height = (int) (UI.getDeviceWidth(context) * 0.15);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(0, 0, 0, 0);
        barChart.setLayoutParams(params);
    }


    public static void createTextAreaForAssetSourceBarChart(View chartLayout, LinearLayout layout, Context context, JsonObject sourcesData) {
        if (chartLayout instanceof HorizontalBarChart) {
            int[] colors = MainActivity.currentTheme.deviceAppStorageChartColors;

            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();

            for (String source : sourcesData.keySet()) {
                double size = sourcesData.get(source).getAsDouble();
                if(size != 0.0){
                    values.add(size);
                    labels.add(source);
                }
            }

            LinearLayout legendLayout = new LinearLayout(context);
            legendLayout.setOrientation(LinearLayout.VERTICAL);
            legendLayout.setGravity(Gravity.CENTER);
            legendLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            for (int i = 0; i < labels.size(); i++) {
                int color = colors[i % colors.length];
                legendLayout.addView(createLegendItemForAssetSourceBarChart(context, labels.get(i), color, values.get(i)));
            }
            layout.addView(legendLayout);

//            SpannableStringBuilder coloredText = new SpannableStringBuilder();
//
//            for (int i = 0; i < entries.size(); i++) {
//                BarEntry entry = entries.get(i);
//                String label = dataSet.getStackLabels()[i];
//                String value = String.valueOf(entry.getY());
//                if (Double.valueOf(value) >= 100) {
//                    value = String.format("%.1f GB", Double.valueOf(value) / 1024);
//                } else {
//                    value = String.format("%.1f MB", Double.valueOf(value));
//                }
//
//                int colorIndex = i % colors.length;
//                SpannableString entryText = new SpannableString(label + ": " + value + "\n");
//                entryText.setSpan(new ForegroundColorSpan(colors[colorIndex]), 0, entryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                coloredText.append(entryText);
//            }
//
//            TextView statusTextView = new TextView(context);
//            statusTextView.setText(coloredText);
//            statusTextView.setTextSize(10);
//            statusTextView.setPadding(32, 0, 0, 0);
//            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//            );
//            layoutParams.gravity = Gravity.CENTER;
//            layoutParams.setMargins(0, 10, 0, 10);
//            statusTextView.setLayoutParams(layoutParams);

//            layout.addView(legendItem);
        }
    }

    public static LinearLayout createLegendItemForAssetSourceBarChart(Context context, String label, int color, double value){
        LinearLayout legendItem = new LinearLayout(context);
        legendItem.setOrientation(LinearLayout.HORIZONTAL);
        legendItem.setGravity(Gravity.CENTER);

        View colorBox = new View(context);
        LinearLayout.LayoutParams colorBoxParams = new LinearLayout.LayoutParams((int) (UI.getDeviceWidth(context) * 0.02), (int) (UI.getDeviceWidth(context) * 0.02));
        colorBoxParams.setMargins(0, 0, 10, 0);
        colorBox.setLayoutParams(colorBoxParams);
        colorBox.setBackgroundColor(color);

        TextView labelText = new TextView(context);
        labelText.setText(label+" : ");
        labelText.setTextSize((int) (UI.getDeviceWidth(context) * 0.009));
        labelText.setTextColor(MainActivity.currentTheme.primaryTextColor);

        TextView valueText = new TextView(context);
        valueText.setText(new PieChartValueFormatter().getFormattedValue((float) value));
        valueText.setTextSize((int) (UI.getDeviceWidth(context) * 0.009));
        valueText.setTextColor(MainActivity.currentTheme.primaryTextColor);

        legendItem.addView(colorBox);
        legendItem.addView(labelText);
        legendItem.addView(valueText);

        return legendItem;
    }

}
