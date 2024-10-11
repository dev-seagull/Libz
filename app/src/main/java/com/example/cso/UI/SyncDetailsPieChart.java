package com.example.cso.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cso.DBHelper;
import com.example.cso.MainActivity;
import com.example.cso.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

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
            entries.add(new PieEntry((int) unsyncedMediaSize, "UnSynced"));
        }
        configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(pieChart, entries);
    }

    public static void configurePieChartDataFormatForDeviceSyncedAssetsLocationStatus(PieChart pieChart, ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);

        int[] colors = MainActivity.currentTheme.syncDetailsPieChartColors;
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setDrawValues(false);
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
        pieChart.setDrawEntryLabels(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setDrawHoleEnabled(false);
    }

    public static void configurePieChartDimensions(PieChart pieChart, Context context) {
        int width = (int) (UI.getDeviceWidth(context) * 0.35);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, width);
        params.setMargins(0,10,0,10);
        pieChart.setLayoutParams(params);
    }

    public static void configurePieChartLegend(PieChart pieChart) {
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);
    }

    public static void createTextAreaForSyncDetailsPieChart(View chartLayout, LinearLayout layout, Context context){
        if(chartLayout instanceof PieChart){
            PieChart pieChart = (PieChart) chartLayout;
            PieData chartData = pieChart.getData();
            PieDataSet dataSet = (PieDataSet) chartData.getDataSet();
            List<PieEntry> entries = dataSet.getValues();
            int[] colors = MainActivity.currentTheme.syncDetailsPieChartColors;

            SpannableStringBuilder coloredText = new SpannableStringBuilder();

            for (int i = 0; i < entries.size(); i++) {
                PieEntry entry = entries.get(i);
                String label = entry.getLabel();
                int value = (int) entry.getValue();

                int colorIndex = i % colors.length;
                SpannableString entryText = new SpannableString(label + " assets count : " + value + "\n\n");
                entryText.setSpan(new ForegroundColorSpan(colors[colorIndex]), 0, entryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                coloredText.append(entryText);
            }

            TextView statusTextView = new TextView(context);
            statusTextView.setText(coloredText);
            statusTextView.setTextSize(10);
//            statusTextView.setPadding(32, 0, 0, 0);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            layoutParams.setMargins(0,10,0,10);
            statusTextView.setLayoutParams(layoutParams);

            layout.addView(statusTextView);
        }
    }

    public static void addPieChartToSyncDetailsStatisticsLayout(PieChart pieChart, Activity activity,
                                                                LinearLayout syncDetailsStatisticsLayout){
        new Thread(() -> {
            ImageView[] loadingImage = new ImageView[]{new ImageView(activity)};
            MainActivity.activity.runOnUiThread(() -> {
                loadingImage[0].setBackgroundResource(R.drawable.yellow_loading);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(128,128);
                params.setMargins(0,64,0,64);
                loadingImage[0].setLayoutParams(params);
                syncDetailsStatisticsLayout.addView(loadingImage[0]);
            });
            View pieChartView = SyncDetailsPieChart.createPieChartView(pieChart,activity);
            activity.runOnUiThread(() -> {
                syncDetailsStatisticsLayout.addView(pieChartView);

                if(pieChartView instanceof PieChart){
                    SyncDetailsPieChart.createTextAreaForSyncDetailsPieChart(pieChartView,syncDetailsStatisticsLayout,activity);
                }

                syncDetailsStatisticsLayout.removeView(loadingImage[0]);
            });
        }).start();
    }

}
