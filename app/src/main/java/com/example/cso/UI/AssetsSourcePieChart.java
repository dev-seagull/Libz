package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.cso.LogHandler;
import com.example.cso.MainActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
            Collections.sort(entries, new Comparator<PieEntry>() {
                @Override
                public int compare(PieEntry e1, PieEntry e2) {
                    return Float.compare(e2.getValue(), e1.getValue());
                }
            });

            float othersValue = 0;
            ArrayList<PieEntry> limitedEntries = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                if (i < 4) {
                    limitedEntries.add(entries.get(i));
                } else {
                    othersValue += entries.get(i).getValue();
                }
            }

            if (othersValue > 0) {
                limitedEntries.add(new PieEntry(othersValue, "Others"));
            }
            entries = limitedEntries;
        }

        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = MainActivity.currentTheme.deviceAppStorageChartColors;
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        dataSet.setValueFormatter(new PieChartValueFormatter());

        dataSet.setDrawValues(true);
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
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(false);
    }

    public static void configurePieChartDimensions(PieChart pieChart, Context context) {
        int width =(int) (UI.getDeviceWidth(context) * 0.35);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width);
        params.setMargins(0,10,0,10);
        pieChart.setLayoutParams(params);
    }

    public static void configurePieChartLegend(PieChart pieChart) {
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);
    }

}
