package com.example.cso.UI;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.cso.MainActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;

public class SyncDetailsPieChart {

    public static View createPieChartView(Context context){
        PieChart pieChart = new PieChart(context);
        configurePieChartDimensions(pieChart, context);
        double libzFolderSize = SyncDetails.getTotalLibzFolderSizes();
        double unsyncedMediaSize = SyncDetails.getTotalUnsyncedAssetsOfDevices();
        configurePieChartDataForSyncDetails(pieChart,libzFolderSize,unsyncedMediaSize);
        configurePieChartLegend(pieChart);
        pieChart.invalidate();
        return pieChart;
    }

    public static void configurePieChartDataForSyncDetails(PieChart pieChart,double libzFolderSize,double unsyncedMediaSize) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) libzFolderSize, "Synced"));
        entries.add(new PieEntry((float) unsyncedMediaSize, "unSynced"));
        configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = MainActivity.currentTheme.deviceAssetsSyncedStatusChartColors;
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setValueFormatter(new PieChartValueFormatter());

        // Enable value lines and set value positions
        dataSet.setDrawValues(true);
        dataSet.setValueLinePart1OffsetPercentage(80f); // Offset of the line
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueLineWidth(2f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);


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
