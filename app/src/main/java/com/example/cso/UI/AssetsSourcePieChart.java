package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
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
    public static View createPieChartForDeviceSourceStatus(Context context, JsonObject data) {
        try{
            PieChart pieChart = new PieChart(context);

            configurePieChartDimensions(pieChart, context);
            if (data != null && data.size() > 0){
                configurePieChartDataForDeviceSourceStatus(pieChart, data);
                configurePieChartLegend(pieChart);
            }else{
                return Details.getErrorAsChartAlternative(context);
            }
            pieChart.invalidate();
            return pieChart;
        }catch (Exception e){
            LogHandler.crashLog(e,"AssetsSourceChart");
            return Details.getErrorAsChartAlternative(context);
        }
    }

    public static void configurePieChartDataForDeviceSourceStatus(PieChart pieChart, JsonObject sourcesData) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (String source : sourcesData.keySet()){
            double size = sourcesData.get(source).getAsDouble();
            entries.add(new PieEntry((float) size, source));
        }
        configurePieChartDataFormatForDeviceSourceStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceSourceStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        if (entries.size() > 5) {
            Collections.sort(
                    entries, (e1, e2) -> Float.compare(e2.getValue(), e1.getValue())
            );
        }

        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = MainActivity.currentTheme.deviceAppStorageChartColors;
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        dataSet.setValueFormatter(new PieChartValueFormatter());

        dataSet.setDrawValues(false);
        dataSet.setValueLinePart1OffsetPercentage(200f);
        dataSet.setValueLinePart1Length(0.5f);
        dataSet.setValueLinePart2Length(0.8f);
        dataSet.setValueLineWidth(2f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(100f);
        dataSet.setValueLineVariableLength(true);


        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setDrawHoleEnabled(false);
    }

    public static void configurePieChartDimensions(PieChart pieChart, Context context) {
        int width =(int) (UI.getDeviceWidth(context) * 0.35);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, width);
        params.setMargins(0,10,0,10);
        pieChart.setLayoutParams(params);
    }

    public static void configurePieChartLegend(PieChart pieChart) {
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);
    }

    public static void createTextAreaForAssetSourcePieChart(View chartLayout, LinearLayout layout, Context context){
        if(chartLayout instanceof PieChart){
            PieChart pieChart = (PieChart) chartLayout;
            PieData chartData = pieChart.getData();
            PieDataSet dataSet = (PieDataSet) chartData.getDataSet();
            List<PieEntry> entries = dataSet.getValues();
            int[] colors = MainActivity.currentTheme.deviceAppStorageChartColors;

            SpannableStringBuilder coloredText = new SpannableStringBuilder();

            for (int i = 0; i < entries.size(); i++) {
                PieEntry entry = entries.get(i);
                String label = entry.getLabel();
                String value = String.valueOf(entry.getValue());
                if (Double.valueOf(value) >= 100){
                    value =  String.format("%.1f GB", Double.valueOf(value) / 1024);
                }else{
                    value =  String.format("%.1f MB", Double.valueOf(value));
                }

                int colorIndex = i % colors.length;
                SpannableString entryText = new SpannableString(label + ": " + value + "\n");
                entryText.setSpan(new ForegroundColorSpan(colors[colorIndex]), 0, entryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                coloredText.append(entryText);
            }

            TextView statusTextView = new TextView(context);
            statusTextView.setText(coloredText);
            statusTextView.setTextSize(14);
            statusTextView.setPadding(32, 0, 0, 0);
            layout.addView(statusTextView);
        }
    }

}
