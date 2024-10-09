package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.cso.DBHelper;
import com.example.cso.MainActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;

public class SyncDetailsPieChart {

    public static View createPieChartView(PieChart pieChart, Activity activity){
        int totalAssets = DBHelper.getNumberOfAssets();
        int syncedAssets = DBHelper.getNumberOfSyncedAssets();
        SyncDetailsPieChart.configurePieChartDimensions(pieChart, activity);
        SyncDetailsPieChart.configurePieChartDataForSyncDetails(pieChart,syncedAssets,totalAssets - syncedAssets);
        SyncDetailsPieChart.configurePieChartLegend(pieChart);
        pieChart.invalidate();

        return pieChart;
    }

    public static void configurePieChartDataForSyncDetails(PieChart pieChart,double synced,double unsyncedMediaSize) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (synced != 0){
            entries.add(new PieEntry((int) synced, "Synced"));
        }
        if (unsyncedMediaSize != 0){
            entries.add(new PieEntry((int) unsyncedMediaSize, "unsynced"));
        }
        configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = MainActivity.currentTheme.deviceAssetsSyncedStatusChartColors;
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setDrawValues(true);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 100) {
                    return String.format("%.2f GB",value / 1024);
                }
                return String.format("%.2f MB", value);
            }
        });

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setDrawHoleEnabled(true);
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
